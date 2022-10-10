package it.lorenzobugiani.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

class LogWriterTest {

    @Test
    @DisplayName("Flush file content at regular interval")
    public void flush() throws IOException, InterruptedException {
        var outputFile = Files.createTempFile("numbers", ".log").toFile();
        var logWriter = new LogWriter(outputFile, Duration.ofMillis(300));

        logWriter.write(1);
        logWriter.write(2);
        logWriter.write(3);
        Thread.sleep(300);

        try (var myReader = new Scanner(outputFile)) {
            var writtenNumbers = new ArrayList<>();
            while (myReader.hasNextLine()) {
                writtenNumbers.add(Integer.parseInt(myReader.nextLine()));
            }

            var expectedNumbers = List.of(1, 2, 3);

            assertThat(writtenNumbers).containsExactlyInAnyOrderElementsOf(expectedNumbers);
        }
    }

}