package it.lorenzobugiani.application;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static it.lorenzobugiani.ExecutorUtils.shutdown;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class Reporter implements Closeable {

    private final PrintWriter printWriter;
    private long uniqueNumbersFromLastReport;
    private long duplicatesFromLastReport;
    private long uniqueNumbersTotal;
    private final Lock lock;
    private final ScheduledExecutorService executor;

    public Reporter(PrintWriter printWriter, Duration reportInterval) {
        this.printWriter = printWriter;
        this.uniqueNumbersFromLastReport = 0;
        this.duplicatesFromLastReport = 0;
        this.uniqueNumbersTotal = 0;
        this.lock = new ReentrantLock();
        this.executor = Executors.newScheduledThreadPool(1);

        executor.scheduleAtFixedRate(
                this::printReport,
                reportInterval.toMillis(),
                reportInterval.toMillis(),
                MILLISECONDS
        );
    }

    public void updateReportingData(
            long uniqueNumbersFromLastReport,
            long duplicatesFromLastReport
    ) {
        lock.lock();
        this.uniqueNumbersFromLastReport += uniqueNumbersFromLastReport;
        this.duplicatesFromLastReport += duplicatesFromLastReport;
        this.uniqueNumbersTotal += uniqueNumbersFromLastReport;
        lock.unlock();
    }

    private void printReport() {
        lock.lock();
        final var duplicatesFromLastReport = this.duplicatesFromLastReport;
        final var uniqueNumbersFromLastReport = this.uniqueNumbersFromLastReport;
        final var uniqueNumbersTotal = this.uniqueNumbersTotal;
        this.duplicatesFromLastReport = 0;
        this.uniqueNumbersFromLastReport = 0;
        lock.unlock();

        printWriter.printf(
                "Received %s unique numbers, %s duplicates. Unique total: %s%n",
                uniqueNumbersFromLastReport,
                duplicatesFromLastReport,
                uniqueNumbersTotal
        );
        printWriter.flush();
    }

    @Override
    public void close() throws IOException {
        shutdown(executor);
    }
}
