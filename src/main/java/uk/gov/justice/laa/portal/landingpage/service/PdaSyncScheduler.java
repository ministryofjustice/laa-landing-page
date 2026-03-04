package uk.gov.justice.laa.portal.landingpage.service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
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
 * - app.pda.sync.scheduler.timeout-minutes: Max time to wait for sync completion (default: 10 minutes)
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

    @Value("${app.pda.sync.scheduler.timeout-minutes:10}")
    private long timeoutMinutes;

    // Metrics
    private final Timer syncTimer;
    private final Counter syncRequestsCounter;
    private final Counter syncSuccessCounter;
    private final Counter syncFailureCounter;
    private final Counter syncErrorsCounter;
    private final Counter firmCreatesCounter;
    private final Counter firmUpdatesCounter;
    private final Counter firmDisablesCounter;
    private final Counter firmReactivatesCounter;
    private final Counter officeCreatesCounter;
    private final Counter officeUpdatesCounter;
    private final Counter officeDeletesCounter;
    private final Counter officeReactivatesCounter;

    // State tracking for gauges
    private volatile long lastSyncTimestamp = 0; // Timestamp of last sync attempt (start)
    private volatile long lastSuccessTimestamp = 0; // Timestamp of last successful sync (completion)
    private volatile long lastSyncStatus = 0; // 0 = unknown, 1 = success, -1 = failure
    private volatile long lastSyncDurationSeconds = 0;
    private volatile long lastSyncFirmsCreated = 0;
    private volatile long lastSyncFirmsUpdated = 0;
    private volatile long lastSyncFirmsDisabled = 0;
    private volatile long lastSyncOfficesCreated = 0;
    private volatile long lastSyncOfficesUpdated = 0;
    private volatile long lastSyncOfficesDeleted = 0;
    private volatile long lastSyncErrorCount = 0;

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

        this.firmDisablesCounter = Counter.builder("pda.sync.firms.disabled")
                .description("Number of firms disabled during sync")
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

        // Gauge metrics for current state
        Gauge.builder("pda.sync.last.timestamp", this, s -> s.lastSyncTimestamp)
                .description("Unix timestamp of last PDA sync attempt (start)")
                .tag("source", "scheduler")
                .register(meterRegistry);

        Gauge.builder("pda.sync.last.success.timestamp", this, s -> s.lastSuccessTimestamp)
                .description("Unix timestamp of last successful PDA sync (completion)")
                .tag("source", "scheduler")
                .register(meterRegistry);

        Gauge.builder("pda.sync.last.status", this, s -> s.lastSyncStatus)
                .description("Status of last PDA sync (1=success, -1=failure, 0=unknown)")
                .tag("source", "scheduler")
                .register(meterRegistry);

        Gauge.builder("pda.sync.last.duration.seconds", this, s -> s.lastSyncDurationSeconds)
                .description("Duration of last PDA sync in seconds")
                .tag("source", "scheduler")
                .register(meterRegistry);

        Gauge.builder("pda.sync.last.firms.created", this, s -> s.lastSyncFirmsCreated)
                .description("Number of firms created in last sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        Gauge.builder("pda.sync.last.firms.updated", this, s -> s.lastSyncFirmsUpdated)
                .description("Number of firms updated in last sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        Gauge.builder("pda.sync.last.firms.disabled", this, s -> s.lastSyncFirmsDisabled)
                .description("Number of firms disabled in last sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        Gauge.builder("pda.sync.last.offices.created", this, s -> s.lastSyncOfficesCreated)
                .description("Number of offices created in last sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        Gauge.builder("pda.sync.last.offices.updated", this, s -> s.lastSyncOfficesUpdated)
                .description("Number of offices updated in last sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        Gauge.builder("pda.sync.last.offices.deleted", this, s -> s.lastSyncOfficesDeleted)
                .description("Number of offices deleted in last sync")
                .tag("source", "scheduler")
                .register(meterRegistry);

        Gauge.builder("pda.sync.last.errors", this, s -> s.lastSyncErrorCount)
                .description("Number of errors in last sync")
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
        firmDisablesCounter.increment(result.getFirmsDisabled());
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
        lastSyncTimestamp = System.currentTimeMillis() / 1000; // Unix timestamp in seconds

        try {
            CompletableFuture<PdaSyncResultDto> future = dataProviderService.synchronizeWithPdaAsync();

            // Wait for completion with timeout to prevent indefinite blocking
            PdaSyncResultDto result = future.get(timeoutMinutes, TimeUnit.MINUTES);

            // Record timing metric
            long duration = System.nanoTime() - startTime;
            syncTimer.record(duration, TimeUnit.NANOSECONDS);

            // Record entity operation metrics
            recordSyncMetrics(result);

            // Update gauge state
            lastSyncDurationSeconds = Duration.ofNanos(duration).toSeconds();
            lastSyncFirmsCreated = result.getFirmsCreated();
            lastSyncFirmsUpdated = result.getFirmsUpdated();
            lastSyncFirmsDisabled = result.getFirmsDisabled();
            lastSyncOfficesCreated = result.getOfficesCreated();
            lastSyncOfficesUpdated = result.getOfficesUpdated();
            lastSyncOfficesDeleted = result.getOfficesDeleted();
            lastSyncErrorCount = result.getErrors().size();

            if (result.getErrors().isEmpty()) {
                syncSuccessCounter.increment();
                lastSyncStatus = 1; // Success
                lastSuccessTimestamp = System.currentTimeMillis() / 1000; // Unix timestamp of successful completion
                log.debug("Scheduled PDA sync completed successfully in {} seconds. Firms: {} created, {} updated, {} disabled, {} reactivated | Offices: {} created, {} updated, {} deleted",
                    Duration.ofNanos(duration).toSeconds(),
                    result.getFirmsCreated(), result.getFirmsUpdated(), result.getFirmsDisabled(), result.getFirmsReactivated(),
                    result.getOfficesCreated(), result.getOfficesUpdated(), result.getOfficesDeleted());
            } else {
                syncFailureCounter.increment();
                syncErrorsCounter.increment(result.getErrors().size());
                lastSyncStatus = -1; // Failure
                log.error("Scheduled PDA sync completed with {} errors in {} seconds",
                    result.getErrors().size(), Duration.ofNanos(duration).toSeconds());
                result.getErrors().forEach(error -> log.error("Sync error: {}", error));
            }

            if (!result.getWarnings().isEmpty()) {
                log.debug("Sync completed with {} warnings", result.getWarnings().size());
                result.getWarnings().forEach(warning -> log.debug("Sync warning: {}", warning));
            }

        } catch (TimeoutException e) {
            long duration = System.nanoTime() - startTime;
            syncTimer.record(duration, TimeUnit.NANOSECONDS);
            syncFailureCounter.increment();
            syncErrorsCounter.increment();
            lastSyncStatus = -1; // Failure
            lastSyncDurationSeconds = Duration.ofNanos(duration).toSeconds();
            lastSyncErrorCount = 1;
            log.error("Scheduled PDA sync timed out after {} minutes", timeoutMinutes, e);
        } catch (Exception e) {
            long duration = System.nanoTime() - startTime;
            syncTimer.record(duration, TimeUnit.NANOSECONDS);
            syncFailureCounter.increment();
            syncErrorsCounter.increment();
            lastSyncStatus = -1; // Failure
            lastSyncDurationSeconds = Duration.ofNanos(duration).toSeconds();
            lastSyncErrorCount = 1;
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
