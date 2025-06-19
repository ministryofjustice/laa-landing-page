package uk.gov.justice.laa.portal.landingpage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.service.ClaimEnrichmentInterface;

/**
 * Controller for handling claim enrichment requests.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(
    value = "/api/v1/claims",
    produces = MediaType.APPLICATION_JSON_VALUE
)
@Tag(name = "Claim Enrichment", description = "APIs for claim enrichment")
public class ClaimEnrichmentController {

    private final ClaimEnrichmentInterface claimEnrichmentService;

    /**
     * Enriches the claims for a given user with additional permissions.
     *
     * @param request The claim enrichment request containing user and token information
     * @return ResponseEntity containing the enriched claims response
     */
    @PostMapping(
        value = "/enrich",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Enrich user claims with permissions",
        description = "Enriches the claims for a given user with additional permissions"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Claims enriched successfully",
        content = @Content(schema = @Schema(implementation = ClaimEnrichmentResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid input provided"
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized access"
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error"
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClaimEnrichmentResponse> enrichClaims(
            @Valid @RequestBody ClaimEnrichmentRequest request) {
        log.info("Received claim enrichment request for user: {}", request.getUserId());
        ClaimEnrichmentResponse response = claimEnrichmentService.enrichClaims(request);
        log.info("Successfully processed claim enrichment for user: {}", request.getUserId());
        return ResponseEntity.ok(response);
    }
}