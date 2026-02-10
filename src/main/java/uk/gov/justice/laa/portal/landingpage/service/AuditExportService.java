package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.persistence.Tuple;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuditExportService {

    private final UserProfileRepository repository;
    private final DateTimeFormatter fileTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

    public AuditCsvExport downloadAuditCsv(String id, String roleFilter, UUID selectedAppId, String selectedUserType) {

        List<Tuple> firmData = repository.findFirmUsers(UUID.fromString(id), roleFilter, selectedAppId, selectedUserType);

        String timestamp = LocalDateTime.now().format(fileTimestamp);
        String filename = "user-access-audit_" + id + "_" + timestamp + ".csv";

        String csv = buildCsv(firmData);
        return new AuditCsvExport(filename, csv.getBytes(StandardCharsets.UTF_8));
    }

    private String buildCsv(List<Tuple> firmData) {
        StringBuilder sb = new StringBuilder();

        // Header row (as requested)
        sb.append("name,email,firmName,firmId,multifirm").append('\n');

        for (Tuple item : firmData) {
            sb.append(csvValue(toStringSafe(item.get("userName")))).append(',')
                    .append(csvValue(toStringSafe(item.get("email")))).append(',')
                    .append(csvValue(toStringSafe(item.get("firmName")))).append(',')
                    .append(csvValue(toStringSafe(item.get("firmId")))).append(',')
                    .append(csvValue(toStringSafe(item.get("multifirm")))).append('\n');
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