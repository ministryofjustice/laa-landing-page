package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimEnrichmentResponse {
    private boolean success;
    private String message;
    private String appName;
    private Set<String> roles;
    // Add other necessary fields
}