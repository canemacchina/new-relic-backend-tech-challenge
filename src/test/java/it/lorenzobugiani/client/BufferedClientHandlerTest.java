package it.lorenzobugiani.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class BufferedClientHandlerTest {

    @Test
    @DisplayName("Read numbers from socket")
    public void readNumbersFromSocket() throws IOException {
        var numbers = 100000;
        var queue = new ArrayBlockingQueue<int[]>(1000);
        int port = 4000;
        var clientHandler = new BufferedClientHandler(queue, 1000, 2000);

        try (var serverSocket = new ServerSocket(port)) {
            var socketFuture = acceptConnection(serverSocket);
            var clientFuture = launchClientHandlerThread(clientHandler, socketFuture);

            try (
                    var socket = new Socket("localhost", port);
                    var writer = socket.getOutputStream()
            ) {
                for (int value = 0; value < numbers; value++) {
                    write(writer, value);
                }
                write(writer, -1);
                await().until(clientFuture::isDone);
            }
        }

        var expected = IntStream.range(0, numbers).boxed().toList();
        verifyElementsInQueue(queue, expected);
    }

    @Test
    @DisplayName("Flush buffer when the timeout occurs")
    public void flushAfter5s() throws IOException {
        var numbers = 100;
        var bufferSize = numbers * 2;
        var queue = new ArrayBlockingQueue<int[]>(1000);
        int port = 4000;
        int flushTimeout = 2000;
        var clientHandler = new BufferedClientHandler(queue, bufferSize, flushTimeout);

        try (var serverSocket = new ServerSocket(port)) {
            var socketFuture = acceptConnection(serverSocket);
            var clientFuture = launchClientHandlerThread(clientHandler, socketFuture);

            try (
                    var socket = new Socket("localhost", port);
                    var writer = socket.getOutputStream()
            ) {
                for (int value = 0; value < numbers; value++) {
                    write(writer, value);
                }
                await().atMost(flushTimeout + 500, MILLISECONDS).until(() -> !queue.isEmpty());

                write(writer, -1);
                await().until(clientFuture::isDone);
            }
        }

        var expected = IntStream.range(0, numbers).boxed().toList();
        verifyElementsInQueue(queue, expected);
    }

    @Test
    @DisplayName("do not flush buffer if is empty")
    public void doNotFlushEmptyBuffer() throws IOException, InterruptedException {
        var queue = new ArrayBlockingQueue<int[]>(1000);
        int port = 4000;
        int flushTimeout = 2000;
        var clientHandler = new BufferedClientHandler(queue, 1000, flushTimeout);

        try (var serverSocket = new ServerSocket(port)) {
            var socketFuture = acceptConnection(serverSocket);
            var clientFuture = launchClientHandlerThread(clientHandler, socketFuture);

            try (
                    var socket = new Socket("localhost", port);
                    var writer = socket.getOutputStream()
            ) {
                Thread.sleep(flushTimeout + 500);
                write(writer, -1);
                await().until(clientFuture::isDone);
            }
        }
        assertThat(queue).isEmpty();
    }

    private CompletableFuture<Socket> acceptConnection(ServerSocket serverSocket) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return serverSocket.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private CompletableFuture<?> launchClientHandlerThread(
            BufferedClientHandler bufferedClientHandler,
            CompletableFuture<Socket> socketFuture) {
        return CompletableFuture.runAsync(() -> {
            try (var socket = socketFuture.get()) {
                bufferedClientHandler.run(socket);
            } catch (IOException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void write(OutputStream writer, int value) throws IOException {
        writer.write(String.valueOf(value).getBytes(UTF_8));
        writer.write(System.lineSeparator().getBytes(UTF_8));
        writer.flush();
    }

    private void verifyElementsInQueue(ArrayBlockingQueue<int[]> queue, List<Integer> expected) {
        var flattenedQueue = queue.stream()
                .flatMap(ints -> Arrays.stream(ints).boxed())
                .toList();

        assertThat(flattenedQueue).containsExactlyElementsOf(expected);
    }
}
