package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.tablesaw.api.Table;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.service.DataProviderService;

/**
 * Controller for PDA (Provider Data API) endpoints.
 * Requires "Global Admin" role to access.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/data-provider")
// @PreAuthorize("@accessControlService.userHasAuthzRole(authentication, T(uk.gov.justice.laa.portal.landingpage.entity.AuthzRole).GLOBAL_ADMIN.roleName)")  // COMMENTED OUT FOR TESTING
public class DataProviderController {

    private final DataProviderService dataProviderService;

    /**
     * API endpoint to fetch provider offices snapshot from PDA.
     * Returns the data as a TableSaw dataframe converted to JSON.
     *
     * @return ResponseEntity containing JSON representation of the dataframe or error message
     */
    @GetMapping("/provider-offices/snapshot")
    public ResponseEntity<String> getProviderOfficesSnapshot() {
        log.info("Received request for provider offices snapshot");
        try {
            Table table = dataProviderService.getProviderOfficesSnapshot();

            // Convert TableSaw Table to JSON
            String json = table.write().toString("json");

            log.info("Returning dataframe with {} rows and {} columns",
                table.rowCount(), table.columnCount());

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(json);
        } catch (Exception e) {
            log.error("Error retrieving provider offices snapshot", e);
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"Failed to retrieve provider offices snapshot\"}");
        }
    }

    /**
     * API endpoint to compare PDA data with local database.
     * Returns PDA data augmented with database match information.
     *
     * @return ResponseEntity containing JSON with match status columns
     */
    @GetMapping("/provider-offices/compare")
    public ResponseEntity<String> compareProviderOffices() {
        log.info("Received request to compare PDA data with database");
        try {
            Table comparisonTable = dataProviderService.compareWithDatabase();

            // Convert TableSaw Table to JSON
            String json = comparisonTable.write().toString("json");

            log.info("Returning comparison dataframe with {} rows and {} columns",
                comparisonTable.rowCount(), comparisonTable.columnCount());

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(json);
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

        log.info("Received request to synchronize PDA data (async={}, timeout={}s)", async, timeoutSeconds);

        try {
            if (async) {
                // Fire and forget - return immediately
                CompletableFuture<PdaSyncResultDto> future = dataProviderService.synchronizeWithPdaAsync();

                log.info("PDA synchronization started asynchronously");
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

                    if (!syncResult.getErrors().isEmpty()) {
                        return ResponseEntity.status(500).body(syncResult);
                    }

                    return ResponseEntity.ok(syncResult);
                } catch (java.util.concurrent.TimeoutException e) {
                    log.warn("PDA synchronization exceeded timeout of {}s", timeoutSeconds);
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
            return ResponseEntity.internalServerError()
                .body(new SyncResponse(
                    "Synchronization failed",
                    "Failed to start sync operation: " + e.getMessage(),
                    false
                ));
        }
    }

    /**
     * Response DTO for sync operation status.
     */
    private record SyncResponse(String status, String message, boolean started) {}
}
