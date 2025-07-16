package uk.gov.justice.laa.portal.landingpage.utils;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.forms.UserDetailsForm;

public class UserUtils {

    public static UserDetailsForm populateUserDetailsFormWithSession(UserDetailsForm userDetailsForm, EntraUserDto user, HttpSession session) {
        if (user.getFirstName() != null) {
            userDetailsForm.setFirstName(user.getFirstName());
        }
        if (user.getLastName() != null) {
            userDetailsForm.setLastName(user.getLastName());
        }
        if (user.getEmail() != null) {
            userDetailsForm.setEmail(user.getEmail());
        }

        userDetailsForm.setIsFirmAdmin(session.getAttribute("isFirmAdmin") == Boolean.TRUE);

        return userDetailsForm;
    }

}
