package it.lorenzobugiani;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ExecutorUtils {

    public static void shutdown(ExecutorService executor) throws IOException {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
