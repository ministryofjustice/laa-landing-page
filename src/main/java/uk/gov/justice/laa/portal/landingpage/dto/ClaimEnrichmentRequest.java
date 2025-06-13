package uk.gov.justice.laa.portal.landingpage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request object for claim enrichment containing user and token information.
 */
@Data
public class ClaimEnrichmentRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Authentication token is required")
    private String token;

    @NotBlank(message = "Target App is required")
    private String targetAppId;

    // Additional fields can be added here as needed
}