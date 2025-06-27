package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimEnrichmentRequest {
    private EntraClaimData data;
    private String eventType;
    private String eventId;
    private Instant time;
}
