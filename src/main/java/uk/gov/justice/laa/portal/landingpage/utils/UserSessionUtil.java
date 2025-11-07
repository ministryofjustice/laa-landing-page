package uk.gov.justice.laa.portal.landingpage.utils;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionSelection;

import java.util.*;

public class UserSessionUtil {

    public static final String USER_SELECTION = "userSelection";

    public static List<String> getAppsByUserId(HttpSession session, String id) {
        HashMap<String, UserSessionSelection> userSessionSelections = getStringUserSessionSelectionHashMap(session);

        return Optional.ofNullable(userSessionSelections)
                .map(map -> map.get(id))
                .map(UserSessionSelection::getAppsSelection)
                .map(ArrayList::new) // convert Set<String> to List<String>
                .orElseGet(ArrayList::new); // return empty list if anything is null
    }


    public static List<String> getListRolesByUserIdAndAppId(HttpSession session, String id, String appId) {
        HashMap<String, UserSessionSelection> userSessionSelections = getStringUserSessionSelectionHashMap(session);

        return Optional.ofNullable(userSessionSelections)
                .map(map -> map.get(id))
                .map(UserSessionSelection::getRolesSelection)
                .map(map -> map.get(appId))
                .orElseGet(ArrayList::new); // return empty list if anything is null

    }

    public static void AddAppsById(HttpSession session, String id, List<String> selectedApps) {
        HashMap<String, UserSessionSelection> userSessionSelections = getStringUserSessionSelectionHashMap(session);

        if (userSessionSelections == null) {
            userSessionSelections = new HashMap<>();
        }

        userSessionSelections.computeIfAbsent(id, app -> new UserSessionSelection())
                .setAppsSelection(new HashSet<>(selectedApps));

        session.setAttribute(USER_SELECTION, userSessionSelections);

    }

    private static HashMap<String, UserSessionSelection> getStringUserSessionSelectionHashMap(HttpSession session) {
        return (HashMap<String, UserSessionSelection>) session.getAttribute(USER_SELECTION);
    }

    public static void AddRolesById(HttpSession session, String id, String appId, List<String> selectedRoles) {
        HashMap<String, UserSessionSelection> userSessionSelections = getStringUserSessionSelectionHashMap(session);

        Map<String, List<String>> userSelection = Optional.ofNullable(userSessionSelections)
                .map(map -> map.get(id))
                .map(UserSessionSelection::getRolesSelection)
                .orElseGet(() -> {
                    var newRolesSelection = new HashMap<String, List<String>>();
                    userSessionSelections.get(id).setRolesSelection(newRolesSelection);
                    return newRolesSelection;
                });

        userSelection.compute(appId, (key, roles) -> {
            if (roles == null) return new ArrayList<>(selectedRoles);
            roles.clear();
            roles.addAll(selectedRoles);
            return roles;
        });
        session.setAttribute(USER_SELECTION, userSessionSelections);
    }

    public static void removeUserSessionById(HttpSession session, String id) {
        HashMap<String, UserSessionSelection> userSessionSelections = getStringUserSessionSelectionHashMap(session);
        userSessionSelections.remove(id);
        session.setAttribute(USER_SELECTION, userSessionSelections);
    }

    public static void removeAppsSessionById(HttpSession session, String id) {
        HashMap<String, UserSessionSelection> userSessionSelections = getStringUserSessionSelectionHashMap(session);
        if (userSessionSelections != null) {
            userSessionSelections.get(id).getAppsSelection().clear();
            session.setAttribute(USER_SELECTION, userSessionSelections);
        }
    }

    public static void removeRolesSessionByIdAndRoleId(HttpSession session, String id, String appId) {
       HashMap<String, UserSessionSelection> userSessionSelections = getStringUserSessionSelectionHashMap(session);
        UserSessionSelection selection = userSessionSelections.get(id);
        if (selection != null && selection.getRolesSelection() != null) {

            if (userSessionSelections.get(id).getRolesSelection().get(appId) != null){
                userSessionSelections.get(id).getRolesSelection().get(appId).clear();
            }
            session.setAttribute(USER_SELECTION, userSessionSelections);
        }
    }

    public static void clearUserSession(HttpSession session, String id){
        session.removeAttribute("grantAccessUserOfficesModel");
        session.removeAttribute("grantAccessSelectedApps");
        session.removeAttribute("grantAccessUserRoles");
        session.removeAttribute("grantAccessUserRolesModel");
        session.removeAttribute("grantAccessAllSelectedRoles");

        removeUserSessionById(session, id);
    }


}
