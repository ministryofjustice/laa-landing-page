package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Data;

@Data
public class EntraUserDto {
    private String id;
    private String email;
    private String fullName;
    private String lastLoggedIn;
}
