package it.lorenzobugiani.application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static it.lorenzobugiani.ExecutorUtils.shutdown;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class LogWriter implements Closeable {

    private static final Logger log = LogManager.getLogger(LogWriter.class);

    private final BufferedWriter writer;
    private final ScheduledExecutorService executor;

    public LogWriter(File outputFile, Duration flushInterval) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(outputFile));
        this.executor = Executors.newScheduledThreadPool(1);

        executor.scheduleAtFixedRate(() -> {
            try {
                writer.flush();
            } catch (IOException e) {
                log.error("Error flushing file", e);
            }
        }, flushInterval.toMillis(), flushInterval.toMillis(), MILLISECONDS);
    }

    public void write(int number) throws IOException {
        writer.write(String.valueOf(number));
        writer.write(System.lineSeparator());
    }

    @Override
    public void close() throws IOException {
        shutdown(executor);
        this.writer.flush();
        this.writer.close();
    }
}
