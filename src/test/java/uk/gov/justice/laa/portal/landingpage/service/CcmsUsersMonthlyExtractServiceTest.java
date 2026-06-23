
package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CcmsUsersMonthlyExtractServiceTest {

    @Mock
    private EntraUserRepository entraUserRepository;

    @Mock
    private ReportUploadService reportUploadService;

    private CcmsUsersMonthlyExtractService service;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(new CcmsUsersMonthlyExtractService(
                entraUserRepository,
                reportUploadService
        ));
    }

    @Test
    void shouldCreateCsvAndUploadToSharePoint() throws Exception {
        when(entraUserRepository.findCcmsUsersWithAppInPeriod(
                eq(UserType.EXTERNAL),
                eq("CCMS PUI"),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(
                new Object[]{"John", "Doe", "john.doe@test.com",
                        LocalDateTime.of(2026, 3, 21, 0, 0)},
                new Object[]{"Jane", "Smith", "jane.smith@test.com",
                        LocalDateTime.of(2026, 4, 1, 0, 0)}
        ));

        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));

        service.downloadCcmsUsersMonthlyExtract();

        Path csv = Files.list(tempDir)
                .filter(p -> p.getFileName().toString().startsWith("ccms_users_"))
                .filter(p -> p.getFileName().toString().endsWith(".csv"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("CSV not created"));

        List<String> lines = Files.readAllLines(csv);

        assertThat(lines).isNotEmpty();

        assertThat(lines.get(0)).isEqualTo(
                "\"First Name\",\"Last Name\",Email,\"Created Date\""
        );

        assertThat(lines).contains(
                "John,Doe,john.doe@test.com,21/03/2026",
                "Jane,Smith,jane.smith@test.com,01/04/2026"
        );

        verify(entraUserRepository, times(1))
                .findCcmsUsersWithAppInPeriod(
                        eq(UserType.EXTERNAL),
                        eq("CCMS PUI"),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
            );

        verify(reportUploadService, times(1))
                .uploadCsvToSharePoint(any(), eq("CCMS_users_monthly_extract"));
    }
}
