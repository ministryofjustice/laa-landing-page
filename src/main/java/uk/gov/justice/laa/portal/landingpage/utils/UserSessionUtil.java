package uk.gov.justice.laa.portal.landingpage.utils;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionSelection;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionSelections;

import java.util.*;

import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getListFromHttpSession;


public class UserSessionUtil {

    public static void removeAllAttribute(HttpSession session) {
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

    public static List<String> getAppsByUserId(HttpSession session, String id) {
        HashMap<String, UserSessionSelection> userSessionSelections = (HashMap<String, UserSessionSelection>) session.getAttribute("userSelection");

        return Optional.ofNullable(userSessionSelections)
                .map(map -> map.get(id))
                .map(UserSessionSelection::getAppsSelection)
                .map(ArrayList::new) // convert Set<String> to List<String>
                .orElseGet(ArrayList::new); // return empty list if anything is null
    }

    public static Map<String, List<String>> getRolesByAppId(HttpSession session, String id, String appId) {
        HashMap<String, UserSessionSelection> userSessionSelections = (HashMap<String, UserSessionSelection>) session.getAttribute("userSelection");

        return Optional.ofNullable(userSessionSelections)
                .map(map -> map.get(id))
                .map(UserSessionSelection::getRolesSelection)
                .orElseGet(HashMap::new); // return empty list if anything is null

    }

    public static List<String> getListRolesByUserIdAndAppId(HttpSession session, String id, String appId) {
        HashMap<String, UserSessionSelection> userSessionSelections = (HashMap<String, UserSessionSelection>) session.getAttribute("userSelection");

        return Optional.ofNullable(userSessionSelections)
                .map(map -> map.get(id))
                .map(UserSessionSelection::getRolesSelection)
                .map(map -> map.get(appId))
                .orElseGet(ArrayList::new); // return empty list if anything is null

    }

    public static void AddAppsById(HttpSession session, String id, List<String> selectedApps) {
        HashMap<String, UserSessionSelection> userSessionSelections = (HashMap<String, UserSessionSelection>) session.getAttribute("userSelection");

        if (userSessionSelections == null) {
            Map<String, UserSessionSelection> userSelectionMap = new HashMap<>();
            userSelectionMap.put(id, new UserSessionSelection());
            userSelectionMap.get(id).setAppsSelection(new HashSet<>(selectedApps));
            session.setAttribute("userSelection", userSelectionMap);
        } else {
            Optional.of(userSessionSelections)
                    .map(map -> map.get(id))
                    .ifPresentOrElse(
                            userSelection -> userSelection.setAppsSelection(new HashSet<>(selectedApps)),
                            () -> {
                                userSessionSelections.put(id, new UserSessionSelection());
                                userSessionSelections.get(id).setAppsSelection(new HashSet<>(selectedApps));
                            }
                    );
            session.setAttribute("userSelection", userSessionSelections);
        }
    }

    public static void AddRolesById(HttpSession session, String id, String appId, List<String> selectedRoles) {

        HashMap<String, UserSessionSelection> userSessionSelections = (HashMap<String, UserSessionSelection>) session.getAttribute("userSelection");

        Optional.ofNullable(userSessionSelections)
                .map(map -> map.get(id))
                .map(UserSessionSelection::getRolesSelection)
                .map(map -> map.get(appId))
                .ifPresentOrElse(
                        userSelection -> {
                            userSelection.clear();
                            userSelection.addAll(selectedRoles);
                        },
                        () -> {


                            //add
                            if (userSessionSelections.get(id).getRolesSelection() != null){
                                boolean hasroles = userSessionSelections.get(id).getRolesSelection().containsKey(appId);
                                if (!hasroles){
                                    userSessionSelections.get(id).getRolesSelection().put(appId, selectedRoles);
                                }


                            } else {
                                Map<String, List<String>> newRolesSelection = new HashMap<>();
                                newRolesSelection.put(appId, selectedRoles);
                                userSessionSelections.get(id).setRolesSelection(newRolesSelection);
                            }
                        }
                );
        session.setAttribute("userSelection", userSessionSelections);

    }

    public static void removeUserSessionById(HttpSession session, String id) {

        HashMap<String, UserSessionSelection> userSessionSelections = (HashMap<String, UserSessionSelection>) session.getAttribute("userSelection");

        userSessionSelections.remove(id);

        session.setAttribute("userSelection", userSessionSelections);

    }


}
