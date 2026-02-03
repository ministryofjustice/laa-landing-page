package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.io.BufferedWriter;
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
public class ExternalUserReportingService {

    private static final String[] HEADERS = {
            "Firm Name",
            "Firm Code",
            "Firm Type",
            "Parent Firm Code",
            "User Count",
            "Admin User Count",
            "Multi-Firm User Count",
            "Disabled User Count"
    };

    private final FirmRepository firmRepository;

    private final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm");
    private String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP);

    public void getExternalUsers() {

        List<Object[]> reportRows = new ArrayList<>();

        reportRows.addAll(firmRepository.findAllFirmExternalUserCount());

        writeToCsv(reportRows);
        log.info("External users written to CSV successfully");
    }

    private void writeToCsv(List<Object[]> rows) {

        String reportDirectory = "\\tmp\\reports";
        Path outputPath = Path.of(reportDirectory, "SiLAS-external-user-report-" + timestamp + ".csv");

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)){
            writer.write(String.join(",", HEADERS));
            writer.newLine();

            for (Object[] row : rows) {
                String firmName = (String) row[0];
                String firmCode = row[1] == null ? "" : (String) row[1];
                String firmType = (String) row[2];
                String parentFirmCode = row[3] == null ? "" : (String) row[3];
                long userCount = (Long) row[4];
                long adminUserCount = (Long) row[5];
                long multiFirmUserCount = (Long) row[6];
                long disabledUserCount = (Long) row[7];

                writer.write(csvValue(firmName));
                writer.write(",");
                writer.write(csvValue(firmCode));
                writer.write(",");
                writer.write(csvValue(firmType));
                writer.write(",");
                writer.write(csvValue(parentFirmCode));
                writer.write(",");
                writer.write(csvValue(Long.toString(userCount)));
                writer.write(",");
                writer.write(csvValue(Long.toString(adminUserCount)));
                writer.write(",");
                writer.write(csvValue(Long.toString(multiFirmUserCount)));
                writer.write(",");
                writer.write(csvValue(Long.toString(disabledUserCount)));
                writer.newLine();
            }
        } catch (IOException e){
            throw new IllegalStateException("Failed to write external users to CSV", e);
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
