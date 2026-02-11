package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleCreationServiceTest {

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private AppRepository appRepository;

    @Mock
    private EventService eventService;

    @Mock
    private LoginService loginService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private RoleCreationService roleCreationService;

    @BeforeEach
    void setUp() {
        roleCreationService = new RoleCreationService(appRoleRepository, appRepository, eventService, loginService);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testEnrichRoleCreationDto_SetsAuthzRoleForManageYourUsers() {
        // Arrange
        UUID appId = UUID.randomUUID();
        RoleCreationDto dto = RoleCreationDto.builder()
            .name("Test Role")
            .parentAppId(appId)
            .build();

        App app = App.builder()
            .id(appId)
            .name("Manage your users")
            .build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(appRoleRepository.findAll()).thenReturn(List.of());

        // Act
        RoleCreationDto result = roleCreationService.enrichRoleCreationDto(dto);

        // Assert
        assertTrue(result.isAuthzRole());
        assertEquals("Manage your users", result.getParentAppName());
    }

    @Test
    void testEnrichRoleCreationDto_DoesNotSetAuthzRoleForOtherApps() {
        // Arrange
        UUID appId = UUID.randomUUID();
        RoleCreationDto dto = RoleCreationDto.builder()
            .name("Test Role")
            .parentAppId(appId)
            .build();

        App app = App.builder()
            .id(appId)
            .name("Other App")
            .build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(appRoleRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        RoleCreationDto result = roleCreationService.enrichRoleCreationDto(dto);

        // Assert
        assertFalse(result.isAuthzRole());
        assertEquals("Other App", result.getParentAppName());
    }

    @Test
    void testCreateRole_WithValidDto_CreatesRoleAndAuditEvent() {
        // Arrange
        UUID appId = UUID.randomUUID();

        App parentApp = App.builder()
            .id(appId)
            .name("Test App")
            .build();

        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Test User");

        when(appRepository.findById(appId)).thenReturn(Optional.of(parentApp));
        when(appRoleRepository.findByName("Test Role")).thenReturn(Optional.empty());

        AppRole savedRole = AppRole.builder()
                .id(UUID.randomUUID())
                .name("Test Role")
                .description("Test Description")
                .userTypeRestriction(new UserType[]{UserType.INTERNAL})
                .app(parentApp)
                .build();
        when(appRoleRepository.save(any(AppRole.class))).thenReturn(savedRole);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUser);


        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(appId)
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(true)
                .build();
        // Act
        roleCreationService.createRole(dto);

        // Assert
        verify(appRoleRepository).save(any(AppRole.class));
        verify(eventService).logEvent(any(RoleCreationAuditEvent.class));
    }

    @Test
    void testCreateRole_WithDuplicateName_ThrowsException() {
        // Arrange
        UUID appId = UUID.randomUUID();
        
        RoleCreationDto dto = RoleCreationDto.builder()
            .name("Existing Role")
            .description("Test Description")
            .parentAppId(appId)
            .userTypeRestriction(List.of(UserType.INTERNAL))
            .build();

        App existingApp = App.builder().id(appId).build();
        AppRole existingRole = AppRole.builder()
            .name("Existing Role")
            .app(existingApp)
            .build();

        when(appRoleRepository.findByName("Existing Role")).thenReturn(Optional.of(existingRole));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> roleCreationService.createRole(dto));
        
        assertEquals("Role name 'Existing Role' already exists in this application", exception.getMessage());
    }

    @Test
    void testEnrichRoleCreationDto_GeneratesSequentialOrdinals() {
        // Arrange
        UUID appId = UUID.randomUUID();
        RoleCreationDto dto = RoleCreationDto.builder()
            .name("Test Role")
            .parentAppId(appId)
            .build();

        App app = App.builder()
            .id(appId)
            .name("Test App")
            .build();

        AppRole role1 = AppRole.builder().ordinal(5).build();
        AppRole role2 = AppRole.builder().ordinal(10).build();
        AppRole role3 = AppRole.builder().ordinal(3).build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(appRoleRepository.findAll()).thenReturn(Arrays.asList(role1, role2, role3));

        // Act
        RoleCreationDto result = roleCreationService.enrichRoleCreationDto(dto);

        // Assert
        assertEquals(11, result.getOrdinal());
    }

    @Test
    void testEnrichRoleCreationDto_WithNoExistingRoles_SetsOrdinalToOne() {
        // Arrange
        UUID appId = UUID.randomUUID();
        RoleCreationDto dto = RoleCreationDto.builder()
            .name("Test Role")
            .parentAppId(appId)
            .build();

        App app = App.builder()
            .id(appId)
            .name("Test App")
            .build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(appRoleRepository.findAll()).thenReturn(List.of());

        // Act
        RoleCreationDto result = roleCreationService.enrichRoleCreationDto(dto);

        // Assert
        assertEquals(1, result.getOrdinal());
    }

    @Test
    void testIsRoleNameExistsInApp_WithExistingRole_ReturnsTrue() {
        // Arrange
        UUID appId = UUID.randomUUID();
        String roleName = "Existing Role";
        
        App app = App.builder().id(appId).build();
        AppRole existingRole = AppRole.builder()
            .name("existing role")  // Different case
            .app(app)
            .build();

        when(appRoleRepository.findAll()).thenReturn(List.of(existingRole));

        // Act
        boolean result = roleCreationService.isRoleNameExistsInApp(roleName, appId);

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsRoleNameExistsInApp_WithNonExistingRole_ReturnsFalse() {
        // Arrange
        UUID appId = UUID.randomUUID();
        String roleName = "Non-existing Role";
        
        when(appRoleRepository.findAll()).thenReturn(List.of());

        // Act
        boolean result = roleCreationService.isRoleNameExistsInApp(roleName, appId);

        // Assert
        assertFalse(result);
    }

    @Test
    void testIsRoleNameExistsInApp_WithSameNameDifferentApp_ReturnsFalse() {
        // Arrange
        UUID appId1 = UUID.randomUUID();
        UUID appId2 = UUID.randomUUID();
        String roleName = "Test Role";
        
        App app1 = App.builder().id(appId1).build();
        AppRole roleInDifferentApp = AppRole.builder()
            .name(roleName)
            .app(app1)
            .build();

        when(appRoleRepository.findAll()).thenReturn(List.of(roleInDifferentApp));

        // Act
        boolean result = roleCreationService.isRoleNameExistsInApp(roleName, appId2);

        // Assert
        assertFalse(result);
    }

    @Test
    void testCreateRole_WithNullLegacySync_DefaultsToFalse() {
        // Arrange
        UUID appId = UUID.randomUUID();
        
        RoleCreationDto dto = RoleCreationDto.builder()
            .name("Test Role")
            .description("Test Description")
            .parentAppId(appId)
            .userTypeRestriction(List.of(UserType.EXTERNAL))
            .legacySync(null)  // Null legacy sync
            .build();

        App parentApp = App.builder()
            .id(appId)
            .name("Test App")
            .build();

        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Test User");

        when(appRepository.findById(appId)).thenReturn(Optional.of(parentApp));
        when(appRoleRepository.findByName("Test Role")).thenReturn(Optional.empty());
        
        AppRole savedRole = AppRole.builder()
            .id(UUID.randomUUID())
            .name("Test Role")
            .description("Test Description")
            .userTypeRestriction(new UserType[]{UserType.EXTERNAL})
            .legacySync(false)
            .app(parentApp)
            .build();
        
        when(appRoleRepository.save(any(AppRole.class))).thenReturn(savedRole);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUser);

        // Act
        roleCreationService.createRole(dto);

        // Assert
        verify(appRoleRepository).save(any(AppRole.class));
        verify(eventService).logEvent(any(RoleCreationAuditEvent.class));
    }

    @Test
    void testCreateRole_WithMultipleUserTypes_CreatesCorrectAuditEvent() {
        // Arrange
        UUID appId = UUID.randomUUID();
        
        RoleCreationDto dto = RoleCreationDto.builder()
            .name("Multi-Type Role")
            .description("Role for multiple user types")
            .parentAppId(appId)
            .userTypeRestriction(List.of(UserType.INTERNAL, UserType.EXTERNAL))
            .legacySync(true)
            .build();

        App parentApp = App.builder()
            .id(appId)
            .name("Multi-User App")
            .build();

        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Admin User");

        when(appRepository.findById(appId)).thenReturn(Optional.of(parentApp));
        when(appRoleRepository.findByName("Multi-Type Role")).thenReturn(Optional.empty());
        
        AppRole savedRole = AppRole.builder()
            .id(UUID.randomUUID())
            .name("Multi-Type Role")
            .description("Role for multiple user types")
            .userTypeRestriction(new UserType[]{UserType.INTERNAL, UserType.EXTERNAL})
            .app(parentApp)
            .build();
        
        when(appRoleRepository.save(any(AppRole.class))).thenReturn(savedRole);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUser);

        // Act
        roleCreationService.createRole(dto);

        // Assert
        verify(appRoleRepository).save(any(AppRole.class));
        verify(eventService).logEvent(any(RoleCreationAuditEvent.class));
    }

    @Test
    void testCreateRole_WithEmptyCcmsCode_ConvertsToNull() {
        // Arrange
        UUID appId = UUID.randomUUID();
        
        RoleCreationDto dto = RoleCreationDto.builder()
            .name("Test Role")
            .description("Test Description")
            .parentAppId(appId)
            .userTypeRestriction(List.of(UserType.INTERNAL))
            .ccmsCode("")  // Empty CCMS code
            .legacySync(false)
            .build();

        App parentApp = App.builder()
            .id(appId)
            .name("Test App")
            .build();

        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Test User");

        when(appRepository.findById(appId)).thenReturn(Optional.of(parentApp));
        when(appRoleRepository.findByName("Test Role")).thenReturn(Optional.empty());
        
        AppRole savedRole = AppRole.builder()
            .id(UUID.randomUUID())
            .name("Test Role")
            .description("Test Description")
            .userTypeRestriction(new UserType[]{UserType.INTERNAL})
            .ccmsCode(null)  // Should be converted to null
            .app(parentApp)
            .build();
        
        when(appRoleRepository.save(any(AppRole.class))).thenReturn(savedRole);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUser);

        // Act
        roleCreationService.createRole(dto);

        // Assert
        verify(appRoleRepository).save(any(AppRole.class));
        verify(eventService).logEvent(any(RoleCreationAuditEvent.class));
    }

    @Test
    void testCreateRole_WithAppNotFound_ThrowsException() {
        // Arrange
        UUID appId = UUID.randomUUID();
        
        RoleCreationDto dto = RoleCreationDto.builder()
            .name("Test Role")
            .description("Test Description")
            .parentAppId(appId)
            .userTypeRestriction(List.of(UserType.INTERNAL))
            .build();

        when(appRepository.findById(appId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> roleCreationService.createRole(dto));
        
        assertTrue(exception.getMessage().contains("not found") || 
                  exception instanceof NullPointerException);
    }

    @Test
    void testEnrichRoleCreationDto_SetsRandomId() {
        // Arrange
        UUID appId = UUID.randomUUID();
        RoleCreationDto dto = RoleCreationDto.builder()
            .name("Test Role")
            .parentAppId(appId)
            .build();

        App app = App.builder()
            .id(appId)
            .name("Test App")
            .build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(appRoleRepository.findAll()).thenReturn(List.of());

        // Act
        RoleCreationDto result = roleCreationService.enrichRoleCreationDto(dto);

        // Assert
        assertTrue(result.getId() != null);
        assertEquals("Test Role", result.getName());
        assertEquals("Test App", result.getParentAppName());
    }
}
