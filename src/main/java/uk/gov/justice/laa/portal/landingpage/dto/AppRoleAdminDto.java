package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for AppRole administration display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppRoleAdminDto {
    private String id;
    private String name;
    private String description;
    private String userTypeRestriction;
    private String parentApp;
    private String parentAppId;
    private String roleGroup;
    private int ordinal;
    private boolean authzRole;
    private String ccmsCode;
}
