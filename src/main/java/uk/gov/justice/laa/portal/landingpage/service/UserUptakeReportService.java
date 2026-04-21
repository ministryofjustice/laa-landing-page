package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserUptakeReportService {

    private final ReportUploadService reportUploadService;
    private final UserProfileRepository userProfileRepository;
    private final String folderPath = "user_uptake_reports";
    private final DateTimeFormatter fileTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");
    private final List<String> metrics = List.of(
            "Internal Users", "Total External Users", "Active External Users", "Complete External Users",
            "External Users with No Roles Assigned", "Disabled External Users", "Incomplete External Users",
            "Activation Pending External Users", "Total Firms with External Users", "Total Firms with Active External Users"
    );

    public void getUserUptakeReport() {

        List<Object[]> reportRows = new ArrayList<>();

        reportRows.add(metrics.toArray());
        reportRows.add(userProfileRepository.findByUserTypes(UserType.INTERNAL).size());
        reportRows.add(userProfileRepository.findByUserTypes(UserType.EXTERNAL).size());
        reportRows.add(userProfileRepository.)



        File userUptake = writeToCsv(reportRows);

        try {
            reportUploadService.uploadCsvToSharePoint(userUptake, folderPath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private File writeToCsv(List<Object[]> rows) {

        String timestamp = LocalDateTime.now().format(fileTimestamp);
        String fileName = "SiLAS-user-uptake-report" + timestamp + ".csv";

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

            List<ReportRow> reportRows = new ArrayList<>(rows.size());
            for (Object[] row : rows) {
                String metric = String.valueOf(row[0]);
                String totalCount = row[1] == null ? "" : String.valueOf(row[1]);
                long multiFirmCount = ((Number) row[2]).longValue();
                reportRows.add(new ReportRow(metric, totalCount, multiFirmCount));
            }

            try (Writer writer = Files.newBufferedWriter(tempFile)) {
                csvMapper.writer(schema).writeValues(writer).writeAll(reportRows);
                writer.write(System.lineSeparator());
            }

            return tempFile.toFile();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to write user uptake report to CSV", e);
        }
    }

    private record ReportRow(
            @JsonProperty("Metric") String metric,
            @JsonProperty("Total Count") String totalCount,
            @JsonProperty("Multi-firm Count") long multiFirmCount
    ) { }
}
