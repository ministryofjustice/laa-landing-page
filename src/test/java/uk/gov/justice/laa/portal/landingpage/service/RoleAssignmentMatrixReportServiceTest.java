package uk.gov.justice.laa.portal.landingpage.service;


import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoleAssignmentMatrixReportServiceTest {


    @InjectMocks
    private RoleAssignmentMatrixReportService matrixReportService;

    @Mock
    private FirmRepository firmRepository;

    @Mock
    private AppRoleRepository appRoleRepository;

    @TempDir
    Path tempDir;

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
        matrixReportService = new RoleAssignmentMatrixReportService(firmRepository, appRoleRepository);

        ReflectionTestUtils.setField(matrixReportService, "reportDirectory", tempDir.toString());
    }

    private Tuple mockResponse(
            UUID firmId,
            String firmName,
            String firmCode,
            String roleName,
            Integer count
    ) {
        Tuple t = Mockito.mock(Tuple.class);

        when(t.get("firmId", UUID.class)).thenReturn(firmId);
        when(t.get("firmName", String.class)).thenReturn(firmName);
        when(t.get("firmCode", String.class)).thenReturn(firmCode);
        when(t.get("roleName", String.class)).thenReturn(roleName);
        when(t.get("roleCount", Integer.class)).thenReturn(count);

        return t;
    }

    @Test
    void generatesCorrectReport() throws IOException {

        UUID firm1 = UUID.randomUUID();
        UUID firm2 = UUID.randomUUID();

        when(appRoleRepository.getExternalRoleNames()).thenReturn(List.of("ROLE_1", "ROLE_2"));

        Tuple row1 = mockResponse(firm1, "Firm1", "F1", "ROLE_1", 2);
        Tuple row2 = mockResponse(firm1, "Firm1", "F1", "ROLE_2", 1);
        Tuple row3 = mockResponse(firm2, "Firm2", "F2", "ROLE_1", 5);

        when(firmRepository.findRoleCountsByFirm()).thenReturn(List.of(row1, row2, row3));

        matrixReportService.getRoleAssignmentMatrixReport();

        Path csvPath = getGeneratedCsvFile();

        List<String> lines = Files.readAllLines(csvPath);

        assertThat(csvPath).exists();

        assertThat(lines).containsExactly(
                "Firm Name,Firm Code,ROLE_1,ROLE_2",
                "Firm1,F1,2,1",
                "Firm2,F2,5,0"
        );
    }
}
