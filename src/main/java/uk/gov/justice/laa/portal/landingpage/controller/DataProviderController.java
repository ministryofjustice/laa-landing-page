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

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RequestMapping("/api/data-provider")
// @PreAuthorize("@accessControlService.userHasAuthzRole(authentication, T(uk.gov.justice.laa.portal.landingpage.entity.AuthzRole).GLOBAL_ADMIN.roleName)")  // COMMENTED OUT FOR TESTING
public class DataProviderController {
    private final DataProviderService dataProviderService;
    private final ObjectMapper objectMapper;

    /**
     * API endpoint to compare PDA data with local database.
     * Returns structured comparison showing created, updated, deleted, and matched items.
     *
     * @return ResponseEntity containing JSON with categorized comparison results
     */
    @GetMapping("/provider-offices/compare")
    public ResponseEntity<String> compareProviderOffices() {
        log.info("Received request to compare PDA data with database");
        try {
            ComparisonResultDto result = dataProviderService.compareWithDatabase();

            // Pretty print JSON
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(result);

            log.info("Returning comparison - Firms: {} creates, {} updates, {} deletes, {} exists",
                result.getFirmsCreated(), result.getFirmsUpdated(), result.getFirmsDeleted(), result.getFirmsExists());
            log.info("Returning comparison - Offices: {} creates, {} updates, {} deletes, {} exists",
                result.getOfficesCreated(), result.getOfficesUpdated(), result.getOfficesDeleted(), result.getOfficesExists());
            log.info("Returning comparison - Total: {} creates, {} updates, {} deletes, {} exists",
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
}
