package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDto;
import uk.gov.justice.laa.portal.landingpage.service.AuditExportService.AuditCsvExport;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditExportServiceTest {

    private final AuditExportService service = new AuditExportService();

    @Test
    void buildsFilenameWithFirmCodeAndTimestamp() {

        List<AuditUserDto> data = List.of(
                AuditUserDto.builder()
                        .firmCode("123")
                        .name("Alice Example")
                        .email("alice@example.com")
                        .firmAssociation("Example Firm")
                        .isMultiFirmUser(false)
                        .build()
        );

        AuditCsvExport export = service.downloadAuditCsv(data, "123", "testName");

        assertNotNull(export);
        assertNotNull(export.filename());
        assertTrue(export.filename().startsWith("user-access-audit_123_"));
        assertTrue(export.filename().endsWith("_UTC.csv"));

        assertTrue(export.filename().matches("^user-access-audit_123_\\d{4}-\\d{2}-\\d{2}_\\d{4}_UTC\\.csv$"),
                "Filename did not match expected pattern: " + export.filename());
    }

    @Test
    void whenDataIsEmptyExportsHeaderOnly() {

        List<AuditUserDto> data = List.of();

        AuditCsvExport export = service.downloadAuditCsv(data, "123", "testName");

        assertNotNull(export);

        String csv = new String(export.bytes(), StandardCharsets.UTF_8);
        assertEquals("Name,Email,\"Firm Name\",\"Firm Code\",Multi-firm," +
                "\"Provider Admin\"\n", csv);
    }

    @Test
    void writesRowsAndEscapesCsvValues() {

        AuditUserDto user = AuditUserDto.builder()
                .name("Doe, John")
                .email("a\"b@example.com")
                .firmAssociation("Firm\nName")
                .firmCode("FC1")
                .isMultiFirmUser(true)
                .isProviderAdmin(true)
                .build();

        AuditCsvExport export = service.downloadAuditCsv(List.of(user), "FC1", "Firm Name");

        String csv = new String(export.bytes(), StandardCharsets.UTF_8);

        String expected =
                "Name,Email,\"Firm Name\",\"Firm Code\",Multi-firm,\"Provider Admin\"\n"
                        + "\"Doe, John\",\"a\"\"b@example.com\",\"Firm Name\",FC1,Yes,Yes\n";

        assertEquals(expected, csv);
    }
}
