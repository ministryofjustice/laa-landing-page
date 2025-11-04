package uk.gov.justice.laa.portal.landingpage.utils;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionSelection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getListFromHttpSession;


public class UserSessionUtil {

    public static void removeAllAttribute(HttpSession session){
        session.removeAttribute("user");
        session.removeAttribute("firm");
        session.removeAttribute("selectedUserType");
        session.removeAttribute("isFirmAdmin");
        session.removeAttribute("isMultiFirmUser");
        session.removeAttribute("multiFirmForm");
        session.removeAttribute("apps");
        session.removeAttribute("roles");
        session.removeAttribute("officeData");
        session.removeAttribute("firmSearchForm");
        session.removeAttribute("firmSearchTerm");
    }

    public static List<String> getAppsByUserId(HttpSession session, String id){
        List<UserSessionSelection> selectedApps = getListFromHttpSession(session, "grantAccessSelectedApps", UserSessionSelection.class)
                .orElse(new ArrayList<>());
        return selectedApps.stream()
                .filter(userSelection -> userSelection.getId().equals(id))
                .flatMap(user -> user.getAppsSelection().stream())
                .toList();

    };


}
