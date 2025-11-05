package uk.gov.justice.laa.portal.landingpage.utils;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionSelection;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionSelections;

import java.util.*;

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
        HashMap<String, UserSessionSelection> userSessionSelections = (HashMap<String, UserSessionSelection>) session.getAttribute("userSelection");

        return Optional.ofNullable(userSessionSelections)
                .map(map -> map.get(id))
                .map(UserSessionSelection::getAppsSelection)
                .map(ArrayList::new) // convert Set<String> to List<String>
                .orElseGet(ArrayList::new); // return empty list if anything is null
    }

    public static List<String> getRolesByUserId(HttpSession session, String id){
        HashMap<String, UserSessionSelection> userSessionSelections = (HashMap<String, UserSessionSelection>) session.getAttribute("userSelection");

        return Optional.ofNullable(userSessionSelections)
                .map(map -> map.get(id))
                .map(UserSessionSelection::getRolesSelection)
                .map(ArrayList::new) // convert Set<String> to List<String>
                .orElseGet(ArrayList::new); // return empty list if anything is null

    }

    public static void AddAppsById(HttpSession session, String id, List<String> selectedApps){
        HashMap<String, UserSessionSelection> userSessionSelections = (HashMap<String, UserSessionSelection>) session.getAttribute("userSelection");

        if (userSessionSelections == null){
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

    public static void AddRolesById(HttpSession session, String id, List<String> selectedRoles) {

        HashMap<String, UserSessionSelection> userSessionSelections = (HashMap<String, UserSessionSelection>) session.getAttribute("userSelection");
        Optional.of(userSessionSelections)
                .map(map -> map.get(id))
                .ifPresentOrElse(
                        userSelection -> userSelection.setAppsSelection(new HashSet<>(selectedRoles)),
                        () -> {
                            userSessionSelections.put(id, new UserSessionSelection());
                            userSessionSelections.get(id).setRolesSelection(new HashSet<>(selectedRoles));
                        }
                );
        session.setAttribute("userSelection", userSessionSelections);
    }



}
