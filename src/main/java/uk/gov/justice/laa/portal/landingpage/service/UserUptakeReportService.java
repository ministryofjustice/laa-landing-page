package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserUptakeReportService {

    private final ReportUploadService reportUploadService;
    private final EntraUserRepository entraUserRepository;
    private final FirmRepository firmRepository;

    private static final String FOLDER_PATH = "user_uptake_reports";
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

    public void getUserUptakeReport() {

        List<ReportRow> rows = List.of(
                new ReportRow(
                        "Internal Users",
                        entraUserRepository.countInternalUsers(),
                        null
                ),
                new ReportRow(
                        "Total External Users",
                        entraUserRepository.countTotalExternalUsers(),
                        entraUserRepository.countTotalExternalMultiFirmUsers()
                ),
                new ReportRow(
                        "Active External Users",
                        entraUserRepository.countActiveExternalUsers(),
                        entraUserRepository.countActiveExternalMultiFirmUsers()
                ),
                new ReportRow(
                        "Complete External Users",
                        entraUserRepository.countCompleteExternalUsers(),
                        entraUserRepository.countCompleteExternalMultiFirmUsers()
                ),
                new ReportRow(
                        "External Users with No Roles Assigned",
                        entraUserRepository.countExternalUsersWithNoRoles(),
                        entraUserRepository.countExternalMultiFirmUsersWithNoRoles()
                ),
                new ReportRow(
                        "Disabled External Users",
                        entraUserRepository.countDisabledExternalUsers(),
                        entraUserRepository.countDisabledExternalMultiFirmUsers()
                ),
                new ReportRow(
                        "Incomplete External Users",
                        entraUserRepository.countIncompleteExternalUsers(),
                        entraUserRepository.countIncompleteExternalMultiFirmUsers()
                ),
                new ReportRow(
                        "Activation Pending External Users",
                        entraUserRepository.countActivationPendingExternalUsers(),
                        entraUserRepository.countActivationPendingExternalMultiFirmUsers()
                ),
                new ReportRow(
                        "Total Firms with External Users",
                        firmRepository.countFirmsWithExternalUsers(),
                        firmRepository.countFirmsWithMultiFirmExternalUsers()
                ),
                new ReportRow(
                        "Total Firms with Active External Users",
                        firmRepository.countFirmsWithActiveExternalUsers(),
                        firmRepository.countFirmsWithActiveMultiFirmExternalUsers()
                )
        );

        File csv = writeToCsv(rows);

        try {
            reportUploadService.uploadCsvToSharePoint(csv, FOLDER_PATH);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to upload user uptake report", e);
        }
    }

    /* CSV writer unchanged except simplified input */
    private File writeToCsv(List<ReportRow> rows) {

        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP);
        String fileName = "SiLAS-user-uptake-report-" + timestamp + ".csv";

        try {
            CsvMapper csvMapper = CsvMapper.builder().build();

            Path tempFile = Paths.get(
                    System.getProperty("java.io.tmpdir"),
                    fileName
            );
            Files.createFile(tempFile);

            CsvSchema schema = CsvSchema.builder()
                    .addColumn("Metric")
                    .addColumn("Total Count")
                    .addColumn("Multi-firm Count")
                    .setUseHeader(true)
                    .build();

            try (Writer writer = Files.newBufferedWriter(tempFile)) {
                csvMapper.writer(schema).writeValues(writer).writeAll(rows);
            }

            return tempFile.toFile();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to write user uptake report to CSV", e
            );
        }
    }

    private record ReportRow(
            @JsonProperty("Metric") String metric,
            @JsonProperty("Total Count") Long totalCount,
            @JsonProperty("Multi-firm Count") Long multiFirmCount
    ) {}
}
