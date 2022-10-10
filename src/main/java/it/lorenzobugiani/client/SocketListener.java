package it.lorenzobugiani.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static it.lorenzobugiani.ExecutorUtils.shutdown;
import static it.lorenzobugiani.client.ExitStatus.TERMINATION;

public class SocketListener {

    private static final Logger log = LogManager.getLogger(SocketListener.class);

    private static final int BUFFER_SIZE = 1024;
    private static final int FLUSH_TIMEOUT = 5000;

    private final int port;
    private final BlockingQueue<int[]> queue;
    private final Semaphore semaphore;
    private final ExecutorService executor;

    public SocketListener(int port, BlockingQueue<int[]> queue, int maxAllowedClient) {
        this.port = port;
        this.queue = queue;

        this.semaphore = new Semaphore(maxAllowedClient);
        this.executor = Executors.newFixedThreadPool(maxAllowedClient);
    }

    public void run() throws IOException, InterruptedException {
        var serverSocket = new ServerSocket(port);
        try {
            log.info("Accepting connections");

            while (true) {
                semaphore.acquire();
                var socket = serverSocket.accept();

                executor.submit(() -> {
                    try {
                        BufferedClientHandler clientHandler =
                                new BufferedClientHandler(queue, BUFFER_SIZE, FLUSH_TIMEOUT);
                        var exitStatus = clientHandler.run(socket);

                        if (exitStatus == TERMINATION) {
                            shutdown(executor);
                            serverSocket.close();
                        }
                    } catch (IOException e) {
                        log.error("Error running a client handler", e);
                    } finally {
                        semaphore.release();
                        try {
                            socket.close();
                        } catch (IOException e) {
                            log.error(e);
                        }
                    }
                });
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                log.error(e);
            }
        }
    }
}
