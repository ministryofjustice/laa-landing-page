package uk.gov.justice.laa.portal.landingpage.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateExternalUserRequest(String email, String firstName, String lastName,
                                        UserType userType, String firmId, boolean multiFirmUser) {
}
