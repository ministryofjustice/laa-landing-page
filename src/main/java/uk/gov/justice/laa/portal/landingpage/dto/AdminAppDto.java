package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for AdminApp administration display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAppDto {
    private String id;
    private String name;
    private String description;
    private int ordinal;
    private boolean enabled;
}
