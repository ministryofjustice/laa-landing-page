package uk.gov.justice.laa.portal.landingpage.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

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

        // Global Admin
        if (userHasAuthzRole(authenticatedUser, "Global Admin")) {
            return true;
        }

        EntraUserDto accessedUser = optionalAccessedUserProfile.get().getEntraUser();

        if (userHasPermission(authenticatedUser, Permission.VIEW_INTERNAL_USER)
                && userService.isInternal(accessedUser.getId())) {
            return true;
        }

        // internal user with external user manager permission
        if (userHasPermission(authenticatedUser, Permission.VIEW_EXTERNAL_USER)
                && !userService.isInternal(accessedUser.getId())
                && userService.isInternal(authenticatedUser.getId())) {
            return true;
        }

        boolean canAccess = userHasPermission(authenticatedUser, Permission.VIEW_EXTERNAL_USER)
                && !userService.isInternal(authenticatedUser.getId())
                && !userService.isInternal(accessedUser.getId())
                && usersAreInSameFirm(authenticatedUser, userProfileId);
        if (!canAccess) {
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            log.warn("User {} does not have permission to access this userId {}", currentUserDto.getName(),
                    userProfileId);
        }
        return canAccess;
    }

    public boolean canViewAllMultiFirmInfo(String userProfileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);

        Optional<UserProfileDto> optionalAccessedUserProfile = userService.getUserProfileById(userProfileId);
        if (optionalAccessedUserProfile.isEmpty() || optionalAccessedUserProfile.get().getUserType().equals(UserType.INTERNAL)) {
            return false;
        }
        if (userHasAuthzRole(authenticatedUser, "Global Admin")) {
            return true;
        }
        if (userHasAuthzRole(authenticatedUser, "External User Admin")) {
            return true;
        }
        if (userHasAuthzRole(authenticatedUser, "External User Manager")) {
            return true;
        }
        return false;
    }

    public boolean canDeleteUser(String userProfileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);

        Optional<UserProfileDto> optionalAccessedUserProfile = userService.getUserProfileById(userProfileId);
        if (optionalAccessedUserProfile.isEmpty()) {
            return false;
        }

        if (optionalAccessedUserProfile.get().getUserType().equals(UserType.INTERNAL)) {
            return false;
        }

        return userHasPermission(authenticatedUser, Permission.DELETE_EXTERNAL_USER);
    }

    /**
     * Check if the authenticated user can delete a specific firm profile.
     * Used for multi-firm users where we delete individual firm access.
     *
     * @param userProfileId the ID of the user profile to delete
     * @return true if user has permission to delete this firm profile
     */
    public boolean canDeleteFirmProfile(String userProfileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);

        if (authenticatedUser == null) {
            log.debug("Authenticated user is null, returning false");
            return false;
        }

        Optional<UserProfileDto> optionalAccessedUserProfile = userService.getUserProfileById(userProfileId);
        if (optionalAccessedUserProfile.isEmpty()) {
            return false;
        }

        UserProfileDto accessedUserProfile = optionalAccessedUserProfile.get();
        // Only external user profiles can be deleted
        if (accessedUserProfile.getUserType().equals(UserType.INTERNAL)) {
            return false;
        }

        // Must be a multi-firm user
        if (accessedUserProfile.getEntraUser() == null || !accessedUserProfile.getEntraUser().isMultiFirmUser()) {
            return false;
        }

        // Check if authenticated user is internal (LAA staff)
        boolean isInternalUser = userService.isInternal(authenticatedUser.getId());

        // Internal users with DELETE_EXTERNAL_USER permission can delete any firm
        // profile
        if (isInternalUser && userHasPermission(authenticatedUser, Permission.DELETE_EXTERNAL_USER)) {
            return true;
        }

        // Check if users are in the same firm
        boolean sameFirm = usersAreInSameFirm(authenticatedUser, userProfileId);

        // External users (firm admins) with DELEGATE_EXTERNAL_USER_ACCESS can only
        // delete profiles from their own firm
        if (!isInternalUser && sameFirm
                && userHasPermission(authenticatedUser, Permission.DELEGATE_EXTERNAL_USER_ACCESS)) {
            return true;
        }

        return false;
    }

    public boolean canEditUser(String userProfileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);

        Optional<UserProfileDto> optionalAccessedUserProfile = userService.getUserProfileById(userProfileId);
        if (optionalAccessedUserProfile.isEmpty()) {
            return false;
        }

        // Global Admin
        if (userHasAuthzRole(authenticatedUser, "Global Admin")) {
            return true;
        }

        EntraUserDto accessedUser = optionalAccessedUserProfile.get().getEntraUser();

        // Internal User Manager editing internal user.
        if (userHasPermission(authenticatedUser, Permission.EDIT_INTERNAL_USER)
                && userService.isInternal(accessedUser.getId())) {
            return true;
        }

        // internal user with external user manager permission accessing external user
        if (userHasPermission(authenticatedUser, Permission.EDIT_EXTERNAL_USER)
                && !userService.isInternal(accessedUser.getId())
                && userService.isInternal(authenticatedUser.getId())) {
            return true;
        }

        // External user accessing external User
        boolean canAccess = userHasPermission(authenticatedUser, Permission.EDIT_EXTERNAL_USER)
                && !userService.isInternal(authenticatedUser.getId())
                && !userService.isInternal(accessedUser.getId())
                && usersAreInSameFirm(authenticatedUser, userProfileId);
        if (!canAccess) {
            log.warn("User {} does not have permission to edit this userId {}", authenticatedUser.getId(),
                    userProfileId);
        }
        return canAccess;
    }

    private boolean usersAreInSameFirm(EntraUser authenticatedUser, String accessedUserProfileId) {
        List<UUID> userManagerFirms = firmService.getUserActiveAllFirms(authenticatedUser).stream().map(FirmDto::getId)
                .toList();
        List<FirmDto> userFirms = firmService.getUserFirmsByUserId(accessedUserProfileId);
        return userFirms.stream().map(FirmDto::getId).anyMatch(userManagerFirms::contains);
    }

    // IDEs may make this appear unused, but it's actually used in the @PreAuthorize
    // annotation in UserController.
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

    public static boolean userHasAuthzRole(EntraUser user, String authzRoleName) {
        return user.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .flatMap(userProfile -> userProfile.getAppRoles().stream())
                .anyMatch(appRole -> appRole.isAuthzRole() && appRole.getName() != null
                        && appRole.getName().equalsIgnoreCase(authzRoleName));
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

    public boolean canSendVerificationEmail(String userProfileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);

        Optional<UserProfileDto> optionalAccessedUserProfile = userService.getUserProfileById(userProfileId);
        if (optionalAccessedUserProfile.isEmpty()) {
            return false;
        }

        EntraUserDto accessedUser = optionalAccessedUserProfile.get().getEntraUser();

        return userService.isInternal(authenticatedUser.getId())
                && !userService.isInternal(accessedUser.getId())
                && userHasAnyGivenPermissions(authenticatedUser,
                        Permission.CREATE_EXTERNAL_USER, Permission.EDIT_EXTERNAL_USER);
    }

}
