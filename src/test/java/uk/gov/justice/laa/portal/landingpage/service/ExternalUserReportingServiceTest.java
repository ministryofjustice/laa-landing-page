package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExternalUserReportingServiceTest {

    @Mock
    private FirmRepository firmRepository;

    @TempDir
    Path tempDir;

    private ExternalUserReportingService service;

    private final Path reportDir = Paths.get("\\tmp\\reports");
    private final String fixedTimestamp = "2025-01-01-12:00";
    private final String filename = "SiLAS-external-user-report-" + fixedTimestamp + ".csv";
    private final Path outputPath = reportDir.resolve(filename);

    @BeforeEach
    void setUp() throws Exception {
        service = new ExternalUserReportingService(firmRepository);

        // Ensure the service writes into a directory that exists (service uses a literal "\\tmp\\reports")
        Files.createDirectories(reportDir);

        // Stabilise the generated filename by fixing the timestamp field
        ReflectionTestUtils.setField(service, "timestamp", fixedTimestamp);
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (Files.exists(outputPath)) {
                Files.delete(outputPath);
            }
            // attempt to remove directory if empty
            if (Files.exists(reportDir) && Files.list(reportDir).findAny().isEmpty()) {
                Files.delete(reportDir);
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    void createsCsvWithFullHeaderAndRows() throws Exception {
        when(firmRepository.findAllFirmExternalUserCount()).thenReturn(List.of(
                // 8 columns: firmName, firmCode, firmType, parentFirmCode, userCount, adminUserCount, multiFirmUserCount, disabledUserCount
                new Object[] {"Firm A", "FRA", "TypeA", "PARENT1", 5L, 1L, 0L, 0L},
                new Object[] {"Firm B", null, "TypeB", "PARENT2", 2L, 0L, 0L, 0L}
        ));

        service.getExternalUsers();

        List<String> lines = Files.readAllLines(outputPath);

        String[] headers = (String[]) ReflectionTestUtils.getField(ExternalUserReportingService.class, "HEADERS");
        assertThat(lines).isNotEmpty();
        assertThat(lines.get(0)).isEqualTo(String.join(",", headers));

        // Full rows: all 8 columns expected
        assertThat(lines).contains(
                "Firm A,FRA,TypeA,PARENT1,5,1,0,0",
                "Firm B,,TypeB,PARENT2,2,0,0,0"
        );
    }

    @Test
    void escapesCsvValuesCorrectlyForAllColumns() throws Exception {
        when(firmRepository.findAllFirmExternalUserCount()).thenReturn(singletonList(
                new Object[] {"Firm, Inc", "ABC\"123", "Type, X", "PARENT\"Y", 1L, 0L, 0L, 0L}
        ));

        service.getExternalUsers();

        List<String> lines = Files.readAllLines(outputPath);

        // header present
        String[] headers = (String[]) ReflectionTestUtils.getField(ExternalUserReportingService.class, "HEADERS");
        assertThat(lines.get(0)).isEqualTo(String.join(",", headers));

        // Check escaped data row (index 1)
        String expected = "\"Firm, Inc\",\"ABC\"\"123\",\"Type, X\",\"PARENT\"\"Y\",1,0,0,0";
        assertThat(lines.get(1)).isEqualTo(expected);
    }
}


