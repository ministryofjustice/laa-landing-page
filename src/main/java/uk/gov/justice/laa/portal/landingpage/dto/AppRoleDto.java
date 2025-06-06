package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Data;

@Data
public class AppRoleDto {
    private String id;
    private String name;
    private AppDto app;
}
