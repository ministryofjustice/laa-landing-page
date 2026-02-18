package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoleAssignmentMatrixReportServiceTest {

    @InjectMocks
    private RoleAssignmentMatrixReportService matrixReportService;

    @Mock
    private FirmRepository firmRepository;

    @Mock
    private AppRoleRepository appRoleRepository;

    @BeforeEach
    void setUp() {
        // Use a spy so we can capture the File returned by writeToCsv()
        matrixReportService = Mockito.spy(new RoleAssignmentMatrixReportService(firmRepository, appRoleRepository));
    }

    private Tuple mockResponse(
            UUID firmId,
            String firmName,
            String firmCode,
            String roleName,
            Number count
    ) {
        Tuple t = Mockito.mock(Tuple.class);

        when(t.get("firmId", UUID.class)).thenReturn(firmId);
        when(t.get("firmName", String.class)).thenReturn(firmName);
        when(t.get("firmCode", String.class)).thenReturn(firmCode);
        when(t.get("roleName", String.class)).thenReturn(roleName);
        when(t.get("userCount", Number.class)).thenReturn(count);

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

        AtomicReference<File> generated = new AtomicReference<>();
        doAnswer(invocation -> {
            File f = (File) invocation.callRealMethod();
            generated.set(f);
            return f;
        }).when(matrixReportService).writeToCsv(anyList(), anyMap());

        matrixReportService.getRoleAssignmentMatrixReport();

        File csvFile = generated.get();
        assertThat(csvFile).as("CSV file should be generated").isNotNull();
        assertThat(csvFile).exists();

        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertThat(lines).containsExactly(
                "\"Firm Name\",\"Firm Code\",ROLE_1,ROLE_2",
                "Firm1,F1,2,1",
                "Firm2,F2,5,0"
        );
    }

    @Test
    void propagatesFailureWhenCsvWriteFails() {
        when(appRoleRepository.getExternalRoleNames()).thenReturn(List.of("ROLE_1"));
        when(firmRepository.findRoleCountsByFirm()).thenReturn(List.of());

        doThrow(new UncheckedIOException("Failed to write role assignment matrix csv", new IOException("boom")))
                .when(matrixReportService)
                .writeToCsv(anyList(), anyMap());

        assertThatThrownBy(() -> matrixReportService.getRoleAssignmentMatrixReport())
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("Failed to write role assignment matrix csv");
    }
}