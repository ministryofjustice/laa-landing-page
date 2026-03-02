package uk.gov.justice.laa.portal.landingpage.utils;

import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import java.util.List;
import java.util.Set;

import static uk.gov.justice.laa.portal.landingpage.utils.UserRoleType.EXTERNAL_USER_ADMIN;
import static uk.gov.justice.laa.portal.landingpage.utils.UserRoleType.FIRM_USER_MANAGER;
import static uk.gov.justice.laa.portal.landingpage.utils.UserRoleType.GLOBAL_ADMIN;

public class RolesUtils {

    public static boolean isCurrentProfileExternalUserAdmin(UserProfile currentUserProfile) {
        return currentUserProfile.getAppRoles().stream()
                .anyMatch(role -> EXTERNAL_USER_ADMIN.getDescription().equals(role.getName()));

    }

    public static boolean isCurrentProfileGlobalAdmin(UserProfile currentUserProfile) {
        return currentUserProfile.getAppRoles().stream()
                .anyMatch(role -> GLOBAL_ADMIN.getDescription().equals(role.getName()));

    }

    public static boolean isProvideAdmin(List<AppRoleDto> userAppRoles) {
        return userAppRoles.stream()
                .anyMatch(role -> FIRM_USER_MANAGER.getDescription().equals(role.getName()));
    }

    public static boolean isProvideAdminBySet(Set<AppRole> userAppRoles) {
        return userAppRoles.stream()
                .anyMatch(role -> FIRM_USER_MANAGER.getDescription().equals(role.getName()));
    }
}
