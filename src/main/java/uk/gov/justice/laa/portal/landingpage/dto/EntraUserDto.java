package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntraUserDto {

    private String id;
    private String email;
    private String fullName;
    private String lastLoggedIn;
    private String entraOid;
    private String firstName;
    private String lastName;
}
