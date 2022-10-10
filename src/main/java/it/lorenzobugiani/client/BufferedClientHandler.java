package it.lorenzobugiani.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import static it.lorenzobugiani.client.ExitStatus.INTERRUPTED;
import static it.lorenzobugiani.client.ExitStatus.INVALID_INPUT;
import static it.lorenzobugiani.client.ExitStatus.SOCKET_CLOSED;
import static it.lorenzobugiani.client.ExitStatus.TERMINATION;

public class BufferedClientHandler {

    private static final Logger log = LogManager.getLogger(BufferedClientHandler.class);

    private static final String TERMINATION_COMMAND = "terminate";

    private final BlockingQueue<int[]> queue;
    private final int flushTimeout;
    private final Buffer buffer;

    public BufferedClientHandler(BlockingQueue<int[]> queue, int bufferSize, int flushTimeout) {
        this.queue = queue;
        this.flushTimeout = flushTimeout;

        this.buffer = new Buffer(bufferSize);
    }

    public ExitStatus run(Socket socket) throws IOException {
        socket.setSoTimeout(flushTimeout);

        ExitStatus exitStatus = null;

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            exitStatus = run(reader);

        } catch (InterruptedException e) {
            exitStatus = INTERRUPTED;
        } finally {
            // flush buffer if the queue has space or loses data
            // instead of waiting for space to avoid blocking termination
            offerBuffer();

            if (exitStatus == INTERRUPTED) {
                Thread.currentThread().interrupt();
            }
        }
        return exitStatus;
    }

    private ExitStatus run(BufferedReader reader) throws IOException, InterruptedException {
        while (!Thread.interrupted()) {
            try {
                //TODO: check input length (must be 9 number)??
                String line = reader.readLine();

                if (line == null) {
                    return SOCKET_CLOSED;
                }

                if (line.equals(TERMINATION_COMMAND)) {
                    return TERMINATION;
                }

                int i;
                try {
                    i = Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    return INVALID_INPUT;
                }

                if (i < 0) {
                    return INVALID_INPUT;
                }

                buffer.put(i);
                if (buffer.isFull()) {
                    flushBuffer();
                }

            } catch (SocketTimeoutException ex) {
                flushBuffer();
            }
        }

        // Exited due to thread interruption
        return INTERRUPTED;
    }

    private void flushBuffer() throws InterruptedException {
        if (buffer.isEmpty()) {
            return;
        }

        queue.put(buffer.flush());
    }

    private void offerBuffer() {
        if (buffer.isEmpty()) {
            return;
        }

        var data = buffer.flush();

        if (!queue.offer(data)) {
            log.warn(
                    "No space left into the queue on client shutdown. Some data will be lost: {}",
                    Arrays.toString(data)
            );
        }
    }
}
