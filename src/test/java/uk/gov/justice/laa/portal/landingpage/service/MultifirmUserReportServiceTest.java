package uk.gov.justice.laa.portal.landingpage.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MultifirmUserReportServiceTest {

    @Mock
    private FirmRepository firmRepository;

    @Mock
    private EntraUserRepository entraUserRepository;

    @TempDir
    Path tempDir;

    private MultifirmUserReportService service;

    private Path getGeneratedCsvFile() throws IOException {
        try (Stream<Path> files = Files.list(tempDir)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".csv"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No CSV file found"));
        }
    }

    @BeforeEach
    void setUp() {
        service = new MultifirmUserReportService(firmRepository, entraUserRepository);

        ReflectionTestUtils.setField(service, "reportDirectory", tempDir.toString());
    }

    @Test
    void pollForMultifirmUsers_createsCsvWithAllRows() throws Exception {

        when(firmRepository.findMultiFirmUserCountsByFirm())
                .thenReturn(List.of(
                        new Object[]{"firm1", "67", 10L},
                        new Object[]{"firm2", "21", 20L}
                ));
        when(entraUserRepository.findUnlinkedMultifirmUsersCount())
                .thenReturn(singletonList(
                        new Object[]{"Unlinked multi-firm users", null, 30L}
                ));
        when(entraUserRepository.findTotalMultiFirmUsersCount())
                .thenReturn(singletonList(
                        new Object[]{"Total multi-firm users", null, 50L}
                ));

        service.getMultifirmUsers();

        Path csvPath = getGeneratedCsvFile();
        List<String> lines = Files.readAllLines(csvPath);

        assertThat(lines).containsExactly(
                "Firm Name, Firm Code, Count",
                "firm1,67,10",
                "firm2,21,20",
                "Unlinked multi-firm users,,30",
                "Total multi-firm users,,50"
        );
    }

    @Test
    void getMultiFirmUsers_escapesCsvValuesCorrectly() throws Exception {

        when(firmRepository.findMultiFirmUserCountsByFirm())
                .thenReturn(singletonList(new Object[] {"Firm, Inc", "ABC\"123", 1L}));
        when(entraUserRepository.findUnlinkedMultifirmUsersCount())
                .thenReturn(singletonList(
                        new Object[]{"Unlinked multi-firm users", null, 0L}
                ));
        when(entraUserRepository.findTotalMultiFirmUsersCount())
                .thenReturn(singletonList(
                        new Object[]{"Total multi-firm users", null, 1L}
                ));
        service.getMultifirmUsers();

        List<String> lines = Files.readAllLines(getGeneratedCsvFile());
        assertThat(lines.get(1)).isEqualTo("\"Firm, Inc\",\"ABC\"\"123\",1");
    }
}
