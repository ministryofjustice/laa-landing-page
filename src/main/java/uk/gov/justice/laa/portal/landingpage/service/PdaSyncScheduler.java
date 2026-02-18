package uk.gov.justice.laa.portal.landingpage.service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;

/**
 * Scheduler service for automatic PDA synchronization.
 * Runs on a configurable schedule to keep firm/office data in sync with Provider Data API.
 *
 * Configuration:
 * - app.pda.sync.scheduler.enabled: Enable/disable automatic sync (default: false)
 * - app.pda.sync.scheduler.cron: Cron expression for sync schedule (default: daily at 7 AM)
 * - app.pda.sync.scheduler.run-on-startup: Run sync immediately on application startup (default: false)
 *
 * The scheduler can be disabled via configuration while keeping the manual /sync endpoint available.
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "app.pda.sync.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class PdaSyncScheduler {

    private final DataProviderService dataProviderService;

    @Value("${app.pda.sync.scheduler.run-on-startup:false}")
    private boolean runOnStartup;

    // Metrics
    private final Timer syncTimer;
    private final Counter syncRequestsCounter;
    private final Counter syncSuccessCounter;
    private final Counter syncFailureCounter;
    private final Counter syncErrorsCounter;
    private final Counter firmCreatesCounter;
    private final Counter firmUpdatesCounter;
    private final Counter firmDeletesCounter;
    private final Counter firmReactivatesCounter;
    private final Counter officeCreatesCounter;
    private final Counter officeUpdatesCounter;
    private final Counter officeDeletesCounter;
    private final Counter officeReactivatesCounter;

    public PdaSyncScheduler(
            DataProviderService dataProviderService,
            MeterRegistry meterRegistry) {
        this.dataProviderService = dataProviderService;

        // Initialize metrics
        this.syncTimer = Timer.builder("pda.sync.duration")
                .description("Time taken to complete PDA synchronization")
                .tag("source", "scheduler")
                .register(meterRegistry);

        this.syncRequestsCounter = Counter.builder("pda.sync.requests")
                .description("Total number of PDA sync requests")
                .tag("source", "scheduler")
                .register(meterRegistry);

        this.syncSuccessCounter = Counter.builder("pda.sync.success")
                .description("Number of successful PDA sync operations")
                .tag("source", "scheduler")
                .register(meterRegistry);

        this.syncFailureCounter = Counter.builder("pda.sync.failure")
                .description("Number of failed PDA sync operations")
                .tag("source", "scheduler")
                .register(meterRegistry);

        this.syncErrorsCounter = Counter.builder("pda.sync.errors")
                .description("Number of errors encountered during PDA sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        // Entity operation counters
        this.firmCreatesCounter = Counter.builder("pda.sync.firms.created")
                .description("Number of firms created during sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        this.firmUpdatesCounter = Counter.builder("pda.sync.firms.updated")
                .description("Number of firms updated during sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        this.firmDeletesCounter = Counter.builder("pda.sync.firms.deleted")
                .description("Number of firms deleted during sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        this.firmReactivatesCounter = Counter.builder("pda.sync.firms.reactivated")
                .description("Number of firms reactivated during sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        this.officeCreatesCounter = Counter.builder("pda.sync.offices.created")
                .description("Number of offices created during sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        this.officeUpdatesCounter = Counter.builder("pda.sync.offices.updated")
                .description("Number of offices updated during sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        this.officeDeletesCounter = Counter.builder("pda.sync.offices.deleted")
                .description("Number of offices deleted during sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        this.officeReactivatesCounter = Counter.builder("pda.sync.offices.reactivated")
                .description("Number of offices reactivated during sync")
                .tag("source", "scheduler")
                .register(meterRegistry);
    }

    /**
     * Records detailed metrics from a PDA sync result.
     *
     * @param result the sync result containing operation counts
     */
    private void recordSyncMetrics(PdaSyncResultDto result) {
        // Firm metrics
        firmCreatesCounter.increment(result.getFirmsCreated());
        firmUpdatesCounter.increment(result.getFirmsUpdated());
        firmDeletesCounter.increment(result.getFirmsDisabled());
        firmReactivatesCounter.increment(result.getFirmsReactivated());

        // Office metrics
        officeCreatesCounter.increment(result.getOfficesCreated());
        officeUpdatesCounter.increment(result.getOfficesUpdated());
        officeDeletesCounter.increment(result.getOfficesDeleted());
        officeReactivatesCounter.increment(result.getOfficesReactivated());
    }

    /**
     * Scheduled PDA synchronization task.
     * Runs according to the cron expression defined in app.pda.sync.scheduler.cron
     * Default: 0 0 7 * * * (daily at 7:00 AM)
     */
    @Scheduled(cron = "${app.pda.sync.scheduler.cron:0 0 7 * * *}")
    public void scheduledSync() {
        log.debug("Starting scheduled PDA synchronization");

        // Increment request counter
        syncRequestsCounter.increment();

        long startTime = System.nanoTime();

        try {
            CompletableFuture<PdaSyncResultDto> future = dataProviderService.synchronizeWithPdaAsync();

            // Wait for completion and get result
            PdaSyncResultDto result = future.get();

            // Record timing metric
            long duration = System.nanoTime() - startTime;
            syncTimer.record(duration, TimeUnit.NANOSECONDS);

            // Record entity operation metrics
            recordSyncMetrics(result);

            if (result.getErrors().isEmpty()) {
                syncSuccessCounter.increment();
                log.debug("Scheduled PDA sync completed successfully in {} seconds. Firms: {} created, {} updated, {} disabled, {} reactivated | Offices: {} created, {} updated, {} deleted",
                    Duration.ofNanos(duration).toSeconds(),
                    result.getFirmsCreated(), result.getFirmsUpdated(), result.getFirmsDisabled(), result.getFirmsReactivated(),
                    result.getOfficesCreated(), result.getOfficesUpdated(), result.getOfficesDeleted());
            } else {
                syncFailureCounter.increment();
                syncErrorsCounter.increment(result.getErrors().size());
                log.error("Scheduled PDA sync completed with {} errors in {} seconds",
                    result.getErrors().size(), Duration.ofNanos(duration).toSeconds());
                result.getErrors().forEach(error -> log.error("Sync error: {}", error));
            }

            if (!result.getWarnings().isEmpty()) {
                log.debug("Sync completed with {} warnings", result.getWarnings().size());
                result.getWarnings().forEach(warning -> log.debug("Sync warning: {}", warning));
            }

        } catch (Exception e) {
            long duration = System.nanoTime() - startTime;
            syncTimer.record(duration, TimeUnit.NANOSECONDS);
            syncFailureCounter.increment();
            syncErrorsCounter.increment();
            log.error("Scheduled PDA sync failed after {} seconds", Duration.ofNanos(duration).toSeconds(), e);
        }
    }

    /**
     * Run PDA sync on application startup if configured to do so.
     * This allows immediate sync on deployment, then reverts to scheduled cron-based sync.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (runOnStartup) {
            log.info("Running PDA sync on startup as configured");
            scheduledSync();
        }
    }
}
