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
import uk.gov.justice.laa.portal.landingpage.entity.*;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Mock
    private EntraUserRepository entraUserRepository;

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
        EntraUser entraUser = EntraUser.builder().id(userId).email("test@email.com").userProfiles(HashSet.newHashSet(1))
                .build();
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
    public void testCannotAccessUserWithNoProfiles() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID accessedUserId = UUID.randomUUID();

        Mockito.when(userService.getUserProfileById(any())).thenReturn(Optional.empty());

        boolean result = accessControlService.canAccessUser(accessedUserId.toString());
        Assertions.assertThat(result).isFalse();
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
        UserProfileDto accessedUserProfile = UserProfileDto.builder().activeProfile(true).entraUser(accessedUser)
                .build();
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
        Mockito.when(userService.getUserProfileById(accessedUserId.toString()))
                .thenReturn(Optional.of(accessedUserProfile));
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
        Mockito.when(userService.getUserProfileById(accessedUserId.toString()))
                .thenReturn(Optional.of(accessedUserProfile));
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

        FirmDto firmDto = FirmDto.builder().id(UUID.randomUUID()).build();
        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        Mockito.when(loginService.getCurrentUser(authentication)).thenReturn(entraUserDto);
        UUID accessedUserId = UUID.randomUUID();
        Mockito.when(firmService.getUserFirmsByUserId(any())).thenReturn(List.of(firmDto));
        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).email("test2@email.com")
                .build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder().activeProfile(true).entraUser(accessedUser)
                .build();
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
        CurrentUserDto entraUserDto = new CurrentUserDto();
        entraUserDto.setName("test");
        UUID accessedUserId = UUID.randomUUID();
        Mockito.when(loginService.getCurrentUser(authentication)).thenReturn(entraUserDto);
        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder().activeProfile(true).entraUser(accessedUser)
                .build();
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
        EntraUser entraUser = EntraUser.builder().id(authenticatedUserId).email("test@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).email("test2@email.com")
                .build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder().id(UUID.randomUUID()).entraUser(accessedUser)
                .userType(UserType.INTERNAL).build();
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
        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).email("test2@email.com")
                .build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder().entraUser(accessedUser).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .entraUser(entraUser).userType(UserType.EXTERNAL).appRoles(Set.of(appRole)).build();
        entraUser.getUserProfiles().add(userProfile);

        UUID firmId = UUID.randomUUID();
        FirmDto firmDto = FirmDto.builder().id(firmId).build();
        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        Mockito.when(firmService.getUserActiveAllFirms(entraUser)).thenReturn(List.of(firmDto));
        Mockito.when(firmService.getUserFirmsByUserId(accessedUserId.toString())).thenReturn(List.of(firmDto));
        Mockito.when(userService.getUserProfileById(accessedUserId.toString()))
                .thenReturn(Optional.of(accessedUserProfile));

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
        EntraUserDto accessedUser = EntraUserDto.builder().id(accessedUserId.toString()).email("test2@email.com")
                .build();
        UserProfileDto accessedUserProfile = UserProfileDto.builder().entraUser(accessedUser).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .entraUser(entraUser).appRoles(Set.of(appRole)).userType(UserType.EXTERNAL).build();
        entraUser.getUserProfiles().add(userProfile);

        CurrentUserDto entraUserDto = new CurrentUserDto();
        entraUserDto.setName("test");
        Mockito.when(userService.getUserProfileById(accessedUserId.toString()))
                .thenReturn(Optional.of(accessedUserProfile));
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
        assertTrue(infoLogs.getFirst().toString().contains("does not have permission to edit this userId"));
    }

    @Test
    public void testExternalParentFirmUserCanAccessChildFirmUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID parentFirmId = UUID.randomUUID();
        UUID childFirmId = UUID.randomUUID();

        EntraUser authUser = EntraUser.builder().id(UUID.randomUUID()).email("parent@firm.test")
                .userProfiles(HashSet.newHashSet(1)).build();
        Firm parent = Firm.builder().id(parentFirmId).build();
        Firm child = Firm.builder().id(childFirmId).parentFirm(parent).build();
        parent.setChildFirms(Set.of(child));
        Permission viewExt = Permission.VIEW_EXTERNAL_USER;
        AppRole authz = AppRole.builder().authzRole(true).permissions(Set.of(viewExt)).build();
        UserProfile authProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(authUser)
                .userType(UserType.EXTERNAL)
                .firm(parent)
                .appRoles(Set.of(authz))
                .build();
        authUser.getUserProfiles().add(authProfile);

        UUID accessedProfileId = UUID.randomUUID();
        EntraUserDto accessedEntra = EntraUserDto.builder().id(UUID.randomUUID().toString()).build();
        UserProfileDto accessedProfile = UserProfileDto.builder().id(accessedProfileId).userType(UserType.EXTERNAL)
                .entraUser(accessedEntra).build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(authUser);
        Mockito.when(firmService.getUserActiveAllFirms(authUser)).thenReturn(List.of(
                FirmDto.builder().id(parentFirmId).build(),
                FirmDto.builder().id(childFirmId).build()));
        Mockito.when(userService.getUserProfileById(accessedProfileId.toString()))
                .thenReturn(Optional.of(accessedProfile));
        Mockito.when(userService.isInternal(accessedEntra.getId())).thenReturn(false);
        Mockito.when(userService.isInternal(authUser.getId())).thenReturn(false);

        Mockito.when(firmService.getUserFirmsByUserId(accessedProfileId.toString()))
                .thenReturn(List.of(FirmDto.builder().id(childFirmId).build()));

        boolean canAccess = accessControlService.canAccessUser(accessedProfileId.toString());
        Assertions.assertThat(canAccess).isTrue();
    }

    @Test
    public void testExternalChildFirmUserCannotAccessParentFirmUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID parentFirmId = UUID.randomUUID();
        UUID childFirmId = UUID.randomUUID();

        EntraUser authUser = EntraUser.builder().id(UUID.randomUUID()).email("child@firm.test")
                .userProfiles(HashSet.newHashSet(1)).build();
        Firm parent = Firm.builder().id(parentFirmId).build();
        Firm child = Firm.builder().id(childFirmId).parentFirm(parent).build();
        Permission viewExt = Permission.VIEW_EXTERNAL_USER;
        AppRole authz = AppRole.builder().authzRole(true).permissions(Set.of(viewExt)).build();
        UserProfile authProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(authUser)
                .userType(UserType.EXTERNAL)
                .firm(child)
                .appRoles(Set.of(authz))
                .build();
        authUser.getUserProfiles().add(authProfile);

        UUID accessedProfileId = UUID.randomUUID();
        EntraUserDto accessedEntra = EntraUserDto.builder().id(UUID.randomUUID().toString()).build();
        UserProfileDto accessedProfile = UserProfileDto.builder().id(accessedProfileId).userType(UserType.EXTERNAL)
                .entraUser(accessedEntra).build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(authUser);
        Mockito.when(userService.getUserProfileById(accessedProfileId.toString()))
                .thenReturn(Optional.of(accessedProfile));
        Mockito.when(userService.isInternal(accessedEntra.getId())).thenReturn(false);
        Mockito.when(userService.isInternal(authUser.getId())).thenReturn(false);

        // Ensure logging path has a non-null CurrentUserDto to prevent NPE when access
        // is denied
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("test");
        Mockito.when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);

        Mockito.when(firmService.getUserFirmsByUserId(accessedProfileId.toString()))
                .thenReturn(List.of(FirmDto.builder().id(parentFirmId).build()));

        boolean canAccess = accessControlService.canAccessUser(accessedProfileId.toString());
        Assertions.assertThat(canAccess).isFalse();
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
        Mockito.when(userService.getUserProfileById(accessedUserId.toString()))
                .thenReturn(Optional.of(accessedUserProfile));
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
        Mockito.when(userService.getUserProfileById(accessedUserId.toString()))
                .thenReturn(Optional.of(accessedUserProfile));
        Mockito.when(userService.isInternal(userId)).thenReturn(false);

        boolean result = accessControlService.canSendVerificationEmail(accessedUserId.toString());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void isUserManager_userNotProviderAdmin() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        UUID appRoleId = UUID.randomUUID();

        Permission userPermission = Permission.EDIT_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().id(appRoleId).name("Firm User").authzRole(true)
                .permissions(Set.of(userPermission)).build();
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

        boolean result = accessControlService
                .authenticatedUserHasAnyGivenPermissions(Permission.DELEGATE_EXTERNAL_USER_ACCESS);
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void isUserManager() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        UUID appRoleId = UUID.randomUUID();

        Permission userPermission = Permission.DELEGATE_EXTERNAL_USER_ACCESS;
        AppRole appRole = AppRole.builder().id(appRoleId).name("Firm User Manager").authzRole(true)
                .permissions(Set.of(userPermission)).build();
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

        boolean result = accessControlService
                .authenticatedUserHasAnyGivenPermissions(Permission.DELEGATE_EXTERNAL_USER_ACCESS);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testCannotDeleteUserWithNoProfiles() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID accessedUserId = UUID.randomUUID();

        Mockito.when(userService.getUserProfileById(any())).thenReturn(Optional.empty());

        boolean result = accessControlService.canDeleteUser(accessedUserId.toString());
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
    public void testGlobalAdminCanDeleteMultiFirmExternalUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID adminId = UUID.randomUUID();
        Permission userPermission = Permission.DELETE_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).name(AuthzRole.GLOBAL_ADMIN.getRoleName())
                .permissions(Set.of(userPermission)).build();
        EntraUser admin = EntraUser.builder().id(adminId).userProfiles(HashSet.newHashSet(1)).build();
        UserProfile adminProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(admin)
                .appRoles(Set.of(appRole))
                .userType(UserType.INTERNAL)
                .build();
        admin.getUserProfiles().add(adminProfile);

        UUID targetProfileId = UUID.randomUUID();
        EntraUserDto targetEntra = EntraUserDto.builder().id(UUID.randomUUID().toString()).multiFirmUser(true).build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .build();

        Mockito.when(userService.isInternal(adminId)).thenReturn(true);
        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(admin);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));

        boolean canDelete = accessControlService.canDeleteUser(targetProfileId.toString());
        Assertions.assertThat(canDelete).isTrue();
    }

    @Test
    public void testInternalUserCanDeleteMultiFirmUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID adminId = UUID.randomUUID();
        Permission userPermission = Permission.DELETE_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).name(AuthzRole.GLOBAL_ADMIN.getRoleName()).permissions(Set.of(userPermission)).build();
        EntraUser admin = EntraUser.builder().id(adminId).userProfiles(HashSet.newHashSet(1)).build();
        UserProfile adminProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(admin)
                .appRoles(Set.of(appRole))
                .userType(UserType.INTERNAL)
                .build();
        admin.getUserProfiles().add(adminProfile);

        UUID targetProfileId = UUID.randomUUID();
        EntraUserDto targetEntra = EntraUserDto.builder().id(UUID.randomUUID().toString()).multiFirmUser(true).build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .build();

        Mockito.when(userService.isInternal(adminId)).thenReturn(true);
        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(admin);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));

        boolean canDelete = accessControlService.canDeleteUser(targetProfileId.toString());
        Assertions.assertThat(canDelete).isTrue();
    }

    @Test
    public void testFirmUserManagerCannotDeleteMultiFirmUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID adminId = UUID.randomUUID();
        Permission userPermission = Permission.DELETE_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).name(AuthzRole.FIRM_USER_MANAGER.getRoleName()).permissions(Set.of(userPermission)).build();
        EntraUser admin = EntraUser.builder().id(adminId).userProfiles(HashSet.newHashSet(1)).build();
        UserProfile adminProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(admin)
                .appRoles(Set.of(appRole))
                .userType(UserType.EXTERNAL)
                .build();
        admin.getUserProfiles().add(adminProfile);

        UUID targetProfileId = UUID.randomUUID();
        EntraUserDto targetEntra = EntraUserDto.builder().id(UUID.randomUUID().toString()).multiFirmUser(true).build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .build();

        Mockito.when(userService.isInternal(adminId)).thenReturn(false);
        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(admin);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));

        boolean canDelete = accessControlService.canDeleteUser(targetProfileId.toString());
        Assertions.assertThat(canDelete).isFalse();
    }

    @Test
    public void testFirmUserManagerCannotDeleteDifferentFirmUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID adminId = UUID.randomUUID();
        Permission userPermission = Permission.DELETE_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).name(AuthzRole.FIRM_USER_MANAGER.getRoleName()).permissions(Set.of(userPermission)).build();
        EntraUser admin = EntraUser.builder().id(adminId).userProfiles(HashSet.newHashSet(1)).build();
        Firm adminFirm = Firm.builder().id(UUID.randomUUID()).name("Admin Firm").build();
        UserProfile adminProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(admin)
                .firm(adminFirm)
                .appRoles(Set.of(appRole))
                .userType(UserType.EXTERNAL)
                .build();
        admin.getUserProfiles().add(adminProfile);

        UUID targetProfileId = UUID.randomUUID();
        FirmDto userFirm = FirmDto.builder().id(UUID.randomUUID()).name("User Firm").build();
        EntraUserDto targetEntra = EntraUserDto.builder().id(UUID.randomUUID().toString()).multiFirmUser(true).build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .firm(userFirm)
                .build();

        Mockito.when(userService.isInternal(adminId)).thenReturn(false);
        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(admin);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));

        boolean canDelete = accessControlService.canDeleteUser(targetProfileId.toString());
        Assertions.assertThat(canDelete).isFalse();
    }

    @Test
    public void testFirmUserManagerCanDeleteSameFirmUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID adminId = UUID.randomUUID();
        Permission userPermission = Permission.DELETE_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).name(AuthzRole.FIRM_USER_MANAGER.getRoleName()).permissions(Set.of(userPermission)).build();
        EntraUser admin = EntraUser.builder().id(adminId).userProfiles(HashSet.newHashSet(1)).build();
        Firm adminFirm = Firm.builder().id(UUID.randomUUID()).name("Admin Firm").build();
        UserProfile adminProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(admin)
                .firm(adminFirm)
                .appRoles(Set.of(appRole))
                .userType(UserType.EXTERNAL)
                .build();
        admin.getUserProfiles().add(adminProfile);

        UUID targetProfileId = UUID.randomUUID();
        FirmDto userFirm = FirmDto.builder().id(adminFirm.getId()).name(adminFirm.getName()).build();
        EntraUserDto targetEntra = EntraUserDto.builder().id(UUID.randomUUID().toString()).build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .firm(userFirm)
                .build();

        Mockito.when(userService.isInternal(adminId)).thenReturn(false);
        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(admin);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));
        Mockito.when(firmService.getUserActiveAllFirms(any(EntraUser.class))).thenReturn(List.of(userFirm));
        Mockito.when(firmService.getUserFirmsByUserId(anyString())).thenReturn(List.of(userFirm));

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
        Mockito.when(userService.getUserProfileById(internalTargetId.toString()))
                .thenReturn(Optional.of(internalTarget));

        boolean canDeleteInternal = accessControlService.canDeleteUser(internalTargetId.toString());
        Assertions.assertThat(canDeleteInternal).isFalse();
    }

    @Test
    public void testFirmUserManagerCanDeleteSingleFirmUserInSameFirm() {
        // Setup authentication
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Create Firm User Manager with DELETE_EXTERNAL_USER permission
        UUID fumId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        Permission deletePermission = Permission.DELETE_EXTERNAL_USER;
        AppRole fumRole = AppRole.builder()
                .id(UUID.randomUUID())
                .name("Firm User Manager")
                .authzRole(true)
                .permissions(Set.of(deletePermission))
                .build();
        EntraUser firmUserManager = EntraUser.builder()
                .id(fumId)
                .email("fum@lawfirm.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile fumProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(firmUserManager)
                .appRoles(Set.of(fumRole))
                .userType(UserType.EXTERNAL)
                .build();
        firmUserManager.getUserProfiles().add(fumProfile);

        // Target: Single-firm external user in the same firm
        UUID targetProfileId = UUID.randomUUID();
        EntraUserDto targetEntra = EntraUserDto.builder()
                .id(UUID.randomUUID().toString())
                .multiFirmUser(false)
                .build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .build();

        FirmDto firmDto = FirmDto.builder().id(firmId).build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(firmUserManager);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));
        Mockito.when(userService.isInternal(fumId)).thenReturn(false);
        Mockito.when(firmService.getUserActiveAllFirms(firmUserManager)).thenReturn(List.of(firmDto));
        Mockito.when(firmService.getUserFirmsByUserId(targetProfileId.toString())).thenReturn(List.of(firmDto));

        boolean canDelete = accessControlService.canDeleteUser(targetProfileId.toString());
        assertThat(canDelete).isTrue();
    }

    @Test
    public void testFirmUserManagerCannotDeleteMultiFirmUser() {

        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);


        UUID fumId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        Permission deletePermission = Permission.DELETE_EXTERNAL_USER;
        AppRole fumRole = AppRole.builder()
                .id(UUID.randomUUID())
                .name("Firm User Manager")
                .authzRole(true)
                .permissions(Set.of(deletePermission))
                .build();
        EntraUser firmUserManager = EntraUser.builder()
                .id(fumId)
                .email("fum@lawfirm.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile fumProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(firmUserManager)
                .appRoles(Set.of(fumRole))
                .userType(UserType.EXTERNAL)
                .build();
        firmUserManager.getUserProfiles().add(fumProfile);


        UUID targetProfileId = UUID.randomUUID();
        EntraUserDto targetEntra = EntraUserDto.builder()
                .id(UUID.randomUUID().toString())
                .multiFirmUser(true)
                .build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .build();

        FirmDto firmDto = FirmDto.builder().id(firmId).build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(firmUserManager);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));
        Mockito.when(userService.isInternal(fumId)).thenReturn(false);


        boolean canDelete = accessControlService.canDeleteUser(targetProfileId.toString());
        assertThat(canDelete).isFalse();
    }

    @Test
    public void testFirmUserManagerCannotDeleteUserFromDifferentFirm() {

        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);


        UUID fumId = UUID.randomUUID();
        UUID fumFirmId = UUID.randomUUID();
        UUID targetFirmId = UUID.randomUUID(); // Different firm
        Permission deletePermission = Permission.DELETE_EXTERNAL_USER;
        AppRole fumRole = AppRole.builder()
                .id(UUID.randomUUID())
                .name("Firm User Manager")
                .authzRole(true)
                .permissions(Set.of(deletePermission))
                .build();
        EntraUser firmUserManager = EntraUser.builder()
                .id(fumId)
                .email("fum@lawfirm.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile fumProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(firmUserManager)
                .appRoles(Set.of(fumRole))
                .userType(UserType.EXTERNAL)
                .build();
        firmUserManager.getUserProfiles().add(fumProfile);


        UUID targetProfileId = UUID.randomUUID();
        EntraUserDto targetEntra = EntraUserDto.builder()
                .id(UUID.randomUUID().toString())
                .multiFirmUser(false)
                .build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .build();

        FirmDto fumFirmDto = FirmDto.builder().id(fumFirmId).build();
        FirmDto targetFirmDto = FirmDto.builder().id(targetFirmId).build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(firmUserManager);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));
        Mockito.when(userService.isInternal(fumId)).thenReturn(false);
        Mockito.when(firmService.getUserActiveAllFirms(firmUserManager)).thenReturn(List.of(fumFirmDto));
        Mockito.when(firmService.getUserFirmsByUserId(targetProfileId.toString())).thenReturn(List.of(targetFirmDto));

        boolean canDelete = accessControlService.canDeleteUser(targetProfileId.toString());
        assertThat(canDelete).isFalse();
    }

    @Test
    public void testCanDeleteFirmProfile_InternalUserWithPermission_CannotDelete() {
        // Setup authentication
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Internal user with DELETE_EXTERNAL_USER permission
        // Internal users should NOT be able to delete firm profiles (only provider
        // admins can)
        UUID userId = UUID.randomUUID();
        Permission deletePermission = Permission.DELETE_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(deletePermission)).build();
        EntraUser internalUser = EntraUser.builder().id(userId).email("internal@justice.gov.uk")
                .userProfiles(HashSet.newHashSet(1)).build();
        UserProfile internalProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(internalUser)
                .appRoles(Set.of(appRole))
                .userType(UserType.INTERNAL)
                .build();
        internalUser.getUserProfiles().add(internalProfile);

        // Target: External multi-firm user profile
        UUID targetProfileId = UUID.randomUUID();
        EntraUserDto targetEntra = EntraUserDto.builder()
                .id(UUID.randomUUID().toString())
                .multiFirmUser(true)
                .build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(internalUser);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));
        Mockito.when(userService.isInternal(userId)).thenReturn(true);

        boolean result = accessControlService.canDeleteFirmProfile(targetProfileId.toString());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testCanDeleteFirmProfile_ExternalFirmAdminInSameFirm_CanDelete() {
        // Setup authentication
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // External firm admin with DELEGATE_EXTERNAL_USER_ACCESS permission
        UUID userId = UUID.randomUUID();
        Permission delegatePermission = Permission.DELEGATE_EXTERNAL_USER_ACCESS;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(delegatePermission)).build();
        EntraUser externalUser = EntraUser.builder().id(userId).email("admin@lawfirm.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        UserProfile externalProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(externalUser)
                .appRoles(Set.of(appRole))
                .userType(UserType.EXTERNAL)
                .build();
        externalUser.getUserProfiles().add(externalProfile);

        // Target: External multi-firm user profile in same firm
        UUID targetProfileId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        EntraUserDto targetEntra = EntraUserDto.builder()
                .id(UUID.randomUUID().toString())
                .multiFirmUser(true)
                .build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .build();

        FirmDto firmDto = FirmDto.builder().id(firmId).build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(externalUser);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));
        Mockito.when(userService.isInternal(userId)).thenReturn(false);
        Mockito.when(firmService.getUserActiveAllFirms(externalUser)).thenReturn(List.of(firmDto));
        Mockito.when(firmService.getUserFirmsByUserId(targetProfileId.toString())).thenReturn(List.of(firmDto));

        boolean result = accessControlService.canDeleteFirmProfile(targetProfileId.toString());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testCanDeleteFirmProfile_ProfileNotFound_ReturnsFalse() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).userProfiles(HashSet.newHashSet(1)).build();
        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        Mockito.when(userService.getUserProfileById(any())).thenReturn(Optional.empty());

        boolean result = accessControlService.canDeleteFirmProfile("non-existent-id");
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testCanDeleteFirmProfile_InternalUserProfile_ReturnsFalse() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).userProfiles(HashSet.newHashSet(1)).build();

        UUID targetProfileId = UUID.randomUUID();
        EntraUserDto targetEntra = EntraUserDto.builder().id(UUID.randomUUID().toString()).build();
        UserProfileDto internalProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.INTERNAL)
                .entraUser(targetEntra)
                .build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString()))
                .thenReturn(Optional.of(internalProfile));

        boolean result = accessControlService.canDeleteFirmProfile(targetProfileId.toString());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testCanDeleteFirmProfile_NotMultiFirmUser_ReturnsFalse() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).userProfiles(HashSet.newHashSet(1)).build();

        UUID targetProfileId = UUID.randomUUID();
        EntraUserDto targetEntra = EntraUserDto.builder()
                .id(UUID.randomUUID().toString())
                .multiFirmUser(false) // Not a multi-firm user
                .build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));

        boolean result = accessControlService.canDeleteFirmProfile(targetProfileId.toString());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testCanDeleteFirmProfile_ExternalUserDifferentFirm_ReturnsFalse() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        Permission delegatePermission = Permission.DELEGATE_EXTERNAL_USER_ACCESS;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(delegatePermission)).build();
        EntraUser externalUser = EntraUser.builder().id(userId).email("admin@lawfirm1.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        UserProfile externalProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(externalUser)
                .appRoles(Set.of(appRole))
                .userType(UserType.EXTERNAL)
                .build();
        externalUser.getUserProfiles().add(externalProfile);

        UUID targetProfileId = UUID.randomUUID();
        EntraUserDto targetEntra = EntraUserDto.builder()
                .id(UUID.randomUUID().toString())
                .multiFirmUser(true)
                .build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .build();

        UUID firmId1 = UUID.randomUUID();
        UUID firmId2 = UUID.randomUUID();
        FirmDto firmDto1 = FirmDto.builder().id(firmId1).build();
        FirmDto firmDto2 = FirmDto.builder().id(firmId2).build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(externalUser);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));
        Mockito.when(userService.isInternal(userId)).thenReturn(false);
        Mockito.when(firmService.getUserActiveAllFirms(externalUser)).thenReturn(List.of(firmDto1));
        Mockito.when(firmService.getUserFirmsByUserId(targetProfileId.toString())).thenReturn(List.of(firmDto2));

        boolean result = accessControlService.canDeleteFirmProfile(targetProfileId.toString());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testCanDeleteFirmProfile_ExternalUserNoPermission_ReturnsFalse() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        // External user WITHOUT DELEGATE_EXTERNAL_USER_ACCESS permission
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of()).build();
        EntraUser externalUser = EntraUser.builder().id(userId).email("user@lawfirm.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        UserProfile externalProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(externalUser)
                .appRoles(Set.of(appRole))
                .userType(UserType.EXTERNAL)
                .build();
        externalUser.getUserProfiles().add(externalProfile);

        UUID targetProfileId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        EntraUserDto targetEntra = EntraUserDto.builder()
                .id(UUID.randomUUID().toString())
                .multiFirmUser(true)
                .build();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(targetEntra)
                .build();

        FirmDto firmDto = FirmDto.builder().id(firmId).build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(externalUser);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));
        Mockito.when(userService.isInternal(userId)).thenReturn(false);
        Mockito.when(firmService.getUserActiveAllFirms(externalUser)).thenReturn(List.of(firmDto));
        Mockito.when(firmService.getUserFirmsByUserId(targetProfileId.toString())).thenReturn(List.of(firmDto));

        boolean result = accessControlService.canDeleteFirmProfile(targetProfileId.toString());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testCanDeleteFirmProfile_NullAuthenticatedUser_ReturnsFalse() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(null);

        boolean result = accessControlService.canDeleteFirmProfile("some-profile-id");
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testCanDeleteFirmProfile_TargetEntraUserNull_ReturnsFalse() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).userProfiles(HashSet.newHashSet(1)).build();

        UUID targetProfileId = UUID.randomUUID();
        UserProfileDto targetProfile = UserProfileDto.builder()
                .id(targetProfileId)
                .userType(UserType.EXTERNAL)
                .entraUser(null) // Target's EntraUser is null
                .build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        Mockito.when(userService.getUserProfileById(targetProfileId.toString())).thenReturn(Optional.of(targetProfile));

        boolean result = accessControlService.canDeleteFirmProfile(targetProfileId.toString());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testCanViewAllFirmsOfMultiFirmUser_WithPermission() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        Permission viewAllMultiFirmPermission = Permission.VIEW_ALL_USER_MULTI_FIRM_PROFILES;
        AppRole appRole = AppRole.builder()
                .authzRole(true)
                .permissions(Set.of(viewAllMultiFirmPermission))
                .build();
        EntraUser user = EntraUser.builder()
                .id(userId)
                .email("admin@example.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(user)
                .appRoles(Set.of(appRole))
                .userType(UserType.INTERNAL)
                .build();
        user.getUserProfiles().add(userProfile);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);

        boolean result = accessControlService.canViewAllFirmsOfMultiFirmUser();
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testCanViewAllFirmsOfMultiFirmUser_WithoutPermission() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        // User without VIEW_ALL_USER_MULTI_FIRM_PROFILES permission
        AppRole appRole = AppRole.builder()
                .authzRole(true)
                .permissions(Set.of(Permission.VIEW_EXTERNAL_USER))
                .build();
        EntraUser user = EntraUser.builder()
                .id(userId)
                .email("user@example.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(user)
                .appRoles(Set.of(appRole))
                .userType(UserType.EXTERNAL)
                .build();
        user.getUserProfiles().add(userProfile);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);

        boolean result = accessControlService.canViewAllFirmsOfMultiFirmUser();
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testUserHasAuthzRole_WithMatchingRole() {
        UUID userId = UUID.randomUUID();
        AppRole globalAdminRole = AppRole.builder()
                .name(AuthzRole.GLOBAL_ADMIN.getRoleName())
                .authzRole(true)
                .build();
        EntraUser user = EntraUser.builder()
                .id(userId)
                .email("admin@example.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(user)
                .appRoles(Set.of(globalAdminRole))
                .userType(UserType.INTERNAL)
                .build();
        user.getUserProfiles().add(userProfile);

        boolean result = AccessControlService.userHasAuthzRole(user, AuthzRole.GLOBAL_ADMIN.getRoleName());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testUserHasAuthzRole_WithoutMatchingRole() {
        UUID userId = UUID.randomUUID();
        AppRole someRole = AppRole.builder()
                .name("Some Other Role")
                .authzRole(true)
                .build();
        EntraUser user = EntraUser.builder()
                .id(userId)
                .email("user@example.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(user)
                .appRoles(Set.of(someRole))
                .userType(UserType.EXTERNAL)
                .build();
        user.getUserProfiles().add(userProfile);

        boolean result = AccessControlService.userHasAuthzRole(user, AuthzRole.GLOBAL_ADMIN.getRoleName());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testUserHasAuthzRole_CaseInsensitive() {
        UUID userId = UUID.randomUUID();
        AppRole globalAdminRole = AppRole.builder()
                .name(AuthzRole.GLOBAL_ADMIN.getRoleName())
                .authzRole(true)
                .build();
        EntraUser user = EntraUser.builder()
                .id(userId)
                .email("admin@example.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(user)
                .appRoles(Set.of(globalAdminRole))
                .userType(UserType.INTERNAL)
                .build();
        user.getUserProfiles().add(userProfile);

        boolean result = AccessControlService.userHasAuthzRole(user, "global admin");
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testUserHasAuthzRole_InactiveProfile() {
        UUID userId = UUID.randomUUID();
        AppRole globalAdminRole = AppRole.builder()
                .name(AuthzRole.GLOBAL_ADMIN.getRoleName())
                .authzRole(true)
                .build();
        EntraUser user = EntraUser.builder()
                .id(userId)
                .email("admin@example.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile inactiveProfile = UserProfile.builder()
                .activeProfile(false) // Inactive profile
                .entraUser(user)
                .appRoles(Set.of(globalAdminRole))
                .userType(UserType.INTERNAL)
                .build();
        user.getUserProfiles().add(inactiveProfile);

        boolean result = AccessControlService.userHasAuthzRole(user, AuthzRole.GLOBAL_ADMIN.getRoleName());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testUserHasAnyGivenPermissions_WithMultiplePermissions() {
        UUID userId = UUID.randomUUID();
        Permission perm1 = Permission.VIEW_EXTERNAL_USER;
        Permission perm2 = Permission.EDIT_EXTERNAL_USER;
        Permission perm3 = Permission.DELETE_EXTERNAL_USER;
        AppRole appRole = AppRole.builder()
                .authzRole(true)
                .permissions(Set.of(perm1, perm2))
                .build();
        EntraUser user = EntraUser.builder()
                .id(userId)
                .email("admin@example.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(user)
                .appRoles(Set.of(appRole))
                .userType(UserType.INTERNAL)
                .build();
        user.getUserProfiles().add(userProfile);

        boolean result = AccessControlService.userHasAnyGivenPermissions(user, perm1, perm3);
        Assertions.assertThat(result).isTrue(); // Has perm1
    }

    @Test
    public void testUserHasAnyGivenPermissions_WithNoMatchingPermissions() {
        UUID userId = UUID.randomUUID();
        Permission perm1 = Permission.VIEW_EXTERNAL_USER;
        Permission perm2 = Permission.EDIT_EXTERNAL_USER;
        Permission perm3 = Permission.DELETE_EXTERNAL_USER;
        AppRole appRole = AppRole.builder()
                .authzRole(true)
                .permissions(Set.of(perm1))
                .build();
        EntraUser user = EntraUser.builder()
                .id(userId)
                .email("user@example.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(user)
                .appRoles(Set.of(appRole))
                .userType(UserType.EXTERNAL)
                .build();
        user.getUserProfiles().add(userProfile);

        boolean result = AccessControlService.userHasAnyGivenPermissions(user, perm2, perm3);
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testUserHasPermission_SinglePermission() {
        UUID userId = UUID.randomUUID();
        Permission permission = Permission.VIEW_EXTERNAL_USER;
        AppRole appRole = AppRole.builder()
                .authzRole(true)
                .permissions(Set.of(permission))
                .build();
        EntraUser user = EntraUser.builder()
                .id(userId)
                .email("user@example.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(user)
                .appRoles(Set.of(appRole))
                .userType(UserType.EXTERNAL)
                .build();
        user.getUserProfiles().add(userProfile);

        boolean result = AccessControlService.userHasPermission(user, permission);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testAuthenticatedUserIsInternalReturnsTrueWhenUserIsInternal() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder()
                .id(userId)
                .email("user@example.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(user)
                .userType(UserType.INTERNAL)
                .build();
        user.getUserProfiles().add(userProfile);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        Mockito.when(userService.isInternal(userId)).thenReturn(true);
        boolean internal = accessControlService.authenticatedUserIsInternal();
        Assertions.assertThat(internal).isTrue();
    }

    @Test
    public void testAuthenticatedUserIsExternalReturnsFalseWhenUserIsExternal() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder()
                .id(userId)
                .email("user@example.com")
                .userProfiles(HashSet.newHashSet(1))
                .build();
        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .entraUser(user)
                .userType(UserType.EXTERNAL)
                .build();
        user.getUserProfiles().add(userProfile);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        Mockito.when(userService.isInternal(userId)).thenReturn(false);
        boolean internal = accessControlService.authenticatedUserIsInternal();
        Assertions.assertThat(internal).isFalse();
    }

    @Test
    void canDisableUser_returnsFalse_whenUserNotFound() {
        Mockito.when(userService.getEntraUserById(any(String.class))).thenReturn(Optional.empty());

        boolean result = accessControlService.canDisableUser("entraUserId");

        Assertions.assertThat(result).isFalse();
    }

    @Test
    void canDisableUser_returnsFalse_whenUserIsMultiFirm() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).email("test@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        AppRole appRole = AppRole.builder().authzRole(true)
                .permissions(Set.of(Permission.EDIT_EXTERNAL_USER, Permission.DISABLE_EXTERNAL_USER)).build();
        UserProfile userProfile = UserProfile.builder().id(UUID.randomUUID()).activeProfile(true)
                .entraUser(entraUser).appRoles(Set.of(appRole)).userType(UserType.EXTERNAL).build();
        entraUser.getUserProfiles().add(userProfile);

        String userId = UUID.randomUUID().toString();
        EntraUserDto userDto = EntraUserDto.builder()
                .id(userId)
                .email("user@example.com")
                .multiFirmUser(true)
                .build();
        Mockito.when(userService.getEntraUserById(userId)).thenReturn(Optional.of(userDto));

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean result = accessControlService.canDisableUser(userId);

        Assertions.assertThat(result).isFalse();
    }

    @Test
    void canDisableUser_returnsFalse_whenUserIsInternal() {
        String userId = UUID.randomUUID().toString();
        EntraUserDto userDto = EntraUserDto.builder()
                .id(userId)
                .email("user@example.com")
                .multiFirmUser(false)
                .build();
        Mockito.when(userService.getEntraUserById(userId)).thenReturn(Optional.of(userDto));
        Mockito.when(userService.isInternal(userId)).thenReturn(true);

        boolean result = accessControlService.canDisableUser(userId);

        Assertions.assertThat(result).isFalse();
    }

    @Test
    void canDisableUser_returnsFalse_whenSameUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(userId).email("test@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        Permission userPermission = Permission.EDIT_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .entraUser(entraUser).appRoles(Set.of(appRole)).userType(UserType.EXTERNAL).build();
        entraUser.getUserProfiles().add(userProfile);

        EntraUserDto entraUserDto = EntraUserDto.builder().id(userId.toString()).build();

        Mockito.when(userService.getEntraUserById(userId.toString())).thenReturn(Optional.of(entraUserDto));
        Mockito.when(userService.isInternal(userId.toString())).thenReturn(false);

        EntraUser authenticatedUser = EntraUser.builder().id(userId).build();

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(authenticatedUser);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean result = accessControlService.canDisableUser(entraUserDto.getId());

        Assertions.assertThat(result).isFalse();
    }

    @Test
    void canDisableUser_returnsFalse_whenUserLacksPermission() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(userId).email("test@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        Permission userPermission = Permission.EDIT_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .entraUser(entraUser).appRoles(Set.of(appRole)).userType(UserType.EXTERNAL).build();
        entraUser.getUserProfiles().add(userProfile);

        EntraUserDto entraUserDto = EntraUserDto.builder().id("accessedUser").build();

        Mockito.when(userService.getEntraUserById(userId.toString())).thenReturn(Optional.of(entraUserDto));
        Mockito.when(userService.isInternal(userId.toString())).thenReturn(false);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean result = accessControlService.canDisableUser(userId.toString());

        Assertions.assertThat(result).isFalse();
    }

    @Test
    void canDisableUser_returnsTrue_whenAllConditionsMet() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(userId).email("test@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        AppRole appRole = AppRole.builder().authzRole(true)
                .permissions(Set.of(Permission.EDIT_EXTERNAL_USER, Permission.DISABLE_EXTERNAL_USER)).build();
        UserProfile userProfile = UserProfile.builder().id(UUID.randomUUID()).activeProfile(true)
                .entraUser(entraUser).appRoles(Set.of(appRole)).userType(UserType.EXTERNAL).build();
        entraUser.getUserProfiles().add(userProfile);

        EntraUserDto entraUserDto = EntraUserDto.builder().id("accessedUser").build();

        Mockito.when(entraUserRepository.findById(userId)).thenReturn(Optional.of(entraUser));
        Mockito.when(userService.getEntraUserById(userId.toString())).thenReturn(Optional.of(entraUserDto));
        Mockito.when(userService.isInternal(userId.toString())).thenReturn(false);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean result = accessControlService.canDisableUser(userId.toString());

        Assertions.assertThat(result).isTrue();
    }

    @Test
    void canDisableUser_internalUser_disableMultiFirmUser() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(userId).email("test@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        AppRole appRole = AppRole.builder().authzRole(true).name(AuthzRole.GLOBAL_ADMIN.getRoleName())
                .permissions(Set.of(Permission.EDIT_EXTERNAL_USER, Permission.DISABLE_EXTERNAL_USER)).build();
        UserProfile userProfile = UserProfile.builder().id(UUID.randomUUID()).activeProfile(true)
                .entraUser(entraUser).appRoles(Set.of(appRole)).userType(UserType.INTERNAL).build();
        entraUser.getUserProfiles().add(userProfile);

        UUID accessedUserId = UUID.randomUUID();
        EntraUser accessedEntraUser = EntraUser.builder().id(accessedUserId).multiFirmUser(true).build();
        EntraUserDto accessedEntraUserDto = EntraUserDto.builder().id(accessedUserId.toString()).multiFirmUser(true).build();

        Mockito.when(entraUserRepository.findById(accessedUserId)).thenReturn(Optional.of(accessedEntraUser));
        Mockito.when(userService.getEntraUserById(accessedUserId.toString())).thenReturn(Optional.of(accessedEntraUserDto));
        Mockito.when(userService.isInternal(accessedUserId.toString())).thenReturn(false);
        Mockito.when(userService.isInternal(userId)).thenReturn(true);

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean result = accessControlService.canDisableUser(accessedUserId.toString());

        Assertions.assertThat(result).isTrue();
    }

    @Test
    void cannotDisableUser_firmUserManager_differentFirm() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        final Firm firm1 = Firm.builder().id(UUID.randomUUID()).build();
        final FirmDto firm1Dto = FirmDto.builder().id(firm1.getId()).build();
        EntraUser entraUser = EntraUser.builder().id(userId).email("test@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        AppRole appRole = AppRole.builder().authzRole(true).name(AuthzRole.FIRM_USER_MANAGER.getRoleName())
                .permissions(Set.of(Permission.EDIT_EXTERNAL_USER, Permission.DISABLE_EXTERNAL_USER)).build();
        UserProfile userProfile = UserProfile.builder().id(UUID.randomUUID()).activeProfile(true).firm(firm1)
                .entraUser(entraUser).appRoles(Set.of(appRole)).userType(UserType.EXTERNAL).build();
        entraUser.getUserProfiles().add(userProfile);

        UUID accessedUserId = UUID.randomUUID();
        final Firm accessedFirm = Firm.builder().id(UUID.randomUUID()).build();
        final FirmDto accessedFirmDto = FirmDto.builder().id(accessedFirm.getId()).build();
        EntraUser accessedEntraUser = EntraUser.builder().id(accessedUserId).multiFirmUser(true).build();
        UserProfile accessedUserProfile = UserProfile.builder().entraUser(accessedEntraUser)
                .activeProfile(true).firm(accessedFirm).id(UUID.randomUUID()).build();
        accessedEntraUser.setUserProfiles(Set.of(accessedUserProfile));
        EntraUserDto accessedEntraUserDto = EntraUserDto.builder().id(accessedUserId.toString()).build();

        Mockito.when(entraUserRepository.findById(accessedUserId)).thenReturn(Optional.of(accessedEntraUser));
        Mockito.when(userService.getEntraUserById(accessedUserId.toString())).thenReturn(Optional.of(accessedEntraUserDto));
        Mockito.when(userService.isInternal(accessedUserId.toString())).thenReturn(false);
        Mockito.when(userService.isInternal(userId)).thenReturn(false);
        Mockito.when(firmService.getUserActiveAllFirms(entraUser)).thenReturn(List.of(firm1Dto));
        Mockito.when(firmService.getUserFirmsByUserId(accessedUserProfile.getId().toString())).thenReturn(List.of(accessedFirmDto));

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean result = accessControlService.canDisableUser(accessedUserId.toString());

        Assertions.assertThat(result).isFalse();
    }

    @Test
    void canDisableUser_firmUserManager_sameFirm() {
        AnonymousAuthenticationToken authentication = Mockito.mock(AnonymousAuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();
        final Firm firm1 = Firm.builder().id(UUID.randomUUID()).build();
        final FirmDto firm1Dto = FirmDto.builder().id(firm1.getId()).build();
        EntraUser entraUser = EntraUser.builder().id(userId).email("test@email.com")
                .userProfiles(HashSet.newHashSet(1)).build();
        AppRole appRole = AppRole.builder().authzRole(true).name(AuthzRole.FIRM_USER_MANAGER.getRoleName())
                .permissions(Set.of(Permission.EDIT_EXTERNAL_USER, Permission.DISABLE_EXTERNAL_USER)).build();
        UserProfile userProfile = UserProfile.builder().id(UUID.randomUUID()).activeProfile(true).firm(firm1)
                .entraUser(entraUser).appRoles(Set.of(appRole)).userType(UserType.EXTERNAL).build();
        entraUser.getUserProfiles().add(userProfile);

        UUID accessedUserId = UUID.randomUUID();
        EntraUser accessedEntraUser = EntraUser.builder().id(accessedUserId).multiFirmUser(true).build();
        UserProfile accessedUserProfile = UserProfile.builder().entraUser(accessedEntraUser)
                .activeProfile(true).firm(firm1).id(UUID.randomUUID()).build();
        accessedEntraUser.setUserProfiles(Set.of(accessedUserProfile));
        EntraUserDto accessedEntraUserDto = EntraUserDto.builder().id(accessedUserId.toString()).build();

        Mockito.when(entraUserRepository.findById(accessedUserId)).thenReturn(Optional.of(accessedEntraUser));
        Mockito.when(userService.getEntraUserById(accessedUserId.toString())).thenReturn(Optional.of(accessedEntraUserDto));
        Mockito.when(userService.isInternal(accessedUserId.toString())).thenReturn(false);
        Mockito.when(userService.isInternal(userId)).thenReturn(false);
        Mockito.when(firmService.getUserActiveAllFirms(entraUser)).thenReturn(List.of(firm1Dto));
        Mockito.when(firmService.getUserFirmsByUserId(accessedUserProfile.getId().toString())).thenReturn(List.of(firm1Dto));

        Mockito.when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean result = accessControlService.canDisableUser(accessedUserId.toString());

        Assertions.assertThat(result).isTrue();
    }

}
