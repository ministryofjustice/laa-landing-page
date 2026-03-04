package uk.gov.justice.laa.portal.landingpage.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.test.util.ReflectionTestUtils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;

/**
 * Tests for PdaSyncScheduler.
 */
@ExtendWith(MockitoExtension.class)
class PdaSyncSchedulerTest {

    @Mock
    private DataProviderService dataProviderService;

    @Mock
    private ApplicationReadyEvent applicationReadyEvent;

    private MeterRegistry meterRegistry;
    private PdaSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        scheduler = new PdaSyncScheduler(dataProviderService, meterRegistry);
        ReflectionTestUtils.setField(scheduler, "runOnStartup", false);
    }

    @Test
    void shouldCreateSchedulerWithMetrics() {
        // Then
        assertThat(meterRegistry.find("pda.sync.duration").timer()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.requests").counter()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.success").counter()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.failure").counter()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.errors").counter()).isNotNull();
    }

    @Test
    void shouldIncrementRequestCounterOnScheduledSync() throws Exception {
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

        Counter requestsCounter = meterRegistry.find("pda.sync.requests").counter();
        double beforeCount = requestsCounter != null ? requestsCounter.count() : 0;

        // When
        scheduler.scheduledSync();

        // Then
        Counter afterCounter = meterRegistry.find("pda.sync.requests").counter();
        assertThat(afterCounter.count()).isEqualTo(beforeCount + 1);
    }

    @Test
    void shouldIncrementSuccessCounterOnSuccessfulSync() throws Exception {
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

        Counter successCounter = meterRegistry.find("pda.sync.success").counter();
        double beforeCount = successCounter != null ? successCounter.count() : 0;

        // When
        scheduler.scheduledSync();

        // Then
        Counter afterCounter = meterRegistry.find("pda.sync.success").counter();
        assertThat(afterCounter.count()).isEqualTo(beforeCount + 1);
    }

    @Test
    void shouldIncrementFailureCounterOnSyncWithErrors() throws Exception {
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

        Counter failureCounter = meterRegistry.find("pda.sync.failure").counter();
        double beforeCount = failureCounter != null ? failureCounter.count() : 0;

        // When
        scheduler.scheduledSync();

        // Then
        Counter afterCounter = meterRegistry.find("pda.sync.failure").counter();
        assertThat(afterCounter.count()).isEqualTo(beforeCount + 1);
    }

    @Test
    void shouldRecordSyncDuration() throws Exception {
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

        Timer timer = meterRegistry.find("pda.sync.duration").timer();
        long beforeCount = timer != null ? timer.count() : 0;

        // When
        scheduler.scheduledSync();

        // Then
        Timer afterTimer = meterRegistry.find("pda.sync.duration").timer();
        assertThat(afterTimer.count()).isEqualTo(beforeCount + 1);
    }

    @Test
    void shouldRecordFirmMetrics() throws Exception {
        // Given
        PdaSyncResultDto result = PdaSyncResultDto.builder()
                .firmsCreated(2)
                .firmsUpdated(3)
                .firmsDisabled(1)
                .firmsReactivated(1)
                .officesCreated(0)
                .officesUpdated(0)
                .officesDeleted(0)
                .officesReactivated(0)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        CompletableFuture<PdaSyncResultDto> future = CompletableFuture.completedFuture(result);
        when(dataProviderService.synchronizeWithPdaAsync()).thenReturn(future);

        // When
        scheduler.scheduledSync();

        // Then
        assertThat(meterRegistry.find("pda.sync.firms.created").counter().count()).isEqualTo(2.0);
        assertThat(meterRegistry.find("pda.sync.firms.updated").counter().count()).isEqualTo(3.0);
        assertThat(meterRegistry.find("pda.sync.firms.disabled").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("pda.sync.firms.reactivated").counter().count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordOfficeMetrics() throws Exception {
        // Given
        PdaSyncResultDto result = PdaSyncResultDto.builder()
                .firmsCreated(0)
                .firmsUpdated(0)
                .firmsDisabled(0)
                .firmsReactivated(0)
                .officesCreated(4)
                .officesUpdated(2)
                .officesDeleted(1)
                .officesReactivated(1)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        CompletableFuture<PdaSyncResultDto> future = CompletableFuture.completedFuture(result);
        when(dataProviderService.synchronizeWithPdaAsync()).thenReturn(future);

        // When
        scheduler.scheduledSync();

        // Then
        assertThat(meterRegistry.find("pda.sync.offices.created").counter().count()).isEqualTo(4.0);
        assertThat(meterRegistry.find("pda.sync.offices.updated").counter().count()).isEqualTo(2.0);
        assertThat(meterRegistry.find("pda.sync.offices.deleted").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("pda.sync.offices.reactivated").counter().count()).isEqualTo(1.0);
    }

    @Test
    void shouldHandleExceptionDuringSync() throws Exception {
        // Given
        when(dataProviderService.synchronizeWithPdaAsync())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test error")));

        Counter failureCounter = meterRegistry.find("pda.sync.failure").counter();
        double beforeCount = failureCounter != null ? failureCounter.count() : 0;

        // When
        scheduler.scheduledSync();

        // Then
        Counter afterCounter = meterRegistry.find("pda.sync.failure").counter();
        assertThat(afterCounter.count()).isEqualTo(beforeCount + 1);
    }

    @Test
    void shouldNotRunOnStartupWhenDisabled() {
        // Given
        ReflectionTestUtils.setField(scheduler, "runOnStartup", false);

        // When
        scheduler.onApplicationReady();

        // Then
        verifyNoInteractions(dataProviderService);
    }

    @Test
    void shouldRunOnStartupWhenEnabled() throws Exception {
        // Given
        ReflectionTestUtils.setField(scheduler, "runOnStartup", true);

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
        scheduler.onApplicationReady();

        // Then
        verify(dataProviderService, times(1)).synchronizeWithPdaAsync();
    }

    @Test
    void shouldHandleWarningsDuringSync() throws Exception {
        // Given
        List<String> warnings = List.of("Warning 1", "Warning 2");
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
                .warnings(warnings)
                .build();

        CompletableFuture<PdaSyncResultDto> future = CompletableFuture.completedFuture(result);
        when(dataProviderService.synchronizeWithPdaAsync()).thenReturn(future);

        Counter successCounter = meterRegistry.find("pda.sync.success").counter();
        double beforeCount = successCounter != null ? successCounter.count() : 0;

        // When
        scheduler.scheduledSync();

        // Then
        Counter afterCounter = meterRegistry.find("pda.sync.success").counter();
        // Warnings don't prevent success
        assertThat(afterCounter.count()).isEqualTo(beforeCount + 1);
    }

    @Test
    void shouldUpdateGaugesOnSuccessfulSync() throws Exception {
        // Given
        PdaSyncResultDto result = PdaSyncResultDto.builder()
                .firmsCreated(5)
                .firmsUpdated(10)
                .firmsDisabled(2)
                .firmsReactivated(1)
                .officesCreated(15)
                .officesUpdated(20)
                .officesDeleted(3)
                .officesReactivated(2)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        CompletableFuture<PdaSyncResultDto> future = CompletableFuture.completedFuture(result);
        when(dataProviderService.synchronizeWithPdaAsync()).thenReturn(future);

        // When
        final long beforeSyncTimestamp = System.currentTimeMillis() / 1000;
        scheduler.scheduledSync();
        final long afterSyncTimestamp = System.currentTimeMillis() / 1000;

        // Then - verify all gauge values
        assertThat(meterRegistry.find("pda.sync.last.status").gauge().value()).isEqualTo(1.0); // Success
        assertThat(meterRegistry.find("pda.sync.last.firms.created").gauge().value()).isEqualTo(5.0);
        assertThat(meterRegistry.find("pda.sync.last.firms.updated").gauge().value()).isEqualTo(10.0);
        assertThat(meterRegistry.find("pda.sync.last.firms.disabled").gauge().value()).isEqualTo(2.0);
        assertThat(meterRegistry.find("pda.sync.last.offices.created").gauge().value()).isEqualTo(15.0);
        assertThat(meterRegistry.find("pda.sync.last.offices.updated").gauge().value()).isEqualTo(20.0);
        assertThat(meterRegistry.find("pda.sync.last.offices.deleted").gauge().value()).isEqualTo(3.0);
        assertThat(meterRegistry.find("pda.sync.last.errors").gauge().value()).isEqualTo(0.0);

        // Verify duration is non-negative (may be 0 for fast operations)
        assertThat(meterRegistry.find("pda.sync.last.duration.seconds").gauge().value()).isGreaterThanOrEqualTo(0.0);

        // Verify timestamps are in reasonable range (within the test execution window)
        double lastSyncTimestamp = meterRegistry.find("pda.sync.last.timestamp").gauge().value();
        assertThat(lastSyncTimestamp).isBetween((double) beforeSyncTimestamp, (double) afterSyncTimestamp);

        double lastSuccessTimestamp = meterRegistry.find("pda.sync.last.success.timestamp").gauge().value();
        assertThat(lastSuccessTimestamp).isBetween((double) beforeSyncTimestamp, (double) afterSyncTimestamp);
    }

    @Test
    void shouldUpdateGaugesOnSyncWithErrors() throws Exception {
        // Given
        List<String> errors = List.of("Error 1", "Error 2", "Error 3");
        PdaSyncResultDto result = PdaSyncResultDto.builder()
                .firmsCreated(2)
                .firmsUpdated(3)
                .firmsDisabled(1)
                .firmsReactivated(0)
                .officesCreated(4)
                .officesUpdated(5)
                .officesDeleted(2)
                .officesReactivated(0)
                .errors(errors)
                .warnings(new ArrayList<>())
                .build();

        CompletableFuture<PdaSyncResultDto> future = CompletableFuture.completedFuture(result);
        when(dataProviderService.synchronizeWithPdaAsync()).thenReturn(future);

        // When
        final long beforeSyncTimestamp = System.currentTimeMillis() / 1000;
        scheduler.scheduledSync();
        final long afterSyncTimestamp = System.currentTimeMillis() / 1000;

        // Then - verify gauge values reflect failure state
        assertThat(meterRegistry.find("pda.sync.last.status").gauge().value()).isEqualTo(-1.0); // Failure
        assertThat(meterRegistry.find("pda.sync.last.firms.created").gauge().value()).isEqualTo(2.0);
        assertThat(meterRegistry.find("pda.sync.last.firms.updated").gauge().value()).isEqualTo(3.0);
        assertThat(meterRegistry.find("pda.sync.last.firms.disabled").gauge().value()).isEqualTo(1.0);
        assertThat(meterRegistry.find("pda.sync.last.offices.created").gauge().value()).isEqualTo(4.0);
        assertThat(meterRegistry.find("pda.sync.last.offices.updated").gauge().value()).isEqualTo(5.0);
        assertThat(meterRegistry.find("pda.sync.last.offices.deleted").gauge().value()).isEqualTo(2.0);
        assertThat(meterRegistry.find("pda.sync.last.errors").gauge().value()).isEqualTo(3.0);

        // Verify duration is non-negative (may be 0 for fast operations)
        assertThat(meterRegistry.find("pda.sync.last.duration.seconds").gauge().value()).isGreaterThanOrEqualTo(0.0);

        // Verify lastSyncTimestamp is updated
        double lastSyncTimestamp = meterRegistry.find("pda.sync.last.timestamp").gauge().value();
        assertThat(lastSyncTimestamp).isBetween((double) beforeSyncTimestamp, (double) afterSyncTimestamp);

        // Verify lastSuccessTimestamp is NOT updated (remains 0 since this is a failure)
        double lastSuccessTimestamp = meterRegistry.find("pda.sync.last.success.timestamp").gauge().value();
        assertThat(lastSuccessTimestamp).isEqualTo(0.0);
    }

    @Test
    void shouldUpdateGaugesOnException() throws Exception {
        // Given
        when(dataProviderService.synchronizeWithPdaAsync())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test error")));

        // When
        final long beforeSyncTimestamp = System.currentTimeMillis() / 1000;
        scheduler.scheduledSync();
        final long afterSyncTimestamp = System.currentTimeMillis() / 1000;

        // Then - verify gauge values reflect failure state
        assertThat(meterRegistry.find("pda.sync.last.status").gauge().value()).isEqualTo(-1.0); // Failure
        assertThat(meterRegistry.find("pda.sync.last.errors").gauge().value()).isEqualTo(1.0);

        // Verify duration is non-negative (recorded even on exception; may be 0 for fast operations)
        assertThat(meterRegistry.find("pda.sync.last.duration.seconds").gauge().value()).isGreaterThanOrEqualTo(0.0);

        // Verify lastSyncTimestamp is updated
        double lastSyncTimestamp = meterRegistry.find("pda.sync.last.timestamp").gauge().value();
        assertThat(lastSyncTimestamp).isBetween((double) beforeSyncTimestamp, (double) afterSyncTimestamp);

        // Verify lastSuccessTimestamp is NOT updated (remains 0)
        double lastSuccessTimestamp = meterRegistry.find("pda.sync.last.success.timestamp").gauge().value();
        assertThat(lastSuccessTimestamp).isEqualTo(0.0);
    }

    @Test
    void shouldUpdateGaugesOnTimeout() throws Exception {
        // Given - set a very short timeout
        ReflectionTestUtils.setField(scheduler, "timeoutMinutes", 0L); // 0 minutes = immediate timeout

        // Create a future that never completes
        CompletableFuture<PdaSyncResultDto> future = new CompletableFuture<>();
        when(dataProviderService.synchronizeWithPdaAsync()).thenReturn(future);

        // When
        final long beforeSyncTimestamp = System.currentTimeMillis() / 1000;
        scheduler.scheduledSync();
        final long afterSyncTimestamp = System.currentTimeMillis() / 1000;

        // Then - verify gauge values reflect failure state
        assertThat(meterRegistry.find("pda.sync.last.status").gauge().value()).isEqualTo(-1.0); // Failure
        assertThat(meterRegistry.find("pda.sync.last.errors").gauge().value()).isEqualTo(1.0);

        // Verify duration is positive (recorded even on timeout)
        assertThat(meterRegistry.find("pda.sync.last.duration.seconds").gauge().value()).isGreaterThanOrEqualTo(0.0);

        // Verify lastSyncTimestamp is updated
        double lastSyncTimestamp = meterRegistry.find("pda.sync.last.timestamp").gauge().value();
        assertThat(lastSyncTimestamp).isBetween((double) beforeSyncTimestamp, (double) afterSyncTimestamp);

        // Verify lastSuccessTimestamp is NOT updated (remains 0)
        double lastSuccessTimestamp = meterRegistry.find("pda.sync.last.success.timestamp").gauge().value();
        assertThat(lastSuccessTimestamp).isEqualTo(0.0);
    }

    @Test
    void shouldResetGaugesAtStartOfSyncAttempt() throws Exception {
        // Given - first run a successful sync to populate gauges
        PdaSyncResultDto successResult = PdaSyncResultDto.builder()
                .firmsCreated(10)
                .firmsUpdated(20)
                .firmsDisabled(5)
                .firmsReactivated(1)
                .officesCreated(30)
                .officesUpdated(40)
                .officesDeleted(15)
                .officesReactivated(2)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        when(dataProviderService.synchronizeWithPdaAsync())
                .thenReturn(CompletableFuture.completedFuture(successResult));

        scheduler.scheduledSync();

        // Verify gauges are populated from first sync
        assertThat(meterRegistry.find("pda.sync.last.status").gauge().value()).isEqualTo(1.0);
        assertThat(meterRegistry.find("pda.sync.last.firms.created").gauge().value()).isEqualTo(10.0);
        assertThat(meterRegistry.find("pda.sync.last.offices.deleted").gauge().value()).isEqualTo(15.0);

        // Given - second sync that will fail (to test reset behavior)
        when(dataProviderService.synchronizeWithPdaAsync())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test error")));

        // When - run second sync
        scheduler.scheduledSync();

        // Then - verify gauges were reset at start of attempt
        // Status should be -1 (failure), not 1 from previous successful run
        assertThat(meterRegistry.find("pda.sync.last.status").gauge().value()).isEqualTo(-1.0);

        // All result gauges should reflect the reset behavior (0 for failed sync without result)
        assertThat(meterRegistry.find("pda.sync.last.firms.created").gauge().value()).isEqualTo(0.0);
        assertThat(meterRegistry.find("pda.sync.last.firms.updated").gauge().value()).isEqualTo(0.0);
        assertThat(meterRegistry.find("pda.sync.last.firms.disabled").gauge().value()).isEqualTo(0.0);
        assertThat(meterRegistry.find("pda.sync.last.offices.created").gauge().value()).isEqualTo(0.0);
        assertThat(meterRegistry.find("pda.sync.last.offices.updated").gauge().value()).isEqualTo(0.0);
        assertThat(meterRegistry.find("pda.sync.last.offices.deleted").gauge().value()).isEqualTo(0.0);

        // Error count should be 1 from the exception
        assertThat(meterRegistry.find("pda.sync.last.errors").gauge().value()).isEqualTo(1.0);
    }

    @Test
    void shouldOnlyUpdateSuccessTimestampOnSuccess() throws Exception {
        // Given - run a successful sync first
        PdaSyncResultDto successResult = PdaSyncResultDto.builder()
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

        when(dataProviderService.synchronizeWithPdaAsync())
                .thenReturn(CompletableFuture.completedFuture(successResult));

        scheduler.scheduledSync();

        // Capture the success timestamp from first sync
        double firstSuccessTimestamp = meterRegistry.find("pda.sync.last.success.timestamp").gauge().value();
        assertThat(firstSuccessTimestamp).isGreaterThan(0.0);

        // Add a small delay to ensure timestamps are different
        Thread.sleep(100);

        // Given - run a failed sync
        List<String> errors = List.of("Error 1");
        PdaSyncResultDto failureResult = PdaSyncResultDto.builder()
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

        when(dataProviderService.synchronizeWithPdaAsync())
                .thenReturn(CompletableFuture.completedFuture(failureResult));

        long beforeSecondSync = System.currentTimeMillis() / 1000;

        // When - run second (failed) sync
        scheduler.scheduledSync();

        long afterSecondSync = System.currentTimeMillis() / 1000;

        // Then - verify lastSyncTimestamp was updated (reflecting the failed attempt)
        double lastSyncTimestamp = meterRegistry.find("pda.sync.last.timestamp").gauge().value();
        assertThat(lastSyncTimestamp).isBetween((double) beforeSecondSync, (double) afterSecondSync);

        // But lastSuccessTimestamp should still be from the first successful sync
        double currentSuccessTimestamp = meterRegistry.find("pda.sync.last.success.timestamp").gauge().value();
        assertThat(currentSuccessTimestamp).isEqualTo(firstSuccessTimestamp);
        assertThat(currentSuccessTimestamp).isLessThanOrEqualTo((double) beforeSecondSync);
    }

    @Test
    void shouldCreateAllGauges() {
        // Then - verify all expected gauges are registered
        assertThat(meterRegistry.find("pda.sync.last.timestamp").gauge()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.last.success.timestamp").gauge()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.last.status").gauge()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.last.duration.seconds").gauge()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.last.firms.created").gauge()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.last.firms.updated").gauge()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.last.firms.disabled").gauge()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.last.offices.created").gauge()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.last.offices.updated").gauge()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.last.offices.deleted").gauge()).isNotNull();
        assertThat(meterRegistry.find("pda.sync.last.errors").gauge()).isNotNull();
    }
}
