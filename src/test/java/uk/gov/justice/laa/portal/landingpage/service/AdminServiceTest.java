package uk.gov.justice.laa.portal.landingpage.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.laa.portal.landingpage.dto.AdminAppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.RoleAssignmentAdminDto;
import uk.gov.justice.laa.portal.landingpage.entity.AdminApp;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AdminAppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AppRepository appRepository;

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;

    @Mock
    private AdminAppRepository adminAppRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
            appRepository,
            appRoleRepository,
            roleAssignmentRepository,
            adminAppRepository
        );
    }

    @Test
    void testGetAllAdminApps_ReturnsEnabledAppsSortedByOrdinal() {
        // Arrange
        List<AdminApp> adminApps = Arrays.asList(
            createAdminApp("App 2", 2, true),
            createAdminApp("App 1", 1, true),
            createAdminApp("Disabled App", 0, false)
        );

        when(adminAppRepository.findAll()).thenReturn(adminApps);

        // Act
        List<AdminAppDto> result = adminService.getAllAdminApps();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("App 1");
        assertThat(result.get(0).getOrdinal()).isEqualTo(1);
        assertThat(result.get(1).getName()).isEqualTo("App 2");
        assertThat(result.get(1).getOrdinal()).isEqualTo(2);

        verify(adminAppRepository).findAll();
    }

    @Test
    void testGetAllAdminApps_FiltersDisabledApps() {
        // Arrange
        List<AdminApp> adminApps = Arrays.asList(
            createAdminApp("Enabled App", 1, true),
            createAdminApp("Disabled App", 2, false)
        );

        when(adminAppRepository.findAll()).thenReturn(adminApps);

        // Act
        List<AdminAppDto> result = adminService.getAllAdminApps();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Enabled App");
    }

    @Test
    void testGetAllApps_ReturnsSortedByOrdinal() {
        // Arrange
        List<App> apps = Arrays.asList(
            createApp("App C", 3),
            createApp("App A", 1),
            createApp("App B", 2)
        );

        when(appRepository.findAll()).thenReturn(apps);

        // Act
        List<AppAdminDto> result = adminService.getAllApps();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("App A");
        assertThat(result.get(1).getName()).isEqualTo("App B");
        assertThat(result.get(2).getName()).isEqualTo("App C");

        verify(appRepository).findAll();
    }

    @Test
    void testGetAdminApps_FiltersAuthzAppsOnly() {
        // Arrange
        List<App> apps = Arrays.asList(
            createAppWithType("Admin App", 1, AppType.AUTHZ),
            createAppWithType("Regular App", 2, AppType.LAA),
            createAppWithType("Another Admin", 3, AppType.AUTHZ)
        );

        when(appRepository.findAll()).thenReturn(apps);

        // Act
        List<AppAdminDto> result = adminService.getAdminApps();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Admin App");
        assertThat(result.get(1).getName()).isEqualTo("Another Admin");
    }

    @Test
    void testGetAllAppRoles_ReturnsSortedByAppAndOrdinal() {
        // Arrange
        App appA = createApp("App A", 1);
        App appB = createApp("App B", 2);

        List<AppRole> roles = Arrays.asList(
            createAppRole("Role B1", appB, 1),
            createAppRole("Role A2", appA, 2),
            createAppRole("Role A1", appA, 1),
            createAppRole("Role B2", appB, 2)
        );

        when(appRoleRepository.findAll()).thenReturn(roles);

        // Act
        List<AppRoleAdminDto> result = adminService.getAllAppRoles();

        // Assert
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getName()).isEqualTo("Role A1");
        assertThat(result.get(1).getName()).isEqualTo("Role A2");
        assertThat(result.get(2).getName()).isEqualTo("Role B1");
        assertThat(result.get(3).getName()).isEqualTo("Role B2");
    }

    @Test
    void testGetAppRolesByApp_FiltersAndSortsByOrdinal() {
        // Arrange
        App targetApp = createApp("Target App", 1);
        App otherApp = createApp("Other App", 2);

        List<AppRole> roles = Arrays.asList(
            createAppRole("Role 3", targetApp, 3),
            createAppRole("Role 1", targetApp, 1),
            createAppRole("Other Role", otherApp, 1),
            createAppRole("Role 2", targetApp, 2)
        );

        when(appRoleRepository.findAll()).thenReturn(roles);

        // Act
        List<AppRoleAdminDto> result = adminService.getAppRolesByApp("Target App");

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("Role 1");
        assertThat(result.get(1).getName()).isEqualTo("Role 2");
        assertThat(result.get(2).getName()).isEqualTo("Role 3");
    }

    @Test
    void testGetAllRoleAssignments_ReturnsSortedByAssigningRole() {
        // Arrange
        AppRole roleA = createSimpleAppRole("Role A");
        AppRole roleB = createSimpleAppRole("Role B");
        AppRole roleC = createSimpleAppRole("Role C");

        List<RoleAssignment> assignments = Arrays.asList(
            createRoleAssignment(roleC, roleA),
            createRoleAssignment(roleA, roleB),
            createRoleAssignment(roleB, roleC)
        );

        when(roleAssignmentRepository.findAll()).thenReturn(assignments);

        // Act
        List<RoleAssignmentAdminDto> result = adminService.getAllRoleAssignments();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAssigningRoleName()).isEqualTo("Role A");
        assertThat(result.get(1).getAssigningRoleName()).isEqualTo("Role B");
        assertThat(result.get(2).getAssigningRoleName()).isEqualTo("Role C");
    }

    @Test
    void testGetAllAppRoles_HandlesUserTypeRestrictions() {
        // Arrange
        App app = createApp("Test App", 1);
        AppRole role = AppRole.builder()
            .id(UUID.randomUUID())
            .name("Restricted Role")
            .description("Role with restrictions")
            .app(app)
            .ordinal(1)
            .authzRole(false)
            .userTypeRestriction(new UserType[]{UserType.INTERNAL, UserType.EXTERNAL})
            .build();

        when(appRoleRepository.findAll()).thenReturn(Arrays.asList(role));

        // Act
        List<AppRoleAdminDto> result = adminService.getAllAppRoles();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserTypeRestriction()).contains("INTERNAL", "EXTERNAL");
    }

    @Test
    void testGetAllAppRoles_HandlesNullApp() {
        // Arrange
        AppRole role = AppRole.builder()
            .id(UUID.randomUUID())
            .name("Orphan Role")
            .description("Role without app")
            .app(null)
            .ordinal(1)
            .authzRole(false)
            .build();

        when(appRoleRepository.findAll()).thenReturn(Arrays.asList(role));

        // Act
        List<AppRoleAdminDto> result = adminService.getAllAppRoles();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParentApp()).isEmpty();
        assertThat(result.get(0).getParentAppId()).isEmpty();
    }

    @Test
    void testGetAppRolesByApp_HandlesEmptyResults() {
        // Arrange
        when(appRoleRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<AppRoleAdminDto> result = adminService.getAppRolesByApp("Non-existent App");

        // Assert
        assertThat(result).isEmpty();
    }

    // Helper methods to create test entities
    private AdminApp createAdminApp(String name, int ordinal, boolean enabled) {
        return AdminApp.builder()
            .id(UUID.randomUUID())
            .name(name)
            .description("Description for " + name)
            .ordinal(ordinal)
            .enabled(enabled)
            .build();
    }

    private App createApp(String name, int ordinal) {
        return App.builder()
            .id(UUID.randomUUID())
            .name(name)
            .description("Description for " + name)
            .ordinal(ordinal)
            .url("https://example.com/" + name.toLowerCase())
            .enabled(true)
            .build();
    }

    private App createAppWithType(String name, int ordinal, AppType appType) {
        return App.builder()
            .id(UUID.randomUUID())
            .name(name)
            .description("Description for " + name)
            .ordinal(ordinal)
            .url("https://example.com/" + name.toLowerCase())
            .enabled(true)
            .appType(appType)
            .build();
    }

    private AppRole createAppRole(String name, App app, int ordinal) {
        return AppRole.builder()
            .id(UUID.randomUUID())
            .name(name)
            .description("Description for " + name)
            .app(app)
            .ordinal(ordinal)
            .authzRole(false)
            .build();
    }

    private AppRole createSimpleAppRole(String name) {
        return AppRole.builder()
            .id(UUID.randomUUID())
            .name(name)
            .description("Description for " + name)
            .ordinal(1)
            .authzRole(false)
            .build();
    }

    private RoleAssignment createRoleAssignment(AppRole assigningRole, AppRole assignableRole) {
        return RoleAssignment.builder()
            .assigningRole(assigningRole)
            .assignableRole(assignableRole)
            .build();
    }
}
