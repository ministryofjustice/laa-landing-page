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
}
