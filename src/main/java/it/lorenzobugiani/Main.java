package it.lorenzobugiani;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;

import it.lorenzobugiani.application.Application;
import it.lorenzobugiani.application.LogWriter;
import it.lorenzobugiani.application.Reporter;
import it.lorenzobugiani.client.SocketListener;

public class Main {

    private static final Logger log = LogManager.getLogger(Main.class);

    private static final int PORT = 4000;
    private static final int MAX_CLIENT_ALLOWED = 5;

    private static final int QUEUE_CAPACITY = 64 * 1024;

    private static final Duration REPORT_INTERVAL = Duration.ofSeconds(10);

    private static final String OUTPUT_FILE_NAME = "./numbers.log";
    private static final Duration OUTPUT_FILE_FLUSH_INTERVAL = Duration.ofSeconds(10);

    public static void main(String[] args) {

        var queue = new ArrayBlockingQueue<int[]>(QUEUE_CAPACITY);
        var outputFile = new File(OUTPUT_FILE_NAME);

        try {
            outputFile.delete();
            outputFile.createNewFile();
        } catch (IOException e) {
            log.error("Error creating log file", e);
            System.exit(255);
        }

        var applicationThread = new Thread(() -> {
            try (
                    var writer = new LogWriter(outputFile, OUTPUT_FILE_FLUSH_INTERVAL);
                    var reporter = new Reporter(new PrintWriter(System.out), REPORT_INTERVAL)
            ) {
                new Application(queue, writer, reporter).run();
            } catch (IOException e) {
                log.error(e);
            }
        });
        applicationThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(applicationThread::interrupt));

        var socketListener = new SocketListener(PORT, queue, MAX_CLIENT_ALLOWED);
        try {
            // block until terminate
            socketListener.run();
        } catch (IOException | InterruptedException e) {
            log.error(e);
        } finally {
            applicationThread.interrupt();
        }
    }
}
