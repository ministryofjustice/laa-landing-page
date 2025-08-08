package uk.gov.justice.laa.portal.landingpage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccessControlService {

    private final UserService userService;

    private final FirmService firmService;

    private final LoginService loginService;

    private static final Logger log = LoggerFactory.getLogger(AccessControlService.class);

    public AccessControlService(UserService userService, LoginService loginService, FirmService firmService) {
        this.userService = userService;
        this.loginService = loginService;
        this.firmService = firmService;
    }

    public boolean canAccessUser(String userProfileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);

        Optional<UserProfileDto> optionalAccessedUserProfile = userService.getUserProfileById(userProfileId);
        if (optionalAccessedUserProfile.isEmpty()) {
            return false;
        }

        if (userHasPermission(authenticatedUser, Permission.VIEW_INTERNAL_USER) && userHasPermission(authenticatedUser, Permission.VIEW_EXTERNAL_USER)) {
            return true;
        }

        EntraUserDto accessedUser = optionalAccessedUserProfile.get().getEntraUser();

        if (userHasPermission(authenticatedUser, Permission.VIEW_INTERNAL_USER) && userService.isInternal(accessedUser.getId())) {
            return true;
        }

        //internal user with external user manager permission
        if (userHasPermission(authenticatedUser, Permission.VIEW_EXTERNAL_USER) && !userService.isInternal(accessedUser.getId())
                && userService.isInternal(authenticatedUser.getId())) {
            return true;
        }

        boolean canAccess = userHasPermission(authenticatedUser, Permission.VIEW_EXTERNAL_USER) && !userService.isInternal(accessedUser.getId())
                && usersAreInSameFirm(authenticatedUser, userProfileId);
        if (!canAccess) {
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            log.warn("User {} does not have permission to access this userId {}", currentUserDto.getName(), userProfileId);
        }
        return canAccess;
    }

    public boolean canEditUser(String userProfileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);

        Optional<UserProfileDto> optionalAccessedUserProfile = userService.getUserProfileById(userProfileId);
        if (optionalAccessedUserProfile.isEmpty()) {
            return false;
        }

        // Only global admin should have both these permissions.
        if (userHasPermission(authenticatedUser, Permission.EDIT_INTERNAL_USER)) {
            return true;
        }

        //internal user with external user manager permission
        if (userHasPermission(authenticatedUser, Permission.VIEW_EXTERNAL_USER) && !userService.isInternal(accessedUser.getId())
                && userService.isInternal(authenticatedUser.getId())) {
            return true;
        }

        boolean canAccess = userHasPermission(authenticatedUser, Permission.EDIT_EXTERNAL_USER) && usersAreInSameFirm(authenticatedUser, userProfileId);
        if (!canAccess) {
            log.warn("User {} does not have permission to edit this userId {}", authenticatedUser.getId(), userProfileId);
        }
        return canAccess;
    }

    private boolean usersAreInSameFirm(EntraUser authenticatedUser, String accessedUserProfileId) {
        List<UUID> userManagerFirms = firmService.getUserAllFirms(authenticatedUser).stream().map(FirmDto::getId).toList();
        List<FirmDto> userFirms = firmService.getUserFirmsByUserId(accessedUserProfileId);
        return userFirms.stream().map(FirmDto::getId).anyMatch(userManagerFirms::contains);
    }

    // IDEs may make this appear unused, but it's actually used in the @PreAuthorize annotation in UserController.
    public boolean authenticatedUserHasPermission(Permission permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);
        return userHasPermission(authenticatedUser, permission);
    }

    public boolean authenticatedUserHasAnyGivenPermissions(Permission... permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);
        return userHasAnyGivenPermissions(authenticatedUser, permission);
    }

    public static boolean userHasPermission(EntraUser entraUser, Permission permission) {
        return userHasAnyGivenPermissions(entraUser, permission);
    }

    public static boolean userHasAnyGivenPermissions(EntraUser entraUser, Permission... permissions) {
        Set<Permission> userPermissions = entraUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .flatMap(userProfile -> userProfile.getAppRoles().stream())
                .filter(AppRole::isAuthzRole)
                .flatMap(appRole -> appRole.getPermissions().stream())
                .collect(Collectors.toSet());
        return Arrays.stream(permissions).anyMatch(userPermissions::contains);
    }



}
