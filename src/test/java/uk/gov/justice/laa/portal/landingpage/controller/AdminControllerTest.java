package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.service.AppService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private AdminController adminController;
    @Mock
    private AppService appService;
    private Model model;

    private List<AppDto> adminApps;
    private List<AppDto> laaApps;
    private List<AppDto> allApps;

    private AppRoleDto ccmsAppRole;
    private List<AppRoleDto> allAppRoles;

    @BeforeEach
    void setUp() {
        adminController = new AdminController(appService);
        model = new ExtendedModelMap();
        loadAppsAndRoles();
    }

    @Test
    void testShowAdministration_WithDefaultTab_LoadsAllData() {
        // Arrange
        when(appService.getAllAdminApps()).thenReturn(adminApps);
        when(appService.getAllLaaApps()).thenReturn(laaApps);
        when(appService.getAllAppRolesForAdmin()).thenReturn(allAppRoles);
        when(appService.getAllAppsForAdmin()).thenReturn(allApps);

        // Act
        String viewName = adminController.showAdministration("admin-apps", null, model);

        // Assert
        assertEquals("silas-administration/administration", viewName);
        assertEquals("SiLAS Administration", model.getAttribute(ModelAttributes.PAGE_TITLE));
        assertEquals("admin-apps", model.getAttribute("activeTab"));

        assertThat(model.getAttribute("adminApps")).isEqualTo(adminApps);
        assertThat(model.getAttribute("apps")).isEqualTo(laaApps);
        assertThat(model.getAttribute("roles")).isEqualTo(allAppRoles);

        verify(appService, times(1)).getAllAdminApps();
        verify(appService, times(1)).getAllLaaApps();
        verify(appService, times(1)).getAllAppRolesForAdmin();
        verify(appService, times(1)).getAllAppsForAdmin();
    }

    @Test
    void testShowAdministration_WithRolesTab_LoadsAllData() {
        // Arrange
        when(appService.getAllAdminApps()).thenReturn(adminApps);
        when(appService.getAllLaaApps()).thenReturn(laaApps);
        when(appService.getAllAppRolesForAdmin()).thenReturn(allAppRoles);
        when(appService.getAllAppsForAdmin()).thenReturn(allApps);

        // Act
        String viewName = adminController.showAdministration("roles", null, model);

        // Assert
        assertEquals("silas-administration/administration", viewName);
        assertEquals("roles", model.getAttribute("activeTab"));

        assertThat(model.getAttribute("roles")).isEqualTo(allAppRoles);

        verify(appService).getAllAppRolesForAdmin();
    }

    @Test
    void testShowAdministration_WithAppFilter_FiltersRolesByApp() {
        // Arrange
        String appFilter = "CCMS case transfer requests";
        when(appService.getAllAdminApps()).thenReturn(adminApps);
        when(appService.getAllLaaApps()).thenReturn(laaApps);
        when(appService.getAllAppsForAdmin()).thenReturn(allApps);

        List<AppRoleDto> filteredRoles = Collections.singletonList(ccmsAppRole);

        when(appService.getAppRolesByApp(appFilter)).thenReturn(filteredRoles);

        // Act
        String viewName = adminController.showAdministration("roles", appFilter, model);

        // Assert
        assertEquals("silas-administration/administration", viewName);
        assertThat(model.getAttribute("roles")).isEqualTo(filteredRoles);
        assertThat(model.getAttribute("appFilter")).isEqualTo(appFilter);

        verify(appService, times(1)).getAppRolesByApp(appFilter);
    }

    @Test
    void testShowAdministration_LoadsAppNamesForFilter() {
        // Arrange
        when(appService.getAllAdminApps()).thenReturn(adminApps);
        when(appService.getAllLaaApps()).thenReturn(laaApps);
        when(appService.getAllAppsForAdmin()).thenReturn(allApps);
        when(appService.getAllAppRolesForAdmin()).thenReturn(allAppRoles);

        // Act
        adminController.showAdministration("roles", null, model);

        // Assert
        @SuppressWarnings("unchecked")
        List<String> appNames = (List<String>) model.getAttribute("appNames");
        assertThat(appNames).containsExactly("LAA App 3", "LAA App 2", "LAA App 1", "CCMS case transfer requests",
                "Admin App 3", "Admin App 2", "Admin App 1");
    }

    @Test
    void testShowAdministration_WithAppsTab_LoadsAllData() {
        // Arrange
        when(appService.getAllAdminApps()).thenReturn(adminApps);
        when(appService.getAllLaaApps()).thenReturn(laaApps);
        when(appService.getAllAppRolesForAdmin()).thenReturn(allAppRoles);

        // Act
        String viewName = adminController.showAdministration("apps", null, model);

        // Assert
        assertEquals("silas-administration/administration", viewName);
        assertEquals("apps", model.getAttribute("activeTab"));
        assertThat(model.getAttribute("apps")).isEqualTo(laaApps);
    }

    @Test
    void testShowAdministration_WithEmptyAppFilter_LoadsAllRoles() {
        // Arrange
        when(appService.getAllAdminApps()).thenReturn(adminApps);
        when(appService.getAllLaaApps()).thenReturn(laaApps);
        when(appService.getAllAppRolesForAdmin()).thenReturn(allAppRoles);

        // Act
        adminController.showAdministration("roles", "", model);

        // Assert
        assertThat(model.getAttribute("roles")).isEqualTo(allAppRoles);
        verify(appService).getAllAppRolesForAdmin();
    }

    private void loadAppsAndRoles() {
        adminApps = new ArrayList<>();
        laaApps = new ArrayList<>();
        allApps = new ArrayList<>();

        List<AppRoleDto> adminAppRoles = new ArrayList<>();
        List<AppRoleDto> laaAppRoles = new ArrayList<>();
        allAppRoles = new ArrayList<>();

        AppDto adminApp1 = AppDto.builder().name("Admin App 1").appType(AppType.AUTHZ).build();
        AppDto adminApp2 = AppDto.builder().name("Admin App 2").appType(AppType.AUTHZ).build();
        AppDto adminApp3 = AppDto.builder().name("Admin App 3").appType(AppType.AUTHZ).build();
        adminApps.addAll(Arrays.asList(adminApp1, adminApp2, adminApp3));
        allApps.addAll(adminApps);

        AppRoleDto adminAppRole1 = AppRoleDto.builder().name("Admin Role 1").app(adminApp1).authzRole(true).build();
        AppRoleDto adminAppRole2 = AppRoleDto.builder().name("Admin Role 2").app(adminApp1).authzRole(true).build();
        AppRoleDto adminAppRole3 = AppRoleDto.builder().name("Admin Role 3").app(adminApp2).authzRole(true).build();
        AppRoleDto adminAppRole4 = AppRoleDto.builder().name("Admin Role 4").app(adminApp3).authzRole(true).build();
        adminAppRoles.addAll(Arrays.asList(adminAppRole1, adminAppRole2, adminAppRole3, adminAppRole4));
        allAppRoles.addAll(adminAppRoles);

        AppDto laaApp1 = AppDto.builder().name("LAA App 1").appType(AppType.LAA).build();
        AppDto laaApp2 = AppDto.builder().name("LAA App 2").appType(AppType.LAA).build();
        AppDto laaApp3 = AppDto.builder().name("LAA App 3").appType(AppType.LAA).build();
        AppDto ccmsApp = AppDto.builder().name("CCMS case transfer requests").appType(AppType.LAA).build();
        laaApps.addAll(Arrays.asList(laaApp1, laaApp2, laaApp3, ccmsApp));
        allApps.addAll(laaApps);

        AppRoleDto laaAppRole1 = AppRoleDto.builder().name("LAA Role 1").app(laaApp1).build();
        AppRoleDto laaAppRole2 = AppRoleDto.builder().name("LAA Role 2").app(laaApp2).build();
        AppRoleDto laaAppRole3 = AppRoleDto.builder().name("LAA Role 3").app(laaApp2).build();
        AppRoleDto laaAppRole4 = AppRoleDto.builder().name("LAA Role 4").app(laaApp3).build();
        ccmsAppRole = AppRoleDto.builder().name("CCMS Role 1").app(ccmsApp).ccmsCode("XXCCMS_TEST_ROLE").build();
        laaAppRoles.addAll(Arrays.asList(laaAppRole1, laaAppRole2, laaAppRole3, laaAppRole4, ccmsAppRole));
        allAppRoles.addAll(laaAppRoles);
    }
}
