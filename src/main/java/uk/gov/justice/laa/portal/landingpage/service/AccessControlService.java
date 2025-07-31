package uk.gov.justice.laa.portal.landingpage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
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

    public boolean canAccessUser(String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);

        if (userHasPermission(authenticatedUser, Permission.VIEW_INTERNAL_USER) && userHasPermission(authenticatedUser, Permission.VIEW_EXTERNAL_USER)) {
            return true;
        }

        if (userHasPermission(authenticatedUser, Permission.VIEW_INTERNAL_USER) && userService.isInternal(userId)) {
            return true;
        }

        boolean canAccess = userHasPermission(authenticatedUser, Permission.VIEW_EXTERNAL_USER) && !userService.isInternal(userId) && usersAreInSameFirm(authenticatedUser, userId);
        if (!canAccess) {
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            log.warn("User {} does not have permission to access this userId {}", currentUserDto.getName(), userId);
        }
        return canAccess;
    }

    public boolean canEditUser(String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser authenticatedUser = loginService.getCurrentEntraUser(authentication);

        Optional<EntraUserDto> optionalAccessedUser = userService.getEntraUserById(userId);
        if (optionalAccessedUser.isEmpty()) {
            return false;
        }

        // Only global admin should have both these permissions.
        if (userHasPermission(authenticatedUser, Permission.EDIT_INTERNAL_USER) && userHasPermission(authenticatedUser, Permission.EDIT_EXTERNAL_USER)) {
            return true;
        }

        EntraUserDto accessedUser = optionalAccessedUser.get();
        boolean internalManagerCanEditInternalUser = userHasPermission(authenticatedUser, Permission.EDIT_INTERNAL_USER) && userService.isInternal(accessedUser.getId());
        if (internalManagerCanEditInternalUser) {
            return true;
        }

        boolean canAccess = userHasPermission(authenticatedUser, Permission.EDIT_EXTERNAL_USER) && usersAreInSameFirm(authenticatedUser, userId);
        if (!canAccess) {
            log.warn("User {} does not have permission to edit this userId {}", authenticatedUser.getId(), userId);
        }
        return canAccess;
    }

    private boolean usersAreInSameFirm(EntraUser authenticatedUser, String accessedUserId) {
        List<UUID> userManagerFirms = firmService.getUserAllFirms(authenticatedUser).stream().map(FirmDto::getId).toList();
        List<FirmDto> userFirms = firmService.getUserFirmsByUserId(accessedUserId);
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
