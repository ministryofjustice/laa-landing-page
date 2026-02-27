package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.security.core.Authentication;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AdminAppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.AppDetailsForm;
import uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm;
import uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm;
import uk.gov.justice.laa.portal.landingpage.forms.AppsOrderForm;
import uk.gov.justice.laa.portal.landingpage.forms.DeleteAppRoleReasonForm;
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
    @Mock
    private BindingResult bindingResult;

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
        String appId = UUID.randomUUID().toString();
        AppDto appDto = AppDto.builder().id(appId).name("Test App").description("Desc").enabled(true).build();

        when(appService.findById(UUID.fromString(appId))).thenReturn(Optional.of(appDto));

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
        String appId = UUID.randomUUID().toString();
        AppDto appDto = AppDto.builder().id(appId).name("Test App").description("Desc").enabled(false).build();
        AppDetailsForm existing = AppDetailsForm.builder().appId(appId).description("Existing").enabled(true).build();
        session.setAttribute("appDetailsForm", existing);

        when(appService.findById(UUID.fromString(appId))).thenReturn(Optional.of(appDto));

        String view = adminController.editAppDetailsGet(appId, model, session);

        assertEquals("silas-administration/edit-app-details", view);
        assertThat(model.getAttribute("appDetailsForm")).isEqualTo(existing);
        assertThat(session.getAttribute("appDetailsForm")).isEqualTo(existing);
    }

    @Test
    void editAppDetailsPost_withValidationErrors_returnsEditViewWithError() {
        MockHttpSession session = new MockHttpSession();
        String appId = UUID.randomUUID().toString();
        AppDto appDto = AppDto.builder().id(appId).name("Test App").description("Desc").enabled(true).build();

        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(result.getAllErrors()).thenReturn(List.of(new ObjectError("description", "Invalid description")));
        when(appService.findById(UUID.fromString(appId))).thenReturn(Optional.of(appDto));

        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("").enabled(true).build();

        String view = adminController.editAppDetailsPost(appId, form, result, model, session);

        assertEquals("silas-administration/edit-app-details", view);
        assertThat(model.getAttribute("errorMessage")).isNotNull();
        assertThat((List<String>) model.getAttribute("errorMessage"))
                .containsExactly("Invalid description");
        assertThat(model.getAttribute("app")).isEqualTo(appDto);
    }

    @Test
    void editAppDetailsPost_withoutErrors_storesInSessionAndRedirects() {
        MockHttpSession session = new MockHttpSession();
        String appId = UUID.randomUUID().toString();

        BindingResult result = mock(BindingResult.class);
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
        String appId = UUID.randomUUID().toString();
        AppDto appDto = AppDto.builder().id(appId).name("App").description("D").enabled(true).build();
        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("D").enabled(true).build();

        session.setAttribute("appId", appId);
        session.setAttribute("appDetailsForm", form);

        when(appService.findById(UUID.fromString(appId))).thenReturn(Optional.of(appDto));

        String view = adminController.confirmAppDetailsGet(appId, model, session);

        assertEquals("silas-administration/edit-app-details-check-answers", view);
        assertThat(model.getAttribute("app")).isEqualTo(appDto);
        assertThat(model.getAttribute("appDetailsForm")).isEqualTo(form);
    }

    @Test
    void confirmAppDetailsGet_throwsWhenSessionAppIdMismatch() {
        MockHttpSession session = new MockHttpSession();
        String appId = UUID.randomUUID().toString();
        session.setAttribute("appId", "different-id");
        session.setAttribute("appDetailsForm", AppDetailsForm.builder().appId(appId).description("D").enabled(true).build());

        assertThrows(RuntimeException.class, () ->
                adminController.confirmAppDetailsGet(appId, model, session)
        );
    }

    @Test
    void confirmAppDetailsPost_savesAppAndLogsEvent() {
        MockHttpSession session = new MockHttpSession();
        String appId = UUID.randomUUID().toString();

        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("New Desc").enabled(false).build();
        session.setAttribute("appId", appId);
        session.setAttribute("appDetailsForm", form);

        Authentication auth = mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Admin User");

        AppDto appDto = AppDto.builder().id(appId).name("App").description("Old").enabled(true).build();
        App appEntity = App.builder().id(UUID.fromString(appId)).name("App").description("New Desc").enabled(false).build();

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);
        when(loginService.getCurrentProfile(auth)).thenReturn(UserProfile.builder().id(UUID.randomUUID()).build());
        when(appService.findById(UUID.fromString(appId))).thenReturn(Optional.of(appDto));
        when(appService.save(any())).thenReturn(appEntity);

        String view = adminController.confirmAppDetailsPost(appId, auth, model, session);

        assertEquals("silas-administration/edit-app-details-confirmation", view);
        verify(eventService).logEvent(any());
        assertThat(session.getAttribute("appDetailsForm")).isNull();
        assertThat(session.getAttribute("appDetailsFormModel")).isNull();
        assertThat(session.getAttribute("appId")).isNull();
    }

    @Test
    void editAppOrderGet_buildsFormWhenNoSessionValue() {
        MockHttpSession session = new MockHttpSession();
        AppDto app1 = AppDto.builder().id(UUID.randomUUID().toString()).name("A").ordinal(1).build();
        AppDto app2 = AppDto.builder().id(UUID.randomUUID().toString()).name("B").ordinal(2).build();
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
        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(result.getAllErrors()).thenReturn(List.of(new ObjectError("apps", "Duplicate ordinal")));

        String view = adminController.editAppOrderPost(form, result, model, new MockHttpSession());

        assertEquals("silas-administration/edit-apps-order", view);
        assertThat(model.getAttribute("errorMessage")).isNotNull();
        assertThat((List<String>) model.getAttribute("errorMessage"))
                .containsExactly("Duplicate ordinal");
    }

    @Test
    void editAppOrderPost_withoutErrors_sortsAndStoresInSession() {
        AppsOrderForm.AppOrderDetailsForm a = new AppsOrderForm.AppOrderDetailsForm("1", 2);
        AppsOrderForm.AppOrderDetailsForm b = new AppsOrderForm.AppOrderDetailsForm("2", 1);
        AppsOrderForm form = AppsOrderForm.builder().apps(new ArrayList<>(Arrays.asList(a, b))).build();
        BindingResult result = mock(BindingResult.class);
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

        Authentication auth = mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Admin");

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);
        when(loginService.getCurrentProfile(auth)).thenReturn(UserProfile.builder().id(UUID.randomUUID()).build());

        String view = adminController.confirmEditAppOrderPost(auth, model, session);

        assertEquals("silas-administration/edit-apps-order-confirmation", view);
        verify(appService).updateAppsOrder(form.getApps());
        verify(eventService).logEvent(any());
        assertThat(session.getAttribute("appsOrderForm")).isNull();
    }

    @Test
    void confirmEditAppOrderPost_throwsWhenNoSessionValue() {
        MockHttpSession session = new MockHttpSession();
        Authentication auth = mock(Authentication.class);

        assertThrows(RuntimeException.class, () ->
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
        when(adminService.getAllAdminApps()).thenReturn(createMockAdminApps());
        when(appService.getAllLaaApps()).thenReturn(createMockApps());
        when(appRoleService.getAllLaaAppRoles()).thenReturn(createMockRoles());

        adminController.showAdministration("roles", null, model, mockHttpSession);

        @SuppressWarnings("unchecked")
        List<String> appNames = (List<String>) model.getAttribute("appNames");
        assertThat(appNames).containsExactly("Apply for criminal legal aid", "Submit a crime form");
    }

    @Test
    void editAppDetailsGet_throwsWhenAppNotFound() {
        MockHttpSession session = new MockHttpSession();
        String appId = UUID.randomUUID().toString();

        when(appService.findById(UUID.fromString(appId))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                adminController.editAppDetailsGet(appId, model, session)
        );
    }

    @Test
    void editAppDetailsPost_withMultipleValidationErrors_buildsErrorMessage() {
        MockHttpSession session = new MockHttpSession();
        String appId = UUID.randomUUID().toString();
        AppDto appDto = AppDto.builder().id(appId).name("Test App").description("Desc").enabled(true).build();

        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(result.getAllErrors()).thenReturn(List.of(
                new ObjectError("description", "Description is required"),
                new ObjectError("name", "Name is required")
        ));
        when(appService.findById(UUID.fromString(appId))).thenReturn(Optional.of(appDto));

        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("").enabled(true).build();

        String view = adminController.editAppDetailsPost(appId, form, result, model, session);

        assertEquals("silas-administration/edit-app-details", view);
        assertThat(model.getAttribute("errorMessage")).isNotNull();
        assertThat((List<String>) model.getAttribute("errorMessage"))
                .containsExactly("Description is required", "Name is required");
    }

    @Test
    void confirmAppDetailsGet_throwsWhenAppIdNotInSession() {
        MockHttpSession session = new MockHttpSession();
        String appId = UUID.randomUUID().toString();
        session.setAttribute("appDetailsForm", AppDetailsForm.builder().appId(appId).description("D").enabled(true).build());

        assertThrows(RuntimeException.class, () ->
                adminController.confirmAppDetailsGet(appId, model, session)
        );
    }

    @Test
    void confirmAppDetailsGet_throwsWhenFormNotInSession() {
        MockHttpSession session = new MockHttpSession();
        String appId = UUID.randomUUID().toString();
        session.setAttribute("appId", appId);

        assertThrows(RuntimeException.class, () ->
                adminController.confirmAppDetailsGet(appId, model, session)
        );
    }

    @Test
    void confirmAppDetailsPost_throwsWhenAppIdNotInSession() {
        MockHttpSession session = new MockHttpSession();
        String appId = UUID.randomUUID().toString();

        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("New Desc").enabled(false).build();
        session.setAttribute("appDetailsForm", form);

        Authentication auth = mock(Authentication.class);

        assertThrows(RuntimeException.class, () ->
                adminController.confirmAppDetailsPost(appId, auth, model, session)
        );
    }

    @Test
    void confirmAppDetailsPost_throwsWhenAppIdNotMatchingInSession() {
        MockHttpSession session = new MockHttpSession();
        String appId = UUID.randomUUID().toString();
        session.setAttribute("appId", UUID.randomUUID().toString());

        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("New Desc").enabled(false).build();
        session.setAttribute("appDetailsForm", form);

        Authentication auth = mock(Authentication.class);

        assertThrows(RuntimeException.class, () ->
                adminController.confirmAppDetailsPost(appId, auth, model, session)
        );
    }

    @Test
    void confirmAppDetailsPost_throwsWhenFormNotInSession() {
        MockHttpSession session = new MockHttpSession();
        String appId = UUID.randomUUID().toString();
        session.setAttribute("appId", appId);

        Authentication auth = mock(Authentication.class);

        assertThrows(RuntimeException.class, () ->
                adminController.confirmAppDetailsPost(appId, auth, model, session)
        );
    }

    @Test
    void confirmAppDetailsPost_throwsWhenAppNotFound() {
        MockHttpSession session = new MockHttpSession();
        String appId = UUID.randomUUID().toString();

        AppDetailsForm form = AppDetailsForm.builder().appId(appId).description("New Desc").enabled(false).build();
        session.setAttribute("appId", appId);
        session.setAttribute("appDetailsForm", form);

        Authentication auth = mock(Authentication.class);

        when(appService.findById(UUID.fromString(appId))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                adminController.confirmAppDetailsPost(appId, auth, model, session)
        );
    }

    @Test
    void editAppOrderGet_returnsEditAppsOrderView() {
        MockHttpSession session = new MockHttpSession();
        AppDto app1 = AppDto.builder().id(UUID.randomUUID().toString()).name("A").ordinal(1).build();
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

        AppRolesOrderForm.AppRolesOrderDetailsForm a =
                AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                        .appRoleId("1")
                        .name("Role 1")
                        .ordinal(1)
                        .build();
        AppRolesOrderForm form =
                AppRolesOrderForm.builder()
                        .appRoles(List.of(a))
                        .build();
        session.setAttribute("appRolesOrderForm", form);
        session.setAttribute("appFilter", appName);
        session.setAttribute("roleId", "some-role-id");

        Authentication auth = mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);
        when(loginService.getCurrentProfile(auth)).thenReturn(UserProfile.builder().id(UUID.randomUUID()).build());

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
        RedirectAttributes redirectAttributes =
                mock(RedirectAttributes.class);

        String view = adminController.editAppRolesOrderGet(model, redirectAttributes, session);

        assertEquals("redirect:/admin/silas-administration#roles", view);
        verify(redirectAttributes).addFlashAttribute("appRolesErrorMessage", "Please select an application to reorder its roles");
    }

    @Test
    void confirmAppRoleDetailsPost_throwsWhenRoleIdNotMatchingInSession() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();
        session.setAttribute("roleId", UUID.randomUUID().toString());

        AppRoleDetailsForm form = AppRoleDetailsForm.builder().appRoleId(roleId).name("New Name").description("New Desc").build();
        session.setAttribute("appRoleDetailsForm", form);

        Authentication auth = mock(Authentication.class);

        assertThrows(RuntimeException.class, () ->
                adminController.confirmAppRoleDetailsPost(roleId, auth, model, session)
        );
    }

    @Test
    void confirmAppRoleDetailsPost_savesRoleAndLogsEvent() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();

        AppRoleDetailsForm form =
                AppRoleDetailsForm.builder()
                        .appRoleId(roleId)
                        .name("New Name")
                        .description("New Desc")
                        .build();
        session.setAttribute("roleId", roleId);
        session.setAttribute("appRoleDetailsForm", form);

        Authentication auth = mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Admin");

        AppRoleDto roleDto = AppRoleDto.builder()
                .id(roleId)
                .name("Old Name")
                .description("Old Desc")
                .build();
        AppRole roleEntity = AppRole.builder()
                .id(UUID.fromString(roleId))
                .name("New Name")
                .description("New Desc")
                .build();

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);
        when(loginService.getCurrentProfile(auth)).thenReturn(UserProfile.builder().id(UUID.randomUUID()).build());
        when(appRoleService.findById(UUID.fromString(roleId))).thenReturn(Optional.of(roleDto));
        when(appRoleService.save(any())).thenReturn(roleEntity);

        String view = adminController.confirmAppRoleDetailsPost(roleId, auth, model, session);

        assertEquals("silas-administration/edit-role-details-confirmation", view);
        verify(eventService).logEvent(any());
        assertThat(session.getAttribute("appRoleDetailsForm")).isNull();
        assertThat(session.getAttribute("appRoleDetailsFormModel")).isNull();
        assertThat(session.getAttribute("roleId")).isNull();
    }

    @Test
    void editAppRolesOrderGet_withoutAppSelection_redirectsWithFlashAttribute() {
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes =
                mock(RedirectAttributes.class);

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

        String view = adminController.editAppRolesOrderGet(model, mock(RedirectAttributes.class), session);

        assertEquals("silas-administration/edit-app-roles-order", view);
        Object appRolesOrderForm = model.getAttribute("appRolesOrderForm");
        assertThat(appRolesOrderForm).isInstanceOf(AppRolesOrderForm.class);
    }

    @Test
    void editAppRolesOrderPost_withoutErrors_sortsAndStoresInSession() {
        MockHttpSession session = new MockHttpSession();
        String appName = "Test App";
        session.setAttribute("appFilter", appName);

        AppRolesOrderForm.AppRolesOrderDetailsForm a =
                AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                        .appRoleId("1")
                        .name("Role 1")
                        .ordinal(2)
                        .build();
        AppRolesOrderForm.AppRolesOrderDetailsForm b =
                AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                        .appRoleId("2")
                        .name("Role 2")
                        .ordinal(1)
                        .build();
        AppRolesOrderForm form =
                AppRolesOrderForm.builder()
                        .appRoles(new ArrayList<>(Arrays.asList(a, b)))
                        .build();
        BindingResult result = mock(BindingResult.class);
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

        AppRolesOrderForm.AppRolesOrderDetailsForm a =
                AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                        .appRoleId("1")
                        .name("Role 1")
                        .ordinal(2)
                        .build();
        AppRolesOrderForm.AppRolesOrderDetailsForm b =
                AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                        .appRoleId("2")
                        .name("Role 2")
                        .ordinal(1)
                        .build();
        AppRolesOrderForm form =
                AppRolesOrderForm.builder()
                        .appRoles(new ArrayList<>(Arrays.asList(a, b)))
                        .build();
        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(result.getAllErrors()).thenReturn(List.of(new ObjectError("appRoles", "Duplicate ordinal")));

        String view = adminController.editAppRolesOrderPost(form, result, model, session);

        assertEquals("silas-administration/edit-app-roles-order", view);
        assertThat(model.getAttribute("errorMessage")).isNotNull();
        assertThat((List<String>) model.getAttribute("errorMessage"))
                .containsExactly("Duplicate ordinal");
    }

    @Test
    void confirmEditAppRolesOrderPost_updatesOrderAndClearsSession() {
        MockHttpSession session = new MockHttpSession();
        String appName = "Test App";

        AppRolesOrderForm.AppRolesOrderDetailsForm a =
                AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                        .appRoleId("1")
                        .name("Role 1")
                        .ordinal(1)
                        .build();
        AppRolesOrderForm form =
                AppRolesOrderForm.builder()
                        .appRoles(List.of(a))
                        .build();
        session.setAttribute("appRolesOrderForm", form);
        session.setAttribute("appFilter", appName);

        Authentication auth = mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Admin");

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);
        when(loginService.getCurrentProfile(auth)).thenReturn(UserProfile.builder().id(UUID.randomUUID()).build());

        String view = adminController.confirmEditAppRolesOrderPost(auth, model, session);

        assertEquals("silas-administration/edit-app-roles-order-confirmation", view);
        verify(appRoleService).updateAppRolesOrder(form.getAppRoles());
        verify(eventService).logEvent(any());
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
        session.setAttribute("appRoleDetailsForm", new AppRoleDetailsForm());
        session.setAttribute("appsOrderForm", new AppsOrderForm());
        session.setAttribute("appRolesOrderForm", new AppRolesOrderForm());
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

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () ->
                        adminController.editAppDetailsGet(invalidAppId, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void editAppRoleDetailsGet_withInvalidUuid_throwsResponseStatusException() {
        MockHttpSession session = new MockHttpSession();
        String invalidRoleId = "invalid-uuid";

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () ->
                        adminController.editAppRoleDetailsGet(invalidRoleId, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void editAppRolesOrderPost_throwsWhenAppNotSelected() {
        MockHttpSession session = new MockHttpSession();

        AppRolesOrderForm form =
                AppRolesOrderForm.builder()
                        .appRoles(List.of(AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                                .appRoleId("1")
                                .name("Role 1")
                                .ordinal(1)
                                .build()))
                        .build();
        BindingResult result = mock(BindingResult.class);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () ->
                        adminController.editAppRolesOrderPost(form, result, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void confirmEditAppRolesOrderPost_throwsWhenFormNotInSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("appFilter", "Test App");

        Authentication auth = mock(Authentication.class);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () ->
                        adminController.confirmEditAppRolesOrderPost(auth, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void confirmEditAppRolesOrderPost_throwsWhenAppFilterNotInSession() {
        MockHttpSession session = new MockHttpSession();
        AppRolesOrderForm.AppRolesOrderDetailsForm a =
                AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                        .appRoleId("1")
                        .name("Role 1")
                        .ordinal(1)
                        .build();
        AppRolesOrderForm form =
                AppRolesOrderForm.builder()
                        .appRoles(List.of(a))
                        .build();
        session.setAttribute("appRolesOrderForm", form);

        Authentication auth = mock(Authentication.class);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () ->
                        adminController.confirmEditAppRolesOrderPost(auth, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
                        .legacySync("No")
                        .build(),
                AppRoleAdminDto.builder()
                        .name("CCMS case transfer requests - Internal")
                        .description("CCMS case transfer requests - Internal User Role")
                        .parentApp("CCMS case transfer requests")
                        .ccmsCode("ccms.transfer.internal")
                        .ordinal(1)
                        .legacySync("No")
                        .build()
        );
    }

    @Test
    void confirmAppDetailsPost_updatesAppWithFormValues() {
        MockHttpSession session = new MockHttpSession();
        String appId = UUID.randomUUID().toString();

        AppDetailsForm form = AppDetailsForm.builder()
                .appId(appId)
                .description("Updated Description")
                .enabled(false)
                .build();
        session.setAttribute("appId", appId);
        session.setAttribute("appDetailsForm", form);

        Authentication auth = mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Admin User");

        AppDto appDto = AppDto.builder()
                .id(appId)
                .name("App")
                .description("Old Description")
                .enabled(true)
                .build();
        App appEntity = App.builder()
                .id(UUID.fromString(appId))
                .name("App")
                .description("Updated Description")
                .enabled(false)
                .build();

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);
        when(loginService.getCurrentProfile(auth)).thenReturn(UserProfile.builder().id(UUID.randomUUID()).build());
        when(appService.findById(UUID.fromString(appId))).thenReturn(Optional.of(appDto));
        when(appService.save(any())).thenReturn(appEntity);

        adminController.confirmAppDetailsPost(appId, auth, model, session);

        assertThat(appDto.isEnabled()).isFalse();
        assertThat(appDto.getDescription()).isEqualTo("Updated Description");
        verify(appService).save(appDto);
    }

    @Test
    void confirmAppRoleDetailsPost_updatesRoleWithFormValues() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();

        AppRoleDetailsForm form =
                AppRoleDetailsForm.builder()
                        .appRoleId(roleId)
                        .name("Updated Role Name")
                        .description("Updated Role Description")
                        .build();
        session.setAttribute("roleId", roleId);
        session.setAttribute("appRoleDetailsForm", form);

        Authentication auth = mock(Authentication.class);
        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Admin");

        AppRoleDto roleDto = AppRoleDto.builder()
                .id(roleId)
                .name("Old Role Name")
                .description("Old Role Description")
                .build();
        AppRole roleEntity = AppRole.builder()
                .id(UUID.fromString(roleId))
                .name("Updated Role Name")
                .description("Updated Role Description")
                .build();

        when(loginService.getCurrentUser(auth)).thenReturn(currentUser);
        when(loginService.getCurrentProfile(auth)).thenReturn(UserProfile.builder().id(UUID.randomUUID()).build());
        when(appRoleService.findById(UUID.fromString(roleId))).thenReturn(Optional.of(roleDto));
        when(appRoleService.save(any())).thenReturn(roleEntity);

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

        AppRolesOrderForm.AppRolesOrderDetailsForm a =
                AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
                        .appRoleId("1")
                        .name("Role 1")
                        .ordinal(2)
                        .build();
        AppRolesOrderForm form =
                AppRolesOrderForm.builder()
                        .appRoles(Arrays.asList(a))
                        .build();
        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        adminController.editAppRolesOrderPost(form, result, model, session);

        AppRolesOrderForm storedForm =
                (AppRolesOrderForm) session.getAttribute("appRolesOrderForm");
        assertThat(storedForm).isNotNull();
        assertThat(storedForm.getAppRoles()).hasSize(1);
        assertThat(storedForm.getAppRoles().getFirst().getOrdinal()).isEqualTo(2);
    }

    @Test
    void editAppRoleDetailsGet_loadsRoleDtoAndCreatesForm() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();
        AppRoleDto roleDto = AppRoleDto.builder()
                .id(roleId)
                .name("Test Role")
                .description("Test Description")
                .build();

        when(appRoleService.findById(UUID.fromString(roleId))).thenReturn(Optional.of(roleDto));

        String view = adminController.editAppRoleDetailsGet(roleId, model, session);

        assertEquals("silas-administration/edit-role-details", view);
        assertThat(model.getAttribute("appRole")).isEqualTo(roleDto);
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("SiLAS Administration");
        Object form = model.getAttribute("appRoleDetailsForm");
        assertThat(form).isInstanceOf(AppRoleDetailsForm.class);
        AppRoleDetailsForm detailsForm =
                (AppRoleDetailsForm) form;
        assertThat(detailsForm.getAppRoleId()).isEqualTo(roleId);
        assertThat(detailsForm.getName()).isEqualTo("Test Role");
        assertThat(detailsForm.getDescription()).isEqualTo("Test Description");
        assertThat(session.getAttribute("appRoleDetailsForm")).isNotNull();
    }

    @Test
    void editAppRoleDetailsGet_usesExistingFormFromSession() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();
        AppRoleDto roleDto = AppRoleDto.builder()
                .id(roleId)
                .name("Original Role Name")
                .description("Original Description")
                .build();
        AppRoleDetailsForm existingForm =
                AppRoleDetailsForm.builder()
                        .appRoleId(roleId)
                        .name("Modified Role Name")
                        .description("Modified Description")
                        .build();
        session.setAttribute("appRoleDetailsForm", existingForm);

        when(appRoleService.findById(UUID.fromString(roleId))).thenReturn(Optional.of(roleDto));

        String view = adminController.editAppRoleDetailsGet(roleId, model, session);

        assertEquals("silas-administration/edit-role-details", view);
        assertThat(model.getAttribute("appRoleDetailsForm")).isEqualTo(existingForm);
        AppRoleDetailsForm retrievedForm =
                (AppRoleDetailsForm) model.getAttribute("appRoleDetailsForm");
        assertThat(retrievedForm.getName()).isEqualTo("Modified Role Name");
        assertThat(retrievedForm.getDescription()).isEqualTo("Modified Description");
    }

    @Test
    void editAppRoleDetailsGet_throwsWhenRoleNotFound() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();

        when(appRoleService.findById(UUID.fromString(roleId))).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () ->
                        adminController.editAppRoleDetailsGet(roleId, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).contains("App role details not found");
    }

    @Test
    void editAppRoleDetailsGet_withInvalidRoleId_throwsBadRequest() {
        MockHttpSession session = new MockHttpSession();
        String invalidRoleId = "not-a-valid-uuid";

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () ->
                        adminController.editAppRoleDetailsGet(invalidRoleId, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void confirmAppRoleDetailsGet_returnsCheckAnswersPageWithRoleAndForm() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();
        AppRoleDto roleDto = AppRoleDto.builder()
                .id(roleId)
                .name("Admin Role")
                .description("Administrator Role")
                .build();
        AppRoleDetailsForm form =
                AppRoleDetailsForm.builder()
                        .appRoleId(roleId)
                        .name("Admin Role")
                        .description("Administrator Role")
                        .build();

        session.setAttribute("roleId", roleId);
        session.setAttribute("appRoleDetailsForm", form);

        when(appRoleService.findById(UUID.fromString(roleId))).thenReturn(Optional.of(roleDto));

        String view = adminController.confirmAppRoleDetailsGet(roleId, model, session);

        assertEquals("silas-administration/edit-role-details-check-answers", view);
        assertThat(model.getAttribute("appRole")).isEqualTo(roleDto);
        assertThat(model.getAttribute("appRoleDetailsForm")).isEqualTo(form);
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("SiLAS Administration");
    }

    @Test
    void confirmAppRoleDetailsGet_throwsWhenRoleIdNotInSession() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();
        session.setAttribute("appRoleDetailsForm", AppRoleDetailsForm.builder()
                .appRoleId(roleId).name("R").description("D").build());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () ->
                        adminController.confirmAppRoleDetailsGet(roleId, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).contains("Role ID not found in session");
    }

    @Test
    void confirmAppRoleDetailsGet_throwsWhenFormNotInSession() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();
        session.setAttribute("roleId", roleId);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () ->
                        adminController.confirmAppRoleDetailsGet(roleId, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).contains("App role details not found in session");
    }

    @Test
    void confirmAppRoleDetailsGet_throwsWhenRoleIdMismatch() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();
        String differentRoleId = UUID.randomUUID().toString();
        session.setAttribute("roleId", differentRoleId);
        session.setAttribute("appRoleDetailsForm", AppRoleDetailsForm.builder()
                .appRoleId(roleId).name("R").description("D").build());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () ->
                        adminController.confirmAppRoleDetailsGet(roleId, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("Invalid request for app details change");
    }

    @Test
    void confirmAppRoleDetailsGet_throwsWhenRoleNotFound() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();
        session.setAttribute("roleId", roleId);
        session.setAttribute("appRoleDetailsForm", AppRoleDetailsForm.builder()
                .appRoleId(roleId).name("R").description("D").build());

        when(appRoleService.findById(UUID.fromString(roleId))).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () ->
                        adminController.confirmAppRoleDetailsGet(roleId, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).contains("App role details not found");
    }

    @Test
    void confirmAppRoleDetailsGet_withInvalidRoleId_throwsBadRequest() {
        MockHttpSession session = new MockHttpSession();
        String invalidRoleId = "invalid-uuid";
        session.setAttribute("roleId", invalidRoleId);
        session.setAttribute("appRoleDetailsForm", AppRoleDetailsForm.builder()
                .appRoleId(invalidRoleId).name("R").description("D").build());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () ->
                        adminController.confirmAppRoleDetailsGet(invalidRoleId, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void editAppRoleDetailsPost_withValidationErrors_returnsEditViewWithError() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();
        AppRoleDto roleDto = AppRoleDto.builder()
                .id(roleId)
                .name("Test Role")
                .description("Test Description")
                .build();

        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(result.getAllErrors()).thenReturn(List.of(new ObjectError("name", "Role name is required")));
        when(appRoleService.findById(UUID.fromString(roleId))).thenReturn(Optional.of(roleDto));

        AppRoleDetailsForm form =
                AppRoleDetailsForm.builder()
                        .appRoleId(roleId)
                        .name("")
                        .description("Desc")
                        .build();

        String view = adminController.editAppRoleDetailsPost(roleId, form, result, model, session);

        assertEquals("silas-administration/edit-role-details", view);
        assertThat(model.getAttribute("errorMessage")).isNotNull();
        assertThat((List<String>) model.getAttribute("errorMessage"))
                .containsExactly("Role name is required");
        assertThat(model.getAttribute("appRole")).isEqualTo(roleDto);
        assertThat(session.getAttribute("appRoleDetailsForm")).isNull();
        assertThat(session.getAttribute("roleId")).isNull();
    }

    @Test
    void editAppRoleDetailsPost_withMultipleValidationErrors_buildsErrorMessage() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();
        AppRoleDto roleDto = AppRoleDto.builder()
                .id(roleId)
                .name("Test Role")
                .description("Test Description")
                .build();

        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(result.getAllErrors()).thenReturn(List.of(
                new ObjectError("name", "Name is required"),
                new ObjectError("description", "Description is required")
        ));
        when(appRoleService.findById(UUID.fromString(roleId))).thenReturn(Optional.of(roleDto));

        AppRoleDetailsForm form =
                AppRoleDetailsForm.builder()
                        .appRoleId(roleId)
                        .name("")
                        .description("")
                        .build();

        String view = adminController.editAppRoleDetailsPost(roleId, form, result, model, session);

        assertEquals("silas-administration/edit-role-details", view);
        assertThat(model.getAttribute("errorMessage")).isNotNull();
        assertThat((List<String>) model.getAttribute("errorMessage"))
                .containsExactly("Name is required", "Description is required");
    }

    @Test
    void editAppRoleDetailsPost_withoutErrors_storesInSessionAndRedirects() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();

        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        AppRoleDetailsForm form =
                AppRoleDetailsForm.builder()
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
        String roleId = UUID.randomUUID().toString();
        AppRoleDto roleDto = AppRoleDto.builder()
                .id(roleId)
                .name("Test Role")
                .description("Test Description")
                .build();

        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(result.getAllErrors()).thenReturn(List.of(new ObjectError("name", "Name required")));
        when(appRoleService.findById(UUID.fromString(roleId))).thenReturn(Optional.of(roleDto));

        AppRoleDetailsForm form =
                AppRoleDetailsForm.builder()
                        .appRoleId(roleId)
                        .name("")
                        .description("Desc")
                        .build();

        MockHttpSession session = new MockHttpSession();
        adminController.editAppRoleDetailsPost(roleId, form, result, model, session);

        assertThat(session.getAttribute("appRoleDetailsForm")).isNull();
        assertThat(session.getAttribute("roleId")).isNull();
    }

    @Test
    void editAppRoleDetailsPost_throwsWhenRoleNotFound() {
        MockHttpSession session = new MockHttpSession();
        String roleId = UUID.randomUUID().toString();

        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        when(appRoleService.findById(UUID.fromString(roleId))).thenReturn(Optional.empty());

        AppRoleDetailsForm form =
                AppRoleDetailsForm.builder()
                        .appRoleId(roleId)
                        .name("")
                        .description("Desc")
                        .build();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () ->
                        adminController.editAppRoleDetailsPost(roleId, form, result, model, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Nested
    class DeleteAppRoleTests {

        @Mock
        private Authentication authentication;

        @Test
        @DisplayName("GET /silas-administration/delete-role: when no app selected -> redirects with flash error")
        void deleteAppRoleGet_noAppInSession_redirectsWithError() {
            // Arrange
            Model model = new ExtendedModelMap();
            RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn(null); // or Optional.empty() logic in helper

            // Act
            String view = adminController.deleteAppRoleGet(model, redirectAttributes, mockHttpSession);

            // Assert
            assertThat(view).isEqualTo("redirect:/admin/silas-administration#roles");
            assertThat(redirectAttributes.getFlashAttributes().get("appRolesErrorMessage"))
                    .isEqualTo("Please select an application to delete its roles");

            // No calls to service when app is missing
            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("GET /silas-administration/delete-role: when app selected -> populates model, session, returns view")
        void deleteAppRoleGet_appPresent_populatesModelAndReturnsView() {
            // Arrange
            Model model = new ExtendedModelMap();
            RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
            String appName = "MyApp";

            when(mockHttpSession.getAttribute("appFilter")).thenReturn(appName);
            List<AppRoleAdminDto> roles = List.of(AppRoleAdminDto.builder().id(UUID.randomUUID().toString()).name("ADMIN").build());
            when(appRoleService.getLaaAppRolesByAppName(appName)).thenReturn(roles);

            // Act
            String view = adminController.deleteAppRoleGet(model, redirectAttributes, mockHttpSession);

            // Assert
            assertThat(view).isEqualTo("silas-administration/delete-app-roles");
            assertThat(model.getAttribute("appName")).isEqualTo(appName);
            assertThat(model.getAttribute("roles")).isEqualTo(roles);

            // Page title present
            assertThat(model.containsAttribute(ModelAttributes.PAGE_TITLE)).isTrue();
            assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE))
                    .isEqualTo(AdminController.SILAS_ADMINISTRATION_TITLE);

            // Session is updated (re-sets the appFilter)
            verify(mockHttpSession).setAttribute("appFilter", appName);

            // Service invoked with the app name
            verify(appRoleService).getLaaAppRolesByAppName(appName);
            verifyNoMoreInteractions(appRoleService);
        }

        @Test
        @DisplayName("GET /silas-administration/delete-role: empty/blank app name -> redirects with flash error")
        void deleteAppRoleGet_blankAppName_redirectsWithError() {
            // Arrange
            Model model = new ExtendedModelMap();
            RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("  "); // blank but non-null

            // Act
            String view = adminController.deleteAppRoleGet(model, redirectAttributes, mockHttpSession);

            // Assert
            assertThat(view).isEqualTo("redirect:/admin/silas-administration#roles");
            assertThat(redirectAttributes.getFlashAttributes().get("appRolesErrorMessage"))
                    .isEqualTo("Please select an application to delete its roles");
            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("POST /silas-administration/delete-role/{roleId}: when session app missing -> 404 ResponseStatusException")
        void deleteAppRolePost_noAppInSession_throws404() {
            // Arrange
            Model model = new ExtendedModelMap();
            String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn(null);

            // Act + Assert
            assertThatThrownBy(() ->
                    adminController.deleteAppRolePost(roleId, "Admin", "SomeApp", model, mockHttpSession)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("App not selected for role ordering");
                    });

            // No further interactions
            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("POST /silas-administration/delete-role/{roleId}: app name mismatch -> error page and errorMessage")
        void deleteAppRolePost_appNameMismatch_returnsGenericError() {
            // Arrange
            Model model = new ExtendedModelMap();
            String roleId = UUID.randomUUID().toString();
            String sessionApp = "SessionApp";
            String incomingApp = "IncomingApp"; // mismatch

            when(mockHttpSession.getAttribute("appFilter")).thenReturn(sessionApp);

            // Act
            String view = adminController.deleteAppRolePost(roleId, "Admin", incomingApp, model, mockHttpSession);

            // Assert
            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("appName")).isEqualTo(sessionApp);
            assertThat(model.getAttribute("errorMessage")).isEqualTo("Error while processing app role management");

            // No service calls in this branch
            verifyNoInteractions(appRoleService);

            // No session changes for roleId/roleName on mismatch
            verify(mockHttpSession, never()).setAttribute(eq("roleIdForDeletion"), any());
            verify(mockHttpSession, never()).setAttribute(eq("roleNameForDeletion"), any());
        }

        @Test
        @DisplayName("POST /silas-administration/delete-role/{roleId}: app name matches -> sets session attributes and redirects to reason page")
        void deleteAppRolePost_happyPath_setsSessionAndRedirects() {
            // Arrange
            Model model = new ExtendedModelMap();
            String roleId = UUID.randomUUID().toString();
            String roleName = "Admin";
            String appName = "MyApp";

            when(mockHttpSession.getAttribute("appFilter")).thenReturn(appName);

            // Act
            String view = adminController.deleteAppRolePost(roleId, roleName, appName, model, mockHttpSession);

            // Assert
            // Model contains appName from session and page title
            assertThat(model.getAttribute("appName")).isEqualTo(appName);
            assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE))
                    .isEqualTo(AdminController.SILAS_ADMINISTRATION_TITLE);

            // Session stores role info for the next step
            verify(mockHttpSession).setAttribute("roleIdForDeletion", roleId);
            verify(mockHttpSession).setAttribute("roleNameForDeletion", roleName);

            // Correct redirect URL
            assertThat(view).isEqualTo("redirect:/admin/silas-administration/delete-role/" + roleId + "/reason");

            verifyNoInteractions(appRoleService);
        }

        private AppRoleDto mockRoleDto(String id, String name, String appName) {
            AppDto appDto = AppDto.builder().name(appName).build();
            return AppRoleDto.builder().id(id).name(name).app(appDto).build();
        }

        @Test
        @DisplayName("GET reason page: missing app name in session -> 404 ResponseStatusException")
        void showReason_missingAppName_throws404() {
            // Arrange
            Model model = new ExtendedModelMap();
            String pathRoleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn(null);

            // Act + Assert
            assertThatThrownBy(() ->
                    adminController.showDeleteAppRoleReasonPage(pathRoleId, mockHttpSession, model)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("App name not found in session");
                    });

            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("GET reason page: missing roleIdForDeletion in session -> 404")
        void showReason_missingRoleId_throws404() {
            Model model = new ExtendedModelMap();
            String pathRoleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(null);

            assertThatThrownBy(() ->
                    adminController.showDeleteAppRoleReasonPage(pathRoleId, mockHttpSession, model)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("Role ID not found in session");
                    });

            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("GET reason page: missing roleNameForDeletion in session -> 404")
        void showReason_missingRoleName_throws404() {
            Model model = new ExtendedModelMap();
            String pathRoleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(pathRoleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn(null);

            assertThatThrownBy(() ->
                    adminController.showDeleteAppRoleReasonPage(pathRoleId, mockHttpSession, model)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("Role name not found in session");
                    });

            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("GET reason page: roleId mismatch between path and session -> returns generic error view")
        void showReason_roleIdMismatch_returnsGenericError() {
            Model model = new ExtendedModelMap();
            String pathRoleId = UUID.randomUUID().toString();
            String sessionRoleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(sessionRoleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");

            String view = adminController.showDeleteAppRoleReasonPage(pathRoleId, mockHttpSession, model);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("Error while processing app role management");

            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("GET reason page: appRole not found -> caught and returns generic error view")
        void showReason_roleNotFound_returnsGenericError() {
            Model model = new ExtendedModelMap();
            String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");

            when(appRoleService.findById(roleId)).thenReturn(Optional.empty()); // will throw in try { ... }

            String view = adminController.showDeleteAppRoleReasonPage(roleId, mockHttpSession, model);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("An error occurred while loading the page");

            verify(appRoleService).findById(roleId);
        }

        @Test
        @DisplayName("GET reason page: role name mismatch -> returns specific error view")
        void showReason_roleNameMismatch_returnsError() {
            Model model = new ExtendedModelMap();
            String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Manager"); // session says Manager

            AppRoleDto dto = mockRoleDto(roleId, "Admin", "MyApp"); // actual role name Admin
            when(appRoleService.findById(roleId)).thenReturn(Optional.of(dto));

            String view = adminController.showDeleteAppRoleReasonPage(roleId, mockHttpSession, model);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("Role name does not match the expected value for the selected role");
        }

        @Test
        @DisplayName("GET reason page: app name mismatch -> returns specific error view")
        void showReason_appNameMismatch_returnsError() {
            Model model = new ExtendedModelMap();
            String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("SessionApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");

            AppRoleDto dto = mockRoleDto(roleId, "Admin", "DbApp"); // Db app != session app
            when(appRoleService.findById(roleId)).thenReturn(Optional.of(dto));

            String view = adminController.showDeleteAppRoleReasonPage(roleId, mockHttpSession, model);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("App name does not match the expected value for the selected role");
        }

        @Test
        @DisplayName("GET reason page: happy path (no pre-existing form) -> populates model and returns reason view")
        void showReason_happyPath_withoutExistingForm() {
            Model model = new ExtendedModelMap();
            String roleId = UUID.randomUUID().toString();
            String appName = "MyApp";
            String roleName = "Admin";

            when(mockHttpSession.getAttribute("appFilter")).thenReturn(appName);
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn(roleName);
            when(mockHttpSession.getAttribute("deleteAppRoleReasonForm")).thenReturn(null);

            AppRoleDto dto = mockRoleDto(roleId, roleName, appName);
            when(appRoleService.findById(roleId)).thenReturn(Optional.of(dto));

            String view = adminController.showDeleteAppRoleReasonPage(roleId, mockHttpSession, model);

            assertThat(view).isEqualTo("silas-administration/delete-app-role-reason");
            assertThat(model.getAttribute("roleName")).isEqualTo(roleName);
            assertThat(model.getAttribute("appName")).isEqualTo(appName);
            assertThat(model.getAttribute("roleId")).isEqualTo(roleId);
            assertThat(model.getAttribute("deleteAppRoleReasonForm")).isNotNull();

            // Optional: assert page title if constant available
            assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE))
                    .isEqualTo(AdminController.SILAS_ADMINISTRATION_TITLE);
        }

        @Test
        @DisplayName("GET reason page: happy path uses existing form from session")
        void showReason_happyPath_withExistingForm() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();
            final String appName = "MyApp";
            final String roleName = "Admin";

            when(mockHttpSession.getAttribute("appFilter")).thenReturn(appName);
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn(roleName);

            DeleteAppRoleReasonForm existingForm = new DeleteAppRoleReasonForm();
            existingForm.setAppRoleId(roleId);
            existingForm.setAppName(appName);
            existingForm.setReason("Cleanup");
            when(mockHttpSession.getAttribute("deleteAppRoleReasonForm")).thenReturn(existingForm);

            AppRoleDto dto = mockRoleDto(roleId, roleName, appName);
            when(appRoleService.findById(roleId)).thenReturn(Optional.of(dto));

            String view = adminController.showDeleteAppRoleReasonPage(roleId, mockHttpSession, model);

            assertThat(view).isEqualTo("silas-administration/delete-app-role-reason");
            assertThat(model.getAttribute("deleteAppRoleReasonForm")).isSameAs(existingForm);
        }

        @Test
        @DisplayName("POST reason: field error on 'reason' -> redisplay reason view")
        void postReason_fieldErrorRedisplaysPage() {
            Model model = new ExtendedModelMap();
            String roleId = UUID.randomUUID().toString();
            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();

            when(bindingResult.hasFieldErrors("reason")).thenReturn(true);

            String view = adminController.processDeleteAppRoleReasonSubmission(
                    roleId, form, bindingResult, mockHttpSession, model);

            assertThat(view).isEqualTo("silas-administration/delete-app-role-reason");
            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("POST reason: missing app name in session -> 404")
        void postReason_missingAppName_throws404() {
            Model model = new ExtendedModelMap();
            String roleId = UUID.randomUUID().toString();
            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();

            when(bindingResult.hasFieldErrors("reason")).thenReturn(false);
            when(mockHttpSession.getAttribute("appFilter")).thenReturn(null);

            assertThatThrownBy(() ->
                    adminController.processDeleteAppRoleReasonSubmission(roleId, form, bindingResult, mockHttpSession, model)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("App name not found in session");
                    });

            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("POST reason: missing roleIdForDeletion -> 404")
        void postReason_missingRoleId_throws404() {
            Model model = new ExtendedModelMap();
            String roleId = UUID.randomUUID().toString();
            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();

            when(bindingResult.hasFieldErrors("reason")).thenReturn(false);
            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(null);

            assertThatThrownBy(() ->
                    adminController.processDeleteAppRoleReasonSubmission(roleId, form, bindingResult, mockHttpSession, model)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("Role ID not found in session");
                    });

            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("POST reason: missing roleNameForDeletion -> 404")
        void postReason_missingRoleName_throws404() {
            Model model = new ExtendedModelMap();
            String roleId = UUID.randomUUID().toString();
            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();

            when(bindingResult.hasFieldErrors("reason")).thenReturn(false);
            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn(null);

            assertThatThrownBy(() ->
                    adminController.processDeleteAppRoleReasonSubmission(roleId, form, bindingResult, mockHttpSession, model)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("Role name not found in session");
                    });

            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("POST reason: role not found -> RuntimeException (propagates)")
        void postReason_roleNotFound_runtimeException() {
            final Model model = new ExtendedModelMap();
            final String pathRoleId = UUID.randomUUID().toString();
            final String sessionRoleId = pathRoleId;
            final DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();
            form.setAppRoleId(sessionRoleId);

            when(bindingResult.hasFieldErrors("reason")).thenReturn(false);
            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(sessionRoleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");
            when(appRoleService.findById(sessionRoleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    adminController.processDeleteAppRoleReasonSubmission(pathRoleId, form, bindingResult, mockHttpSession, model)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessage("App Role not found");
        }

        @Test
        @DisplayName("POST reason: path roleId mismatch with session -> generic error view")
        void postReason_roleIdMismatch_returnsGenericError() {
            final Model model = new ExtendedModelMap();
            final String pathRoleId = UUID.randomUUID().toString();
            final String sessionRoleId = UUID.randomUUID().toString();

            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();
            form.setAppRoleId(sessionRoleId);

            when(bindingResult.hasFieldErrors("reason")).thenReturn(false);
            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(sessionRoleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");

            AppRoleDto dto = mockRoleDto(sessionRoleId, "Admin", "MyApp");
            when(appRoleService.findById(sessionRoleId)).thenReturn(Optional.of(dto));

            String view = adminController.processDeleteAppRoleReasonSubmission(pathRoleId, form, bindingResult, mockHttpSession, model);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("Error while processing app role management");
        }

        @Test
        @DisplayName("POST reason: role name mismatch -> specific error view")
        void postReason_roleNameMismatch_returnsError() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();

            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();
            form.setAppRoleId(roleId);

            when(bindingResult.hasFieldErrors("reason")).thenReturn(false);
            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Manager"); // mismatch

            AppRoleDto dto = mockRoleDto(roleId, "Admin", "MyApp");
            when(appRoleService.findById(roleId)).thenReturn(Optional.of(dto));

            String view = adminController.processDeleteAppRoleReasonSubmission(roleId, form, bindingResult, mockHttpSession, model);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("Role name does not match the expected value for the selected role");
        }

        @Test
        @DisplayName("POST reason: app name mismatch -> specific error view")
        void postReason_appNameMismatch_returnsError() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();

            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();
            form.setAppRoleId(roleId);

            when(bindingResult.hasFieldErrors("reason")).thenReturn(false);
            when(mockHttpSession.getAttribute("appFilter")).thenReturn("SessionApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");

            AppRoleDto dto = mockRoleDto(roleId, "Admin", "DbApp");
            when(appRoleService.findById(roleId)).thenReturn(Optional.of(dto));

            String view = adminController.processDeleteAppRoleReasonSubmission(roleId, form, bindingResult, mockHttpSession, model);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("App name does not match the expected value for the selected role");
        }

        @Test
        @DisplayName("POST reason: form roleId mismatch -> specific error view")
        void postReason_formRoleIdMismatch_returnsError() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();

            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();
            form.setAppRoleId(UUID.randomUUID().toString()); // mismatch with session id

            when(bindingResult.hasFieldErrors("reason")).thenReturn(false);
            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");

            AppRoleDto dto = mockRoleDto(roleId, "Admin", "MyApp");
            when(appRoleService.findById(roleId)).thenReturn(Optional.of(dto));

            String view = adminController.processDeleteAppRoleReasonSubmission(roleId, form, bindingResult, mockHttpSession, model);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("Role ID does not match the expected value for the selected role");
        }

        @Test
        @DisplayName("POST reason: happy path -> form stored in session and redirect to check-answers with role id")
        void postReason_happyPath_redirects() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();
            final String appName = "MyApp";
            final String roleName = "Admin";

            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();
            form.setAppRoleId(roleId);
            form.setAppName(appName);
            form.setReason("Housekeeping");

            when(bindingResult.hasFieldErrors("reason")).thenReturn(false);
            when(mockHttpSession.getAttribute("appFilter")).thenReturn(appName);
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn(roleName);

            AppRoleDto dto = mockRoleDto(roleId, roleName, appName);
            when(appRoleService.findById(roleId)).thenReturn(Optional.of(dto));

            String view = adminController.processDeleteAppRoleReasonSubmission(roleId, form, bindingResult, mockHttpSession, model);

            // Form saved to session
            verify(mockHttpSession).setAttribute("deleteAppRoleReasonForm", form);

            // Redirect contains the ID from the DTO
            assertThat(view).isEqualTo("redirect:/admin/silas-administration/delete-role/" + roleId + "/check-answers");
        }

        @Test
        @DisplayName("GET check-answers: missing app name -> 404")
        void showCheckAnswers_missingApp_throws404() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn(null);

            assertThatThrownBy(() ->
                    adminController.showDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("App name not found in session");
                    });

            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("GET check-answers: missing roleIdForDeletion -> 404")
        void showCheckAnswers_missingRoleId_throws404() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(null);

            assertThatThrownBy(() ->
                    adminController.showDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("Role ID not found in session");
                    });

            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("GET check-answers: missing roleNameForDeletion -> 404")
        void showCheckAnswers_missingRoleName_throws404() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn(null);

            assertThatThrownBy(() ->
                    adminController.showDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("Role name not found in session");
                    });

            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("GET check-answers: missing deleteAppRoleReasonForm -> 404")
        void showCheckAnswers_missingForm_throws404() {
            Model model = new ExtendedModelMap();
            String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");
            when(mockHttpSession.getAttribute("deleteAppRoleReasonForm")).thenReturn(null);

            assertThatThrownBy(() ->
                    adminController.showDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("Delete Role form not found in session");
                    });

            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("GET check-answers: roleId mismatch (path vs session) -> error view")
        void showCheckAnswers_roleIdMismatch_returnsError() {
            final Model model = new ExtendedModelMap();
            final String pathRoleId = UUID.randomUUID().toString();
            final String sessionRoleId = UUID.randomUUID().toString();

            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();
            form.setAppRoleId(pathRoleId); // even though session mismatches, we also need form in session

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(sessionRoleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");
            when(mockHttpSession.getAttribute("deleteAppRoleReasonForm")).thenReturn(form);

            String view = adminController.showDeleteAppRoleCheckAnswersPage(pathRoleId, mockHttpSession, model);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("Error while processing app role management");

            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("GET check-answers: form.appRoleId mismatch with path -> error view")
        void showCheckAnswers_formRoleIdMismatch_returnsError() {
            final Model model = new ExtendedModelMap();
            final String pathRoleId = UUID.randomUUID().toString();
            final String sessionRoleId = pathRoleId;

            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();
            form.setAppRoleId(UUID.randomUUID().toString()); // mismatch

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(sessionRoleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");
            when(mockHttpSession.getAttribute("deleteAppRoleReasonForm")).thenReturn(form);

            String view = adminController.showDeleteAppRoleCheckAnswersPage(pathRoleId, mockHttpSession, model);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("Error while processing app role management");

            verifyNoInteractions(appRoleService);
        }

        @Test
        @DisplayName("GET check-answers: happy path -> model populated and counts added")
        void showCheckAnswers_happyPath_populatesModel() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();
            final String appName = "MyApp";
            final String roleName = "Admin";

            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();
            form.setAppRoleId(roleId);
            form.setAppName(appName);
            form.setReason("Housekeeping");

            when(mockHttpSession.getAttribute("appFilter")).thenReturn(appName);
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn(roleName);
            when(mockHttpSession.getAttribute("deleteAppRoleReasonForm")).thenReturn(form);

            when(appRoleService.countNoOfRoleAssignments(roleId)).thenReturn(12L);
            when(appRoleService.countNoOfFirmsWithRoleAssignments(roleId)).thenReturn(3L);

            String view = adminController.showDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model);

            assertThat(view).isEqualTo("silas-administration/delete-app-role-check-answers");
            assertThat(model.getAttribute("reason")).isEqualTo("Housekeeping");
            assertThat(model.getAttribute("roleName")).isEqualTo(roleName);
            assertThat(model.getAttribute("appName")).isEqualTo(appName);
            assertThat(model.getAttribute("roleId")).isEqualTo(roleId);
            assertThat(model.getAttribute("noOfUserProfilesAffected")).isEqualTo(12L);
            assertThat(model.getAttribute("noOfFirmsAffected")).isEqualTo(3L);
            assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE))
                    .isEqualTo(AdminController.SILAS_ADMINISTRATION_TITLE);

            verify(appRoleService).countNoOfRoleAssignments(roleId);
            verify(appRoleService).countNoOfFirmsWithRoleAssignments(roleId);
            verifyNoMoreInteractions(appRoleService);
        }

        @Test
        @DisplayName("GET check-answers: when counting throws -> caught and generic error view")
        void showCheckAnswers_countsThrow_returnsGenericError() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();
            final String appName = "MyApp";

            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();
            form.setAppRoleId(roleId);
            form.setAppName(appName);
            form.setReason("Cleanup");

            when(mockHttpSession.getAttribute("appFilter")).thenReturn(appName);
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");
            when(mockHttpSession.getAttribute("deleteAppRoleReasonForm")).thenReturn(form);

            when(appRoleService.countNoOfRoleAssignments(roleId)).thenThrow(new RuntimeException("boom"));

            String view = adminController.showDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("An error occurred while loading the page");
        }

        @Test
        @DisplayName("POST check-answers: missing app name -> 404")
        void postCheckAnswers_missingApp_throws404() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn(null);

            assertThatThrownBy(() ->
                    adminController.submitDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model, authentication)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("App name not found in session");
                    });

            verifyNoInteractions(appRoleService, loginService);
        }

        @Test
        @DisplayName("POST check-answers: missing roleIdForDeletion -> 404")
        void postCheckAnswers_missingRoleId_throws404() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(null);

            assertThatThrownBy(() ->
                    adminController.submitDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model, authentication)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("Role ID not found in session");
                    });

            verifyNoInteractions(appRoleService, loginService);
        }

        @Test
        @DisplayName("POST check-answers: missing roleNameForDeletion -> 404")
        void postCheckAnswers_missingRoleName_throws404() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn(null);

            assertThatThrownBy(() ->
                    adminController.submitDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model, authentication)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("Role name not found in session");
                    });

            verifyNoInteractions(appRoleService, loginService);
        }

        @Test
        @DisplayName("POST check-answers: role not found -> RuntimeException (propagates)")
        void postCheckAnswers_roleNotFound_runtimeException() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");
            when(appRoleService.findById(roleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    adminController.submitDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model, authentication)
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("App Role not found");
        }

        @Test
        @DisplayName("POST check-answers: role name mismatch -> specific error view")
        void postCheckAnswers_roleNameMismatch_returnsError() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Manager"); // expected name in session

            AppRoleDto dto = mockRoleDto(roleId, "Admin", "MyApp");
            when(appRoleService.findById(roleId)).thenReturn(Optional.of(dto));

            String view = adminController.submitDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model, authentication);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("Role name does not match the expected value for the selected role");
        }

        @Test
        @DisplayName("POST check-answers: app name mismatch -> specific error view")
        void postCheckAnswers_appNameMismatch_returnsError() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("SessionApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");

            AppRoleDto dto = mockRoleDto(roleId, "Admin", "DbApp");
            when(appRoleService.findById(roleId)).thenReturn(Optional.of(dto));

            String view = adminController.submitDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model, authentication);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("App name does not match the expected value for the selected role");
        }

        @Test
        @DisplayName("POST check-answers: missing deleteAppRoleReasonForm -> 404")
        void postCheckAnswers_missingForm_throws404() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");
            when(mockHttpSession.getAttribute("deleteAppRoleReasonForm")).thenReturn(null);

            AppRoleDto dto = mockRoleDto(roleId, "Admin", "MyApp");
            when(appRoleService.findById(roleId)).thenReturn(Optional.of(dto));

            assertThatThrownBy(() ->
                    adminController.submitDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model, authentication)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(rse.getReason()).isEqualTo("Delete Role form not found in session");
                    });
        }

        @Test
        @DisplayName("POST check-answers: roleId mismatch with session or form -> generic error view")
        void postCheckAnswers_roleIdMismatch_returnsError() {
            final Model model = new ExtendedModelMap();
            final String pathRoleId = UUID.randomUUID().toString();
            final String sessionRoleId = UUID.randomUUID().toString();

            when(mockHttpSession.getAttribute("appFilter")).thenReturn("MyApp");
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(sessionRoleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn("Admin");

            AppRoleDto dto = mockRoleDto(sessionRoleId, "Admin", "MyApp");
            when(appRoleService.findById(sessionRoleId)).thenReturn(Optional.of(dto));

            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();
            form.setAppRoleId(sessionRoleId); // ok for form, but path != session
            when(mockHttpSession.getAttribute("deleteAppRoleReasonForm")).thenReturn(form);

            String view = adminController.submitDeleteAppRoleCheckAnswersPage(pathRoleId, mockHttpSession, model, authentication);

            assertThat(view).isEqualTo("errors/error-generic");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("Error while processing app role management");
        }

        @Test
        @DisplayName("POST check-answers: happy path -> calls delete and returns confirmation view")
        void postCheckAnswers_happyPath_callsDeleteAndReturnsConfirmation() {
            final Model model = new ExtendedModelMap();
            final String roleId = UUID.randomUUID().toString();
            final String appName = "MyApp";
            final String roleName = "Admin";
            final String reason = "Housekeeping";

            when(mockHttpSession.getAttribute("appFilter")).thenReturn(appName);
            when(mockHttpSession.getAttribute("roleIdForDeletion")).thenReturn(roleId);
            when(mockHttpSession.getAttribute("roleNameForDeletion")).thenReturn(roleName);

            AppRoleDto dto = mockRoleDto(roleId, roleName, appName);
            when(appRoleService.findById(roleId)).thenReturn(Optional.of(dto));

            DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();
            form.setAppRoleId(roleId);
            form.setAppName(appName);
            form.setReason(reason);
            when(mockHttpSession.getAttribute("deleteAppRoleReasonForm")).thenReturn(form);

            // login service stubbing
            final UUID entraOid = UUID.randomUUID();
            final UUID profileId = UUID.randomUUID();

            CurrentUserDto currentUser = new CurrentUserDto();
            currentUser.setUserId(entraOid);
            currentUser.setName("Admin");

            when(loginService.getCurrentUser(authentication)).thenReturn(currentUser);
            when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder().id(profileId).build());

            String view = adminController.submitDeleteAppRoleCheckAnswersPage(roleId, mockHttpSession, model, authentication);

            // Verify service delete invoked with correct args
            verify(appRoleService).deleteAppRole(profileId, entraOid, appName, roleId, reason);

            assertThat(view).isEqualTo("silas-administration/delete-app-roles-confirmation");
        }

    }
}
