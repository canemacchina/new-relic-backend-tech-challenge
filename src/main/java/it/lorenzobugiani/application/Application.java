package it.lorenzobugiani.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class Application {

    private static final int BATCH_SIZE = 1024;

    private final BlockingQueue<int[]> blockingQueue;
    private final LogWriter logWriter;
    private final Reporter reporter;

    private final BitSet bitSet;
    private long uniqueNumbersFromLastReport;
    private long duplicatesFromLastReport;

    public Application(BlockingQueue<int[]> blockingQueue, LogWriter logWriter, Reporter reporter) {
        this.blockingQueue = blockingQueue;
        this.logWriter = logWriter;
        this.reporter = reporter;

        bitSet = new BitSet(10 ^ 9);
        uniqueNumbersFromLastReport = 0;
        duplicatesFromLastReport = 0;
    }

    public void run() throws IOException {

        try {
            // Avoid creating an object on every iteration
            final List<int[]> batch = new ArrayList<>(BATCH_SIZE);
            while (!Thread.interrupted()) {
                readFromQueue(batch);

                for (final int[] items : batch) {
                    for (final int item : items) {
                        if (alreadySeen(item)) {
                            duplicatesFromLastReport++;
                        } else {
                            markAsSeen(item);
                            uniqueNumbersFromLastReport++;
                            logWriter.write(item);
                        }
                    }
                }
                reportData();
            }
        } catch (InterruptedException e) {
        } finally {
            Thread.currentThread().interrupt();
        }
    }

    private void readFromQueue(List<int[]> batch) throws InterruptedException {
        batch.clear();
        if (blockingQueue.drainTo(batch, BATCH_SIZE) == 0) {
            batch.add(blockingQueue.take());
        }
    }

    private boolean alreadySeen(int item) {
        return bitSet.get(item);
    }

    private void markAsSeen(int item) {
        bitSet.set(item);
    }

    private void reportData() {
        reporter.updateReportingData(uniqueNumbersFromLastReport, duplicatesFromLastReport);
        duplicatesFromLastReport = 0;
        uniqueNumbersFromLastReport = 0;
    }
}