package uk.gov.justice.laa.portal.landingpage.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntraUserDto implements Serializable {

    private String id;
    private String email;
    private String fullName;
    private String lastLoggedIn;
    private String entraOid;
    private String firstName;
    private String lastName;
    private boolean multiFirmUser;
    private UserStatus userStatus;
}
