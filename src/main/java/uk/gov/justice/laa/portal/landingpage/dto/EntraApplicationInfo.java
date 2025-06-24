package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EntraApplicationInfo {
    private String id;
    private String displayName;
}
