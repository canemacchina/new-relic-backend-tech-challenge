package it.lorenzobugiani.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.awaitility.Awaitility.await;

class SocketListenerTest {

    @Test
    @DisplayName("On terminate command stop and do not accept more connections")
    public void stop() throws InterruptedException, IOException {
        var queue = new ArrayBlockingQueue<int[]>(1000);
        int port = 4000;
        int maxAllowedClients = 2;

        var future = CompletableFuture.runAsync(() -> {
            var socketListener = new SocketListener(port, queue, maxAllowedClients);

            try {
                socketListener.run();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        Thread.sleep(500);

        sendTermination(port);

        await().until(future::isDone);

        assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> new Socket("localhost", port));
    }

    @Test
    @DisplayName("On not valid input dont' stop and accept more connections")
    public void notValidInput() throws InterruptedException, IOException {
        var queue = new ArrayBlockingQueue<int[]>(1000);
        int port = 4000;
        int maxAllowedClients = 2;

        var future = CompletableFuture.runAsync(() -> {
            var socketListener = new SocketListener(port, queue, maxAllowedClients);

            try {
                socketListener.run();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        Thread.sleep(500);

        try (
                var socket = new Socket("localhost", port);
                var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            writer.write("not valid number" + System.lineSeparator());

            assertThatNoException()
                    .isThrownBy(() -> {
                        Socket socket2 = new Socket("localhost", port);
                        socket2.close();
                    });

        }

        sendTermination(port);

        await().until(future::isDone);
    }

    private static void sendTermination(int port) throws IOException {
        try (
                var socket = new Socket("localhost", port);
                var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            writer.write("terminate" + System.lineSeparator());
        }
    }

}