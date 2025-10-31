package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.*;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ByPassScreenService {

    private final AppRoleService appRoleService;
    private final LoginService loginService;
    private final RoleAssignmentService roleAssignmentService;
    private final UserService userService;
    private final EventService eventService;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Set<String> targetNames = Set.of("CrimeApplyAccess", "CrimeFormSubmitAccess", "CivilApplyAccess");

    public void byPassRolesScreen(Authentication authentication, String id, List<String> selectedApps, UserType userType) {

        List<AppRoleDto> appRoles = appRoleService.getByAppIdsAndUserRestriction(selectedApps, userType);

        List<AppRoleDto> appIdsFilteredByTargetNames = appRoles.stream()
                .filter(role -> targetNames.contains(role.getName()))
                .toList();

        if (appIdsFilteredByTargetNames.isEmpty()) {
            return;
        }

        UserProfile userProfile = loginService.getCurrentProfile(authentication);

        List<String> nonEditableRoles = userService.getUserAppRolesByUserId(id).stream()
                .filter(role -> !roleAssignmentService.canUserAssignRolesForApp(userProfile, role.getApp()))
                .map(AppRoleDto::getId)
                .toList();
        //save on the database
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);

        UserProfileDto user = userService.getUserProfileById(id).orElse(null);
        // list of app
        List<String> appRoleSelectedList =
                appIdsFilteredByTargetNames.stream()
                        .map(AppRoleDto::getId)
                        .toList();

//        if (roleAssignmentService.canAssignRole(userProfile.getAppRoles(), appSelectedList)) {
//            logger.error("User does not have sufficient permissions to assign the selected roles: userId={}, attemptedRoleIds={}",
//                    userProfile.getId(),
//                    appRoles.stream().map(AppRoleDto::getId).toList());
//            throw new RuntimeException("User does not have sufficient permissions to assign the selected roles");
//        }

        Map<String, String> updateResult = userService.updateUserRoles(id, appRoleSelectedList, nonEditableRoles, currentUserDto.getUserId());
        UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(
                userProfile.getId(),
                currentUserDto,
                user != null ? user.getEntraUser() : null, updateResult.get("diff"),
                "role");
        eventService.logEvent(updateUserAuditEvent);


        //remove apps from the request to bypass the screen

        List<String> appSelectedList =
                appIdsFilteredByTargetNames.stream()
                        .map(AppRoleDto::getApp)
                        .map(AppDto::getId)
                        .toList();

        selectedApps.removeIf(appSelectedList::contains);

    }
}
