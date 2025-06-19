package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class ClaimEnrichmentRequest {
    private EntraClaimData data;
    private String eventType;
    private String eventId;
    private Instant time;
}
