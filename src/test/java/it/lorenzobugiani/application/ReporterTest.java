package it.lorenzobugiani.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ReporterTest {

    @Test
    @DisplayName("Generate the report")
    public void generateTheReport() {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter writer = new PrintWriter(stringWriter)) {
            var reporter = new Reporter(writer, Duration.ofSeconds(1));

            reset(stringWriter);
            reporter.updateReportingData(1, 2);
            reporter.updateReportingData(1, 2);

            await().atLeast(1, SECONDS).until(() -> stringWriter.toString().length() > 0);

            String output = stringWriter.toString();

            assertThat(output).isEqualTo(
                    "Received 2 unique numbers, 4 duplicates. Unique total: 2"
                            + System.lineSeparator());

            reset(stringWriter);
            reporter.updateReportingData(1, 2);

            await().atLeast(1, SECONDS).until(() -> stringWriter.toString().length() > 0);

            output = stringWriter.toString();
            assertThat(output).isEqualTo(
                    "Received 1 unique numbers, 2 duplicates. Unique total: 3"
                            + System.lineSeparator());
        }
    }

    private static void reset(StringWriter stringWriter) {
        stringWriter.getBuffer().setLength(0);
    }

}