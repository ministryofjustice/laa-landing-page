package uk.gov.justice.laa.portal.landingpage.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.ComparisonResultDto;
import uk.gov.justice.laa.portal.landingpage.service.DataProviderService;

/**
 * Controller for PDA (Provider Data API) endpoints.
 * Requires "Global Admin" role to access.
 */
@Slf4j
@RestController
@RequestMapping("/api/pda")
@PreAuthorize("@accessControlService.userHasAuthzRole(authentication, T(uk.gov.justice.laa.portal.landingpage.entity.AuthzRole).GLOBAL_ADMIN.roleName)")
public class DataProviderController {
    private final DataProviderService dataProviderService;
    private final ObjectMapper objectMapper;

    public DataProviderController(
            DataProviderService dataProviderService,
            ObjectMapper objectMapper) {
        this.dataProviderService = dataProviderService;
        this.objectMapper = objectMapper;
    }

    /**
     * API endpoint to compare PDA data with local database.
     * Returns structured comparison showing created, updated, deleted, and matched items.
     *
     * @return ResponseEntity containing JSON with categorized comparison results
     */
    @GetMapping("/compare")
    public ResponseEntity<String> compareProviderOffices() {
        log.debug("Received request to compare PDA data with database");
        try {
            ComparisonResultDto result = dataProviderService.compareWithDatabase();

            // Pretty print JSON
            final String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(result);

            log.debug("Returning comparison - Firms: {} creates, {} updates, {} disables, {} exists",
                result.getFirmCreates(), result.getFirmUpdates(), result.getFirmDisables(), result.getFirmExists());
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
}
