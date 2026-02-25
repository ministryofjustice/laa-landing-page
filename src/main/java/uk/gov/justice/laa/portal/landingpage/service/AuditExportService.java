package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDto;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@AllArgsConstructor
public class AuditExportService {

    private static final String HEADER = "Name,Email,\"Firm Name\",\"Firm Code\",Multi-firm\n";
    private final DateTimeFormatter fileTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

    public AuditCsvExport downloadAuditCsv(List<AuditUserDto> firmData, String firmCode) {

        String timestamp = LocalDateTime.now().format(fileTimestamp);
        String filename = "user-access-audit_" + firmCode + "_" + timestamp + "_UTC.csv";

        String csv = buildCsv(firmData);
        return new AuditCsvExport(filename, csv.getBytes(StandardCharsets.UTF_8));
    }

    private String buildCsv(List<AuditUserDto> firmData) {
        CsvMapper mapper = CsvMapper.builder().build();

        CsvSchema schema = CsvSchema.builder()
                .setUseHeader(false)
                .setColumnSeparator(',')
                .setLineSeparator("\n")
                .addColumn("Name")
                .addColumn("Email")
                .addColumn("Firm Name")
                .addColumn("Firm Code")
                .addColumn("Multi-firm")
                .build();

        List<AuditUserCsvRow> rows = firmData.stream()
                .map(u -> new AuditUserCsvRow(
                        toStringSafe(u.getName()),
                        toStringSafe(u.getEmail()),
                        toStringSafe(u.getFirmAssociation()),
                        toStringSafe(u.getFirmCode()),
                        u.isMultiFirmUser() ? "Yes" : "No"
                ))
                .toList();

        try (StringWriter out = new StringWriter()) {
            out.write(HEADER);
            mapper.writer(schema)
                    .writeValues(out)
                    .writeAll(rows);

            if (rows.isEmpty() && !out.toString().endsWith("\n")) {
                out.append('\n');
            }

            return out.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build audit CSV", e);
        }
    }

    private String toStringSafe(Object value) {
        return value == null ? "" : value.toString();
    }

    @JsonPropertyOrder({ "Name", "Email", "Firm Name", "Firm Code", "Multi-firm" })
    private record AuditUserCsvRow(
            @JsonProperty("Name") String name,
            @JsonProperty("Email") String email,
            @JsonProperty("Firm Name") String firmName,
            @JsonProperty("Firm Code") String firmCode,
            @JsonProperty("Multi-firm") String multiFirm
    ) {}

    public record AuditCsvExport(String filename, byte[] bytes) {}
}
