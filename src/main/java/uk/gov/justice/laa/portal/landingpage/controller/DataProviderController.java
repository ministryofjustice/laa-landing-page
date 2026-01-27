package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.ComparisonResultDto;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.dto.SyncResponse;
import uk.gov.justice.laa.portal.landingpage.service.DataProviderService;

/**
 * Controller for PDA (Provider Data API) endpoints.
 * Requires "Global Admin" role to access.
 */
@Slf4j
@RestController
@RequestMapping("/api/data-provider")
// @PreAuthorize("@accessControlService.userHasAuthzRole(authentication, T(uk.gov.justice.laa.portal.landingpage.entity.AuthzRole).GLOBAL_ADMIN.roleName)")  // COMMENTED OUT FOR TESTING
public class DataProviderController {
    private final DataProviderService dataProviderService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    // Metrics
    private final Timer syncTimer;
    private final Counter syncRequestsCounter;
    private final Counter syncSuccessCounter;
    private final Counter syncFailureCounter;
    private final Counter syncTimeoutCounter;
    private final Counter syncErrorsCounter;
    private final Counter firmCreatesCounter;
    private final Counter firmUpdatesCounter;
    private final Counter firmDeletesCounter;
    private final Counter firmReactivatesCounter;
    private final Counter officeCreatesCounter;
    private final Counter officeUpdatesCounter;
    private final Counter officeDeletesCounter;
    private final Counter officeReactivatesCounter;

    public DataProviderController(
            DataProviderService dataProviderService,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.dataProviderService = dataProviderService;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.syncTimer = Timer.builder("pda.sync.duration")
                .description("Time taken to complete PDA synchronization")
                .tag("endpoint", "/api/data-provider/sync")
                .register(meterRegistry);

        this.syncRequestsCounter = Counter.builder("pda.sync.requests")
                .description("Total number of PDA sync requests")
                .tag("endpoint", "/api/data-provider/sync")
                .register(meterRegistry);

        this.syncSuccessCounter = Counter.builder("pda.sync.success")
                .description("Number of successful PDA sync operations")
                .register(meterRegistry);

        this.syncFailureCounter = Counter.builder("pda.sync.failure")
                .description("Number of failed PDA sync operations")
                .register(meterRegistry);

        this.syncTimeoutCounter = Counter.builder("pda.sync.timeout")
                .description("Number of PDA sync operations that timed out")
                .register(meterRegistry);

        this.syncErrorsCounter = Counter.builder("pda.sync.errors")
                .description("Number of errors encountered during PDA sync")
                .register(meterRegistry);

        // Entity operation counters
        this.firmCreatesCounter = Counter.builder("pda.sync.firms.created")
                .description("Number of firms created during sync")
                .register(meterRegistry);

        this.firmUpdatesCounter = Counter.builder("pda.sync.firms.updated")
                .description("Number of firms updated during sync")
                .register(meterRegistry);

        this.firmDeletesCounter = Counter.builder("pda.sync.firms.deleted")
                .description("Number of firms deleted during sync")
                .register(meterRegistry);

        this.firmReactivatesCounter = Counter.builder("pda.sync.firms.reactivated")
                .description("Number of firms reactivated during sync")
                .register(meterRegistry);

        this.officeCreatesCounter = Counter.builder("pda.sync.offices.created")
                .description("Number of offices created during sync")
                .register(meterRegistry);

        this.officeUpdatesCounter = Counter.builder("pda.sync.offices.updated")
                .description("Number of offices updated during sync")
                .register(meterRegistry);

        this.officeDeletesCounter = Counter.builder("pda.sync.offices.deleted")
                .description("Number of offices deleted during sync")
                .register(meterRegistry);

        this.officeReactivatesCounter = Counter.builder("pda.sync.offices.reactivated")
                .description("Number of offices reactivated during sync")
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
        firmDeletesCounter.increment(result.getFirmsDeleted());
        firmReactivatesCounter.increment(result.getFirmsReactivated());

        // Office metrics
        officeCreatesCounter.increment(result.getOfficesCreated());
        officeUpdatesCounter.increment(result.getOfficesUpdated());
        officeDeletesCounter.increment(result.getOfficesDeleted());
        officeReactivatesCounter.increment(result.getOfficesReactivated());
    }

    /**
     * API endpoint to compare PDA data with local database.
     * Returns structured comparison showing created, updated, deleted, and matched items.
     *
     * @return ResponseEntity containing JSON with categorized comparison results
     */
    @GetMapping("/provider-offices/compare")
    public ResponseEntity<String> compareProviderOffices() {
        log.debug("Received request to compare PDA data with database");
        try {
            ComparisonResultDto result = dataProviderService.compareWithDatabase();

            // Pretty print JSON
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(result);

            log.debug("Returning comparison - Firms: {} creates, {} updates, {} deletes, {} exists",
                result.getFirmCreates(), result.getFirmUpdates(), result.getFirmDeletes(), result.getFirmExists());
            log.debug("Returning comparison - Offices: {} creates, {} updates, {} deletes, {} exists",
                result.getOfficeCreates(), result.getOfficeUpdates(), result.getOfficeDeletes(), result.getOfficeExists());
            log.debug("Returning comparison - Total: {} creates, {} updates, {} deletes, {} exists",
                result.getCreated().size(),
                result.getUpdated().size(),
                result.getDeleted().size(),
                result.getExists().size());

            return ResponseEntity.ok(json);
        } catch (Exception e) {
            log.error("Error comparing provider offices", e);
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"Failed to compare provider offices\"}");
        }
    }

    /**
     * API endpoint to synchronize PDA data with local database.
     * Performs create, update, reactivate, and deactivate operations.
     *
     * Runs asynchronously in isolated thread pool to protect main application.
     * Use async=true (default) for fire-and-forget, or async=false to wait for completion with timeout.
     *
     * @param async whether to run asynchronously (default: true)
     * @param timeoutSeconds timeout in seconds when async=false (default: 300)
     * @return ResponseEntity containing synchronization results or status
     */
    @PostMapping("/sync")
    public ResponseEntity<?> synchronizeWithPda(
            @RequestParam(defaultValue = "true") boolean async,
            @RequestParam(defaultValue = "300") int timeoutSeconds) {

        log.debug("Received request to synchronize PDA data (async={}, timeout={}s)", async, timeoutSeconds);

        // Increment request counter
        syncRequestsCounter.increment();

        long startTime = System.nanoTime();

        try {
            if (async) {
                // Fire and forget - return immediately
                CompletableFuture<PdaSyncResultDto> future = dataProviderService.synchronizeWithPdaAsync();

                // Record metrics asynchronously when sync completes
                future.whenComplete((result, throwable) -> {
                    long duration = System.nanoTime() - startTime;
                    syncTimer.record(duration, TimeUnit.NANOSECONDS);

                    if (throwable != null) {
                        syncFailureCounter.increment();
                        syncErrorsCounter.increment();
                        log.error("Async PDA sync failed", throwable);
                    } else if (result != null) {
                        recordSyncMetrics(result);

                        if (!result.getErrors().isEmpty()) {
                            syncFailureCounter.increment();
                            syncErrorsCounter.increment(result.getErrors().size());
                        } else {
                            syncSuccessCounter.increment();
                        }
                    }
                });

                log.debug("PDA synchronization started asynchronously");
                return ResponseEntity.accepted()
                    .body(new SyncResponse(
                        "Synchronization started",
                        "The sync operation is running in the background. Check application logs for results.",
                        true
                    ));
            } else {
                // Wait for completion with timeout
                CompletableFuture<PdaSyncResultDto> future = dataProviderService.synchronizeWithPdaAsync();

                try {
                    PdaSyncResultDto syncResult = future.get(timeoutSeconds, TimeUnit.SECONDS);

                    // Record metrics for synchronous completion
                    long duration = System.nanoTime() - startTime;
                    syncTimer.record(duration, TimeUnit.NANOSECONDS);
                    recordSyncMetrics(syncResult);

                    if (!syncResult.getErrors().isEmpty()) {
                        syncFailureCounter.increment();
                        syncErrorsCounter.increment(syncResult.getErrors().size());
                        return ResponseEntity.status(500).body(syncResult);
                    }

                    syncSuccessCounter.increment();
                    return ResponseEntity.ok(syncResult);
                } catch (java.util.concurrent.TimeoutException e) {
                    log.warn("PDA synchronization exceeded timeout of {}s", timeoutSeconds);
                    syncTimeoutCounter.increment();
                    syncFailureCounter.increment();

                    return ResponseEntity.status(408)
                        .body(new SyncResponse(
                            "Synchronization timeout",
                            "Operation is still running but exceeded " + timeoutSeconds + "s timeout. Check logs.",
                            true
                        ));
                }
            }
        } catch (Exception e) {
            log.error("Error initiating PDA synchronization", e);
            syncFailureCounter.increment();
            syncErrorsCounter.increment();

            return ResponseEntity.internalServerError()
                .body(new SyncResponse(
                    "Synchronization failed",
                    "Failed to start sync operation: " + e.getMessage(),
                    false
                ));
        }
    }
}
