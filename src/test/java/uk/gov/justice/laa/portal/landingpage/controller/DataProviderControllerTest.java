package uk.gov.justice.laa.portal.landingpage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.justice.laa.portal.landingpage.dto.ComparisonResultDto;
import uk.gov.justice.laa.portal.landingpage.service.DataProviderService;

/**
 * Tests for DataProviderController.
 */
@ExtendWith(MockitoExtension.class)
class DataProviderControllerTest {

    @Mock
    private DataProviderService dataProviderService;

    private ObjectMapper objectMapper;
    private DataProviderController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        controller = new DataProviderController(dataProviderService, objectMapper);
    }

    @Test
    void shouldReturnComparisonResult() throws Exception {
        // Given
        ComparisonResultDto comparisonResult = ComparisonResultDto.builder()
                .firmCreates(1)
                .firmUpdates(2)
                .firmDisables(0)
                .firmExists(10)
                .officeCreates(3)
                .officeUpdates(1)
                .officeDeletes(0)
                .officeExists(20)
                .build();

        when(dataProviderService.compareWithDatabase()).thenReturn(comparisonResult);

        // When
        ResponseEntity<String> response = controller.compareProviderOffices();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"firmCreates\" : 1");
        assertThat(response.getBody()).contains("\"firmUpdates\" : 2");
        verify(dataProviderService).compareWithDatabase();
    }

    @Test
    void shouldHandleComparisonError() {
        // Given
        when(dataProviderService.compareWithDatabase()).thenThrow(new RuntimeException("Test error"));

        // When
        ResponseEntity<String> response = controller.compareProviderOffices();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("error");
    }

    @Test
    void shouldHandleEmptyComparisonResult() throws Exception {
        // Given
        ComparisonResultDto comparisonResult = ComparisonResultDto.builder()
                .firmCreates(0)
                .firmUpdates(0)
                .firmDisables(0)
                .firmExists(0)
                .officeCreates(0)
                .officeUpdates(0)
                .officeDeletes(0)
                .officeExists(0)
                .build();

        when(dataProviderService.compareWithDatabase()).thenReturn(comparisonResult);

        // When
        ResponseEntity<String> response = controller.compareProviderOffices();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}
