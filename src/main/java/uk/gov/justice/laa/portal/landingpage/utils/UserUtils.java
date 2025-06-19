package uk.gov.justice.laa.portal.landingpage.utils;

import com.microsoft.graph.models.User;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.forms.UserDetailsForm;

public class UserUtils {

    public static UserDetailsForm populateUserDetailsFormWithSession(UserDetailsForm userDetailsForm, User user, HttpSession session) {
        if (user.getGivenName() != null) {
            userDetailsForm.setFirstName(user.getGivenName());
        }
        if (user.getSurname() != null) {
            userDetailsForm.setLastName(user.getSurname());
        }
        if (user.getMail() != null) {
            userDetailsForm.setEmail(user.getMail());
        }

        userDetailsForm.setIsFirmAdmin(session.getAttribute("isFirmAdmin").equals(Boolean.TRUE));

        return userDetailsForm;
    }

}
