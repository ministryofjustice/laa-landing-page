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
}
