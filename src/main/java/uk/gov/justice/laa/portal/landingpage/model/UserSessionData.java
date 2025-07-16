package uk.gov.justice.laa.portal.landingpage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.List;
import java.util.Set;

/**
 * UserSessionData
 */
@Data
@AllArgsConstructor
@Builder
public class UserSessionData {
    private String name;
    private String accessToken;
    private EntraUserDto user;
    private Set<LaaApplication> laaApplications;
    List<UserType> userTypes;
}
