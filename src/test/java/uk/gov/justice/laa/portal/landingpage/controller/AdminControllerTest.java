package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.security.core.Authentication;

import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AdminAppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.forms.AppDetailsForm;
import uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm;
import uk.gov.justice.laa.portal.landingpage.forms.AppsOrderForm;
import uk.gov.justice.laa.portal.landingpage.service.AdminService;
import uk.gov.justice.laa.portal.landingpage.service.AppRoleService;
import uk.gov.justice.laa.portal.landingpage.service.AppService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.entity.App;

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
        when(appRoleService.getAllLaaAppRoles()).thenReturn(roles);

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
        verify(appRoleService).getAllLaaAppRoles();
    }

    @Test
    void testShowAdministration_WithRolesTab_LoadsAllData() {
        // Arrange
        List<AdminAppDto> adminApps = createMockAdminApps();
        List<AppDto> apps = createMockApps();
        List<AppRoleAdminDto> roles = createMockRoles();

        when(adminService.getAllAdminApps()).thenReturn(adminApps);
        when(appService.getAllLaaApps()).thenReturn(apps);
        when(appRoleService.getAllLaaAppRoles()).thenReturn(roles);

        // Act
        String viewName = adminController.showAdministration("roles", null, model, mockHttpSession);

        // Assert
        assertEquals("silas-administration/administration", viewName);
        assertEquals("roles", model.getAttribute("activeTab"));

        assertThat(model.getAttribute("roles")).isEqualTo(roles);

        verify(appRoleService).getAllLaaAppRoles();
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
        when(appRoleService.getLaaAppRolesByAppName(appFilter)).thenReturn(filteredRoles);

        // Act
        String viewName = adminController.showAdministration("roles", appFilter, model, mockHttpSession);

        // Assert
        assertEquals("silas-administration/administration", viewName);
        assertThat(model.getAttribute("roles")).isEqualTo(filteredRoles);
        assertThat(model.getAttribute("appFilter")).isEqualTo(appFilter);

        verify(appRoleService).getLaaAppRolesByAppName(appFilter);
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
        when(appRoleService.getAllLaaAppRoles()).thenReturn(createMockRoles());

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
        when(appRoleService.getAllLaaAppRoles()).thenReturn(allRoles);

        adminController.showAdministration("roles", null, model, mockHttpSession);

        assertThat(model.getAttribute("roles")).isEqualTo(allRoles);
        verify(appRoleService).getAllLaaAppRoles();
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
        when(appRoleService.getAllLaaAppRoles()).thenReturn(createMockRoles());
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
        assertThat(errorMessage).contains("<br/>");
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
    void confirmAppDetailsPost_throwsWhenAppIdNotMatchingInSession() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();
        session.setAttribute("appId", UUID.randomUUID().toString());

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
    void editAppOrderGet_returnsEditAppsOrderView() {
        MockHttpSession session = new MockHttpSession();
        AppDto app1 = AppDto.builder().id(java.util.UUID.randomUUID().toString()).name("A").ordinal(1).build();
        when(appService.getAllLaaApps()).thenReturn(List.of(app1));

        String view = adminController.editAppOrderGet(model, session);

        assertEquals("silas-administration/edit-apps-order", view);
        assertThat(model.getAttribute("appsOrderForm")).isNotNull();
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("SiLAS Administration");
    }

    @Test
    void confirmEditAppRolesOrderPost_removesAllRelatedSessionAttributes() {
        MockHttpSession session = new MockHttpSession();
        String appName = "Test App";

        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm a =
            uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                .appRoleId("1")
                .name("Role 1")
                .ordinal(1)
                .build();
        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm form =
            uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.builder()
                .appRoles(List.of(a))
                .build();
        session.setAttribute("appRolesOrderForm", form);
        session.setAttribute("appFilter", appName);
        session.setAttribute("roleId", "some-role-id");

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(java.util.UUID.randomUUID());

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);

        String view = adminController.confirmEditAppRolesOrderPost(auth, model, session);

        assertEquals("silas-administration/edit-app-roles-order-confirmation", view);
        assertThat(session.getAttribute("appRolesOrderForm")).isNull();
        assertThat(session.getAttribute("appFilter")).isNull();
        assertThat(session.getAttribute("roleId")).isNull();
    }

    @Test
    void cancelWithAdminAppsTab_clearsSessionAndRedirects() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("appDetailsForm", new AppDetailsForm());

        String view = adminController.cancel(session, "admin-apps");

        assertEquals("redirect:/admin/silas-administration#admin-apps", view);
        assertThat(session.getAttribute("appDetailsForm")).isNull();
    }

    @Test
    void editAppRolesOrderGet_withBlankAppName_redirectsWithFlashAttribute() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("appFilter", "   ");
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
            org.mockito.Mockito.mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

        String view = adminController.editAppRolesOrderGet(model, redirectAttributes, session);

        assertEquals("redirect:/admin/silas-administration#roles", view);
        verify(redirectAttributes).addFlashAttribute("appRolesErrorMessage", "Please select an application to reorder its roles");
    }

    @Test
    void confirmAppRoleDetailsPost_throwsWhenRoleIdNotMatchingInSession() {
        MockHttpSession session = new MockHttpSession();
        String roleId = java.util.UUID.randomUUID().toString();
        session.setAttribute("roleId", UUID.randomUUID().toString());

        AppRoleDetailsForm form = AppRoleDetailsForm.builder().appRoleId(roleId).name("New Name").description("New Desc").build();
        session.setAttribute("appRoleDetailsForm", form);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                adminController.confirmAppRoleDetailsPost(roleId, auth, model, session)
        );
    }

    @Test
    void confirmAppRoleDetailsPost_savesRoleAndLogsEvent() {
        MockHttpSession session = new MockHttpSession();
        String roleId = java.util.UUID.randomUUID().toString();

        uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm form =
            uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.builder()
                .appRoleId(roleId)
                .name("New Name")
                .description("New Desc")
                .build();
        session.setAttribute("roleId", roleId);
        session.setAttribute("appRoleDetailsForm", form);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(java.util.UUID.randomUUID());
        currentUser.setName("Admin");

        uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto roleDto = uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto.builder()
            .id(roleId)
            .name("Old Name")
            .description("Old Desc")
            .build();
        uk.gov.justice.laa.portal.landingpage.entity.AppRole roleEntity = uk.gov.justice.laa.portal.landingpage.entity.AppRole.builder()
            .id(java.util.UUID.fromString(roleId))
            .name("New Name")
            .description("New Desc")
            .build();

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);
        when(appRoleService.findById(java.util.UUID.fromString(roleId))).thenReturn(java.util.Optional.of(roleDto));
        when(appRoleService.save(org.mockito.ArgumentMatchers.any())).thenReturn(roleEntity);

        String view = adminController.confirmAppRoleDetailsPost(roleId, auth, model, session);

        assertEquals("silas-administration/edit-role-details-confirmation", view);
        verify(eventService).logEvent(org.mockito.ArgumentMatchers.any());
        assertThat(session.getAttribute("appRoleDetailsForm")).isNull();
        assertThat(session.getAttribute("appRoleDetailsFormModel")).isNull();
        assertThat(session.getAttribute("roleId")).isNull();
    }

    @Test
    void editAppRolesOrderGet_withoutAppSelection_redirectsWithFlashAttribute() {
        MockHttpSession session = new MockHttpSession();
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
            org.mockito.Mockito.mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

        String view = adminController.editAppRolesOrderGet(model, redirectAttributes, session);

        assertEquals("redirect:/admin/silas-administration#roles", view);
        verify(redirectAttributes).addFlashAttribute("appRolesErrorMessage", "Please select an application to reorder its roles");
    }

    @Test
    void editAppRolesOrderGet_withAppSelection_buildsFormWhenNotInSession() {
        MockHttpSession session = new MockHttpSession();
        String appName = "Test App";
        session.setAttribute("appFilter", appName);

        List<AppRoleAdminDto> appRoles = Arrays.asList(
            AppRoleAdminDto.builder().id("1").name("Role 1").ordinal(1).build(),
            AppRoleAdminDto.builder().id("2").name("Role 2").ordinal(2).build()
        );

        when(appRoleService.getLaaAppRolesByAppName(appName)).thenReturn(appRoles);

        String view = adminController.editAppRolesOrderGet(model, org.mockito.Mockito.mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class), session);

        assertEquals("silas-administration/edit-app-roles-order", view);
        Object appRolesOrderForm = model.getAttribute("appRolesOrderForm");
        assertThat(appRolesOrderForm).isInstanceOf(uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.class);
    }

    @Test
    void editAppRolesOrderPost_withoutErrors_sortsAndStoresInSession() {
        MockHttpSession session = new MockHttpSession();
        String appName = "Test App";
        session.setAttribute("appFilter", appName);

        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm a =
            uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                .appRoleId("1")
                .name("Role 1")
                .ordinal(2)
                .build();
        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm b =
            uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                .appRoleId("2")
                .name("Role 2")
                .ordinal(1)
                .build();
        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm form =
            uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.builder()
                .appRoles(new ArrayList<>(Arrays.asList(a, b)))
                .build();
        BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        String view = adminController.editAppRolesOrderPost(form, result, model, session);

        assertEquals("silas-administration/edit-app-roles-order-check-answers", view);
        assertThat(session.getAttribute("appRolesOrderForm")).isNotNull();
    }

    @Test
    void editAppRolesOrderPost_withValidationErrors_sortsAndStoresInSession() {
        MockHttpSession session = new MockHttpSession();
        String appName = "Test App";
        session.setAttribute("appFilter", appName);

        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm a =
                uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                        .appRoleId("1")
                        .name("Role 1")
                        .ordinal(2)
                        .build();
        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm b =
                uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                        .appRoleId("2")
                        .name("Role 2")
                        .ordinal(1)
                        .build();
        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm form =
                uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.builder()
                        .appRoles(new ArrayList<>(Arrays.asList(a, b)))
                        .build();
        BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(result.getAllErrors()).thenReturn(List.of(new ObjectError("appRoles", "Duplicate ordinal")));

        String view = adminController.editAppRolesOrderPost(form, result, model, session);

        assertEquals("silas-administration/edit-app-roles-order", view);
        assertThat(model.getAttribute("errorMessage")).isNotNull();
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Duplicate ordinal");
    }

    @Test
    void confirmEditAppRolesOrderPost_updatesOrderAndClearsSession() {
        MockHttpSession session = new MockHttpSession();
        String appName = "Test App";

        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm a =
            uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                .appRoleId("1")
                .name("Role 1")
                .ordinal(1)
                .build();
        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm form =
            uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.builder()
                .appRoles(List.of(a))
                .build();
        session.setAttribute("appRolesOrderForm", form);
        session.setAttribute("appFilter", appName);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(java.util.UUID.randomUUID());
        currentUser.setName("Admin");

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);

        String view = adminController.confirmEditAppRolesOrderPost(auth, model, session);

        assertEquals("silas-administration/edit-app-roles-order-confirmation", view);
        verify(appRoleService).updateAppRolesOrder(form.getAppRoles());
        verify(eventService).logEvent(org.mockito.ArgumentMatchers.any());
        assertThat(session.getAttribute("appRolesOrderForm")).isNull();
        assertThat(session.getAttribute("appFilter")).isNull();
    }

    @Test
    void cancelWithValidTab_redirectsToAdminPageWithTab() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("appDetailsForm", new AppDetailsForm());
        session.setAttribute("appId", "test-id");

        String view = adminController.cancel(session, "apps");

        assertEquals("redirect:/admin/silas-administration#apps", view);
        assertThat(session.getAttribute("appDetailsForm")).isNull();
        assertThat(session.getAttribute("appId")).isNull();
    }

    @Test
    void cancelWithInvalidTab_defaultsToAdminAppsTab() {
        MockHttpSession session = new MockHttpSession();

        String view = adminController.cancel(session, "invalid-tab");

        assertEquals("redirect:/admin/silas-administration#admin-apps", view);
    }

    @Test
    void cancelWithRolesTab_clearsAllSessionAttributes() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("appDetailsForm", new AppDetailsForm());
        session.setAttribute("appDetailsFormModel", new ExtendedModelMap());
        session.setAttribute("appId", "test-id");
        session.setAttribute("appRoleDetailsForm", new uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm());
        session.setAttribute("appsOrderForm", new AppsOrderForm());
        session.setAttribute("appRolesOrderForm", new uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm());
        session.setAttribute("roleId", "role-123");
        session.setAttribute("appFilter", "test-app");

        String view = adminController.cancel(session, "roles");

        assertEquals("redirect:/admin/silas-administration#roles", view);
        assertThat(session.getAttribute("appDetailsForm")).isNull();
        assertThat(session.getAttribute("appDetailsFormModel")).isNull();
        assertThat(session.getAttribute("appId")).isNull();
        assertThat(session.getAttribute("appRoleDetailsForm")).isNull();
        assertThat(session.getAttribute("appsOrderForm")).isNull();
        assertThat(session.getAttribute("appRolesOrderForm")).isNull();
        assertThat(session.getAttribute("roleId")).isNull();
        assertThat(session.getAttribute("appFilter")).isNull();
    }

    @Test
    void editAppDetailsGet_withInvalidUuid_throwsResponseStatusException() {
        MockHttpSession session = new MockHttpSession();
        String invalidAppId = "not-a-uuid";

        org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class, () ->
                adminController.editAppDetailsGet(invalidAppId, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void editAppRoleDetailsGet_withInvalidUuid_throwsResponseStatusException() {
        MockHttpSession session = new MockHttpSession();
        String invalidRoleId = "invalid-uuid";

        org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class, () ->
                adminController.editAppRoleDetailsGet(invalidRoleId, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void editAppRolesOrderPost_throwsWhenAppNotSelected() {
        MockHttpSession session = new MockHttpSession();

        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm form =
            uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.builder()
                .appRoles(List.of(uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                    .appRoleId("1")
                    .name("Role 1")
                    .ordinal(1)
                    .build()))
                .build();
        BindingResult result = org.mockito.Mockito.mock(BindingResult.class);

        org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class, () ->
                adminController.editAppRolesOrderPost(form, result, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    @Test
    void confirmEditAppRolesOrderPost_throwsWhenFormNotInSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("appFilter", "Test App");

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);

        org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class, () ->
                adminController.confirmEditAppRolesOrderPost(auth, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    @Test
    void confirmEditAppRolesOrderPost_throwsWhenAppFilterNotInSession() {
        MockHttpSession session = new MockHttpSession();
        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm a =
            uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                .appRoleId("1")
                .name("Role 1")
                .ordinal(1)
                .build();
        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm form =
            uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.builder()
                .appRoles(List.of(a))
                .build();
        session.setAttribute("appRolesOrderForm", form);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);

        org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class, () ->
                adminController.confirmEditAppRolesOrderPost(auth, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
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

    @Test
    void confirmAppDetailsPost_updatesAppWithFormValues() {
        MockHttpSession session = new MockHttpSession();
        String appId = java.util.UUID.randomUUID().toString();

        AppDetailsForm form = AppDetailsForm.builder()
            .appId(appId)
            .description("Updated Description")
            .enabled(false)
            .build();
        session.setAttribute("appId", appId);
        session.setAttribute("appDetailsForm", form);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(java.util.UUID.randomUUID());
        currentUser.setName("Admin User");

        AppDto appDto = AppDto.builder()
            .id(appId)
            .name("App")
            .description("Old Description")
            .enabled(true)
            .build();
        App appEntity = App.builder()
            .id(java.util.UUID.fromString(appId))
            .name("App")
            .description("Updated Description")
            .enabled(false)
            .build();

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);
        when(appService.findById(java.util.UUID.fromString(appId))).thenReturn(java.util.Optional.of(appDto));
        when(appService.save(org.mockito.ArgumentMatchers.any())).thenReturn(appEntity);

        adminController.confirmAppDetailsPost(appId, auth, model, session);

        assertThat(appDto.isEnabled()).isFalse();
        assertThat(appDto.getDescription()).isEqualTo("Updated Description");
        verify(appService).save(appDto);
    }

    @Test
    void confirmAppRoleDetailsPost_updatesRoleWithFormValues() {
        MockHttpSession session = new MockHttpSession();
        String roleId = java.util.UUID.randomUUID().toString();

        uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm form =
            uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.builder()
                .appRoleId(roleId)
                .name("Updated Role Name")
                .description("Updated Role Description")
                .build();
        session.setAttribute("roleId", roleId);
        session.setAttribute("appRoleDetailsForm", form);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(java.util.UUID.randomUUID());
        currentUser.setName("Admin");

        uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto roleDto = uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto.builder()
            .id(roleId)
            .name("Old Role Name")
            .description("Old Role Description")
            .build();
        uk.gov.justice.laa.portal.landingpage.entity.AppRole roleEntity = uk.gov.justice.laa.portal.landingpage.entity.AppRole.builder()
            .id(java.util.UUID.fromString(roleId))
            .name("Updated Role Name")
            .description("Updated Role Description")
            .build();

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);
        when(appRoleService.findById(java.util.UUID.fromString(roleId))).thenReturn(java.util.Optional.of(roleDto));
        when(appRoleService.save(org.mockito.ArgumentMatchers.any())).thenReturn(roleEntity);

        adminController.confirmAppRoleDetailsPost(roleId, auth, model, session);

        assertThat(roleDto.getName()).isEqualTo("Updated Role Name");
        assertThat(roleDto.getDescription()).isEqualTo("Updated Role Description");
        verify(appRoleService).save(roleDto);
    }

@Test
void editAppRolesOrderPost_storesFormInSession() {
    MockHttpSession session = new MockHttpSession();
    String appName = "Test App";
    session.setAttribute("appFilter", appName);

    uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm a =
        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
            .appRoleId("1")
            .name("Role 1")
            .ordinal(2)
            .build();
    uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm form =
        uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm.builder()
            .appRoles(Arrays.asList(a))
            .build();
    BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
    when(result.hasErrors()).thenReturn(false);

    adminController.editAppRolesOrderPost(form, result, model, session);

    uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm storedForm =
        (uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm) session.getAttribute("appRolesOrderForm");
    assertThat(storedForm).isNotNull();
    assertThat(storedForm.getAppRoles()).hasSize(1);
    assertThat(storedForm.getAppRoles().getFirst().getOrdinal()).isEqualTo(2);
}

// ...existing tests...

@Test
void editAppRoleDetailsGet_loadsRoleDtoAndCreatesForm() {
    MockHttpSession session = new MockHttpSession();
    String roleId = java.util.UUID.randomUUID().toString();
    uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto roleDto = uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto.builder()
        .id(roleId)
        .name("Test Role")
        .description("Test Description")
        .build();

    when(appRoleService.findById(java.util.UUID.fromString(roleId))).thenReturn(java.util.Optional.of(roleDto));

    String view = adminController.editAppRoleDetailsGet(roleId, model, session);

    assertEquals("silas-administration/edit-role-details", view);
    assertThat(model.getAttribute("appRole")).isEqualTo(roleDto);
    assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("SiLAS Administration");
    assertThat(model.getAttribute(ModelAttributes.PAGE_SUMMARY)).isEqualTo("Legal Aid Services");
    Object form = model.getAttribute("appRoleDetailsForm");
    assertThat(form).isInstanceOf(uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.class);
    uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm detailsForm =
        (uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm) form;
    assertThat(detailsForm.getAppRoleId()).isEqualTo(roleId);
    assertThat(detailsForm.getName()).isEqualTo("Test Role");
    assertThat(detailsForm.getDescription()).isEqualTo("Test Description");
    assertThat(session.getAttribute("appRoleDetailsForm")).isNotNull();
}

@Test
void editAppRoleDetailsGet_usesExistingFormFromSession() {
    MockHttpSession session = new MockHttpSession();
    String roleId = java.util.UUID.randomUUID().toString();
    uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto roleDto = uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto.builder()
        .id(roleId)
        .name("Original Role Name")
        .description("Original Description")
        .build();
    uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm existingForm =
        uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.builder()
            .appRoleId(roleId)
            .name("Modified Role Name")
            .description("Modified Description")
            .build();
    session.setAttribute("appRoleDetailsForm", existingForm);

    when(appRoleService.findById(java.util.UUID.fromString(roleId))).thenReturn(java.util.Optional.of(roleDto));

    String view = adminController.editAppRoleDetailsGet(roleId, model, session);

    assertEquals("silas-administration/edit-role-details", view);
    assertThat(model.getAttribute("appRoleDetailsForm")).isEqualTo(existingForm);
    uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm retrievedForm =
        (uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm) model.getAttribute("appRoleDetailsForm");
    assertThat(retrievedForm.getName()).isEqualTo("Modified Role Name");
    assertThat(retrievedForm.getDescription()).isEqualTo("Modified Description");
}

@Test
void editAppRoleDetailsGet_throwsWhenRoleNotFound() {
    MockHttpSession session = new MockHttpSession();
    String roleId = java.util.UUID.randomUUID().toString();

    when(appRoleService.findById(java.util.UUID.fromString(roleId))).thenReturn(java.util.Optional.empty());

    org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
        org.springframework.web.server.ResponseStatusException.class, () ->
            adminController.editAppRoleDetailsGet(roleId, model, session)
    );

    assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    assertThat(exception.getReason()).contains("App role details not found");
}

@Test
void editAppRoleDetailsGet_withInvalidRoleId_throwsBadRequest() {
    MockHttpSession session = new MockHttpSession();
    String invalidRoleId = "not-a-valid-uuid";

    org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
        org.springframework.web.server.ResponseStatusException.class, () ->
            adminController.editAppRoleDetailsGet(invalidRoleId, model, session)
    );

    assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
}

@Test
void confirmAppRoleDetailsGet_returnsCheckAnswersPageWithRoleAndForm() {
    MockHttpSession session = new MockHttpSession();
    String roleId = java.util.UUID.randomUUID().toString();
    uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto roleDto = uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto.builder()
        .id(roleId)
        .name("Admin Role")
        .description("Administrator Role")
        .build();
    uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm form =
        uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.builder()
            .appRoleId(roleId)
            .name("Admin Role")
            .description("Administrator Role")
            .build();

    session.setAttribute("roleId", roleId);
    session.setAttribute("appRoleDetailsForm", form);

    when(appRoleService.findById(java.util.UUID.fromString(roleId))).thenReturn(java.util.Optional.of(roleDto));

    String view = adminController.confirmAppRoleDetailsGet(roleId, model, session);

    assertEquals("silas-administration/edit-role-details-check-answers", view);
    assertThat(model.getAttribute("appRole")).isEqualTo(roleDto);
    assertThat(model.getAttribute("appRoleDetailsForm")).isEqualTo(form);
    assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("SiLAS Administration");
}

@Test
void confirmAppRoleDetailsGet_throwsWhenRoleIdNotInSession() {
    MockHttpSession session = new MockHttpSession();
    String roleId = java.util.UUID.randomUUID().toString();
    session.setAttribute("appRoleDetailsForm", uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.builder()
        .appRoleId(roleId).name("R").description("D").build());

    org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
        org.springframework.web.server.ResponseStatusException.class, () ->
            adminController.confirmAppRoleDetailsGet(roleId, model, session)
    );

    assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    assertThat(exception.getReason()).contains("Role ID not found in session");
}

@Test
void confirmAppRoleDetailsGet_throwsWhenFormNotInSession() {
    MockHttpSession session = new MockHttpSession();
    String roleId = java.util.UUID.randomUUID().toString();
    session.setAttribute("roleId", roleId);

    org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
        org.springframework.web.server.ResponseStatusException.class, () ->
            adminController.confirmAppRoleDetailsGet(roleId, model, session)
    );

    assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    assertThat(exception.getReason()).contains("App role details not found in session");
}

@Test
void confirmAppRoleDetailsGet_throwsWhenRoleIdMismatch() {
    MockHttpSession session = new MockHttpSession();
    String roleId = java.util.UUID.randomUUID().toString();
    String differentRoleId = java.util.UUID.randomUUID().toString();
    session.setAttribute("roleId", differentRoleId);
    session.setAttribute("appRoleDetailsForm", uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.builder()
        .appRoleId(roleId).name("R").description("D").build());

    org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
        org.springframework.web.server.ResponseStatusException.class, () ->
            adminController.confirmAppRoleDetailsGet(roleId, model, session)
    );

    assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    assertThat(exception.getReason()).contains("Invalid request for app details change");
}

@Test
void confirmAppRoleDetailsGet_throwsWhenRoleNotFound() {
    MockHttpSession session = new MockHttpSession();
    String roleId = java.util.UUID.randomUUID().toString();
    session.setAttribute("roleId", roleId);
    session.setAttribute("appRoleDetailsForm", uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.builder()
        .appRoleId(roleId).name("R").description("D").build());

    when(appRoleService.findById(java.util.UUID.fromString(roleId))).thenReturn(java.util.Optional.empty());

    org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
        org.springframework.web.server.ResponseStatusException.class, () ->
            adminController.confirmAppRoleDetailsGet(roleId, model, session)
    );

    assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    assertThat(exception.getReason()).contains("App role details not found");
}

@Test
void confirmAppRoleDetailsGet_withInvalidRoleId_throwsBadRequest() {
    MockHttpSession session = new MockHttpSession();
    String invalidRoleId = "invalid-uuid";
    session.setAttribute("roleId", invalidRoleId);
    session.setAttribute("appRoleDetailsForm", uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.builder()
        .appRoleId(invalidRoleId).name("R").description("D").build());

    org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
        org.springframework.web.server.ResponseStatusException.class, () ->
            adminController.confirmAppRoleDetailsGet(invalidRoleId, model, session)
    );

    assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
}

@Test
void editAppRoleDetailsPost_withValidationErrors_returnsEditViewWithError() {
    MockHttpSession session = new MockHttpSession();
    String roleId = java.util.UUID.randomUUID().toString();
    uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto roleDto = uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto.builder()
        .id(roleId)
        .name("Test Role")
        .description("Test Description")
        .build();

    BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
    when(result.hasErrors()).thenReturn(true);
    when(result.getAllErrors()).thenReturn(List.of(new ObjectError("name", "Role name is required")));
    when(appRoleService.findById(java.util.UUID.fromString(roleId))).thenReturn(java.util.Optional.of(roleDto));

    uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm form =
        uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.builder()
            .appRoleId(roleId)
            .name("")
            .description("Desc")
            .build();

    String view = adminController.editAppRoleDetailsPost(roleId, form, result, model, session);

    assertEquals("silas-administration/edit-role-details", view);
    assertThat(model.getAttribute("errorMessage")).isEqualTo("Role name is required");
    assertThat(model.getAttribute("appRole")).isEqualTo(roleDto);
    assertThat(session.getAttribute("appRoleDetailsForm")).isNull();
    assertThat(session.getAttribute("roleId")).isNull();
}

@Test
void editAppRoleDetailsPost_withMultipleValidationErrors_buildsErrorMessage() {
    MockHttpSession session = new MockHttpSession();
    String roleId = java.util.UUID.randomUUID().toString();
    uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto roleDto = uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto.builder()
        .id(roleId)
        .name("Test Role")
        .description("Test Description")
        .build();

    BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
    when(result.hasErrors()).thenReturn(true);
    when(result.getAllErrors()).thenReturn(List.of(
        new ObjectError("name", "Name is required"),
        new ObjectError("description", "Description is required")
    ));
    when(appRoleService.findById(java.util.UUID.fromString(roleId))).thenReturn(java.util.Optional.of(roleDto));

    uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm form =
        uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.builder()
            .appRoleId(roleId)
            .name("")
            .description("")
            .build();

    String view = adminController.editAppRoleDetailsPost(roleId, form, result, model, session);

    assertEquals("silas-administration/edit-role-details", view);
    String errorMessage = (String) model.getAttribute("errorMessage");
    assertThat(errorMessage).contains("Name is required");
    assertThat(errorMessage).contains("Description is required");
    assertThat(errorMessage).contains("<br/>");
}

@Test
void editAppRoleDetailsPost_withoutErrors_storesInSessionAndRedirects() {
    MockHttpSession session = new MockHttpSession();
    String roleId = java.util.UUID.randomUUID().toString();

    BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
    when(result.hasErrors()).thenReturn(false);

    uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm form =
        uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.builder()
            .appRoleId(roleId)
            .name("Updated Role")
            .description("Updated Description")
            .build();

    String view = adminController.editAppRoleDetailsPost(roleId, form, result, model, session);

    assertEquals(String.format("redirect:/admin/silas-administration/role/%s/check-answers", roleId), view);
    assertThat(session.getAttribute("appRoleDetailsForm")).isEqualTo(form);
    assertThat(session.getAttribute("roleId")).isEqualTo(roleId);
    assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("SiLAS Administration");
}


@Test
void editAppRoleDetailsPost_doesNotStoreFormWhenValidationFails() {
    MockHttpSession session = new MockHttpSession();
    String roleId = java.util.UUID.randomUUID().toString();
    uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto roleDto = uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto.builder()
        .id(roleId)
        .name("Test Role")
        .description("Test Description")
        .build();

    BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
    when(result.hasErrors()).thenReturn(true);
    when(result.getAllErrors()).thenReturn(List.of(new ObjectError("name", "Name required")));
    when(appRoleService.findById(java.util.UUID.fromString(roleId))).thenReturn(java.util.Optional.of(roleDto));

    uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm form =
        uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.builder()
            .appRoleId(roleId)
            .name("")
            .description("Desc")
            .build();

    adminController.editAppRoleDetailsPost(roleId, form, result, model, session);

    assertThat(session.getAttribute("appRoleDetailsForm")).isNull();
    assertThat(session.getAttribute("roleId")).isNull();
}

@Test
void editAppRoleDetailsPost_throwsWhenRoleNotFound() {
    MockHttpSession session = new MockHttpSession();
    String roleId = java.util.UUID.randomUUID().toString();

    BindingResult result = org.mockito.Mockito.mock(BindingResult.class);
    when(result.hasErrors()).thenReturn(true);
    when(result.getAllErrors()).thenReturn(List.of(new ObjectError("name", "Error")));
    when(appRoleService.findById(java.util.UUID.fromString(roleId))).thenReturn(java.util.Optional.empty());

    uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm form =
        uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm.builder()
            .appRoleId(roleId)
            .name("")
            .description("Desc")
            .build();

    org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
        org.springframework.web.server.ResponseStatusException.class, () ->
            adminController.editAppRoleDetailsPost(roleId, form, result, model, session)
    );

    assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
}
}
