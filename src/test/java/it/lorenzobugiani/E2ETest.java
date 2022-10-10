package it.lorenzobugiani;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

class E2ETest {

    @Test
    @DisplayName("Check unique numbers")
    public void checkUniqueNumbers() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<?> mainTask = executor.submit(() -> Main.main(new String[0]));

        Thread.sleep(500);

        executor.submit(() -> {
            try (
                    var socket = new Socket("localhost", 4000);
                    var printWriter = new PrintWriter(socket.getOutputStream());
            ) {

                var numbers = new ArrayList<Integer>();

                IntStream.range(0, 100000).forEach(value -> {
                    numbers.add(value);
                    numbers.add(value);
                    numbers.add(value);
                });

                Collections.shuffle(numbers);

                numbers.forEach(integer -> {
                    printWriter.write(integer + System.lineSeparator());
                    printWriter.flush();
                });
                printWriter.write("terminate" + System.lineSeparator());
                printWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        await().until(mainTask::isDone);

        File numbersFile = new File("./numbers.log");

        try (var myReader = new Scanner(numbersFile)) {
            var writtenNumbers = new ArrayList<>();
            while (myReader.hasNextLine()) {
                writtenNumbers.add(Integer.parseInt(myReader.nextLine()));
            }

            if (writtenNumbers.size() > 100000) {
                fail("Too many numbers");
            }

            assertThat(writtenNumbers).hasSize(100000);

            var expectedNumbers = IntStream.range(0, 100000).boxed().toList();
            assertThat(writtenNumbers).containsExactlyInAnyOrderElementsOf(expectedNumbers);

        } catch (FileNotFoundException e) {
            fail("File not found");
        }
    }
}
