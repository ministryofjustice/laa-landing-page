package uk.gov.justice.laa.portal.landingpage.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDto;

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
        StringBuilder sb = new StringBuilder();

        // Header row (as requested)
        sb.append("Name, Email, Firm Name,Firm ID, Multi-firm").append('\n');

        for (AuditUserDto user : data) {
            sb.append(csvValue(toStringSafe(user.getName()))).append(',')
                    .append(csvValue(toStringSafe(user.getEmail()))).append(',')
                    .append(csvValue(toStringSafe(user.getFirmAssociation()))).append(',')
                    .append(csvValue(toStringSafe(id))).append(',')
                    .append(csvValue(toStringSafe(user.isMultiFirmUser()))).append('\n');
        }

        return sb.toString();
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