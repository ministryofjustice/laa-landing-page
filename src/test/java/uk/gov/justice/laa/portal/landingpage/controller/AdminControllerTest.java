package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.ArrayList;
import java.util.Arrays;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AdminAppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.forms.AppDetailsForm;
import uk.gov.justice.laa.portal.landingpage.forms.AppsOrderForm;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.AdminService;
import uk.gov.justice.laa.portal.landingpage.service.AppRoleService;
import uk.gov.justice.laa.portal.landingpage.service.AppService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import org.springframework.validation.BeanPropertyBindingResult;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;
    @Mock
    private LoginService loginService;
    @Mock
    private EventService eventService;
    @Mock
    private AppService appService;
    @Mock
    private AppRoleService appRoleService;
    @Mock
    private MockHttpSession mockHttpSession;

    private AdminController adminController;
    private Model model;

    @BeforeEach
    void setUp() {
        adminController = new AdminController(loginService, eventService, adminService, appService, appRoleService);
        model = new ExtendedModelMap();
    }

    @Test
    void testShowAdministration_WithDefaultTab_LoadsAllData() {
        // Arrange
        List<AdminAppDto> adminApps = createMockAdminApps();
        List<AppDto> apps = createMockApps();
        List<AppRoleAdminDto> roles = createMockRoles();

        when(adminService.getAllAdminApps()).thenReturn(adminApps);
        when(appService.getAllLaaApps()).thenReturn(apps);
        when(adminService.getAllAppRoles()).thenReturn(roles);

        // Act
        String viewName = adminController.showAdministration("admin-apps", null, model, mockHttpSession);

        // Assert
        assertEquals("silas-administration/administration", viewName);
        assertEquals("SiLAS Administration", model.getAttribute(ModelAttributes.PAGE_TITLE));
        assertEquals("admin-apps", model.getAttribute("activeTab"));

        assertThat(model.getAttribute("adminApps")).isEqualTo(adminApps);
        assertThat(model.getAttribute("apps")).isEqualTo(apps);
        assertThat(model.getAttribute("roles")).isEqualTo(roles);

        verify(adminService).getAllAdminApps();
        // getAllApps is called twice - once for apps data and once for app names filter
        verify(appService, times(1)).getAllLaaApps();
        verify(adminService).getAllAppRoles();
    }

    @Test
    void testShowAdministration_WithRolesTab_LoadsAllData() {
        // Arrange
        List<AdminAppDto> adminApps = createMockAdminApps();
        List<AppDto> apps = createMockApps();
        List<AppRoleAdminDto> roles = createMockRoles();

        when(adminService.getAllAdminApps()).thenReturn(adminApps);
        when(appService.getAllLaaApps()).thenReturn(apps);
        when(adminService.getAllAppRoles()).thenReturn(roles);

        // Act
        String viewName = adminController.showAdministration("roles", null, model, mockHttpSession);

        // Assert
        assertEquals("silas-administration/administration", viewName);
        assertEquals("roles", model.getAttribute("activeTab"));

        assertThat(model.getAttribute("roles")).isEqualTo(roles);

        verify(adminService).getAllAppRoles();
    }

    @Test
    void testShowAdministration_WithAppFilter_FiltersRolesByApp() {
        // Arrange
        List<AppRoleAdminDto> filteredRoles = new ArrayList<>();
        filteredRoles.add(
            AppRoleAdminDto.builder()
                .name("CCMS Viewer")
                .description("View only role")
                .parentApp("CCMS case transfer requests")
                .ordinal(0)
                .build()
        );
        List<AdminAppDto> adminApps = createMockAdminApps();
        List<AppDto> apps = createMockApps();
        String appFilter = "CCMS case transfer requests";

        when(adminService.getAllAdminApps()).thenReturn(adminApps);
        when(appService.getAllLaaApps()).thenReturn(apps);
        when(adminService.getAppRolesByApp(appFilter)).thenReturn(filteredRoles);

        // Act
        String viewName = adminController.showAdministration("roles", appFilter, model, mockHttpSession);

        // Assert
        assertEquals("silas-administration/administration", viewName);
        assertThat(model.getAttribute("roles")).isEqualTo(filteredRoles);
        assertThat(model.getAttribute("appFilter")).isEqualTo(appFilter);

        verify(adminService).getAppRolesByApp(appFilter);
    }

    @Test
    void editAppDetailsGet_createsFormWhenNotInSession() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();
        AppDto appDto = AppDto.builder().id(appId).name("Test App").description("Desc").enabled(true).build();

        when(appService.findById(java.util.UUID.fromString(appId))).thenReturn(java.util.Optional.of(appDto));

        String view = adminController.editAppDetailsGet(appId, model, session);

        assertEquals("silas-administration/edit-app-details", view);
        assertThat(model.getAttribute("app")).isEqualTo(appDto);
        Object form = model.getAttribute("appDetailsForm");
        assertThat(form).isInstanceOf(AppDetailsForm.class);
        AppDetailsForm detailsForm = (AppDetailsForm) form;
        assertThat(detailsForm).isNotNull();
        assertThat(detailsForm.getAppId()).isEqualTo(appId);
        assertThat(session.getAttribute("appDetailsForm")).isNotNull();
    }

    @Test
    void editAppDetailsGet_usesExistingFormFromSession() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();
        AppDto appDto = AppDto.builder().id(appId).name("Test App").description("Desc").enabled(false).build();
        AppDetailsForm existing = AppDetailsForm.builder().appId(appId).description("Existing").enabled(true).build();
        session.setAttribute("appDetailsForm", existing);

        when(appService.findById(java.util.UUID.fromString(appId))).thenReturn(java.util.Optional.of(appDto));

        String view = adminController.editAppDetailsGet(appId, model, session);

        assertEquals("silas-administration/edit-app-details", view);
        assertThat(model.getAttribute("appDetailsForm")).isEqualTo(existing);
        assertThat(session.getAttribute("appDetailsForm")).isEqualTo(existing);
    }

    @Test
    void editAppDetailsPost_withValidationErrors_returnsEditViewWithError() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();
        AppDto appDto = AppDto.builder().id(appId).name("Test App").description("Desc").enabled(true).build();

        BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(result.getAllErrors()).thenReturn(List.of(new ObjectError("description", "Invalid description")));
        when(appService.findById(java.util.UUID.fromString(appId))).thenReturn(java.util.Optional.of(appDto));

        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("").enabled(true).build();

        String view = adminController.editAppDetailsPost(appId, form, result, model, session);

        assertEquals("silas-administration/edit-app-details", view);
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Invalid description");
        assertThat(model.getAttribute("app")).isEqualTo(appDto);
    }

    @Test
    void editAppDetailsPost_withoutErrors_storesInSessionAndRedirects() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();

        BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("Desc").enabled(true).build();

        String view = adminController.editAppDetailsPost(appId, form, result, model, session);

        assertEquals(String.format("redirect:/admin/silas-administration/app/%s/check-answers", appId), view);
        assertThat(session.getAttribute("appDetailsForm")).isEqualTo(form);
        assertThat(session.getAttribute("appId")).isEqualTo(appId);
    }

    @Test
    void confirmAppDetailsGet_returnsCheckAnswersWhenSessionMatches() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();
        AppDto appDto = AppDto.builder().id(appId).name("App").description("D").enabled(true).build();
        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("D").enabled(true).build();

        session.setAttribute("appId", appId);
        session.setAttribute("appDetailsForm", form);

        when(appService.findById(java.util.UUID.fromString(appId))).thenReturn(java.util.Optional.of(appDto));

        String view = adminController.confirmAppDetailsGet(appId, model, session);

        assertEquals("silas-administration/edit-app-details-check-answers", view);
        assertThat(model.getAttribute("app")).isEqualTo(appDto);
        assertThat(model.getAttribute("appDetailsForm")).isEqualTo(form);
    }

    @Test
    void confirmAppDetailsGet_throwsWhenSessionAppIdMismatch() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();
        session.setAttribute("appId", "different-id");
        session.setAttribute("appDetailsForm", AppDetailsForm.builder().appId(appId).description("D").enabled(true).build());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
            adminController.confirmAppDetailsGet(appId, model, session)
        );
    }

    @Test
    void confirmAppDetailsPost_savesAppAndLogsEvent() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();

        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("New Desc").enabled(false).build();
        session.setAttribute("appId", appId);
        session.setAttribute("appDetailsForm", form);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(java.util.UUID.randomUUID());
        currentUser.setName("Admin User");

        AppDto appDto = AppDto.builder().id(appId).name("App").description("Old").enabled(true).build();
        App appEntity = App.builder().id(java.util.UUID.fromString(appId)).name("App").description("New Desc").enabled(false).build();

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);
        when(appService.findById(java.util.UUID.fromString(appId))).thenReturn(java.util.Optional.of(appDto));
        when(appService.save(org.mockito.ArgumentMatchers.any())).thenReturn(appEntity);

        String view = adminController.confirmAppDetailsPost(appId, auth, model, session);

        assertEquals("silas-administration/edit-app-details-confirmation", view);
        verify(eventService).logEvent(org.mockito.ArgumentMatchers.any());
        assertThat(session.getAttribute("appDetailsForm")).isNull();
        assertThat(session.getAttribute("appDetailsFormModel")).isNull();
        assertThat(session.getAttribute("appId")).isNull();
    }

    @Test
    void editAppOrderGet_buildsFormWhenNoSessionValue() {
        MockHttpSession session = new MockHttpSession();
        AppDto app1 = AppDto.builder().id(java.util.UUID.randomUUID().toString()).name("A").ordinal(1).build();
        AppDto app2 = AppDto.builder().id(java.util.UUID.randomUUID().toString()).name("B").ordinal(2).build();
        when(appService.getAllLaaApps()).thenReturn(List.of(app1, app2));

        String view = adminController.editAppOrderGet(model, session);

        assertEquals("silas-administration/edit-apps-order", view);
        Object appsOrderForm = model.getAttribute("appsOrderForm");
        assertThat(appsOrderForm).isNotNull();
        assertThat(appsOrderForm).isInstanceOf(AppsOrderForm.class);
        AppsOrderForm form = (AppsOrderForm) appsOrderForm;
        assertThat(form.getApps()).hasSize(2);
    }

    @Test
    void editAppOrderGet_usesExistingAppsOrderFormFromSession() {
        MockHttpSession session = new MockHttpSession();
        AppsOrderForm.AppOrderDetailsForm details = new AppsOrderForm.AppOrderDetailsForm("1", 5);
        AppsOrderForm existing = AppsOrderForm.builder().apps(List.of(details)).build();
        session.setAttribute("appsOrderForm", existing);

        String view = adminController.editAppOrderGet(model, session);

        assertEquals("silas-administration/edit-apps-order", view);
        assertThat(model.getAttribute("appsOrderForm")).isEqualTo(existing);
    }

    @Test
    void editAppOrderPost_withErrors_returnsEditViewWithErrorMessage() {
        AppsOrderForm form = AppsOrderForm.builder().apps(List.of(new AppsOrderForm.AppOrderDetailsForm("1", 1))).build();
        BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(result.getAllErrors()).thenReturn(List.of(new ObjectError("apps", "Duplicate ordinal")));

        String view = adminController.editAppOrderPost(form, result, model, new MockHttpSession());

        assertEquals("silas-administration/edit-apps-order", view);
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Duplicate ordinal");
    }

    @Test
    void editAppOrderPost_withoutErrors_sortsAndStoresInSession() {
        AppsOrderForm.AppOrderDetailsForm a = new AppsOrderForm.AppOrderDetailsForm("1", 2);
        AppsOrderForm.AppOrderDetailsForm b = new AppsOrderForm.AppOrderDetailsForm("2", 1);
        AppsOrderForm form = AppsOrderForm.builder().apps(new ArrayList<>(Arrays.asList(a, b))).build();
        BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        MockHttpSession session = new MockHttpSession();
        String view = adminController.editAppOrderPost(form, result, model, session);

        assertEquals("silas-administration/edit-apps-order-check-answers", view);
        @SuppressWarnings("unchecked")
        List<AppsOrderForm.AppOrderDetailsForm> appsOrderList = (List<AppsOrderForm.AppOrderDetailsForm>) model.getAttribute("appsOrderList");
        assertThat(appsOrderList).extracting(AppsOrderForm.AppOrderDetailsForm::getOrdinal).containsExactly(1, 2);
        assertThat(session.getAttribute("appsOrderForm")).isNotNull();
    }

    @Test
    void confirmEditAppOrderPost_updatesOrder_logsEvent_and_clearsSession() {
        MockHttpSession session = new MockHttpSession();
        AppsOrderForm.AppOrderDetailsForm a = new AppsOrderForm.AppOrderDetailsForm("1", 1);
        AppsOrderForm form = AppsOrderForm.builder().apps(List.of(a)).build();
        session.setAttribute("appsOrderForm", form);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(java.util.UUID.randomUUID());
        currentUser.setName("Admin");

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);

        String view = adminController.confirmEditAppOrderPost(auth, model, session);

        assertEquals("silas-administration/edit-apps-order-confirmation", view);
        verify(appService).updateAppsOrder(form.getApps());
        verify(eventService).logEvent(org.mockito.ArgumentMatchers.any());
        assertThat(session.getAttribute("appsOrderForm")).isNull();
    }

    @Test
    void confirmEditAppOrderPost_throwsWhenNoSessionValue() {
        MockHttpSession session = new MockHttpSession();
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
            adminController.confirmEditAppOrderPost(auth, model, session)
        );
    }

    @Test
    void showAdministration_clearsSessionAttributes() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("appDetailsForm", new AppDetailsForm());
        session.setAttribute("appDetailsFormModel", new ExtendedModelMap());
        session.setAttribute("appId", "test-id");
        session.setAttribute("appsOrderForm", new AppsOrderForm());

        when(adminService.getAllAdminApps()).thenReturn(createMockAdminApps());
        when(appService.getAllLaaApps()).thenReturn(createMockApps());
        when(adminService.getAllAppRoles()).thenReturn(createMockRoles());

        adminController.showAdministration("admin-apps", null, model, session);

        assertThat(session.getAttribute("appDetailsForm")).isNull();
        assertThat(session.getAttribute("appDetailsFormModel")).isNull();
        assertThat(session.getAttribute("appId")).isNull();
        assertThat(session.getAttribute("appsOrderForm")).isNull();
    }

    @Test
    void showAdministration_withNullAppFilter_loadsAllRoles() {
        List<AppRoleAdminDto> allRoles = createMockRoles();

        when(adminService.getAllAdminApps()).thenReturn(createMockAdminApps());
        when(appService.getAllLaaApps()).thenReturn(createMockApps());
        when(adminService.getAllAppRoles()).thenReturn(allRoles);

        adminController.showAdministration("roles", null, model, mockHttpSession);

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
    void testConfirmCheckYourAnswers_CreatesRoleAndRedirects() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .build();
        session.setAttribute("roleCreationDto", roleCreationDto);

        doNothing().when(appRoleService).createRole(roleCreationDto);

        // Act
        String result = adminController.confirmCheckYourAnswers(session, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/silas-administration/roles/create/confirmation", result);
        verify(appRoleService).createRole(roleCreationDto);
        assertThat(session.getAttribute("roleCreationDto")).isNull();
        assertThat(session.getAttribute("createdRole")).isEqualTo(roleCreationDto);
    }

    @Test
    void testConfirmCheckYourAnswers_WithServiceException_HandlesError() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .build();
        session.setAttribute("roleCreationDto", roleCreationDto);

        RuntimeException exception = new RuntimeException("Test error");
        doThrow(exception).when(appRoleService).createRole(roleCreationDto);

        // Act
        String result = adminController.confirmCheckYourAnswers(session, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/silas-administration?tab=roles#roles", result);
        verify(appRoleService).createRole(roleCreationDto);
    }

    @Test
    void showAdministration_loadsAppNamesForFilter_sorted() {
        java.util.List<uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto> mockApps = Arrays.asList(
            uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto.builder().id("1").name("Zebra App").ordinal(0).build(),
            uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto.builder().id("2").name("Alpha App").ordinal(1).build(),
            uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto.builder().id("3").name("Beta App").ordinal(2).build()
        );

        when(adminService.getAllAdminApps()).thenReturn(createMockAdminApps());
        when(appService.getAllLaaApps()).thenReturn(createMockApps());
        when(adminService.getAllAppRoles()).thenReturn(createMockRoles());
        when(adminService.getAllApps()).thenReturn(mockApps);

        adminController.showAdministration("roles", null, model, mockHttpSession);

        @SuppressWarnings("unchecked")
        List<String> appNames = (List<String>) model.getAttribute("appNames");
        assertThat(appNames).containsExactly("Alpha App", "Beta App", "Zebra App");
    }

    @Test
    void editAppDetailsGet_throwsWhenAppNotFound() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();

        when(appService.findById(java.util.UUID.fromString(appId))).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
            adminController.editAppDetailsGet(appId, model, session)
        );
    }

    @Test
    void editAppDetailsPost_withMultipleValidationErrors_buildsErrorMessage() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();
        AppDto appDto = AppDto.builder().id(appId).name("Test App").description("Desc").enabled(true).build();

        BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(result.getAllErrors()).thenReturn(List.of(
            new ObjectError("description", "Description is required"),
            new ObjectError("name", "Name is required")
        ));
        when(appService.findById(java.util.UUID.fromString(appId))).thenReturn(java.util.Optional.of(appDto));

        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("").enabled(true).build();

        String view = adminController.editAppDetailsPost(appId, form, result, model, session);

        assertEquals("silas-administration/edit-app-details", view);
        String errorMessage = (String) model.getAttribute("errorMessage");
        assertThat(errorMessage).contains("Description is required");
        assertThat(errorMessage).contains("Name is required");
        assertThat(errorMessage).contains("\n");
    }

    @Test
    void confirmAppDetailsGet_throwsWhenAppIdNotInSession() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();
        session.setAttribute("appDetailsForm", AppDetailsForm.builder().appId(appId).description("D").enabled(true).build());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
            adminController.confirmAppDetailsGet(appId, model, session)
        );
    }

    @Test
    void confirmAppDetailsGet_throwsWhenFormNotInSession() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();
        session.setAttribute("appId", appId);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
            adminController.confirmAppDetailsGet(appId, model, session)
        );
    }

    @Test
    void confirmAppDetailsPost_throwsWhenAppIdNotInSession() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();

        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("New Desc").enabled(false).build();
        session.setAttribute("appDetailsForm", form);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
            adminController.confirmAppDetailsPost(appId, auth, model, session)
        );
    }

    @Test
    void confirmAppDetailsPost_throwsWhenFormNotInSession() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();
        session.setAttribute("appId", appId);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
            adminController.confirmAppDetailsPost(appId, auth, model, session)
        );
    }

    @Test
    void confirmAppDetailsPost_throwsWhenAppNotFound() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();

        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("New Desc").enabled(false).build();
        session.setAttribute("appId", appId);
        session.setAttribute("appDetailsForm", form);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);

        when(appService.findById(java.util.UUID.fromString(appId))).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
            adminController.confirmAppDetailsPost(appId, auth, model, session)
        );
    }

    @Test
    void editAppOrderPost_with_multipleErrorMessages_buildsErrorMessage() {
        AppsOrderForm form = AppsOrderForm.builder().apps(List.of(new AppsOrderForm.AppOrderDetailsForm("1", 1))).build();
        BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(result.getAllErrors()).thenReturn(List.of(
            new ObjectError("apps", "Error 1"),
            new ObjectError("apps", "Error 2")
        ));

        String view = adminController.editAppOrderPost(form, result, model, new MockHttpSession());

        assertEquals("silas-administration/edit-apps-order", view);
        String errorMessage = (String) model.getAttribute("errorMessage");
        assertThat(errorMessage).contains("Error 1");
        assertThat(errorMessage).contains("Error 2");
        assertThat(errorMessage).contains("\n");
    }

    @Test
    void editAppOrderPost_withEmptyAppsList_storesInSession() {
        AppsOrderForm form = AppsOrderForm.builder().apps(new ArrayList<>()).build();
        BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        MockHttpSession session = new MockHttpSession();
        String view = adminController.editAppOrderPost(form, result, model, session);

        assertEquals("silas-administration/edit-apps-order-check-answers", view);
        assertThat(session.getAttribute("appsOrderForm")).isNotNull();
    }

    @Test
    void confirmEditAppOrderPost_throwsWhenFormNotInSession() {
        MockHttpSession session = new MockHttpSession();
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
            adminController.confirmEditAppOrderPost(auth, model, session)
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
    void testConfirmCheckYourAnswers_WithValidationException_HandlesError() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Invalid Role")
                .description("Test Description")
                .build();
        session.setAttribute("roleCreationDto", roleCreationDto);

        IllegalArgumentException exception = new IllegalArgumentException("Role name already exists");
        doThrow(exception).when(appRoleService).createRole(roleCreationDto);

        // Act
        String result = adminController.confirmCheckYourAnswers(session, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/silas-administration?tab=roles#roles", result);
        verify(appRoleService).createRole(roleCreationDto);
        assertThat(session.getAttribute("roleCreationDto")).isNotNull();
    }

    @Test
    void testConfirmCheckYourAnswers_WithCompleteRoleData_ProcessesSuccessfully() {
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

        doNothing().when(appRoleService).createRole(roleCreationDto);

        // Act
        String result = adminController.confirmCheckYourAnswers(session, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/silas-administration/roles/create/confirmation", result);
        verify(appRoleService).createRole(roleCreationDto);
        assertThat(session.getAttribute("roleCreationDto")).isNull();
        assertThat(session.getAttribute("createdRole")).isEqualTo(roleCreationDto);
    }

    @Test
    void testConfirmCheckYourAnswers_WithDatabaseException_HandlesError() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("DB Error Role")
                .description("Test Description")
                .build();
        session.setAttribute("roleCreationDto", roleCreationDto);

        RuntimeException dbException = new RuntimeException("Database connection failed");
        doThrow(dbException).when(appRoleService).createRole(roleCreationDto);

        // Act
        String result = adminController.confirmCheckYourAnswers(session, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/silas-administration?tab=roles#roles", result);
        verify(appRoleService).createRole(roleCreationDto);
    }

    @Test
    void testConfirmCheckYourAnswers_WithEmptyRoleData_HandlesGracefully() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("")
                .description("")
                .build();
        session.setAttribute("roleCreationDto", roleCreationDto);

        doNothing().when(appRoleService).createRole(roleCreationDto);

        // Act
        String result = adminController.confirmCheckYourAnswers(session, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/silas-administration/roles/create/confirmation", result);
        verify(appRoleService).createRole(roleCreationDto);
        assertThat(session.getAttribute("roleCreationDto")).isNull();
        assertThat(session.getAttribute("createdRole")).isEqualTo(roleCreationDto);
    }

    @Test
    void testConfirmCheckYourAnswers_WithConcurrentModification_HandlesError() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        RoleCreationDto roleCreationDto = RoleCreationDto.builder()
                .name("Concurrent Role")
                .description("Test Description")
                .build();
        session.setAttribute("roleCreationDto", roleCreationDto);

        RuntimeException concurrencyException = new RuntimeException("Optimistic locking failure");
        doThrow(concurrencyException).when(appRoleService).createRole(roleCreationDto);

        // Act
        String result = adminController.confirmCheckYourAnswers(session, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/silas-administration?tab=roles#roles", result);
        verify(appRoleService).createRole(roleCreationDto);
    }

    @Test
    void testShowRoleCreationForm_WithNoSessionData_CreatesNewDto() {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        List<AppDto> apps = createMockApps();
        when(appService.getAllLaaApps()).thenReturn(apps);

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

        List<AppDto> apps = createMockApps();
        when(appService.getAllLaaApps()).thenReturn(apps);

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

        when(appRoleService.isRoleNameExistsInApp("Valid Role", appId)).thenReturn(false);
        when(appRoleService.enrichRoleCreationDto(roleCreationDto)).thenReturn(enrichedDto);

        // Act
        String result = adminController.processRoleCreation(roleCreationDto, bindingResult, model, session);

        // Assert
        assertEquals("redirect:/admin/silas-administration/roles/create/check-your-answers", result);
        assertThat(session.getAttribute("roleCreationDto")).isEqualTo(enrichedDto);
        verify(appRoleService).enrichRoleCreationDto(roleCreationDto);
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
        List<AppDto> apps = createMockApps();

        when(appRoleService.isRoleNameExistsInApp("Duplicate Role", appId)).thenReturn(true);
        when(appService.getAllLaaApps()).thenReturn(apps);

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
        List<AppDto> apps = createMockApps();

        when(appService.getAllLaaApps()).thenReturn(apps);

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

        when(appRoleService.enrichRoleCreationDto(roleCreationDto)).thenReturn(enrichedDto);

        // Act
        String result = adminController.processRoleCreation(roleCreationDto, bindingResult, model, session);

        // Assert
        assertEquals("redirect:/admin/silas-administration/roles/create/check-your-answers", result);
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

        when(appRoleService.enrichRoleCreationDto(roleCreationDto)).thenReturn(enrichedDto);

        // Act
        String result = adminController.processRoleCreation(roleCreationDto, bindingResult, model, session);

        // Assert
        assertEquals("redirect:/admin/silas-administration/roles/create/check-your-answers", result);
        assertThat(session.getAttribute("roleCreationDto")).isEqualTo(enrichedDto);
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

    private List<AppDto> createMockApps() {
        return Arrays.asList(
            AppDto.builder()
                .name("Apply for criminal legal aid")
                .description("Make an application for criminal legal aid")
                .ordinal(0)
                .build(),
            AppDto.builder()
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
