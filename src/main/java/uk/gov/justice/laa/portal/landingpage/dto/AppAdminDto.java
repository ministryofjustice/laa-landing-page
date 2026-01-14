package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for App administration display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppAdminDto {
    private String id;
    private String name;
    private String description;
    private int ordinal;
    private String url;
    private boolean enabled;
    private String appType;
}
