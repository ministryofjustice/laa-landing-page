package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Data;
import uk.gov.justice.laa.portal.landingpage.entity.RoleType;

@Data
public class AppRoleDto {
    private String id;
    private String name;
    private AppDto app;
    private RoleType roleType;
}
