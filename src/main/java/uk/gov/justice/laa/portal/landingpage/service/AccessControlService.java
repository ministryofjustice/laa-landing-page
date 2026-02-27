package uk.gov.justice.laa.portal.landingpage.service;

import java.util.Arrays;
import java.util.HashSet;
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
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.exception.UserNotFoundException;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;

@Service
public class AccessControlService {

    private final UserService userService;

    private final FirmService firmService;

    private final LoginService loginService;

    private final EntraUserRepository entraUserRepository;

    private static final Logger log = LoggerFactory.getLogger(AccessControlService.class);

    public AccessControlService(UserService userService, LoginService loginService,
            FirmService firmService, EntraUserRepository entraUserRepository) {
        this.userService = userService;
        this.loginService = loginService;
        this.firmService = firmService;
        this.entraUserRepository = entraUserRepository;
    }

    public boolean canAccessUser(String userProfileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);

        Optional<UserProfileDto> optionalAccessedUserProfile = userService.getUserProfileById(userProfileId);
        if (optionalAccessedUserProfile.isEmpty()) {
            return false;
        }

        // Global Admin
        if (userHasAuthzRole(authenticatedUser, AuthzRole.GLOBAL_ADMIN.getRoleName())) {
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
            log.warn("User {} does not have permission to access this userId {}",
                    currentUserDto.getName(), userProfileId);
        }
        return canAccess;
    }

    public boolean canViewAllFirmsOfMultiFirmUser() {
        return authenticatedUserHasPermission(Permission.VIEW_ALL_USER_MULTI_FIRM_PROFILES);
    }

    public boolean canConvertUserToMultiFirm(String entraUserId) {
        EntraUserDto accessedUser = userService.getEntraUserById(entraUserId)
                .orElseThrow(() -> new UserNotFoundException(String.format("User with id '%s' not found", entraUserId)));

        boolean isMultiFirmUser = accessedUser.isMultiFirmUser();

        return !isMultiFirmUser && authenticatedUserIsInternal() && authenticatedUserHasPermission(Permission.EDIT_EXTERNAL_USER)
                && authenticatedUserHasPermission(Permission.CONVERT_USER_TO_MULTI_FIRM);
    }

    public boolean canDeleteUser(String userProfileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);

        Optional<UserProfileDto> optionalAccessedUserProfile = userService.getUserProfileById(userProfileId);
        if (optionalAccessedUserProfile.isEmpty()) {
            return false;
        }

        UserProfileDto accessedUserProfile = optionalAccessedUserProfile.get();
        if (accessedUserProfile.getUserType().equals(UserType.INTERNAL)) {
            return false;
        }

        boolean isInternalUser = userService.isInternal(authenticatedUser.getId());
        if (!isInternalUser && accessedUserProfile.getEntraUser().isMultiFirmUser()) {
            return false;
        }

        // FUM can only delete own firm user
        boolean isFirmUserManager = isFirmUserManager(authenticatedUser);
        boolean sameFirm = usersAreInSameFirm(authenticatedUser, userProfileId);
        if (isFirmUserManager && !sameFirm) {
            return false;
        }

        return userHasPermission(authenticatedUser, Permission.DELETE_EXTERNAL_USER);
    }

    /**
     * Check if the authenticated user can delete a user without a profile
     *
     * @param entraUserId the ID of the EntraUser to delete (as String)
     * @return true if user has permission to delete this user
     */
    public boolean canDeleteUserWithoutProfile(String entraUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);

        if (authenticatedUser == null) {
            log.debug("Authenticated user is null, returning false");
            return false;
        }

        log.debug("Checking canDeleteUserWithoutProfile for user: {}, target: {}",
                authenticatedUser.getEmail(), entraUserId);

        // Check if target user exists - lookup by database ID (not entra_oid)
        Optional<EntraUser> targetUserOpt = entraUserRepository.findById(UUID.fromString(entraUserId));
        if (targetUserOpt.isEmpty()) {
            log.debug("Target user not found with ID: {}", entraUserId);
            return false;
        }

        EntraUser targetUser = targetUserOpt.get();

        // Cannot delete internal users (even if they somehow have no profile)
        if (targetUser.getUserProfiles() != null && targetUser.getUserProfiles().stream()
                .anyMatch(p -> p.getUserType() == UserType.INTERNAL)) {
            log.debug("Target user is internal, cannot delete");
            return false;
        }

        // Check roles and permissions
        boolean hasGlobalAdmin = userHasAuthzRole(authenticatedUser, "Global Admin");
        boolean hasQualityAssurance = userHasAuthzRole(authenticatedUser, "Quality & Assurance");
        boolean hasDeletePermission = userHasPermission(authenticatedUser, Permission.DELETE_AUDIT_USER);

        log.debug(
                "Authorization checks - Global Admin: {}, Quality & Assurance: {}, DELETE_AUDIT_USER: {}",
                hasGlobalAdmin, hasQualityAssurance, hasDeletePermission);

        // Require DELETE_AUDIT_USER permission AND either:
        // 1. Global Admin role alone, OR
        // 2. Both Quality & Assurance AND Global Admin roles
        boolean hasRequiredRoles = hasGlobalAdmin || (hasQualityAssurance && hasGlobalAdmin);

        return hasDeletePermission && hasRequiredRoles;
    }

    public boolean canDisableUser(String entraUserId) {
        Optional<EntraUserDto> accessedUserOptional = userService.getEntraUserById(entraUserId);
        if (accessedUserOptional.isEmpty()) {
            return false;
        }

        EntraUserDto accessedUser = accessedUserOptional.get();
        boolean isAccessedUserInternal = userService.isInternal(entraUserId);
        if (isAccessedUserInternal) {
            return false;
        }


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);

        boolean isInternalUser = userService.isInternal(authenticatedUser.getId());
        if (!isInternalUser && accessedUser.isMultiFirmUser()) {
            return false;
        }

        // FUM can only delete own firm user
        boolean isFirmUserManager = isFirmUserManager(authenticatedUser);
        UserProfile activeProfile = entraUserRepository.findByIdWithAssociations(UUID.fromString(entraUserId))
                        .map(EntraUser::getUserProfiles)
                        .orElseGet(HashSet::new)
                        .stream()
                        .filter(UserProfile::isActiveProfile)
                        .findFirst()
                        .orElse(null);
        boolean sameFirm = activeProfile != null && usersAreInSameFirm(authenticatedUser, activeProfile.getId().toString());
        if (isFirmUserManager && !sameFirm) {
            return false;
        }

        return !accessedUser.getId().equals(authenticatedUser.getId().toString())
                && userHasPermission(authenticatedUser, Permission.DISABLE_EXTERNAL_USER);
    }

    /**
     * Check if the authenticated user can delete a specific firm profile.
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
        if (accessedUserProfile.getEntraUser() == null
                || !accessedUserProfile.getEntraUser().isMultiFirmUser()) {
            return false;
        }

        // Check if authenticated user is internal (LAA staff)
        boolean isInternalUser = userService.isInternal(authenticatedUser.getId());

        // Internal User with delegate role can revoke firm access
        if (isInternalUser) {
            return userHasPermission(authenticatedUser, Permission.DELEGATE_EXTERNAL_USER_ACCESS_INTERNAL);
        }

        // Check if users are in the same firm
        boolean sameFirm = usersAreInSameFirm(authenticatedUser, userProfileId);

        // External users (firm admins) with DELEGATE_EXTERNAL_USER_ACCESS can only
        // delete profiles from their own firm
        return sameFirm
                && userHasPermission(authenticatedUser, Permission.DELEGATE_EXTERNAL_USER_ACCESS);
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
            log.warn("User {} does not have permission to edit this userId {}",
                    authenticatedUser.getId(), userProfileId);
        }
        return canAccess;
    }

    private boolean isFirmUserManager(EntraUser user) {
        return Optional.ofNullable(user)
                .map(EntraUser::getUserProfiles)
                .orElse(Set.of())
                .stream()
                .filter(p -> p != null && p.isActiveProfile())
                .findFirst()
                .map(p -> Optional.ofNullable(p.getAppRoles())
                        .orElse(Set.of())
                        .stream()
                        .anyMatch(r ->
                                r != null
                                        && r.getName() != null
                                        && AuthzRole.FIRM_USER_MANAGER.getRoleName().equals(r.getName())
                        )
                )
                .orElse(false);
    }

    private boolean usersAreInSameFirm(EntraUser authenticatedUser, String accessedUserProfileId) {
        List<UUID> userManagerFirms = firmService.getUserActiveAllFirms(authenticatedUser).stream()
                .map(FirmDto::getId).toList();
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

    public boolean authenticatedUserIsInternal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);
        return userService.isInternal(authenticatedUser.getId());
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
        return user.getUserProfiles().stream().filter(UserProfile::isActiveProfile)
                .flatMap(userProfile -> userProfile.getAppRoles().stream())
                .anyMatch(appRole -> appRole.isAuthzRole() && appRole.getName() != null
                        && appRole.getName().equalsIgnoreCase(authzRoleName));
    }

    public boolean userHasAuthzRole(Authentication authentication, String authzRoleName) {
        EntraUser user = loginService.getCurrentEntraUser(authentication);
        return userHasAuthzRole(user, authzRoleName);
    }

    public static boolean userHasAnyGivenPermissions(EntraUser entraUser,
            Permission... permissions) {
        Set<Permission> userPermissions = entraUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .flatMap(userProfile -> userProfile.getAppRoles().stream())
                .filter(AppRole::isAuthzRole).flatMap(appRole -> appRole.getPermissions().stream())
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
                && userHasAnyGivenPermissions(authenticatedUser, Permission.CREATE_EXTERNAL_USER,
                        Permission.EDIT_EXTERNAL_USER);
    }

}
