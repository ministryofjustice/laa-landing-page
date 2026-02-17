package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalUserReportingServiceTest {

    @Mock
    private FirmRepository firmRepository;

    @TempDir
    Path tempDir;

    private ExternalUserReportingService service;

    @BeforeEach
    void setUp() {
        service = new ExternalUserReportingService(firmRepository);

        org.springframework.test.util.ReflectionTestUtils
                .setField(service, "reportDirectory", tempDir);
    }

    @Test
    void createsCsvWithFullHeaderAndRows() throws Exception {

        when(firmRepository.findAllFirmExternalUserCount()).thenReturn(List.of(
                new Object[]{"Firm A", "FRA", "TypeA", "PARENT1", 5L, 1L, 0L, 0L},
                new Object[]{"Firm B", null, "TypeB", "PARENT2", 2L, 0L, 0L, 0L}
        ));

        service.downloadExternalUserCsv();

        Path csv = Files.list(tempDir)
                .filter(p -> p.getFileName().toString().startsWith("SiLAS-external-user-report-")
                        && p.getFileName().toString().endsWith(".csv"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("CSV not created"));

        List<String> lines = Files.readAllLines(csv);

        assertThat(lines).isNotEmpty();

        assertThat(lines.get(0)).isEqualTo(
                "\"Firm Name\",\"Firm Code\",\"Firm Type\",\"Parent Firm Code\"," +
                        "\"User Count\",\"Admin User Count\",\"Multi-Firm User Count\",\"Disabled User Count\""
        );

        assertThat(lines).contains(
                "\"Firm A\",FRA,TypeA,PARENT1,5,1,0,0",
                "\"Firm B\",,TypeB,PARENT2,2,0,0,0"
        );
    }

    @Test
    void escapesCsvValuesCorrectlyForAllColumns() throws Exception {

        when(firmRepository.findAllFirmExternalUserCount()).thenReturn(singletonList(
                new Object[]{"Firm, Inc", "ABC\"123", "Type, X", "PARENT\"Y", 1L, 0L, 0L, 0L}
        ));

        service.downloadExternalUserCsv();

        Path csv = Files.list(tempDir)
                .filter(p -> p.getFileName().toString().startsWith("SiLAS-external-user-report-")
                        && p.getFileName().toString().endsWith(".csv"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("CSV not created"));

        List<String> lines = Files.readAllLines(csv);

        String expected =
                "\"Firm, Inc\",\"ABC\"\"123\",\"Type, X\",\"PARENT\"\"Y\",1,0,0,0";

        assertThat(lines.get(1)).isEqualTo(expected);
    }
}
