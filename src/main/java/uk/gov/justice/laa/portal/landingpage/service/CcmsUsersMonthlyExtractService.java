package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CcmsUsersMonthlyExtractService {

    private final EntraUserRepository entraUserRepository;
    private final ReportUploadService reportUploadService;
    private final String folderPath = "CCMS_users_monthly_extract";
    private final DateTimeFormatter fileTimestamp = DateTimeFormatter.ofPattern("MM-yyyy");

    public void downloadCcmsUsersMonthlyExtract() {
        LocalDate referenceDate = LocalDate.now();

        LocalDate endBoundary = referenceDate.withDayOfMonth(20);
        LocalDate startBoundary = endBoundary.minusMonths(1);

        LocalDateTime start = startBoundary.atStartOfDay();
        LocalDateTime end = endBoundary.atStartOfDay();

        List<Object[]> rows = entraUserRepository.findCcmsUsersWithAppInPeriod(
                UserType.EXTERNAL, "CCMS PUI", start, end);

        File csv = writeToCsv(rows, startBoundary, endBoundary);

        try {
            reportUploadService.uploadCsvToSharePoint(csv, folderPath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        log.info("Ccms user monthly report written to CSV successfully for window {} -> {}", startBoundary, endBoundary.minusDays(1));
    }

    private File writeToCsv(List<Object[]> rows, LocalDate startBoundary, LocalDate endBoundary) {

        String timestamp = LocalDateTime.now().format(fileTimestamp);
        String filename = "ccms_users_" + timestamp + ".csv";

        try {
            CsvMapper mapper = CsvMapper.builder().build();

            CsvSchema schema = CsvSchema.builder()
                    .addColumn("First Name")
                    .addColumn("Last Name")
                    .addColumn("Email")
                    .setUseHeader(true)
                    .build();

            Path tempFile = Paths.get(
                    System.getProperty("java.io.tmpdir"),
                    filename
            );
            Files.deleteIfExists(tempFile);
            Files.createFile(tempFile);

            try (BufferedWriter writer = Files.newBufferedWriter(tempFile);
                 SequenceWriter sequenceWriter = mapper
                         .writer(schema)
                         .writeValues(writer)) {

                for (Object[] row : rows) {
                    List<Object> csvRow = List.of(
                            row[0] == null ? "" : row[0],
                            row[1] == null ? "" : row[1],
                            row[2] == null ? "" : row[2]
                    );
                    sequenceWriter.write(csvRow);
                }
                return tempFile.toFile();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write ccms users monthly extract to CSV", e);
        }
    }
}