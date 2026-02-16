package uk.gov.justice.laa.portal.landingpage.service;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MultifirmUserReportService {

    private final FirmRepository firmRepository;
    private final EntraUserRepository entraUserRepository;
    private String reportDirectory = System.getProperty("java.io.tmpdir") + File.separator + "reports";
    private final DateTimeFormatter fileTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

    public void getMultifirmUsers() {

        List<Object[]> reportRows = new ArrayList<>();

        reportRows.addAll(firmRepository.findMultiFirmUserCountsByFirm());
        reportRows.addAll(entraUserRepository.findUnlinkedMultifirmUsersCount());
        reportRows.addAll(entraUserRepository.findTotalMultiFirmUsersCount());
        writeToCsv(reportRows);
        log.info("Multifirm users written to CSV successfully");
    }

    private String writeToCsv(List<Object[]> rows) {

        String timestamp = LocalDateTime.now().format(fileTimestamp);
        Path outputPath = Path.of(reportDirectory, "SiLAS-multifirm-user-report-" + timestamp + ".csv");

        try {
            Files.createDirectories(outputPath.getParent());

            CsvMapper csvMapper = CsvMapper.builder().build();

            CsvSchema schema = CsvSchema.builder()
                    .addColumn("Firm Name")
                    .addColumn("Firm Code")
                    .addColumn("Count")
                    .setUseHeader(true)
                    .build();

            List<ReportRow> reportRows = new ArrayList<>(rows.size());
            for (Object[] row : rows) {
                String firmName = (String) row[0];
                String firmCode = row[1] == null ? "" : (String) row[1];
                long count = ((Number) row[2]).longValue();
                reportRows.add(new ReportRow(firmName, firmCode, count));
            }

            try (var writer = Files.newBufferedWriter(outputPath)) {
                csvMapper.writer(schema).writeValues(writer).writeAll(reportRows);
                writer.write(System.lineSeparator());
            }

            return outputPath.toAbsolutePath().toString();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to write multifirm users to CSV", e);
        }
    }

    private record ReportRow(
            @JsonProperty("Firm Name") String firmName,
            @JsonProperty("Firm Code") String firmCode,
            @JsonProperty("Count") long count
    ) { }
}
