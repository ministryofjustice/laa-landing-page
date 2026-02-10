package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AdminAppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.service.AdminService;
import uk.gov.justice.laa.portal.landingpage.service.RoleCreationService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private RoleCreationService roleCreationService;

    private AdminController adminController;
    private Model model;

    @BeforeEach
    void setUp() {
        adminController = new AdminController(adminService, roleCreationService);
        model = new ExtendedModelMap();
    }

    @Test
    void testShowAdministration_WithDefaultTab_LoadsAllData() {
        // Arrange
        List<AdminAppDto> adminApps = createMockAdminApps();
        List<AppAdminDto> apps = createMockApps();
        List<AppRoleAdminDto> roles = createMockRoles();

        when(adminService.getAllAdminApps()).thenReturn(adminApps);
        when(adminService.getAllApps()).thenReturn(apps);
        when(adminService.getAllAppRoles()).thenReturn(roles);

        // Act
        String viewName = adminController.showAdministration("admin-apps", null, model);

        // Assert
        assertEquals("silas-administration/administration", viewName);
        assertEquals("SiLAS Administration", model.getAttribute(ModelAttributes.PAGE_TITLE));
        assertEquals("admin-apps", model.getAttribute("activeTab"));

        assertThat(model.getAttribute("adminApps")).isEqualTo(adminApps);
        assertThat(model.getAttribute("apps")).isEqualTo(apps);
        assertThat(model.getAttribute("roles")).isEqualTo(roles);

        verify(adminService).getAllAdminApps();
        // getAllApps is called twice - once for apps data and once for app names filter
        verify(adminService, org.mockito.Mockito.times(2)).getAllApps();
        verify(adminService).getAllAppRoles();
    }

    @Test
    void testShowAdministration_WithRolesTab_LoadsAllData() {
        // Arrange
        List<AdminAppDto> adminApps = createMockAdminApps();
        List<AppAdminDto> apps = createMockApps();
        List<AppRoleAdminDto> roles = createMockRoles();

        when(adminService.getAllAdminApps()).thenReturn(adminApps);
        when(adminService.getAllApps()).thenReturn(apps);
        when(adminService.getAllAppRoles()).thenReturn(roles);

        // Act
        String viewName = adminController.showAdministration("roles", null, model);

        // Assert
        assertEquals("silas-administration/administration", viewName);
        assertEquals("roles", model.getAttribute("activeTab"));

        assertThat(model.getAttribute("roles")).isEqualTo(roles);

        verify(adminService).getAllAppRoles();
    }

    @Test
    void testShowAdministration_WithAppFilter_FiltersRolesByApp() {
        // Arrange
        String appFilter = "CCMS case transfer requests";
        List<AdminAppDto> adminApps = createMockAdminApps();
        List<AppAdminDto> apps = createMockApps();
        List<AppRoleAdminDto> filteredRoles = Arrays.asList(
            AppRoleAdminDto.builder()
                .name("CCMS Viewer")
                .description("View only role")
                .parentApp("CCMS case transfer requests")
                .ordinal(0)
                .build()
        );

        when(adminService.getAllAdminApps()).thenReturn(adminApps);
        when(adminService.getAllApps()).thenReturn(apps);
        when(adminService.getAppRolesByApp(appFilter)).thenReturn(filteredRoles);

        // Act
        String viewName = adminController.showAdministration("roles", appFilter, model);

        // Assert
        assertEquals("silas-administration/administration", viewName);
        assertThat(model.getAttribute("roles")).isEqualTo(filteredRoles);
        assertThat(model.getAttribute("appFilter")).isEqualTo(appFilter);

        verify(adminService).getAppRolesByApp(appFilter);
    }

    @Test
    void testShowAdministration_LoadsAppNamesForFilter() {
        // Arrange
        List<AppAdminDto> apps = Arrays.asList(
            AppAdminDto.builder().name("App C").build(),
            AppAdminDto.builder().name("App A").build(),
            AppAdminDto.builder().name("App B").build()
        );

        when(adminService.getAllAdminApps()).thenReturn(createMockAdminApps());
        when(adminService.getAllApps()).thenReturn(apps);
        when(adminService.getAllAppRoles()).thenReturn(createMockRoles());

        // Act
        adminController.showAdministration("roles", null, model);

        // Assert
        @SuppressWarnings("unchecked")
        List<String> appNames = (List<String>) model.getAttribute("appNames");
        assertThat(appNames).containsExactly("App A", "App B", "App C");
    }

    @Test
    void testShowAdministration_WithAppsTab_LoadsAllData() {
        // Arrange
        List<AdminAppDto> adminApps = createMockAdminApps();
        List<AppAdminDto> apps = createMockApps();
        List<AppRoleAdminDto> roles = createMockRoles();

        when(adminService.getAllAdminApps()).thenReturn(adminApps);
        when(adminService.getAllApps()).thenReturn(apps);
        when(adminService.getAllAppRoles()).thenReturn(roles);

        // Act
        String viewName = adminController.showAdministration("apps", null, model);

        // Assert
        assertEquals("silas-administration/administration", viewName);
        assertEquals("apps", model.getAttribute("activeTab"));
        assertThat(model.getAttribute("apps")).isEqualTo(apps);
    }

    @Test
    void testShowAdministration_WithEmptyAppFilter_LoadsAllRoles() {
        // Arrange
        List<AppRoleAdminDto> allRoles = createMockRoles();

        when(adminService.getAllAdminApps()).thenReturn(createMockAdminApps());
        when(adminService.getAllApps()).thenReturn(createMockApps());
        when(adminService.getAllAppRoles()).thenReturn(allRoles);

        // Act
        adminController.showAdministration("roles", "", model);

        // Assert
        assertThat(model.getAttribute("roles")).isEqualTo(allRoles);
        verify(adminService).getAllAppRoles();
    }


    @Test
    void testShowCheckYourAnswers_ReturnsCheckAnswersView() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .build();
        session.setAttribute("roleCreationDto", roleCreationDto);

        // Act
        String result = adminController.showCheckYourAnswers(model, session);

        // Assert
        assertEquals("silas-administration/create-role-check-answers", result);
        assertThat(model.getAttribute("roleCreationDto")).isEqualTo(roleCreationDto);
    }

    @Test
    void testConfirmRoleCreation_CreatesRoleAndRedirects() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .build();
        session.setAttribute("roleCreationDto", roleCreationDto);

        doNothing().when(roleCreationService).createRole(roleCreationDto);

        // Act
        String result = adminController.confirmRoleCreation(session, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/silas-administration?tab=roles", result);
        verify(roleCreationService).createRole(roleCreationDto);
        assertThat(session.getAttribute("roleCreationDto")).isNull();
    }

    @Test
    void testConfirmRoleCreation_WithServiceException_HandlesError() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .build();
        session.setAttribute("roleCreationDto", roleCreationDto);

        RuntimeException exception = new RuntimeException("Test error");
        doThrow(exception).when(roleCreationService).createRole(roleCreationDto);

        // Act
        String result = adminController.confirmRoleCreation(session, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/silas-administration?tab=roles", result);
        verify(roleCreationService).createRole(roleCreationDto);
    }

    // Helper methods to create mock data
    private List<AdminAppDto> createMockAdminApps() {
        return Arrays.asList(
            AdminAppDto.builder()
                .name("Manage your users")
                .description("Manage user access and permissions")
                .ordinal(0)
                .build(),
            AdminAppDto.builder()
                .name("User access audit table")
                .description("View all registered users")
                .ordinal(1)
                .build()
        );
    }

    private List<AppAdminDto> createMockApps() {
        return Arrays.asList(
            AppAdminDto.builder()
                .name("Apply for criminal legal aid")
                .description("Make an application for criminal legal aid")
                .ordinal(0)
                .build(),
            AppAdminDto.builder()
                .name("Submit a crime form")
                .description("Submit crime forms")
                .ordinal(1)
                .build()
        );
    }

    private List<AppRoleAdminDto> createMockRoles() {
        return Arrays.asList(
            AppRoleAdminDto.builder()
                .name("CCMS case transfer requests - Viewer")
                .description("CCMS case transfer requests - Internal User Viewer Role")
                .parentApp("CCMS case transfer requests")
                .ccmsCode("ccms.transfer.viewer")
                .ordinal(0)
                .roleGroup("Default")
                .build(),
            AppRoleAdminDto.builder()
                .name("CCMS case transfer requests - Internal")
                .description("CCMS case transfer requests - Internal User Role")
                .parentApp("CCMS case transfer requests")
                .ccmsCode("ccms.transfer.internal")
                .ordinal(1)
                .roleGroup("Default")
                .build()
        );
    }
}
