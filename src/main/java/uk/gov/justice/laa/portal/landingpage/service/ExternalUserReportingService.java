package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExternalUserReportingService {

    private final FirmRepository firmRepository;
    private final DateTimeFormatter fileTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

    public void downloadExternalUserCsv() {
        List<Object[]> reportRows = new ArrayList<>(firmRepository.findAllFirmExternalUserCount());
        File Csv = writeToCsv(reportRows);
        //TODO: Output path code

        log.info("External user report written to CSV successfully");
    }

    private File writeToCsv(List<Object[]> rows) {

        String timestamp = LocalDateTime.now().format(fileTimestamp);
        String filename = "SiLAS-external-user-report-" + timestamp;

        try {
            CsvMapper mapper = CsvMapper.builder().build();

            CsvSchema schema = CsvSchema.builder()
                    .addColumn("Firm Name")
                    .addColumn("Firm Code")
                    .addColumn("Firm Type")
                    .addColumn("Parent Firm Code")
                    .addColumn("User Count")
                    .addColumn("Admin User Count")
                    .addColumn("Multi-Firm User Count")
                    .addColumn("Disabled User Count")
                    .setUseHeader(true)
                    .build();

            Path tempFile = Files.createTempFile(filename, ".csv");
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile);
                 SequenceWriter sequenceWriter = mapper
                         .writer(schema)
                         .writeValues(writer)) {

                for (Object[] row : rows) {

                    List<Object> csvRow = List.of(
                            row[0],
                            row[1] == null ? "" : row[1],
                            row[2],
                            row[3] == null ? "" : row[3],
                            row[4],
                            row[5],
                            row[6],
                            row[7]
                    );
                    sequenceWriter.write(csvRow);
                }
                return tempFile.toFile();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write external users to CSV", e);
        }
    }
}
