package uk.gov.justice.laa.portal.landingpage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring.addListAppenderToLogger;

@ExtendWith(MockitoExtension.class)
public class AccessControlServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private FirmService firmService;

    @Mock
    private LoginService loginService;

    @InjectMocks
    private AccessControlService accessControlService;

    @Test
    public void testCanAccessUserInternalUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        UUID accessedUserId = UUID.randomUUID();
        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder().entraUser(accessedUser).build();
        Permission userPermission = Permission.VIEW_INTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        EntraUser entraUser = EntraUser.builder().id(userId).email("test@email.com").userProfiles(HashSet.newHashSet(1)).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .entraUser(entraUser).appRoles(Set.of(appRole)).userType(UserType.INTERNAL).build();
        entraUser.getUserProfiles().add(userProfile);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        Mockito.when(userService.isInternal(accessedUserId.toString())).thenReturn(Boolean.TRUE);
        Mockito.when(userService.getUserProfileById(any())).thenReturn(Optional.of(accessedUserProfile));

        boolean result = accessControlService.canAccessUser(accessedUserId.toString());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testCanAccessUserExternalSameFirm() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        UUID accessedUserId = UUID.randomUUID();
        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder().activeProfile(true).entraUser(accessedUser).build();
        Permission userPermission = Permission.VIEW_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        EntraUser entraUser = EntraUser.builder().id(userId).email("test@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .entraUser(entraUser).appRoles(Set.of(appRole)).userType(UserType.EXTERNAL).build();
        entraUser.getUserProfiles().add(userProfile);

        UUID firmId = UUID.randomUUID();
        FirmDto firmDto = FirmDto.builder().id(firmId).build();
        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        Mockito.when(firmService.getUserActiveAllFirms(entraUser)).thenReturn(List.of(firmDto));
        Mockito.when(firmService.getUserFirmsByUserId(any())).thenReturn(List.of(firmDto));
        Mockito.when(userService.getUserProfileById(any())).thenReturn(Optional.of(accessedUserProfile));


        boolean result = accessControlService.canAccessUser(userId.toString());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testInternalUserWithExternalUserManagerRoleCanAccessExternalUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        UUID accessedUserId = UUID.randomUUID();

        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder()
                .activeProfile(true)
                .id(accessedUserId)
                .userType(UserType.EXTERNAL)
                .entraUser(accessedUser)
                .build();

        Permission userPermission = Permission.VIEW_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        EntraUser authenticatedUser = EntraUser.builder().id(userId).email("internal@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        UserProfile authenticatedUserProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(authenticatedUser)
                .appRoles(Set.of(appRole))
                .userType(UserType.INTERNAL) // Internal user type
                .build();
        authenticatedUser.getUserProfiles().add(authenticatedUserProfile);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(authenticatedUser);
        Mockito.when(userService.getUserProfileById(accessedUserId.toString())).thenReturn(Optional.of(accessedUserProfile));
        Mockito.when(userService.isInternal(accessedUserId.toString())).thenReturn(false);
        Mockito.when(userService.isInternal(userId)).thenReturn(true);

        boolean result = accessControlService.canAccessUser(accessedUserId.toString());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testInternalUserWithExternalUserManagerRoleCanEditExternalUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        UUID accessedUserId = UUID.randomUUID();

        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder()
                .activeProfile(true)
                .id(accessedUserId)
                .userType(UserType.EXTERNAL)
                .entraUser(accessedUser)
                .build();

        Permission userPermission = Permission.EDIT_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        EntraUser authenticatedUser = EntraUser.builder().id(userId).email("internal@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        UserProfile authenticatedUserProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(authenticatedUser)
                .appRoles(Set.of(appRole))
                .userType(UserType.INTERNAL) // Internal user type
                .build();
        authenticatedUser.getUserProfiles().add(authenticatedUserProfile);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(authenticatedUser);
        Mockito.when(userService.getUserProfileById(accessedUserId.toString())).thenReturn(Optional.of(accessedUserProfile));
        Mockito.when(userService.isInternal(accessedUserId.toString())).thenReturn(false);
        Mockito.when(userService.isInternal(userId)).thenReturn(true);

        boolean result = accessControlService.canEditUser(accessedUserId.toString());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testCanAccessUserFalseExternalDifferentFirm() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(userId).email("test@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        Permission userPermission = Permission.VIEW_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .entraUser(entraUser).userType(UserType.EXTERNAL).appRoles(Set.of(appRole)).build();
        entraUser.getUserProfiles().add(userProfile);

        CurrentUserDto entraUserDto = new CurrentUserDto();
        entraUserDto.setName("test");

        FirmDto firmDto1 = FirmDto.builder().id(UUID.randomUUID()).build();
        FirmDto firmDto2 = FirmDto.builder().id(UUID.randomUUID()).build();
        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        Mockito.when(loginService.getCurrentUser(authentication)).thenReturn(entraUserDto);
        UUID accessedUserId = UUID.randomUUID();
        Mockito.when(firmService.getUserActiveAllFirms(entraUser)).thenReturn(List.of(firmDto1));
        Mockito.when(firmService.getUserFirmsByUserId(any())).thenReturn(List.of(firmDto2));
        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).email("test2@email.com").build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder().activeProfile(true).entraUser(accessedUser).build();
        Mockito.when(userService.getUserProfileById(any())).thenReturn(Optional.of(accessedUserProfile));


        boolean result = accessControlService.canAccessUser(userId.toString());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testCanAccessUserFalseUserFirmsEmpty() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(userId).email("test@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        Permission userPermission = Permission.VIEW_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .entraUser(entraUser).userType(UserType.EXTERNAL).appRoles(Set.of(appRole)).build();
        entraUser.getUserProfiles().add(userProfile);

        FirmDto firmDto = FirmDto.builder().id(UUID.randomUUID()).build();
        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        Mockito.when(firmService.getUserActiveAllFirms(entraUser)).thenReturn(List.of(firmDto));
        Mockito.when(firmService.getUserFirmsByUserId(any())).thenReturn(Collections.emptyList());
        CurrentUserDto entraUserDto = new CurrentUserDto();
        entraUserDto.setName("test");
        UUID accessedUserId = UUID.randomUUID();
        Mockito.when(loginService.getCurrentUser(authentication)).thenReturn(entraUserDto);
        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder().activeProfile(true).entraUser(accessedUser).build();
        Mockito.when(userService.getUserProfileById(any())).thenReturn(Optional.of(accessedUserProfile));

        boolean result = accessControlService.canAccessUser(userId.toString());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testCanEditUserInternalUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID authenticatedUserId = UUID.randomUUID();
        UUID accessedUserId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(authenticatedUserId).email("test@email.com").userProfiles(HashSet.newHashSet(1)).build();
        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).email("test2@email.com").build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder().id(UUID.randomUUID()).entraUser(accessedUser).userType(UserType.INTERNAL).build();
        Permission userPermission = Permission.EDIT_INTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .entraUser(entraUser).userType(UserType.INTERNAL).appRoles(Set.of(appRole)).build();
        entraUser.getUserProfiles().add(userProfile);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        Mockito.when(userService.getUserProfileById(any())).thenReturn(Optional.of(accessedUserProfile));
        Mockito.when(userService.isInternal(anyString())).thenReturn(true);

        boolean result = accessControlService.canEditUser(accessedUserProfile.getId().toString());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testCanEditUserExternalSameFirm() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        UUID accessedUserId = UUID.randomUUID();
        Permission userPermission = Permission.EDIT_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        EntraUser entraUser = EntraUser.builder().id(userId).email("test@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).email("test2@email.com").build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder().entraUser(accessedUser).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .entraUser(entraUser).userType(UserType.EXTERNAL).appRoles(Set.of(appRole)).build();
        entraUser.getUserProfiles().add(userProfile);

        UUID firmId = UUID.randomUUID();
        FirmDto firmDto = FirmDto.builder().id(firmId).build();
        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        Mockito.when(firmService.getUserActiveAllFirms(entraUser)).thenReturn(List.of(firmDto));
        Mockito.when(firmService.getUserFirmsByUserId(accessedUserId.toString())).thenReturn(List.of(firmDto));
        Mockito.when(userService.getUserProfileById(accessedUserId.toString())).thenReturn(Optional.of(accessedUserProfile));


        boolean result = accessControlService.canEditUser(accessedUserId.toString());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testCanEditUserFalseExternalDifferentFirmAndLog() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        UUID accessedUserId = UUID.randomUUID();
        Permission userPermission = Permission.EDIT_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        EntraUser entraUser = EntraUser.builder().id(userId).email("test@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).email("test2@email.com").build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder().entraUser(accessedUser).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .entraUser(entraUser).appRoles(Set.of(appRole)).userType(UserType.EXTERNAL).build();
        entraUser.getUserProfiles().add(userProfile);

        CurrentUserDto entraUserDto = new CurrentUserDto();
        entraUserDto.setName("test");
        Mockito.when(userService.getUserProfileById(accessedUserId.toString())).thenReturn(Optional.of(accessedUserProfile));
        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        FirmDto firmDto1 = FirmDto.builder().id(UUID.randomUUID()).build();
        Mockito.when(firmService.getUserActiveAllFirms(entraUser)).thenReturn(List.of(firmDto1));
        FirmDto firmDto2 = FirmDto.builder().id(UUID.randomUUID()).build();
        Mockito.when(firmService.getUserFirmsByUserId(accessedUserId.toString())).thenReturn(List.of(firmDto2));
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(AccessControlService.class);

        boolean result = accessControlService.canEditUser(accessedUserId.toString());
        Assertions.assertThat(result).isFalse();
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.WARN);
        assertEquals(1, infoLogs.size());
        assertTrue(infoLogs.get(0).toString().contains("does not have permission to edit this userId"));
    }

    @Test
    public void testCanSendVerificationEmail() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        UUID accessedUserId = UUID.randomUUID();

        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder()
                .activeProfile(true)
                .id(accessedUserId)
                .userType(UserType.EXTERNAL)
                .entraUser(accessedUser)
                .build();

        Permission userPermission = Permission.EDIT_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        EntraUser authenticatedUser = EntraUser.builder().id(userId).email("internal@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        UserProfile authenticatedUserProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(authenticatedUser)
                .appRoles(Set.of(appRole))
                .userType(UserType.INTERNAL) // Internal user type
                .build();
        authenticatedUser.getUserProfiles().add(authenticatedUserProfile);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(authenticatedUser);
        Mockito.when(userService.getUserProfileById(accessedUserId.toString())).thenReturn(Optional.of(accessedUserProfile));
        Mockito.when(userService.isInternal(accessedUserId.toString())).thenReturn(false);
        Mockito.when(userService.isInternal(userId)).thenReturn(true);

        boolean result = accessControlService.canSendVerificationEmail(accessedUserId.toString());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testFirmAdminCannotSendVerificationEmail() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        UUID accessedUserId = UUID.randomUUID();

        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder()
                .activeProfile(true)
                .id(accessedUserId)
                .userType(UserType.EXTERNAL)
                .entraUser(accessedUser)
                .build();

        Permission userPermission = Permission.EDIT_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        EntraUser authenticatedUser = EntraUser.builder().id(userId).email("external@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        UserProfile authenticatedUserProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(authenticatedUser)
                .appRoles(Set.of(appRole))
                .userType(UserType.EXTERNAL) // External user type
                .build();
        authenticatedUser.getUserProfiles().add(authenticatedUserProfile);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(authenticatedUser);
        Mockito.when(userService.getUserProfileById(accessedUserId.toString())).thenReturn(Optional.of(accessedUserProfile));
        Mockito.when(userService.isInternal(userId)).thenReturn(false);

        boolean result = accessControlService.canSendVerificationEmail(accessedUserId.toString());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testGlobalAdminCanDeleteExternalUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID adminId = UUID.randomUUID();
        Permission userPermission = Permission.DELETE_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        EntraUser admin = EntraUser.builder().id(adminId).userProfiles(HashSet.newHashSet(1)).build();
        UserProfile adminProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(admin)
                .appRoles(Set.of(appRole))
                .userType(UserType.INTERNAL)
                .build();
        admin.getUserProfiles().add(adminProfile);

        UUID targetProfileId = UUID.randomUUID();
        EntraUserDto targetEntra = EntraUserDto.builder().id(UUID.randomUUID().toString()).build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(admin);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));

        boolean canDelete = accessControlService.canDeleteUser(targetProfileId.toString());
        Assertions.assertThat(canDelete).isTrue();
    }

    @Test
    public void testInternalUserCannotDeleteInternalUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        Permission userPermission = Permission.DELETE_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        EntraUser user = EntraUser.builder().id(userId).userProfiles(HashSet.newHashSet(1)).build();
        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(user)
                .appRoles(Set.of(appRole))
                .userType(UserType.INTERNAL)
                .build();
        user.getUserProfiles().add(userProfile);

        UUID internalTargetId = UUID.randomUUID();
        EntraUserDto internalTargetEntra = EntraUserDto.builder().id(UUID.randomUUID().toString()).build();
        UserProfileDto internalTarget = UserProfileDto.builder()
                .id(internalTargetId)
                .userType(UserType.INTERNAL)
                .entraUser(internalTargetEntra)
                .build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        Mockito.when(userService.getUserProfileById(internalTargetId.toString())).thenReturn(Optional.of(internalTarget));

        boolean canDeleteInternal = accessControlService.canDeleteUser(internalTargetId.toString());
        Assertions.assertThat(canDeleteInternal).isFalse();
    }

}
