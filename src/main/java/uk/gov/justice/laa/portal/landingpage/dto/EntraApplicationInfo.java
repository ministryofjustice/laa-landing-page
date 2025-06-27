package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntraApplicationInfo {
    private String id;
    private String displayName;
}
