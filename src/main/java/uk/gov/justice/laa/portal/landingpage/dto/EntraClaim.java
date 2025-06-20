package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EntraClaim {
    private String type;
    private String value;
}
