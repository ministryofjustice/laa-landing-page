package uk.gov.justice.laa.portal.landingpage.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@AllArgsConstructor
public class AuditExportService {

    private final DateTimeFormatter fileTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

    public AuditCsvExport downloadAuditCsv(String id, List<AuditUserDto> data) {

        String timestamp = LocalDateTime.now().format(fileTimestamp);
        String filename = "user-access-audit_" + id + "_" + timestamp + ".csv";
        String csv = buildCsv(id, data);
        return new AuditCsvExport(filename, csv.getBytes(StandardCharsets.UTF_8));
    }

    private String buildCsv(String id, List<AuditUserDto> data) {
        StringWriter stringWriter = new StringWriter();

        try (BufferedWriter writer = new BufferedWriter(stringWriter)) {
            writer.write("Name,Email,Firm Name,Firm Code,Multi-firm");
            writer.write('\n');

            for (AuditUserDto user : data) {
                writer.write(csvValue(toStringSafe(user.getName())));
                writer.write(',');
                writer.write(csvValue(toStringSafe(user.getEmail())));
                writer.write(',');
                writer.write(csvValue(toStringSafe(user.getFirmAssociation())));
                writer.write(',');
                writer.write(csvValue(toStringSafe(user.getFirmCode())));
                writer.write(',');
                writer.write(csvValue(user.isMultiFirmUser() ? "Yes" : "No"));
                writer.write('\n');
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build audit CSV", e);
        }

        return stringWriter.toString();
    }

    private String toStringSafe(Object value) {
        return value == null ? "" : value.toString();
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public record AuditCsvExport(String filename, byte[] bytes) {}
}