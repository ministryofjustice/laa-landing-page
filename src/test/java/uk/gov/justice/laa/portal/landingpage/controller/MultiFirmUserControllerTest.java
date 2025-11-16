package uk.gov.justice.laa.portal.landingpage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.view.RedirectView;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AddUserProfileAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.ApplicationsForm;
import uk.gov.justice.laa.portal.landingpage.forms.MultiFirmUserForm;
import uk.gov.justice.laa.portal.landingpage.forms.OfficesForm;
import uk.gov.justice.laa.portal.landingpage.forms.RolesForm;
import uk.gov.justice.laa.portal.landingpage.model.OfficeModel;
import uk.gov.justice.laa.portal.landingpage.model.UserRole;
import uk.gov.justice.laa.portal.landingpage.service.AppRoleService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.RoleAssignmentService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.utils.CcmsRoleGroupsUtil;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;

@ExtendWith(MockitoExtension.class)
public class MultiFirmUserControllerTest {

    private MultiFirmUserController controller;

    @Mock
    private UserService userService;
    @Mock
    private LoginService loginService;
    @Mock
    private Authentication authentication;
    @Mock
    private AppRoleService appRoleService;
    @Mock
    private RoleAssignmentService roleAssignmentService;
    @Mock
    private OfficeService officeService;
    @Mock
    private EventService eventService;
    @Mock
    private FirmService firmService;
    @Mock
    private BindingResult bindingResult;
    @Mock
    private ApplicationsForm applicationsForm;

    private HttpSession session;
    private Model model;

    @BeforeEach
    public void setUp() {
        ModelMapper mapper = new ModelMapper();
        model = new ExtendedModelMap();
        session = new MockHttpSession();
        controller = new MultiFirmUserController(userService, loginService, appRoleService,
                roleAssignmentService, officeService, eventService, mapper, firmService);
    }

    private MultiFirmUserForm createForm() {
        return MultiFirmUserForm.builder().email("test@email.com").build();
    }

    private BindingResult mockBindingResult(boolean hasErrors) {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(hasErrors);
        return bindingResult;
    }

    private void assertSessionAndModelPopulated(Model model, HttpSession session) {
        assertThat(model.getAttribute("multiFirmUserForm")).isNotNull();
        assertThat(session.getAttribute("multiFirmUserForm")).isNotNull();
    }

    @Test
    public void addUserProfile() {
        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder()
                .firm(Firm.builder().build()).build());
        String result = controller.addUserProfile(model, session, authentication);
        assertThat(result).isEqualTo("multi-firm-user/select-user");

        assertThat(model.getAttribute("multiFirmUserForm")).isNotNull();
        assertThat(model.getAttribute("email")).isNull();
        assertThat(session.getAttribute("multiFirmUserForm")).isNull();
        assertThat(session.getAttribute("entraUser")).isNull();
    }

    @Test
    public void addUserProfileOnRevisit() {
        MultiFirmUserForm form = createForm();
        session.setAttribute("multiFirmUserForm", form);
        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder()
                .firm(Firm.builder().build()).build());
        String result = controller.addUserProfile(model, session, authentication);
        assertThat(result).isEqualTo("multi-firm-user/select-user");

        assertThat(model.getAttribute("multiFirmUserForm")).isNotNull();
        assertThat(model.getAttribute("email")).isNotNull();
        assertThat(session.getAttribute("multiFirmUserForm")).isNotNull();
        assertThat(session.getAttribute("entraUser")).isNull();
    }

    @Test
    public void addUserProfilePost_NotaMultiFirmUser() {
        MultiFirmUserForm form = createForm();
        BindingResult bindingResult = mockBindingResult(false);

        Firm userFirm = Firm.builder().name("test").build();
        UserProfile userProfile = UserProfile.builder().firm(userFirm).build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(false).email(form.getEmail())
                .userProfiles(Set.of(userProfile)).build();
        when(userService.findEntraUserByEmail(form.getEmail())).thenReturn(Optional.of(entraUser));

        String result = controller.addUserProfilePost(form, bindingResult, model, session, authentication);

        assertThat(result).isEqualTo("multi-firm-user/select-user");
        verify(bindingResult).rejectValue("email", "error.email",
                "This user cannot be added at this time. Contact your Contract Manager to check their access permissions.");
    }

    @Test
    public void addUserProfilePost_AlreadyaMemberOfTheFirm() {
        MultiFirmUserForm form = createForm();
        BindingResult bindingResult = mockBindingResult(false);

        Firm userFirm = Firm.builder().name("test").build();
        UserProfile userProfile = UserProfile.builder().firm(userFirm).build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(true).email(form.getEmail())
                .userProfiles(Set.of(userProfile)).build();
        when(userService.findEntraUserByEmail(form.getEmail())).thenReturn(Optional.of(entraUser));

        UserProfile adminUserProfile = UserProfile.builder().firm(userFirm).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(adminUserProfile);

        String result = controller.addUserProfilePost(form, bindingResult, model, session, authentication);

        assertThat(result).isEqualTo("multi-firm-user/select-user");
        verify(bindingResult).rejectValue("email", "error.email",
                "This user already has a profile for this firm. You can amend their access from the Manage your users table.");
    }

    @Test
    public void addUserProfilePost_validMultiFirmUser() {
        MultiFirmUserForm form = createForm();
        BindingResult bindingResult = mockBindingResult(false);

        Firm userFirm = Firm.builder().name("test").build();
        UserProfile userProfile = UserProfile.builder().firm(userFirm).build();
        EntraUser entraUser = EntraUser.builder().email(form.getEmail())
                .multiFirmUser(true).userProfiles(Set.of(userProfile)).build();
        when(userService.findEntraUserByEmail(form.getEmail())).thenReturn(Optional.of(entraUser));

        Firm adminFirm = Firm.builder().name("admin firm").build();
        UserProfile adminUserProfile = UserProfile.builder().firm(adminFirm).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(adminUserProfile);

        String result = controller.addUserProfilePost(form, bindingResult, model, session, authentication);

        assertThat(result).isEqualTo("redirect:/admin/multi-firm/user/add/profile/select/apps");
        assertThat(model.getAttribute("entraUser")).isNotNull();
        assertThat(session.getAttribute("entraUser")).isNotNull();
    }

    @Test
    public void addUserProfilePost_notMultiFirmUser() {
        MultiFirmUserForm form = createForm();
        BindingResult bindingResult = mockBindingResult(false);
        EntraUser entraUser = EntraUser.builder().multiFirmUser(false).email(form.getEmail()).build();
        when(userService.findEntraUserByEmail(form.getEmail())).thenReturn(Optional.of(entraUser));

        String result = controller.addUserProfilePost(form, bindingResult, model, session, authentication);

        assertThat(result).isEqualTo("multi-firm-user/select-user");
        verify(bindingResult).rejectValue("email", "error.email",
                "This user cannot be added at this time. Contact your Contract Manager to check their access permissions.");

        assertThat(model.getAttribute("multiFirmUserForm")).isNull();
        assertThat(session.getAttribute("multiFirmUserForm")).isNotNull();
        assertThat(model.getAttribute("entraUser")).isNull();
        assertThat(session.getAttribute("entraUser")).isNull();
    }

    @Test
    public void addUserProfilePost_userNotFound() {
        MultiFirmUserForm form = createForm();
        BindingResult bindingResult = mockBindingResult(false);
        when(userService.findEntraUserByEmail(form.getEmail())).thenReturn(Optional.empty());

        String result = controller.addUserProfilePost(form, bindingResult, model, session, authentication);

        assertThat(result).isEqualTo("multi-firm-user/select-user");
        verify(bindingResult).rejectValue("email", "error.email",
                "We could not find this user. Try again or ask the Legal Aid Agency to create a new account for them.");

        assertThat(model.getAttribute("multiFirmUserForm")).isNull();
        assertThat(session.getAttribute("multiFirmUserForm")).isNotNull();
        assertThat(model.getAttribute("entraUser")).isNull();
        assertThat(session.getAttribute("entraUser")).isNull();
    }

    @Test
    public void addUserProfilePost_invalidForm() {
        MultiFirmUserForm form = MultiFirmUserForm.builder().build();
        BindingResult bindingResult = mockBindingResult(true);

        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder()
                .firm(Firm.builder().build()).build());

        String result = controller.addUserProfilePost(form, bindingResult, model, session, authentication);

        assertThat(result).isEqualTo("multi-firm-user/select-user");
        assertSessionAndModelPopulated(model, session);
        assertThat(model.getAttribute("entraUser")).isNull();
        assertThat(session.getAttribute("entraUser")).isNull();
    }

    @Test
    void testSelectUserApps_validData_shouldReturnViewAndSetAttributes() {
        ApplicationsForm form = new ApplicationsForm();
        form.setApps(List.of("app1"));

        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setFullName("John Doe");

        AppDto app1 = new AppDto();
        app1.setId("app1");
        AppDto app2 = new AppDto();
        app2.setId("app2");

        session.setAttribute("applicationsForm", form);
        session.setAttribute("entraUser", entraUser);

        UserProfile profile = UserProfile.builder().build();

        when(userService.getAppsByUserType(UserType.EXTERNAL)).thenReturn(List.of(app1, app2));
        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);
        when(roleAssignmentService.canUserAssignRolesForApp(profile, app1)).thenReturn(true);
        when(roleAssignmentService.canUserAssignRolesForApp(profile, app2)).thenReturn(false);

        String view = controller.selectUserApps(model, session, authentication);

        assertThat(view).isEqualTo("multi-firm-user/select-user-apps");
        assertThat(model.getAttribute("applicationsForm")).isEqualTo(form);
        assertThat(model.getAttribute("entraUser")).isEqualTo(entraUser);
        String pageTitle = (String) model.getAttribute(ModelAttributes.PAGE_TITLE);
        assertThat(pageTitle).contains("John Doe");
        assertThat(session.getAttribute("addProfileUserAppsModel")).isEqualTo(model);
    }

    @Test
    void testSelectUserApps_missingApplicationsForm_shouldUseNewForm() {
        session.setAttribute("applicationsForm", null);
        session.setAttribute("entraUser", new EntraUserDto());

        UserProfile profile = UserProfile.builder().build();

        when(userService.getAppsByUserType(UserType.EXTERNAL)).thenReturn(List.of());
        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);

        String view = controller.selectUserApps(model, session, authentication);

        assertThat(view).isEqualTo("multi-firm-user/select-user-apps");
        assertThat(model.getAttribute("applicationsForm")).isInstanceOf(ApplicationsForm.class);
    }

    @Test
    void testSelectUserApps_nullSelectedApps_shouldHandleGracefully() {
        ApplicationsForm form = new ApplicationsForm(); // apps is null
        session.setAttribute("applicationsForm", form);
        session.setAttribute("entraUser", new EntraUserDto());

        UserProfile profile = UserProfile.builder().build();

        when(userService.getAppsByUserType(UserType.EXTERNAL)).thenReturn(List.of());
        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);

        String view = controller.selectUserApps(model, session, authentication);

        assertThat(view).isEqualTo("multi-firm-user/select-user-apps");
    }

    @Test
    void testSelectUserApps_missingEntraUser_shouldThrowException() {
        session.setAttribute("applicationsForm", new ApplicationsForm());
        session.setAttribute("entraUser", null);

        assertThatThrownBy(() -> controller.selectUserApps(model, session, authentication))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testSelectUserApps_noAssignableApps_shouldSetEmptyAppsList() {
        ApplicationsForm form = new ApplicationsForm();
        form.setApps(List.of("app1"));

        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setFullName("Jane Doe");

        AppDto app1 = new AppDto();
        app1.setId("app1");
        AppDto app2 = new AppDto();
        app2.setId("app2");

        session.setAttribute("applicationsForm", form);
        session.setAttribute("entraUser", entraUser);

        UserProfile profile = UserProfile.builder().build();

        when(userService.getAppsByUserType(UserType.EXTERNAL)).thenReturn(List.of(app1, app2));
        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);
        when(roleAssignmentService.canUserAssignRolesForApp(profile, app1)).thenReturn(false);
        when(roleAssignmentService.canUserAssignRolesForApp(profile, app2)).thenReturn(false);

        String view = controller.selectUserApps(model, session, authentication);

        List modelApps = (List) model.getAttribute("apps");
        assertThat(modelApps).isEmpty();
        assertThat(view).isEqualTo("multi-firm-user/select-user-apps");
    }

    @Test
    void testValidationErrors_NoModelInSession() {
        when(bindingResult.hasErrors()).thenReturn(true);

        String view = controller.selectUserAppsPost(applicationsForm, bindingResult, model, session);

        assertThat(view).isEqualTo("multi-firm-user/select-user-apps");
        assertThat(model.getAttribute("addProfileUserAppsModel")).isNull();
    }

    @Test
    void testValidationErrors_WithModelInSession() {
        when(bindingResult.hasErrors()).thenReturn(true);

        Model modelFromSession = mock(Model.class);
        session.setAttribute("addProfileUserAppsModel", modelFromSession);
        when(modelFromSession.getAttribute("entraUser")).thenReturn("user");
        when(modelFromSession.getAttribute("apps")).thenReturn(List.of("app1", "app2"));

        String view = controller.selectUserAppsPost(applicationsForm, bindingResult, model, session);

        assertThat(view).isEqualTo("multi-firm-user/select-user-apps");
        assertThat(model.getAttribute("entraUser")).isEqualTo("user");
        List modelApps = (List) model.getAttribute("apps");
        assertThat(modelApps).containsAll(List.of("app1", "app2"));
    }

    @Test
    void testNoValidationErrors_WithAppsSelected() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(applicationsForm.getApps()).thenReturn(List.of("app1", "app2"));

        String view = controller.selectUserAppsPost(applicationsForm, bindingResult, model, session);

        assertThat(view).startsWith("redirect:/admin/multi-firm/user/add/profile/select/roles");
        assertThat(session.getAttribute("applicationsForm")).isEqualTo(applicationsForm);
        List selectedApps = (List) session.getAttribute("addProfileSelectedApps");
        assertThat(selectedApps).containsAll(List.of("app1", "app2"));
        assertThat(session.getAttribute("addProfileUserAppsModel")).isNull();
    }

    @Test
    void testNoValidationErrors_NoAppsSelected() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(applicationsForm.getApps()).thenReturn(null);

        String view = controller.selectUserAppsPost(applicationsForm, bindingResult, model, session);

        assertThat(view).startsWith("redirect:/admin/multi-firm/user/add/profile/select/roles");
        assertThat(session.getAttribute("applicationsForm")).isEqualTo(applicationsForm);
        assertThat(session.getAttribute("addProfileSelectedApps")).isInstanceOf(List.class);
        List selectedApps = (List) session.getAttribute("addProfileSelectedApps");
        assertThat(selectedApps).isEmpty();
        assertThat(session.getAttribute("addProfileUserAppsModel")).isNull();
    }

    @Test
    void shouldRedirectIfNoAppsSelected() {
        session.setAttribute("addProfileSelectedApps", List.of());

        String view = controller.selectUserAppRoles(0, new RolesForm(), authentication, model, session);

        assertThat(view).isEqualTo("redirect:/admin/multi-firm/user/add/profile/select/apps");
    }

    @Test
    void shouldResetIndexIfOutOfBounds() {
        session.setAttribute("addProfileSelectedApps", List.of("app-id-1"));

        AppRoleDto roleDto = new AppRoleDto();
        roleDto.setId(UUID.randomUUID().toString());

        EntraUserDto userDto = new EntraUserDto();
        userDto.setFullName("Test User");
        session.setAttribute("entraUser", userDto);

        UserProfile userProfile = UserProfile.builder().appRoles(Set.of()).build();
        AppDto appDto = AppDto.builder().id("app-id-1").name("App One").build();

        when(userService.getAppRolesByAppIdAndUserType("app-id-1", UserType.EXTERNAL)).thenReturn(List.of(roleDto));
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);
        when(roleAssignmentService.filterRoles(any(Set.class), any(List.class))).thenReturn(List.of(roleDto));
        when(userService.getAppByAppId("app-id-1")).thenReturn(Optional.of(appDto));

        AppRoleViewModel viewModel = new AppRoleViewModel();
        viewModel.setSelected(false);

        String view = controller.selectUserAppRoles(5, new RolesForm(), authentication, model, session);

        assertThat(view).isEqualTo("multi-firm-user/select-user-app-roles");
        assertThat(model.getAttribute("addProfileSelectedAppIndex")).isEqualTo(0);
    }

    @Test
    void shouldUseExistingSelectedRolesFromSession() {
        String appId = "app-id-1";
        String roleId = UUID.randomUUID().toString();

        session.setAttribute("addProfileSelectedApps", List.of(appId));
        session.setAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());

        Map<Integer, List<String>> selectedRolesMap = new HashMap<>();
        selectedRolesMap.put(0, List.of(roleId));
        session.setAttribute("addUserProfileAllSelectedRoles", selectedRolesMap);

        AppRoleDto roleDto = new AppRoleDto();
        roleDto.setId(roleId);

        AppDto appDto = AppDto.builder().id(appId).name("App One").build();

        UserProfile userProfile = UserProfile.builder().appRoles(Set.of()).build();

        when(userService.getAppRolesByAppIdAndUserType(appId, UserType.EXTERNAL)).thenReturn(List.of(roleDto));
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);
        when(roleAssignmentService.filterRoles(any(), any())).thenReturn(List.of(roleDto));
        when(userService.getAppByAppId(appId)).thenReturn(Optional.of(appDto));

        AppRoleViewModel viewModel = new AppRoleViewModel();
        viewModel.setSelected(true);

        String view = controller.selectUserAppRoles(0, new RolesForm(), authentication, model, session);

        assertThat(view).isEqualTo("multi-firm-user/select-user-app-roles");
        assertThat(((AppRoleViewModel) ((List<?>) model.getAttribute("roles")).get(0)).isSelected()).isTrue();
    }

    @Test
    void shouldInitializeEmptySelectedRolesIfNoneInSession() {
        String appId = "app-id-1";
        String roleId = UUID.randomUUID().toString();

        session.setAttribute("addProfileSelectedApps", List.of(appId));
        session.setAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());

        AppRoleDto roleDto = new AppRoleDto();
        roleDto.setId(roleId);

        AppDto appDto = AppDto.builder().id(appId).name("App One").build();

        UserProfile userProfile = UserProfile.builder().appRoles(Set.of()).build();

        when(userService.getAppRolesByAppIdAndUserType(appId, UserType.EXTERNAL)).thenReturn(List.of(roleDto));
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);
        when(roleAssignmentService.filterRoles(any(), any())).thenReturn(List.of(roleDto));
        when(userService.getAppByAppId(appId)).thenReturn(Optional.of(appDto));

        AppRoleViewModel viewModel = new AppRoleViewModel();
        viewModel.setSelected(false);

        String view = controller.selectUserAppRoles(0, new RolesForm(), authentication, model, session);

        assertThat(view).isEqualTo("multi-firm-user/select-user-app-roles");
        assertThat(((AppRoleViewModel) ((List<?>) model.getAttribute("roles")).get(0)).isSelected()).isFalse();
    }

    @Test
    void shouldSetBackUrlCorrectlyForFirstApp() {
        session.setAttribute("addProfileSelectedApps", List.of("app-id-1"));
        session.setAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());

        AppRoleDto roleDto = new AppRoleDto();
        roleDto.setId(UUID.randomUUID().toString());

        AppDto appDto = AppDto.builder().id("app-id-1").name("App One").build();

        UserProfile userProfile = UserProfile.builder().appRoles(Set.of()).build();

        when(userService.getAppRolesByAppIdAndUserType("app-id-1", UserType.EXTERNAL)).thenReturn(List.of(roleDto));
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);
        when(roleAssignmentService.filterRoles(any(), any())).thenReturn(List.of(roleDto));
        when(userService.getAppByAppId("app-id-1")).thenReturn(Optional.of(appDto));

        AppRoleViewModel viewModel = new AppRoleViewModel();
        viewModel.setSelected(false);

        String view = controller.selectUserAppRoles(0, new RolesForm(), authentication, model, session);

        assertThat(model.getAttribute("backUrl")).isEqualTo("/admin/multi-firm/user/add/profile/select/apps");
    }

    @Test
    void shouldSetBackUrlCorrectlyForSubsequentApps() {
        session.setAttribute("addProfileSelectedApps", List.of("app-id-1", "app-id-2"));
        session.setAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());

        AppRoleDto roleDto = new AppRoleDto();
        roleDto.setId(UUID.randomUUID().toString());

        AppDto appDto = AppDto.builder().id("app-id-2").name("App Two").build();

        UserProfile userProfile = UserProfile.builder().appRoles(Set.of()).build();

        when(userService.getAppRolesByAppIdAndUserType("app-id-2", UserType.EXTERNAL)).thenReturn(List.of(roleDto));
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);
        when(roleAssignmentService.filterRoles(any(), any())).thenReturn(List.of(roleDto));
        when(userService.getAppByAppId("app-id-2")).thenReturn(Optional.of(appDto));

        AppRoleViewModel viewModel = new AppRoleViewModel();
        viewModel.setSelected(false);

        String view = controller.selectUserAppRoles(1, new RolesForm(), authentication, model, session);

        assertThat(model.getAttribute("backUrl"))
                .isEqualTo("/admin/multi-firm/user/add/profile/select/roles?selectedAppIndex=0");
    }

    @Test
    void shouldHandleInvalidRoleIdGracefully() {
        String appId = "app-id-1";
        String invalidRoleId = UUID.randomUUID().toString();

        session.setAttribute("addProfileSelectedApps", List.of(appId));
        session.setAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());

        Map<Integer, List<String>> selectedRolesMap = new HashMap<>();
        selectedRolesMap.put(0, List.of(invalidRoleId));
        session.setAttribute("addUserProfileAllSelectedRoles", selectedRolesMap);

        AppRoleDto validRole = new AppRoleDto();
        validRole.setId(UUID.randomUUID().toString());

        AppDto appDto = AppDto.builder().id(appId).name("App One").build();

        UserProfile userProfile = UserProfile.builder().appRoles(Set.of()).build();

        when(userService.getAppRolesByAppIdAndUserType(appId, UserType.EXTERNAL)).thenReturn(List.of(validRole));
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);
        when(roleAssignmentService.filterRoles(any(), any())).thenReturn(List.of(validRole));
        when(userService.getAppByAppId(appId)).thenReturn(Optional.of(appDto));

        AppRoleViewModel viewModel = new AppRoleViewModel();
        viewModel.setSelected(false); // invalid role should not be marked selected

        String view = controller.selectUserAppRoles(0, new RolesForm(), authentication, model, session);

        assertThat(view).isEqualTo("multi-firm-user/select-user-app-roles");
        assertThat(((AppRoleViewModel) ((List<?>) model.getAttribute("roles")).get(0)).isSelected()).isFalse();
    }

    @Test
    void shouldHandleMultipleRoleSelections() {
        String appId = "app-id-1";
        String roleId1 = UUID.randomUUID().toString();
        String roleId2 = UUID.randomUUID().toString();

        session.setAttribute("addProfileSelectedApps", List.of(appId));
        session.setAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());

        Map<Integer, List<String>> selectedRolesMap = new HashMap<>();
        selectedRolesMap.put(0, List.of(roleId1, roleId2));
        session.setAttribute("addUserProfileAllSelectedRoles", selectedRolesMap);

        AppRoleDto roleDto1 = new AppRoleDto();
        roleDto1.setId(roleId1);
        AppRoleDto roleDto2 = new AppRoleDto();
        roleDto2.setId(roleId2);

        AppDto appDto = AppDto.builder().id(appId).name("App One").build();

        UserProfile userProfile = UserProfile.builder().appRoles(Set.of()).build();

        when(userService.getAppRolesByAppIdAndUserType(appId, UserType.EXTERNAL))
                .thenReturn(List.of(roleDto1, roleDto2));
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);
        when(roleAssignmentService.filterRoles(any(), any())).thenReturn(List.of(roleDto1, roleDto2));
        when(userService.getAppByAppId(appId)).thenReturn(Optional.of(appDto));

        AppRoleViewModel viewModel1 = new AppRoleViewModel();
        viewModel1.setSelected(true);
        AppRoleViewModel viewModel2 = new AppRoleViewModel();
        viewModel2.setSelected(true);

        String view = controller.selectUserAppRoles(0, new RolesForm(), authentication, model, session);

        assertThat(view).isEqualTo("multi-firm-user/select-user-app-roles");

        List<AppRoleViewModel> roles = (List<AppRoleViewModel>) model.getAttribute("roles");
        assertThat(roles).hasSize(2);
        assertThat(roles).allMatch(AppRoleViewModel::isSelected);
    }

    @Test
    void shouldRedirectIfSessionModelMissing() {
        String view = controller.selectUserAppRolesPost(new RolesForm(), null, 0, authentication, model, session);
        assertThat(view).isEqualTo("redirect:/admin/multi-firm/user/add/profile/select/roles");
    }

    @Test
    void shouldHandleValidationErrorsAndDeselectUnselectedRoles() {
        AppRoleViewModel role1 = new AppRoleViewModel();
        role1.setId("role1");
        role1.setSelected(true);

        AppRoleViewModel role2 = new AppRoleViewModel();
        role2.setId("role2");
        role2.setSelected(true);

        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("roles", List.of(role1, role2));
        sessionModel.addAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());
        sessionModel.addAttribute("addProfileSelectedAppIndex", 0);
        sessionModel.addAttribute("addProfileCurrentApp", AppDto.builder().name("app-id").build());

        session.setAttribute("addProfileUserRolesModel", sessionModel);

        RolesForm form = new RolesForm();
        form.setRoles(List.of("role1")); // role2 should be deselected

        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);

        String view = controller.selectUserAppRolesPost(form, result, 0, authentication, model, session);

        assertThat(view).isEqualTo("multi-firm-user/select-user-app-roles");

        List<AppRoleViewModel> updatedRoles = (List<AppRoleViewModel>) model.getAttribute("roles");
        assertThat(updatedRoles).hasSize(2);
        assertThat(updatedRoles).anyMatch(r -> r.getId().equals("role1") && r.isSelected());
        assertThat(updatedRoles).anyMatch(r -> r.getId().equals("role2") && !r.isSelected());

        assertThat(model.getAttribute("backUrl")).isEqualTo("/admin/multi-firm/user/add/profile/select/apps");
    }

    @Test
    void shouldRedirectToOfficesIfLastAppAndRolesAreAssignable() {
        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("addProfileSelectedAppIndex", 1);
        session.setAttribute("addProfileUserRolesModel", sessionModel);
        session.setAttribute("addProfileSelectedApps", List.of("app1", "app2"));

        RolesForm form = new RolesForm();
        form.setRoles(List.of("role1", "role2"));

        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        UserProfile profile = UserProfile.builder().appRoles(Set.of()).build();

        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);
        when(roleAssignmentService.canAssignRole(any(), eq(List.of("role1", "role2")))).thenReturn(true);

        String view = controller.selectUserAppRolesPost(form, result, 1, authentication, model, session);

        assertThat(view).isEqualTo("redirect:/admin/multi-firm/user/add/profile/select/offices");

        Map<Integer, List<String>> storedRoles = (Map<Integer, List<String>>) session
                .getAttribute("addUserProfileAllSelectedRoles");
        assertThat(storedRoles).containsEntry(1, List.of("role1", "role2"));
        assertThat(session.getAttribute("addProfileUserRolesModel")).isNull();
    }

    @Test
    void shouldStayOnPageIfRolesNotAssignable() {
        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("addProfileSelectedAppIndex", 0);
        session.setAttribute("addProfileUserRolesModel", sessionModel);
        session.setAttribute("addProfileSelectedApps", List.of("app1"));

        RolesForm form = new RolesForm();
        form.setRoles(List.of("role1"));

        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        UserProfile profile = UserProfile.builder().appRoles(Set.of()).build();

        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);
        when(roleAssignmentService.canAssignRole(any(), eq(List.of("role1")))).thenReturn(false);

        String view = controller.selectUserAppRolesPost(form, result, 0, authentication, model, session);

        assertThat(view).isEqualTo("multi-firm-user/select-user-app-roles");
    }

    @Test
    void shouldRedirectToNextAppIfNotLast() {
        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("addProfileSelectedAppIndex", 0);
        session.setAttribute("addProfileUserRolesModel", sessionModel);
        session.setAttribute("addProfileSelectedApps", List.of("app1", "app2"));

        RolesForm form = new RolesForm();
        form.setRoles(List.of("role1"));

        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        String view = controller.selectUserAppRolesPost(form, result, 0, authentication, model, session);

        assertThat(view).isEqualTo("redirect:/admin/multi-firm/user/add/profile/select/roles?selectedAppIndex=1");

        Map<Integer, List<String>> storedRoles = (Map<Integer, List<String>>) session
                .getAttribute("addUserProfileAllSelectedRoles");
        assertThat(storedRoles).containsEntry(0, List.of("role1"));

        Model updatedModel = (Model) session.getAttribute("addProfileUserRolesModel");
        assertThat(updatedModel.getAttribute("addProfileSelectedAppIndex")).isEqualTo(1);
    }

    @Test
    void shouldHandleNoRolesSelectedGracefully() {
        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("addProfileSelectedAppIndex", 0);
        session.setAttribute("addProfileUserRolesModel", sessionModel);
        session.setAttribute("addProfileSelectedApps", List.of("app1"));

        RolesForm form = new RolesForm(); // roles is null
        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        UserProfile profile = UserProfile.builder().appRoles(Set.of()).build();

        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);
        when(roleAssignmentService.canAssignRole(any(), eq(List.of()))).thenReturn(false);

        String view = controller.selectUserAppRolesPost(form, result, 0, authentication, model, session);

        assertThat(view).isEqualTo("multi-firm-user/select-user-app-roles");

        Map<Integer, List<String>> storedRoles = (Map<Integer, List<String>>) session
                .getAttribute("addUserProfileAllSelectedRoles");
        assertThat(storedRoles).containsEntry(0, null);
    }

    @Test
    void shouldStayOnPageIfRoleAssignmentFails() {
        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("addProfileSelectedAppIndex", 0);
        session.setAttribute("addProfileUserRolesModel", sessionModel);
        session.setAttribute("addProfileSelectedApps", List.of("app1"));

        RolesForm form = new RolesForm();
        form.setRoles(List.of("role1", "role2"));

        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        UserProfile profile = UserProfile.builder().appRoles(Set.of()).build();

        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);
        when(roleAssignmentService.canAssignRole(any(), eq(List.of("role1", "role2")))).thenReturn(false);

        String view = controller.selectUserAppRolesPost(form, result, 0, authentication, model, session);

        assertThat(view).isEqualTo("multi-firm-user/select-user-app-roles");

        Map<Integer, List<String>> storedRoles = (Map<Integer, List<String>>) session
                .getAttribute("addUserProfileAllSelectedRoles");
        assertThat(storedRoles).containsEntry(0, List.of("role1", "role2"));
    }

    @Test
    void testIsCcmsApp_true_dueToAppName() {
        // Setup
        String appId = "app-id";
        List<String> selectedAppIds = List.of(appId);
        session.setAttribute("addProfileSelectedApps", selectedAppIds);

        AppDto appDto = AppDto.builder().id(appId).name("CCMS Application").build();
        when(userService.getAppByAppId(appId)).thenReturn(Optional.of(appDto));

        when(userService.getAppRolesByAppIdAndUserType(eq(appId), eq(UserType.EXTERNAL)))
                .thenReturn(List.of());

        UserProfile userProfile = UserProfile.builder().appRoles(Set.of()).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);

        when(roleAssignmentService.filterRoles(any(), any())).thenReturn(List.of());

        session.setAttribute("addUserProfileAllSelectedRoles", null);
        session.setAttribute("entraUser", new EntraUserDto());

        // Act
        String view = controller.selectUserAppRoles(0, new RolesForm(), authentication, model, session);

        // Assert
        assertThat(model.getAttribute("isCcmsApp")).isEqualTo(true);
        assertThat(view).isEqualTo("multi-firm-user/select-user-app-roles");
    }

    @Test
    void testIsCcmsApp_true_dueToRole() {
        // Setup
        String appId = "app-id";
        List<String> selectedAppIds = List.of(appId);
        session.setAttribute("addProfileSelectedApps", selectedAppIds);

        AppDto appDto = new AppDto();
        appDto.setName("Non-CCMS App");
        when(userService.getAppByAppId(appId)).thenReturn(Optional.of(appDto));

        AppRoleDto ccmsRole = new AppRoleDto();
        ccmsRole.setId(UUID.randomUUID().toString());
        ccmsRole.setCcmsCode("CCMS_CODE");
        when(userService.getAppRolesByAppIdAndUserType(eq(appId), eq(UserType.EXTERNAL)))
                .thenReturn(List.of(ccmsRole));

        UserProfile userProfile = UserProfile.builder().appRoles(Set.of()).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);

        when(roleAssignmentService.filterRoles(any(), any())).thenReturn(List.of(ccmsRole));

        session.setAttribute("addUserProfileAllSelectedRoles", null);
        session.setAttribute("entraUser", new EntraUserDto());

        // Mock CCMS role check
        try (MockedStatic<CcmsRoleGroupsUtil> mockedStatic = mockStatic(CcmsRoleGroupsUtil.class)) {

            mockedStatic.when(() -> CcmsRoleGroupsUtil.isCcmsRole("CCMS_CODE")).thenReturn(true);
            mockedStatic.when(() -> CcmsRoleGroupsUtil.organizeCcmsRolesBySection(any())).thenReturn(Map.of());

            // Act
            String view = controller.selectUserAppRoles(0, new RolesForm(), authentication, model, session);

            // Assert
            assertThat(model.getAttribute("isCcmsApp")).isEqualTo(true);
            assertThat(view).isEqualTo("multi-firm-user/select-user-app-roles");
        }

    }

    @Test
    void shouldLoadOfficesFromUserProfileAndMarkSelected() {
        EntraUserDto user = EntraUserDto.builder().fullName("Test User").build();
        session.setAttribute("entraUser", user);

        OfficesForm officesForm = OfficesForm.builder().offices(List.of("00000000-0000-0000-0000-000000000001"))
                .build();
        session.setAttribute("officesForm", officesForm);

        Office.Address address = Office.Address.builder().addressLine1("Line1").addressLine2("Line2")
                .addressLine3("Line3").city("City").postcode("12345").build();
        Office office = Office.builder().id(UUID.fromString("00000000-0000-0000-0000-000000000001")).code("office1")
                .address(address).build();
        Firm firm = Firm.builder().offices(Set.of(office)).build();
        UserProfile profile = UserProfile.builder().firm(firm).build();

        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);

        String view = controller.addProfileSelectOffices(model, session, authentication);

        assertThat(view).isEqualTo("multi-firm-user/select-user-offices");

        List<OfficeModel> officeData = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(officeData).hasSize(1);
        assertThat(officeData.get(0).isSelected()).isTrue();

        assertThat(model.getAttribute("hasAllOffices")).isEqualTo(false);
        assertThat(model.getAttribute("officesForm")).isEqualTo(officesForm);
        assertThat(model.getAttribute("entraUser")).isEqualTo(user);
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE))
                .isEqualTo("Add profile - Select offices - Test User");

        assertThat(session.getAttribute("addProfileUserOfficesModel")).isEqualTo(model);
    }

    @Test
    void shouldHandleMissingOfficesFormGracefully() {
        EntraUserDto user = EntraUserDto.builder().fullName("Test User").build();
        session.setAttribute("entraUser", user);

        Office.Address address = Office.Address.builder().addressLine1("Line1").addressLine2("Line2")
                .addressLine3("Line3").city("City").postcode("12345").build();
        Office office = Office.builder().id(UUID.fromString("00000000-0000-0000-0000-000000000001")).code("office1")
                .address(address).build();
        Firm firm = Firm.builder().offices(Set.of(office)).build();
        UserProfile profile = UserProfile.builder().firm(firm).build();

        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);

        String view = controller.addProfileSelectOffices(model, session, authentication);

        assertThat(view).isEqualTo("multi-firm-user/select-user-offices");

        List<OfficeModel> officeData = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(officeData).hasSize(1);
        assertThat(officeData.get(0).isSelected()).isFalse();

        OfficesForm form = (OfficesForm) model.getAttribute("officesForm");
        assertThat(form.getOffices()).isEmpty();
    }

    @Test
    void shouldSetHasAllOfficesTrueIfAllSelected() {
        EntraUserDto user = EntraUserDto.builder().fullName("Test User").build();
        session.setAttribute("entraUser", user);

        OfficesForm officesForm = OfficesForm.builder().offices(List.of("ALL")).build();
        session.setAttribute("officesForm", officesForm);

        Office.Address address = Office.Address.builder().addressLine1("Line1").addressLine2("Line2")
                .addressLine3("Line3").city("City").postcode("12345").build();
        Office office = Office.builder().id(UUID.fromString("00000000-0000-0000-0000-000000000001")).code("office1")
                .address(address).build();
        Firm firm = Firm.builder().offices(Set.of(office)).build();
        UserProfile profile = UserProfile.builder().firm(firm).build();

        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);

        String view = controller.addProfileSelectOffices(model, session, authentication);

        assertThat(view).isEqualTo("multi-firm-user/select-user-offices");
        assertThat(model.getAttribute("hasAllOffices")).isEqualTo(true);
    }

    @Test
    void shouldThrowIfEntraUserMissing() {
        assertThatThrownBy(() -> controller.addProfileSelectOffices(model, session, authentication))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void shouldIgnoreInvalidOfficeIdInForm() {
        EntraUserDto user = EntraUserDto.builder().fullName("Test User").build();
        session.setAttribute("entraUser", user);

        // Form contains an office ID not present in the firm
        OfficesForm officesForm = OfficesForm.builder().offices(List.of("invalid-office-id")).build();
        session.setAttribute("officesForm", officesForm);

        Office.Address address = Office.Address.builder().addressLine1("Line1").addressLine2("Line2")
                .addressLine3("Line3").city("City").postcode("12345").build();
        Office office = Office.builder().id(UUID.fromString("00000000-0000-0000-0000-000000000001")).code("office1")
                .address(address).build();
        Firm firm = Firm.builder().offices(Set.of(office)).build();
        UserProfile profile = UserProfile.builder().firm(firm).build();

        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);

        String view = controller.addProfileSelectOffices(model, session, authentication);

        assertThat(view).isEqualTo("multi-firm-user/select-user-offices");

        List<OfficeModel> officeData = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(officeData).hasSize(1);
        assertThat(officeData.getFirst().isSelected()).isFalse(); // invalid ID should not match
    }

    @Test
    void shouldRedirectIfValidationErrorsAndNoSessionModel() {
        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);

        OfficesForm form = OfficesForm.builder().offices(List.of("office1")).build();

        String view = controller.addProfileSelectOfficesPost(form, result, model, session);

        assertThat(view).isEqualTo("redirect:/admin/multi-firm/user/add/profile/select/offices");
    }

    @Test
    void shouldHandleValidationErrorsAndDeselectUnselectedOffices() {
        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);

        OfficeModel office1 = new OfficeModel("office1", null, "office1", true);
        OfficeModel office2 = new OfficeModel("office2", null, "office2", true);

        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("officeData", List.of(office1, office2));
        sessionModel.addAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());
        session.setAttribute("addProfileUserOfficesModel", sessionModel);
        OfficesForm form = OfficesForm.builder().offices(List.of("office1")).build();

        String view = controller.addProfileSelectOfficesPost(form, result, model, session);

        assertThat(view).isEqualTo("multi-firm-user/select-user-offices");

        List<OfficeModel> updatedOfficeData = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(updatedOfficeData).hasSize(2);
        assertThat(updatedOfficeData).anyMatch(o -> o.getId().equals("office1") && o.isSelected());
        assertThat(updatedOfficeData).anyMatch(o -> o.getId().equals("office2") && !o.isSelected());
    }

    @Test
    void shouldStoreSelectedOfficesAndRedirectToCheckAnswers() {
        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        OfficesForm form = OfficesForm.builder().offices(List.of("office1", "office2")).build();

        String view = controller.addProfileSelectOfficesPost(form, result, model, session);

        assertThat(view).isEqualTo("redirect:/admin/multi-firm/user/add/profile/check-answers");

        List<String> storedOffices = (List<String>) session.getAttribute("userOffices");
        assertThat(storedOffices).containsExactly("office1", "office2");

        OfficesForm storedForm = (OfficesForm) session.getAttribute("officesForm");
        assertThat(storedForm.getOffices()).containsExactly("office1", "office2");
    }

    @Test
    void shouldHandleEmptyOfficeSelectionGracefully() {
        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        OfficesForm form = OfficesForm.builder().offices(null).build();

        String view = controller.addProfileSelectOfficesPost(form, result, model, session);

        assertThat(view).isEqualTo("redirect:/admin/multi-firm/user/add/profile/check-answers");

        List<String> storedOffices = (List<String>) session.getAttribute("userOffices");
        assertThat(storedOffices).isEmpty();
    }

    @Test
    void shouldHandleMissingAppRolesMapGracefully() {
        session.setAttribute("userOffices", List.of("office1"));
        session.setAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());

        Firm firm = Firm.builder().build();
        UserProfile profile = UserProfile.builder().firm(firm).build();

        UserProfileDto profileDto = new UserProfileDto();
        profileDto.setFirm(new FirmDto());

        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);
        when(officeService.getOfficesByIds(List.of("office1"))).thenReturn(List.of(new OfficeDto()));

        when(appRoleService.getByIds(any())).thenReturn(List.of());

        String view = controller.checkAnswerAndAddProfile(model, authentication, session);

        assertThat(view).isEqualTo("multi-firm-user/add-profile-check-answers");
        assertThat(model.getAttribute("firm")).isEqualTo(profileDto.getFirm());
        assertThat(model.getAttribute("userOffices")).isInstanceOf(List.class);
        assertThat(model.getAttribute("selectedAppRole")).isInstanceOf(List.class);
        assertThat(model.getAttribute("externalUser")).isEqualTo(true);
        assertThat(model.getAttribute("user")).isEqualTo(session.getAttribute("entraUser"));
    }

    @Test
    void shouldSkipOfficeLookupIfAllSelected() {
        session.setAttribute("userOffices", List.of("ALL"));
        session.setAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());

        Firm firm = Firm.builder().build();
        UserProfile profile = UserProfile.builder().firm(firm).build();

        FirmDto firmDto = FirmDto.builder().build();
        UserProfileDto profileDto = UserProfileDto.builder().firm(firmDto).build();

        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);
        when(appRoleService.getByIds(any())).thenReturn(List.of());

        String view = controller.checkAnswerAndAddProfile(model, authentication, session);

        assertThat(view).isEqualTo("multi-firm-user/add-profile-check-answers");
        assertThat(model.getAttribute("userOffices")).isEqualTo(List.of());
    }

    @Test
    void shouldMapAppRolesCorrectlyAndSortByOrdinal() {
        session.setAttribute("userOffices", List.of("office1"));
        session.setAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());

        Map<Integer, List<String>> appRolesMap = new HashMap<>();
        appRolesMap.put(0, List.of("role1", "role2"));
        session.setAttribute("addUserProfileAllSelectedRoles", appRolesMap);

        AppRoleDto role1 = AppRoleDto.builder().id("role1").name("Role One").ordinal(2).build();
        AppRoleDto role2 = AppRoleDto.builder().id("role2").name("Role Two").ordinal(1).build();

        Firm firm = Firm.builder().build();
        UserProfile profile = UserProfile.builder().firm(firm).build();

        FirmDto firmDto = FirmDto.builder().build();
        UserProfileDto profileDto = UserProfileDto.builder().firm(firmDto).build();

        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);
        when(officeService.getOfficesByIds(List.of("office1"))).thenReturn(List.of(new OfficeDto()));
        when(appRoleService.getByIds(List.of("role1", "role2"))).thenReturn(List.of(role1, role2));

        String view = controller.checkAnswerAndAddProfile(model, authentication, session);

        assertThat(view).isEqualTo("multi-firm-user/add-profile-check-answers");

        List<UserRole> selectedRoles = (List<UserRole>) model.getAttribute("selectedAppRole");
        assertThat(selectedRoles).hasSize(2);
        assertThat(selectedRoles.get(0).getRoleName()).isEqualTo("Role Two"); // sorted by ordinal
        assertThat(selectedRoles.get(1).getRoleName()).isEqualTo("Role One");
        assertThat(model.getAttribute("isMultiFirmUser")).isEqualTo(true);
    }

    @Test
    void shouldThrowIfRoleAssignmentFails() {
        session.setAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());
        session.setAttribute("addUserProfileAllSelectedRoles", Map.of(0, List.of("role1")));

        AppRoleDto roleDto = AppRoleDto.builder().id("role1").name("Role One").ordinal(1).build();
        UserProfile profile = UserProfile.builder().appRoles(Set.of()).build();

        when(appRoleService.getByIds(List.of("role1"))).thenReturn(List.of(roleDto));
        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);
        when(roleAssignmentService.canAssignRole(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> controller.checkAnswerAndAddProfilePost(authentication, session, model))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("sufficient permissions to assign the selected roles");
    }

    @Test
    void shouldThrowIfOfficeAssignmentFails() {
        session.setAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());
        session.setAttribute("addUserProfileAllSelectedRoles", Map.of());
        session.setAttribute("userOffices", List.of("office1"));

        OfficeDto officeDto = OfficeDto.builder().id(UUID.randomUUID()).code("office1").build();
        Office office = Office.builder().id(UUID.randomUUID()).code("office2").address(Office.Address.builder().build()).build();
        Firm currentUsersFirm = Firm.builder().id(UUID.randomUUID()).offices(Set.of(office)).build();
        UserProfile profile = UserProfile.builder().firm(currentUsersFirm).build();
        Office targetOffice = Office.builder().id(UUID.randomUUID()).code("officeX").address(Office.Address.builder().build()).build();
        UUID targetFirmId = UUID.randomUUID();
        Firm targetFirm = Firm.builder().id(targetFirmId).offices(Set.of(targetOffice)).build();
        session.setAttribute("delegateTargetFirmId", targetFirmId.toString());

        when(appRoleService.getByIds(any())).thenReturn(List.of());
        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);
        when(roleAssignmentService.canAssignRole(any(), any())).thenReturn(true);
        when(officeService.getOfficesByIds(List.of("office1"))).thenReturn(List.of(officeDto));
        when(firmService.getById(targetFirmId)).thenReturn(targetFirm);

        assertThatThrownBy(() -> controller.checkAnswerAndAddProfilePost(authentication, session, model))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Office assignment is not permitted");
    }

    @Test
    void shouldSkipOfficeValidationIfAllSelected() {
        session.setAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());
        session.setAttribute("addUserProfileAllSelectedRoles", Map.of());
        session.setAttribute("userOffices", List.of("ALL"));

        UserProfileDto profileDto = new UserProfileDto();
        profileDto.setFirm(new FirmDto());

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("admin");

        UserProfile profile = UserProfile.builder().firm(Firm.builder().build()).appRoles(Set.of()).build();

        when(appRoleService.getByIds(any())).thenReturn(List.of());
        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);
        when(roleAssignmentService.canAssignRole(any(), any())).thenReturn(true);
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        when(userService.addMultiFirmUserProfile(any(), any(), any(), any(), any()))
                .thenReturn(UserProfile.builder().id(UUID.randomUUID()).build());

        String view = controller.checkAnswerAndAddProfilePost(authentication, session, model);

        assertThat(view).isEqualTo("redirect:/admin/multi-firm/user/add/profile/confirmation");
    }

    @Test
    void shouldCreateProfileAndLogAuditEvent() {
        session.setAttribute("entraUser", EntraUserDto.builder().fullName("Test User").build());
        session.setAttribute("addUserProfileAllSelectedRoles", Map.of(0, List.of("role1", "role2")));
        session.setAttribute("userOffices", List.of("office1"));

        AppRoleDto role1 = AppRoleDto.builder().id("role1").name("Role One").ordinal(1).build();
        AppRoleDto role2 = AppRoleDto.builder().id("role2").name("Role Two").ordinal(2).build();

        Office office = Office.builder().id(UUID.randomUUID()).code("office1").address(Office.Address.builder().build())
                .build();
        Firm firm = Firm.builder().offices(Set.of(office)).build();
        UserProfile profile = UserProfile.builder().firm(firm).build();

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("admin");

        OfficeDto officeDto = OfficeDto.builder().code("office1").build();

        when(appRoleService.getByIds(List.of("role1", "role2"))).thenReturn(List.of(role1, role2));
        when(loginService.getCurrentProfile(authentication)).thenReturn(profile);
        when(roleAssignmentService.canAssignRole(any(), any())).thenReturn(true);
        when(officeService.getOfficesByIds(List.of("office1"))).thenReturn(List.of(officeDto));
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        when(userService.addMultiFirmUserProfile(any(), any(), any(), any(), any()))
                .thenReturn(UserProfile.builder().id(UUID.randomUUID()).build());

        String view = controller.checkAnswerAndAddProfilePost(authentication, session, model);

        assertThat(view).isEqualTo("redirect:/admin/multi-firm/user/add/profile/confirmation");
        verify(eventService).logEvent(any(AddUserProfileAuditEvent.class));
    }

    @Test
    void shouldUseEntraUserFromSessionAndSetModelAttributes() {
        EntraUserDto user = EntraUserDto.builder().firstName("Alice").lastName("Smith").fullName("Alice Smith").build();
        session.setAttribute("entraUser", user);

        String view = controller.addProfileConfirmation(model, session);

        assertThat(view).isEqualTo("multi-firm-user/add-profile-confirmation");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("User profile created - Alice Smith");
    }

    @Test
    void shouldFallbackToDefaultUserIfMissingInSession() {
        String view = controller.addProfileConfirmation(model, session);

        assertThat(view).isEqualTo("multi-firm-user/add-profile-confirmation");

        EntraUserDto fallbackUser = (EntraUserDto) model.getAttribute("user");
        assertThat(fallbackUser.getFirstName()).isEqualTo("Unknown");
        assertThat(fallbackUser.getLastName()).isEqualTo("Unknown");
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("User profile created - Unknown Unknown");
    }

    @Test
    void shouldClearSessionAttributes() {
        session.setAttribute("entraUser", EntraUserDto.builder().firstName("Alice").lastName("Smith").build());
        session.setAttribute("userOffices", List.of("office1"));
        session.setAttribute("officesForm", new OfficesForm());
        session.setAttribute("addUserProfileAllSelectedRoles", Map.of(0, List.of("role1")));

        controller.addProfileConfirmation(model, session);

        // Assuming clearSessionAttributes removes these keys
        assertThat(session.getAttribute("userOffices")).isNull();
        assertThat(session.getAttribute("officesForm")).isNull();
        assertThat(session.getAttribute("addUserProfileAllSelectedRoles")).isNull();
    }

    @Test
    public void cancelUserProfileCreation_shouldClearAllSessionAttributes() {
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("entraUser", new EntraUserDto());
        testSession.setAttribute("multiFirmUserForm", new MultiFirmUserForm());

        String view = controller.cancelUserProfileCreation(testSession);

        assertThat(view).isEqualTo("redirect:/admin/users");
        assertThat(testSession.getAttribute("user")).isNull();
        assertThat(testSession.getAttribute("userProfile")).isNull();
        assertThat(testSession.getAttribute("firm")).isNull();
        assertThat(testSession.getAttribute("isFirmAdmin")).isNull();
        assertThat(testSession.getAttribute("apps")).isNull();
        assertThat(testSession.getAttribute("roles")).isNull();
        assertThat(testSession.getAttribute("officeData")).isNull();
    }

    @Test
    public void handleAuthorizationException_withAuthorizationDeniedException_redirectsToNotAuthorized() {
        // Arrange
        MockHttpSession mockSession = new MockHttpSession();
        AuthorizationDeniedException authException = new AuthorizationDeniedException("Access denied");
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Act
        RedirectView result = controller.handleAuthorizationException(authException, mockSession, request);

        // Assert
        assertThat(result.getUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void handleAuthorizationException_withAccessDeniedException_redirectsToNotAuthorized() {
        // Arrange
        MockHttpSession mockSession = new MockHttpSession();
        AccessDeniedException accessException = new AccessDeniedException("Access denied");
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Act
        RedirectView result = controller.handleAuthorizationException(accessException, mockSession, request);

        // Assert
        assertThat(result.getUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void handleAuthorizationException_logsWarningMessage() {
        // Arrange
        MockHttpSession mockSession = new MockHttpSession();
        AuthorizationDeniedException authException = new AuthorizationDeniedException("Test access denied");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/admin/users");

        Logger logger = (Logger) LoggerFactory.getLogger(MultiFirmUserController.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            // Act
            controller.handleAuthorizationException(authException, mockSession, request);

            // Assert
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).hasSize(1);
            ILoggingEvent log = logsList.getFirst();

            assertThat(log.getLevel()).isEqualTo(Level.WARN);
            assertThat(log.getMessage()).isEqualTo(
                    "Authorization denied while accessing user: reason='{}', method='{}', uri='{}', referer='{}', savedRequest='{}'");
            assertThat(log.getArgumentArray()).containsExactly("Test access denied", "GET", "/admin/users", null, null);
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    public void whenHandleException_thenRedirectToErrorPage() {

        // Arrange & Act
        RedirectView result = controller.handleException(new Exception());

        // Assert
        assertThat(result.getUrl()).isEqualTo("/error");
    }

    @Test
    public void deleteFirmProfileConfirm_shouldReturnConfirmationView() {
        // Arrange
        String userProfileId = "123e4567-e89b-12d3-a456-426614174000";
        java.util.UUID entraUserId = java.util.UUID.randomUUID();

        uk.gov.justice.laa.portal.landingpage.dto.FirmDto firmDto = uk.gov.justice.laa.portal.landingpage.dto.FirmDto
                .builder()
                .name("Test Law Firm")
                .code("12345")
                .build();

        uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto entraUserDto = uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto
                .builder()
                .id(entraUserId.toString())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .multiFirmUser(true)
                .build();

        uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto userProfileDto = uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto
                .builder()
                .entraUser(entraUserDto)
                .firm(firmDto)
                .activeProfile(true)
                .build();

        when(userService.getUserProfileById(userProfileId)).thenReturn(Optional.of(userProfileDto));

        // Act
        String result = controller.deleteFirmProfileConfirm(userProfileId, model);

        // Assert
        assertThat(result).isEqualTo("multi-firm-user/delete-profile-confirm");
        assertThat(model.getAttribute("userProfile")).isNotNull();
        assertThat(model.getAttribute("user")).isEqualTo(entraUserDto);
    }

    @Test
    public void deleteFirmProfileConfirm_notMultiFirmUser_shouldThrowException() {
        // Arrange
        String userProfileId = "123e4567-e89b-12d3-a456-426614174000";

        uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto entraUserDto = uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto
                .builder()
                .id(java.util.UUID.randomUUID().toString())
                .multiFirmUser(false) // Not multi-firm
                .build();

        uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto userProfileDto = uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto
                .builder()
                .entraUser(entraUserDto)
                .build();

        when(userService.getUserProfileById(userProfileId)).thenReturn(Optional.of(userProfileDto));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> controller.deleteFirmProfileConfirm(userProfileId, model));
    }

    @Test
    public void deleteFirmProfileExecute_withYes_shouldDeleteAndRedirect() {
        // Arrange
        String userProfileId = "123e4567-e89b-12d3-a456-426614174000";
        final String confirm = "yes";
        java.util.UUID actorId = java.util.UUID.randomUUID();

        uk.gov.justice.laa.portal.landingpage.dto.FirmDto firmDto = uk.gov.justice.laa.portal.landingpage.dto.FirmDto
                .builder()
                .name("Test Law Firm")
                .build();

        uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto entraUserDto = uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto
                .builder()
                .id(java.util.UUID.randomUUID().toString())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .multiFirmUser(true)
                .build();

        uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto userProfileDto = uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto
                .builder()
                .entraUser(entraUserDto)
                .firm(firmDto)
                .build();

        uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto currentUserDto = new uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto();
        currentUserDto.setUserId(actorId);
        when(userService.getUserProfileById(userProfileId)).thenReturn(Optional.of(userProfileDto));
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        when(userService.deleteFirmProfile(Mockito.eq(userProfileId), Mockito.eq(actorId))).thenReturn(true);

        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes = Mockito
                .mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

        // Act
        String result = controller.deleteFirmProfileExecute(userProfileId, confirm, authentication, redirectAttributes,
                model);

        // Assert
        assertThat(result).isEqualTo("redirect:/admin/users");
        verify(userService).deleteFirmProfile(Mockito.eq(userProfileId), Mockito.eq(actorId));
        verify(redirectAttributes).addFlashAttribute(Mockito.eq("successMessage"), Mockito.anyString());
    }

    @Test
    public void deleteFirmProfileExecute_withNo_shouldRedirectToManageUser() {
        // Arrange
        String userProfileId = "123e4567-e89b-12d3-a456-426614174000";
        String confirm = "no";

        uk.gov.justice.laa.portal.landingpage.dto.FirmDto firmDto = uk.gov.justice.laa.portal.landingpage.dto.FirmDto
                .builder()
                .name("Test Law Firm")
                .code("ABC123")
                .build();

        uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto entraUserDto = uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto
                .builder()
                .id(java.util.UUID.randomUUID().toString())
                .multiFirmUser(true)
                .build();

        uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto userProfileDto = uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto
                .builder()
                .entraUser(entraUserDto)
                .firm(firmDto)
                .build();

        when(userService.getUserProfileById(userProfileId)).thenReturn(Optional.of(userProfileDto));

        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes = Mockito
                .mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

        // Act
        String result = controller.deleteFirmProfileExecute(userProfileId, confirm, authentication, redirectAttributes,
                model);

        // Assert
        assertThat(result).isEqualTo("redirect:/admin/users/manage/" + userProfileId);
        verify(userService, Mockito.never()).deleteFirmProfile(Mockito.anyString(), Mockito.any());
    }

    // Note: The test for null confirm parameter has been removed because the
    // parameter
    // is now required=true, so Spring will handle missing parameter validation at
    // the
    // framework level before the controller method is invoked.

    @Test
    void shouldIncludeParentAndChildrenWhenNoQuery() {
        Firm child1 = Firm.builder().id(UUID.randomUUID()).name("Child One").code("1001").build();
        Firm child2 = Firm.builder().id(UUID.randomUUID()).name("Child Two").code("1002").build();
        Firm parent = Firm.builder().id(UUID.randomUUID()).name("Parent").code("9999").childFirms(Set.of(child1, child2)).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder().firm(parent).build());
        when(firmService.getFilteredChildFirms(parent, null)).thenReturn(List.of(child1, child2));
        when(firmService.includeParentFirm(parent, null)).thenReturn(true);

        String view = controller.selectDelegateFirm(null, model, session, authentication);

        assertThat(view).isEqualTo("multi-firm-user/select-firm");
        assertThat(model.getAttribute("parentFirm")).isInstanceOf(FirmDto.class);
        assertThat((Boolean) model.getAttribute("includeParent")).isTrue();
        List<?> childList = (List<?>) model.getAttribute("childFirms");
        assertThat(childList).hasSize(2);
    }

    @Test
    void shouldHideParentRowWhenQueryDoesNotMatchParent() {
        Firm child = Firm.builder().id(UUID.randomUUID()).name("Alpha Firm").code("A1").build();
        Firm parent = Firm.builder().id(UUID.randomUUID()).name("Parent").code("9999").childFirms(Set.of(child)).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder().firm(parent).build());
        when(firmService.getFilteredChildFirms(parent, "alpha")).thenReturn(List.of(child));
        when(firmService.includeParentFirm(parent, "alpha")).thenReturn(false);

        String view = controller.selectDelegateFirm("alpha", model, session, authentication);

        assertThat(view).isEqualTo("multi-firm-user/select-firm");
        assertThat((Boolean) model.getAttribute("includeParent")).isFalse();
        List<?> childList = (List<?>) model.getAttribute("childFirms");
        assertThat(childList).hasSize(1);
    }

    @Test
    void shouldStoreSelectionViaChooseAndRedirect() {
        Firm child = Firm.builder().id(UUID.randomUUID()).name("Child One").build();
        Firm parent = Firm.builder().id(UUID.randomUUID()).name("Parent").childFirms(Set.of(child)).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder().firm(parent).build());

        String result = controller.selectDelegateFirmGet(child.getId().toString(), session, authentication);

        assertThat(result).isEqualTo("redirect:/admin/multi-firm/user/add/profile");
        assertThat(session.getAttribute("delegateTargetFirmId")).isEqualTo(child.getId().toString());
    }

    @Test
    void shouldStoreSelectionForParentOrChildAndRejectInvalid() {
        Firm child = Firm.builder().id(UUID.randomUUID()).name("Child One").build();
        Firm parent = Firm.builder().id(UUID.randomUUID()).name("Parent").childFirms(Set.of(child)).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder().firm(parent).build());

        String res1 = controller.selectDelegateFirmPost(child.getId().toString(), session, authentication);
        assertThat(res1).isEqualTo("redirect:/admin/multi-firm/user/add/profile");

        String res2 = controller.selectDelegateFirmPost(parent.getId().toString(), session, authentication);
        assertThat(res2).isEqualTo("redirect:/admin/multi-firm/user/add/profile");

        String res3 = controller.selectDelegateFirmPost(UUID.randomUUID().toString(), session, authentication);
        assertThat(res3).isEqualTo("redirect:/admin/multi-firm/user/add/profile/select/firm");
    }

    @Test
    void shouldSetBackUrlToFirmSelectWhenParentHasChildren() {
        UUID selected = UUID.randomUUID();
        session.setAttribute("delegateTargetFirmId", selected.toString());

        Firm child = Firm.builder().id(selected).name("Child").build();
        Firm parent = Firm.builder().id(UUID.randomUUID()).name("Parent").childFirms(Set.of(child)).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder().firm(parent).build());

        String view = controller.addUserProfile(model, session, authentication);
        assertThat(view).isEqualTo("multi-firm-user/select-user");
        assertThat(model.getAttribute("backUrl")).isEqualTo("/admin/multi-firm/user/add/profile/select/firm");
    }

    @Test
    void shouldSetBackUrlToUsersWhenNoChildren() {
        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder()
                .firm(Firm.builder().build()).build());

        String view = controller.addUserProfile(model, session, authentication);
        assertThat(view).isEqualTo("multi-firm-user/select-user");
        assertThat(model.getAttribute("backUrl")).isEqualTo("/admin/users");
    }

    @Test
    void shouldErrorWhenSelectedFirmAlreadyAssigned() {
        BindingResult result = mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);

        UUID firmId = UUID.randomUUID();
        Firm selectedFirm = Firm.builder().id(firmId).name("Selected Firm").build();
        session.setAttribute("delegateTargetFirmId", firmId.toString());
        when(firmService.getById(firmId)).thenReturn(selectedFirm);
        MultiFirmUserForm form = MultiFirmUserForm.builder().email("user@example.com").build();
        EntraUser entraUser = EntraUser.builder().email(form.getEmail()).multiFirmUser(true)
                .userProfiles(Set.of(UserProfile.builder().firm(selectedFirm).activeProfile(true).build()))
                .build();
        when(userService.findEntraUserByEmail(form.getEmail())).thenReturn(Optional.of(entraUser));

        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder().firm(Firm.builder().build()).build());

        String view = controller.addUserProfilePost(form, result, model, session, authentication);
        assertThat(view).isEqualTo("multi-firm-user/select-user");
        verify(result).rejectValue(eq("email"), eq("error.email"), eq("This user already has a profile for this firm. You can amend their access from the Manage your users table."));
    }

}
