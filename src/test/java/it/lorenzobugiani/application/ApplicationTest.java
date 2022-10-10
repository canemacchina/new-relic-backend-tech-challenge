package it.lorenzobugiani.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import it.lorenzobugiani.NullOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ApplicationTest {

    @Test
    @DisplayName("Write unique numbers to file")
    public void writeToFile() throws IOException, InterruptedException {
        BlockingQueue<int[]> queue = new ArrayBlockingQueue<>(2);
        queue.add(new int[]{1, 2, 3, 4, 5});
        queue.add(new int[]{3, 4, 5, 6, 7, 8});
        var outputFile = Files.createTempFile("numbers", ".log").toFile();
        try (var logWriter = new LogWriter(outputFile, Duration.ofMillis(300))) {
            PrintWriter printWriter = new PrintWriter(
                    new OutputStreamWriter(new NullOutputStream()));
            Reporter reporter = new Reporter(printWriter, Duration.ofSeconds(5));
            Application application = new Application(queue, logWriter, reporter);

            Thread applicationThread = new Thread(() -> {
                try {
                    application.run();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            applicationThread.start();

            // Simulate stop
            Thread.sleep(500);
            applicationThread.interrupt();

            await().until(() -> !applicationThread.isAlive());
        }

        try (var myReader = new Scanner(outputFile)) {
            var writtenNumbers = new ArrayList<>();
            while (myReader.hasNextLine()) {
                writtenNumbers.add(Integer.parseInt(myReader.nextLine()));
            }

            var expectedNumbers = List.of(1, 2, 3, 4, 5, 6, 7, 8);

            assertThat(writtenNumbers).containsExactlyInAnyOrderElementsOf(expectedNumbers);
        }
    }

}