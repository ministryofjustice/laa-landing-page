package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import uk.gov.justice.laa.portal.landingpage.dto.ComparisonResultDto;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.dto.SyncResponse;
import uk.gov.justice.laa.portal.landingpage.service.DataProviderService;

/**
 * Tests for DataProviderController.
 */
@ExtendWith(MockitoExtension.class)
class DataProviderControllerTest {

    @Mock
    private DataProviderService dataProviderService;

    private ObjectMapper objectMapper;
    private MeterRegistry meterRegistry;
    private DataProviderController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        meterRegistry = new SimpleMeterRegistry();
        controller = new DataProviderController(dataProviderService, objectMapper, meterRegistry);
    }

    @Test
    void shouldReturnComparisonResult() throws Exception {
        // Given
        ComparisonResultDto comparisonResult = ComparisonResultDto.builder()
                .firmCreates(1)
                .firmUpdates(2)
                .firmDeletes(0)
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
    void shouldStartAsyncSynchronization() {
        // Given
        PdaSyncResultDto result = PdaSyncResultDto.builder()
                .firmsCreated(1)
                .firmsUpdated(0)
                .firmsDisabled(0)
                .firmsReactivated(0)
                .officesCreated(1)
                .officesUpdated(0)
                .officesDeleted(0)
                .officesReactivated(0)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        CompletableFuture<PdaSyncResultDto> future = CompletableFuture.completedFuture(result);
        when(dataProviderService.synchronizeWithPdaAsync()).thenReturn(future);

        // When
        ResponseEntity<?> response = controller.synchronizeWithPda(true, 300);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isInstanceOf(SyncResponse.class);

        SyncResponse syncResponse = (SyncResponse) response.getBody();
        assertThat(syncResponse.started()).isTrue();
        assertThat(syncResponse.status()).contains("started");

        verify(dataProviderService).synchronizeWithPdaAsync();
    }

    @Test
    void shouldWaitForSynchronousSynchronization() throws Exception {
        // Given
        PdaSyncResultDto result = PdaSyncResultDto.builder()
                .firmsCreated(1)
                .firmsUpdated(0)
                .firmsDisabled(0)
                .firmsReactivated(0)
                .officesCreated(1)
                .officesUpdated(0)
                .officesDeleted(0)
                .officesReactivated(0)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        CompletableFuture<PdaSyncResultDto> future = CompletableFuture.completedFuture(result);
        when(dataProviderService.synchronizeWithPdaAsync()).thenReturn(future);

        // When
        ResponseEntity<?> response = controller.synchronizeWithPda(false, 300);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(PdaSyncResultDto.class);

        PdaSyncResultDto syncResult = (PdaSyncResultDto) response.getBody();
        assertThat(syncResult.getFirmsCreated()).isEqualTo(1);
        assertThat(syncResult.getOfficesCreated()).isEqualTo(1);

        verify(dataProviderService).synchronizeWithPdaAsync();
    }

    @Test
    void shouldReturnErrorWhenSyncHasErrors() {
        // Given
        List<String> errors = List.of("Error 1", "Error 2");
        PdaSyncResultDto result = PdaSyncResultDto.builder()
                .firmsCreated(0)
                .firmsUpdated(0)
                .firmsDisabled(0)
                .firmsReactivated(0)
                .officesCreated(0)
                .officesUpdated(0)
                .officesDeleted(0)
                .officesReactivated(0)
                .errors(errors)
                .warnings(new ArrayList<>())
                .build();

        CompletableFuture<PdaSyncResultDto> future = CompletableFuture.completedFuture(result);
        when(dataProviderService.synchronizeWithPdaAsync()).thenReturn(future);

        // When
        ResponseEntity<?> response = controller.synchronizeWithPda(false, 300);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(PdaSyncResultDto.class);
    }

    @Test
    void shouldHandleSyncException() {
        // Given
        when(dataProviderService.synchronizeWithPdaAsync())
                .thenThrow(new RuntimeException("Test error"));

        // When
        ResponseEntity<?> response = controller.synchronizeWithPda(true, 300);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(SyncResponse.class);

        SyncResponse syncResponse = (SyncResponse) response.getBody();
        assertThat(syncResponse.started()).isFalse();
        assertThat(syncResponse.status()).contains("failed");
    }

    @Test
    void shouldIncrementRequestCounter() {
        // Given
        PdaSyncResultDto result = PdaSyncResultDto.builder()
                .firmsCreated(1)
                .firmsUpdated(0)
                .firmsDisabled(0)
                .firmsReactivated(0)
                .officesCreated(1)
                .officesUpdated(0)
                .officesDeleted(0)
                .officesReactivated(0)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        CompletableFuture<PdaSyncResultDto> future = CompletableFuture.completedFuture(result);
        when(dataProviderService.synchronizeWithPdaAsync()).thenReturn(future);

        double beforeCount = meterRegistry.find("pda.sync.requests").counter().count();

        // When
        controller.synchronizeWithPda(true, 300);

        // Then
        double afterCount = meterRegistry.find("pda.sync.requests").counter().count();
        assertThat(afterCount).isEqualTo(beforeCount + 1);
    }

    @Test
    void shouldUseDefaultTimeoutValue() {
        // Given
        PdaSyncResultDto result = PdaSyncResultDto.builder()
                .firmsCreated(1)
                .firmsUpdated(0)
                .firmsDisabled(0)
                .firmsReactivated(0)
                .officesCreated(1)
                .officesUpdated(0)
                .officesDeleted(0)
                .officesReactivated(0)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        CompletableFuture<PdaSyncResultDto> future = CompletableFuture.completedFuture(result);
        when(dataProviderService.synchronizeWithPdaAsync()).thenReturn(future);

        // When - using default timeout (300 seconds)
        ResponseEntity<?> response = controller.synchronizeWithPda(false, 300);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldRecordMetricsForSuccessfulSync() throws Exception {
        // Given
        PdaSyncResultDto result = PdaSyncResultDto.builder()
                .firmsCreated(2)
                .firmsUpdated(1)
                .firmsDisabled(1)
                .firmsReactivated(1)
                .officesCreated(3)
                .officesUpdated(2)
                .officesDeleted(1)
                .officesReactivated(1)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        CompletableFuture<PdaSyncResultDto> future = CompletableFuture.completedFuture(result);
        when(dataProviderService.synchronizeWithPdaAsync()).thenReturn(future);

        // When
        ResponseEntity<?> response = controller.synchronizeWithPda(false, 300);

        // Then - wait a bit for async metric recording
        Thread.sleep(100);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meterRegistry.find("pda.sync.firms.created").counter()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.offices.created").counter()).isNotNull();
    }

    @Test
    void shouldHandleEmptyComparisonResult() throws Exception {
        // Given
        ComparisonResultDto comparisonResult = ComparisonResultDto.builder()
                .firmCreates(0)
                .firmUpdates(0)
                .firmDeletes(0)
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
