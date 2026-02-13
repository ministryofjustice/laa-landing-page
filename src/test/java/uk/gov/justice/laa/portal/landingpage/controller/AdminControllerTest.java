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
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.AdminService;
import uk.gov.justice.laa.portal.landingpage.service.RoleCreationService;
import org.springframework.validation.BindingResult;
import org.springframework.validation.BeanPropertyBindingResult;

import java.util.UUID;

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

    @Test
    void testShowCheckYourAnswers_WithCompleteRoleData_DisplaysAllFields() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Complete Test Role")
                .description("Complete test description")
                .ccmsCode("TEST123")
                .legacySync(true)
                .authzRole(true)
                .build();
        session.setAttribute("roleCreationDto", roleCreationDto);

        // Act
        String result = adminController.showCheckYourAnswers(model, session);

        // Assert
        assertEquals("silas-administration/create-role-check-answers", result);
        assertThat(model.getAttribute("roleCreationDto")).isEqualTo(roleCreationDto);
    }

    @Test
    void testConfirmRoleCreation_WithValidationException_HandlesError() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Invalid Role")
                .description("Test Description")
                .build();
        session.setAttribute("roleCreationDto", roleCreationDto);

        IllegalArgumentException exception = new IllegalArgumentException("Role name already exists");
        doThrow(exception).when(roleCreationService).createRole(roleCreationDto);

        // Act
        String result = adminController.confirmRoleCreation(session, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/silas-administration?tab=roles", result);
        verify(roleCreationService).createRole(roleCreationDto);
        // Session should not be cleared on error to preserve user input
        assertThat(session.getAttribute("roleCreationDto")).isNotNull();
    }

    @Test
    void testConfirmRoleCreation_WithCompleteRoleData_ProcessesSuccessfully() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Complete Role")
                .description("Complete description")
                .ccmsCode("COMPLETE123")
                .legacySync(false)
                .authzRole(true)
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
    void testConfirmRoleCreation_WithDatabaseException_HandlesError() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("DB Error Role")
                .description("Test Description")
                .build();
        session.setAttribute("roleCreationDto", roleCreationDto);

        RuntimeException dbException = new RuntimeException("Database connection failed");
        doThrow(dbException).when(roleCreationService).createRole(roleCreationDto);

        // Act
        String result = adminController.confirmRoleCreation(session, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/silas-administration?tab=roles", result);
        verify(roleCreationService).createRole(roleCreationDto);
    }

    @Test
    void testConfirmRoleCreation_WithEmptyRoleData_HandlesGracefully() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("")
                .description("")
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
    void testConfirmRoleCreation_WithConcurrentModification_HandlesError() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Concurrent Role")
                .description("Test Description")
                .build();
        session.setAttribute("roleCreationDto", roleCreationDto);

        RuntimeException concurrencyException = new RuntimeException("Optimistic locking failure");
        doThrow(concurrencyException).when(roleCreationService).createRole(roleCreationDto);

        // Act
        String result = adminController.confirmRoleCreation(session, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/silas-administration?tab=roles", result);
        verify(roleCreationService).createRole(roleCreationDto);
    }

    @Test
    void testShowRoleCreationForm_WithNoSessionData_CreatesNewDto() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        List<AppAdminDto> apps = createMockApps();
        when(adminService.getAllApps()).thenReturn(apps);

        // Act
        String result = adminController.showRoleCreationForm(model, session);

        // Assert
        assertEquals("silas-administration/create-role", result);
        assertThat(model.getAttribute("roleCreationDto")).isInstanceOf(RoleCreationDto.class);
        assertThat(model.getAttribute("apps")).isEqualTo(apps);
        assertThat(model.getAttribute("userTypes")).isEqualTo(UserType.values());
        assertThat(model.getAttribute("firmTypes")).isEqualTo(FirmType.values());
    }

    @Test
    void testShowRoleCreationForm_WithExistingSessionData_UsesExistingDto() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RoleCreationDto existingDto = RoleCreationDto.builder()
                .name("Existing Role")
                .description("Existing Description")
                .build();
        session.setAttribute("roleCreationDto", existingDto);
        
        List<AppAdminDto> apps = createMockApps();
        when(adminService.getAllApps()).thenReturn(apps);

        // Act
        String result = adminController.showRoleCreationForm(model, session);

        // Assert
        assertEquals("silas-administration/create-role", result);
        assertThat(model.getAttribute("roleCreationDto")).isEqualTo(existingDto);
        assertThat(model.getAttribute("apps")).isEqualTo(apps);
        assertThat(model.getAttribute("userTypes")).isEqualTo(UserType.values());
        assertThat(model.getAttribute("firmTypes")).isEqualTo(FirmType.values());
    }

    @Test
    void testProcessRoleCreation_WithValidData_RedirectsToCheckAnswers() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        UUID appId = UUID.randomUUID();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Valid Role")
                .description("Valid Description")
                .parentAppId(appId)
                .build();
        
        BindingResult bindingResult = new BeanPropertyBindingResult(roleCreationDto, "roleCreationDto");
        
        RoleCreationDto enrichedDto = RoleCreationDto.builder()
                .name("Valid Role")
                .description("Valid Description")
                .parentAppId(appId)
                .ordinal(1)
                .authzRole(false)
                .build();

        when(roleCreationService.isRoleNameExistsInApp("Valid Role", appId)).thenReturn(false);
        when(roleCreationService.enrichRoleCreationDto(roleCreationDto)).thenReturn(enrichedDto);

        // Act
        String result = adminController.processRoleCreation(roleCreationDto, bindingResult, model, session);

        // Assert
        assertEquals("redirect:/admin/roles/create/check-your-answers", result);
        assertThat(session.getAttribute("roleCreationDto")).isEqualTo(enrichedDto);
        verify(roleCreationService).enrichRoleCreationDto(roleCreationDto);
    }

    @Test
    void testProcessRoleCreation_WithDuplicateRoleName_ShowsValidationError() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        UUID appId = UUID.randomUUID();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Duplicate Role")
                .description("Valid Description")
                .parentAppId(appId)
                .build();
        
        BindingResult bindingResult = new BeanPropertyBindingResult(roleCreationDto, "roleCreationDto");
        List<AppAdminDto> apps = createMockApps();

        when(roleCreationService.isRoleNameExistsInApp("Duplicate Role", appId)).thenReturn(true);
        when(adminService.getAllApps()).thenReturn(apps);

        // Act
        String result = adminController.processRoleCreation(roleCreationDto, bindingResult, model, session);

        // Assert
        assertEquals("silas-administration/create-role", result);
        assertThat(bindingResult.hasErrors()).isTrue();
        assertThat(bindingResult.getFieldError("name")).isNotNull();
        assertThat(model.getAttribute("apps")).isEqualTo(apps);
        assertThat(model.getAttribute("userTypes")).isEqualTo(UserType.values());
        assertThat(model.getAttribute("firmTypes")).isEqualTo(FirmType.values());
    }

    @Test
    void testProcessRoleCreation_WithBindingErrors_ReturnsToForm() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("")  // Invalid empty name
                .description("Valid Description")
                .build();
        
        BindingResult bindingResult = new BeanPropertyBindingResult(roleCreationDto, "roleCreationDto");
        bindingResult.rejectValue("name", "required", "Name is required");
        List<AppAdminDto> apps = createMockApps();

        when(adminService.getAllApps()).thenReturn(apps);

        // Act
        String result = adminController.processRoleCreation(roleCreationDto, bindingResult, model, session);

        // Assert
        assertEquals("silas-administration/create-role", result);
        assertThat(bindingResult.hasErrors()).isTrue();
        assertThat(model.getAttribute("apps")).isEqualTo(apps);
        assertThat(model.getAttribute("userTypes")).isEqualTo(UserType.values());
        assertThat(model.getAttribute("firmTypes")).isEqualTo(FirmType.values());
    }

    @Test
    void testProcessRoleCreation_WithNullAppId_SkipsValidation() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Valid Role")
                .description("Valid Description")
                .parentAppId(null)  // Null app ID
                .build();
        
        BindingResult bindingResult = new BeanPropertyBindingResult(roleCreationDto, "roleCreationDto");
        
        RoleCreationDto enrichedDto = RoleCreationDto.builder()
                .name("Valid Role")
                .description("Valid Description")
                .parentAppId(null)
                .ordinal(1)
                .build();

        when(roleCreationService.enrichRoleCreationDto(roleCreationDto)).thenReturn(enrichedDto);

        // Act
        String result = adminController.processRoleCreation(roleCreationDto, bindingResult, model, session);

        // Assert
        assertEquals("redirect:/admin/roles/create/check-your-answers", result);
        assertThat(session.getAttribute("roleCreationDto")).isEqualTo(enrichedDto);
    }

    @Test
    void testProcessRoleCreation_WithNullRoleName_SkipsValidation() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        UUID appId = UUID.randomUUID();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name(null)  // Null role name
                .description("Valid Description")
                .parentAppId(appId)
                .build();
        
        BindingResult bindingResult = new BeanPropertyBindingResult(roleCreationDto, "roleCreationDto");
        
        RoleCreationDto enrichedDto = RoleCreationDto.builder()
                .name(null)
                .description("Valid Description")
                .parentAppId(appId)
                .ordinal(1)
                .build();

        when(roleCreationService.enrichRoleCreationDto(roleCreationDto)).thenReturn(enrichedDto);

        // Act
        String result = adminController.processRoleCreation(roleCreationDto, bindingResult, model, session);

        // Assert
        assertEquals("redirect:/admin/roles/create/check-your-answers", result);
        assertThat(session.getAttribute("roleCreationDto")).isEqualTo(enrichedDto);
    }

    // Additional tests for showAdministration method variations
    @Test
    void testShowAdministration_WithAppFilter_FiltersRoles() {
        // Arrange
        List<AdminAppDto> adminApps = createMockAdminApps();
        List<AppAdminDto> apps = createMockApps();
        List<AppRoleAdminDto> filteredRoles = Arrays.asList(
            AppRoleAdminDto.builder()
                .name("Filtered Role")
                .parentApp("Test App")
                .build()
        );

        when(adminService.getAllAdminApps()).thenReturn(adminApps);
        when(adminService.getAllApps()).thenReturn(apps);
        when(adminService.getAppRolesByApp("Test App")).thenReturn(filteredRoles);

        // Act
        String result = adminController.showAdministration("roles", "Test App", model);

        // Assert
        assertEquals("silas-administration/administration", result);
        assertThat(model.getAttribute("roles")).isEqualTo(filteredRoles);
        assertThat(model.getAttribute("appFilter")).isEqualTo("Test App");
        verify(adminService).getAppRolesByApp("Test App");
    }

    @Test
    void testShowAdministration_WithEmptyAppFilter_LoadsAllRolesAgain() {
        // Arrange
        List<AdminAppDto> adminApps = createMockAdminApps();
        List<AppAdminDto> apps = createMockApps();
        List<AppRoleAdminDto> allRoles = createMockRoles();

        when(adminService.getAllAdminApps()).thenReturn(adminApps);
        when(adminService.getAllApps()).thenReturn(apps);
        when(adminService.getAllAppRoles()).thenReturn(allRoles);

        // Act
        String result = adminController.showAdministration("roles", "", model);

        // Assert
        assertEquals("silas-administration/administration", result);
        assertThat(model.getAttribute("roles")).isEqualTo(allRoles);
        assertThat(model.getAttribute("appFilter")).isEqualTo("");
        verify(adminService).getAllAppRoles();
    }

    @Test
    void testShowAdministration_WithDifferentTabs_SetsCorrectActiveTab() {
        // Arrange
        List<AdminAppDto> adminApps = createMockAdminApps();
        List<AppAdminDto> apps = createMockApps();
        List<AppRoleAdminDto> allRoles = createMockRoles();

        when(adminService.getAllAdminApps()).thenReturn(adminApps);
        when(adminService.getAllApps()).thenReturn(apps);
        when(adminService.getAllAppRoles()).thenReturn(allRoles);

        // Act & Assert for different tabs
        String result1 = adminController.showAdministration("admin-apps", null, model);
        assertEquals("silas-administration/administration", result1);
        assertThat(model.getAttribute("activeTab")).isEqualTo("admin-apps");

        String result2 = adminController.showAdministration("apps", null, model);
        assertEquals("silas-administration/administration", result2);
        assertThat(model.getAttribute("activeTab")).isEqualTo("apps");

        String result3 = adminController.showAdministration("roles", null, model);
        assertEquals("silas-administration/administration", result3);
        assertThat(model.getAttribute("activeTab")).isEqualTo("roles");
    }
}
