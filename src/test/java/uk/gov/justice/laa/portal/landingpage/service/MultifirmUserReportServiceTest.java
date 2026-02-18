package uk.gov.justice.laa.portal.landingpage.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultifirmUserReportServiceTest {

    @Mock
    private FirmRepository firmRepository;

    @Mock
    private EntraUserRepository entraUserRepository;

    private MultifirmUserReportService service;

    private static final String REPORT_PREFIX = "SiLAS-multifirm-user-report-";

    @BeforeEach
    void setUp() {
        service = new MultifirmUserReportService(firmRepository, entraUserRepository);
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
                    .orElseThrow(() -> new AssertionError("No CSV file found in system temp"));
        }
    }

    @Test
    void getMultifirmUsers_createsCsvWithAllRows() throws Exception {
        when(firmRepository.findMultiFirmUserCountsByFirm())
                .thenReturn(List.of(
                        new Object[] { "firm1", "67", 10L },
                        new Object[] { "firm2", "21", 20L }
                ));
        when(entraUserRepository.findUnlinkedMultifirmUsersCount())
                .thenReturn(List.<Object[]>of(
                        new Object[] { "Unlinked multi-firm users", null, 30L }
                ));
        when(entraUserRepository.findTotalMultiFirmUsersCount())
                .thenReturn(List.<Object[]>of(
                        new Object[] { "Total multi-firm users", null, 50L }
                ));

        Instant notBefore = Instant.now();
        service.getMultifirmUsers();

        Path csvPath = getGeneratedCsvFile();
        List<String> lines = Files.readAllLines(csvPath);

        assertThat(lines).containsExactly(
                "\"Firm Name\",\"Firm Code\",Count",
                "firm1,67,10",
                "firm2,21,20",
                "\"Unlinked multi-firm users\",,30",
                "\"Total multi-firm users\",,50",
                ""
        );
    }

    @Test
    void getMultiFirmUsers_escapesCsvValuesCorrectly() throws Exception {
        when(firmRepository.findMultiFirmUserCountsByFirm())
                .thenReturn(List.<Object[]>of(new Object[] { "Firm, Inc", "ABC\"123", 1L }));
        when(entraUserRepository.findUnlinkedMultifirmUsersCount())
                .thenReturn(List.<Object[]>of(
                        new Object[] { "Unlinked multi-firm users", null, 0L }
                ));
        when(entraUserRepository.findTotalMultiFirmUsersCount())
                .thenReturn(List.<Object[]>of(
                        new Object[] { "Total multi-firm users", null, 1L }
                ));

        Instant notBefore = Instant.now();
        service.getMultifirmUsers();

        List<String> lines = Files.readAllLines(getGeneratedCsvFile());
        assertThat(lines.get(1)).isEqualTo("\"Firm, Inc\",\"ABC\"\"123\",1");
    }
}