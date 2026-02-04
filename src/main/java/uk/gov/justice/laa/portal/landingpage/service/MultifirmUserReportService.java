package uk.gov.justice.laa.portal.landingpage.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.io.BufferedWriter;
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

    private void writeToCsv(List<Object[]> rows){

        String timestamp = LocalDateTime.now().format(fileTimestamp);
        Path outputPath = Path.of(reportDirectory, "SiLAS-multifirm-user-report-" + timestamp + ".csv");

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)){
            writer.write("Firm Name, Firm Code, Count");
            writer.newLine();

            for (Object[] row : rows) {
                String firmName = (String) row[0];
                String firmCode = row[1] == null ? "" : (String) row[1];
                long count = ((Number) row[2]).longValue();

                writer.write(csvValue(firmName));
                writer.write(",");
                writer.write(csvValue(firmCode));
                writer.write(",");
                writer.write(Long.toString(count));
                writer.newLine();
            }
        } catch (IOException e){
            throw new IllegalStateException("Failed to write multifirm users to CSV", e);
        }
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
