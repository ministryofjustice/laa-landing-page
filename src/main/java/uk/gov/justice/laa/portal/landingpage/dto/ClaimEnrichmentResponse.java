package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimEnrichmentResponse {
    private boolean success;
    private String message;
    private String appName;
    private String userId;
    private String email;
    private List<String> roles;
    private List<String> officeIds;
}