package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUptakeReportServiceTest {

    @Mock
    private EntraUserRepository entraUserRepository;

    @Mock
    private FirmRepository firmRepository;

    @Mock
    private ReportUploadService reportUploadService;

    private UserUptakeReportService service;

    private static final String REPORT_PREFIX = "SiLAS-user-uptake-report-";

    @BeforeEach
    void setUp() {
        service = new UserUptakeReportService(
                reportUploadService,
                entraUserRepository,
                firmRepository
        );
    }

    private Path getGeneratedCsvFile() throws IOException {
        Path systemTempDir = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> files = Files.list(systemTempDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(REPORT_PREFIX))
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .max(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }))
                    .orElseThrow(() ->
                            new AssertionError("No user uptake CSV file found in system temp"));
        }
    }

    @Test
    void getUserUptakeReport_createsCsvWithAllMetrics() throws Exception {
        when(entraUserRepository.countInternalUsers()).thenReturn(5L);
        when(entraUserRepository.countTotalExternalUsers()).thenReturn(20L);
        when(entraUserRepository.countTotalExternalMultiFirmUsers()).thenReturn(8L);
        when(entraUserRepository.countActiveExternalUsers()).thenReturn(15L);
        when(entraUserRepository.countActiveExternalMultiFirmUsers()).thenReturn(6L);
        when(entraUserRepository.countCompleteExternalUsers()).thenReturn(10L);
        when(entraUserRepository.countCompleteExternalMultiFirmUsers()).thenReturn(4L);
        when(entraUserRepository.countExternalUsersWithNoRoles()).thenReturn(3L);
        when(entraUserRepository.countExternalMultiFirmUsersWithNoRoles()).thenReturn(2L);
        when(entraUserRepository.countDisabledExternalUsers()).thenReturn(2L);
        when(entraUserRepository.countDisabledExternalMultiFirmUsers()).thenReturn(1L);
        when(entraUserRepository.countIncompleteExternalUsers()).thenReturn(5L);
        when(entraUserRepository.countIncompleteExternalMultiFirmUsers()).thenReturn(2L);
        when(entraUserRepository.countActivationPendingExternalUsers()).thenReturn(4L);
        when(entraUserRepository.countActivationPendingExternalMultiFirmUsers()).thenReturn(1L);
        when(firmRepository.countFirmsWithExternalUsers()).thenReturn(7L);
        when(firmRepository.countFirmsWithMultiFirmExternalUsers()).thenReturn(3L);
        when(firmRepository.countFirmsWithActiveExternalUsers()).thenReturn(5L);
        when(firmRepository.countFirmsWithActiveMultiFirmExternalUsers()).thenReturn(2L);

        service.getUserUptakeReport();

        verify(reportUploadService)
                .uploadCsvToSharePoint(any(File.class), eq("user_uptake_reports"));

        Path csvPath = getGeneratedCsvFile();
        List<String> lines = Files.readAllLines(csvPath);

        assertThat(lines).containsExactly(
                "Metric,\"Total Count\",\"Multi-firm Count\"",
                "\"Internal Users\",5,",
                "\"Total External Users\",20,8",
                "\"Active External Users\",15,6",
                "\"Complete External Users\",10,4",
                "\"External Users with No Roles Assigned\",3,2",
                "\"Disabled External Users\",2,1",
                "\"Incomplete External Users\",5,2",
                "\"Activation Pending External Users\",4,1",
                "\"Total Firms with External Users\",7,3",
                "\"Total Firms with Active External Users\",5,2"
        );
    }
}
