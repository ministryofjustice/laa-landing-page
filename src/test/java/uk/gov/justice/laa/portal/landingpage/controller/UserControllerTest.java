package uk.gov.justice.laa.portal.landingpage.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.view.RedirectView;

import com.microsoft.graph.models.User;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeData;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.exception.CreateUserDetailsIncompleteException;
import uk.gov.justice.laa.portal.landingpage.forms.ApplicationsForm;
import uk.gov.justice.laa.portal.landingpage.forms.OfficesForm;
import uk.gov.justice.laa.portal.landingpage.forms.RolesForm;
import uk.gov.justice.laa.portal.landingpage.forms.UserDetailsForm;
import uk.gov.justice.laa.portal.landingpage.model.OfficeModel;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.UserRole;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppViewModel;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private UserController userController;

    @Mock
    private LoginService loginService;
    @Mock
    private UserService userService;
    @Mock
    private OfficeService officeService;
    @Mock
    private EventService eventService;
    @Mock
    private FirmService firmService;
    @Mock
    private HttpSession session;
    @Mock
    private Authentication authentication;

    private Model model;

    @BeforeEach
    void setUp() {
        userController = new UserController(loginService, userService, officeService, eventService, firmService,
                new MapperConfig().modelMapper());
        model = new ExtendedModelMap();
    }

    @Test
    void displayAllUsers() {
        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setUsers(new ArrayList<>());
        paginatedUsers.setNextPageLink("nextPageLink");
        paginatedUsers.setPreviousPageLink("previousPageLink");
        paginatedUsers.setTotalUsers(100);
        paginatedUsers.setTotalPages(10);
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().build());
        when(userService.isInternal(any()))
                .thenReturn(false);
        when(userService.getPageOfUsersByNameOrEmail(any(), anyBoolean(), anyBoolean(), any(), anyInt(), anyInt())).thenReturn(paginatedUsers);

        String view = userController.displayAllUsers(10, 1, null, null, false, model, session, authentication);

        assertThat(view).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(paginatedUsers.getUsers());
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(10);
        assertThat(model.getAttribute("page")).isEqualTo(1);
        assertThat(model.getAttribute("totalUsers")).isEqualTo(100L);
        assertThat(model.getAttribute("totalPages")).isEqualTo(10);
    }

    @Test
    void editUser() {
        EntraUserDto user = new EntraUserDto();
        user.setFullName("Test User");

        when(userService.getEntraUserById(anyString())).thenReturn(Optional.of(user));

        String view = userController.editUser("userId", model);

        assertThat(view).isEqualTo("edit-user");
        assertThat(model.getAttribute("user")).isEqualTo(user);
    }

    @Test
    void givenUsersExist_whenDisplayAllUsers_thenPopulatesModelAndReturnsUsersView() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(List.of(new EntraUserDto(), new EntraUserDto()));
        mockPaginatedUsers.setNextPageLink("nextLink123");
        mockPaginatedUsers.setPreviousPageLink("prevLink456");
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().build());
        when(userService.isInternal(any()))
                .thenReturn(true);
        when(userService.getPageOfUsersByNameOrEmail(any(), anyBoolean(), anyBoolean(), any(), eq(1), eq(10))).thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(10, 1, null, null, false, model, session, authentication);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(mockPaginatedUsers.getUsers());
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(10);
        verify(userService).getPageOfUsersByNameOrEmail(isNull(), eq(false), eq(false), any(), eq(1), eq(10));
    }

    @Test
    void givenNoUsers_whenDisplayAllUsers_thenReturnsEmptyListInModel() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(new ArrayList<>());
        mockPaginatedUsers.setNextPageLink(null);
        mockPaginatedUsers.setPreviousPageLink(null);
        when(userService.getPageOfUsersByNameOrEmail(any(), anyBoolean(), anyBoolean(), any(), anyInt(), anyInt())).thenReturn(mockPaginatedUsers);
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().build());
        when(userService.isInternal(any()))
                .thenReturn(true);
        // Act
        String viewName = userController.displayAllUsers(10, 1, null, null, false, model, session, authentication);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(new ArrayList<>());
        verify(userService).getPageOfUsersByNameOrEmail(null, false, false, null, 1, 10);
    }

    @Test
    void testDisplayAllUsersSearchesUsersWhenSearchTermIsGiven() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(new ArrayList<>());
        when(userService.getPageOfUsersByNameOrEmail(eq("Test"), anyBoolean(), anyBoolean(), any(), anyInt(), anyInt())).thenReturn(mockPaginatedUsers);
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().build());
        when(userService.isInternal(any()))
                .thenReturn(true);
        // Act
        String viewName = userController.displayAllUsers(10, 1, null, "Test", false, model, session, authentication);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(new ArrayList<>());
        verify(userService).getPageOfUsersByNameOrEmail("Test", false, false, null, 1, 10);
    }

    @Test
    void testDisplayAllUsersDoesNotSearchUsersWhenSearchTermIsEmpty() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(new ArrayList<>());
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().build());
        when(userService.isInternal(any()))
                .thenReturn(true);
        when(userService.getPageOfUsersByNameOrEmail(anyString(), anyBoolean(), anyBoolean(), any(), anyInt(), anyInt())).thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(10, 1, null, "", false, model, session, authentication);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(new ArrayList<>());
        verify(userService).getPageOfUsersByNameOrEmail("", false, false, null, 1, 10);
    }

    @Test
    void testSearchPageOfUsersForExternal() {
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(new ArrayList<>());
        when(userService.getPageOfUsersByNameOrEmail(anyString(), anyBoolean(), anyBoolean(), anyList(), anyInt(), anyInt())).thenReturn(mockPaginatedUsers);
        userController.getPageOfUsersForExternal(new ArrayList<>(), "searchTerm", true, 1, 10);
        verify(userService).getPageOfUsersByNameOrEmail(eq("searchTerm"), eq(false), eq(true), anyList(), eq(1), eq(10));
    }

    @Test
    void givenValidUserId_whenEditUser_thenFetchesUserAndReturnsEditView() {

        // Arrange
        String userId = "user123";
        EntraUserDto mockUser = new EntraUserDto();
        mockUser.setId(userId);
        mockUser.setFullName("Test User");
        when(userService.getEntraUserById(userId)).thenReturn(Optional.of(mockUser));

        // Act
        String viewName = userController.editUser(userId, model);

        // Assert
        assertThat(viewName).isEqualTo("edit-user");
        assertThat(model.getAttribute("user")).isEqualTo(mockUser);
        verify(userService).getEntraUserById(userId);
    }

    @Test
    void givenInvalidUserId_whenEditUser_thenReturnsEditViewWithErrorOrRedirect() {

        // Arrange
        String userId = "invalid-user";
        when(userService.getEntraUserById(userId)).thenReturn(Optional.empty());

        // Act
        String viewName = userController.editUser(userId, model);

        verify(userService).getEntraUserById(userId);
    }

    @Test
    void disableUsers() throws IOException {
        List<String> ids = List.of("1", "2", "3");
        String view = userController.disableUsers(ids);
        assertThat(view).isEqualTo("redirect:/users");
    }

    @Test
    void manageUser_shouldAddUserAndLastLoggedInToModelAndReturnManageUserView() {
        // Arrange
        String userId = "user42";
        EntraUserDto mockUser = new EntraUserDto();
        mockUser.setId(userId);
        mockUser.setFullName("Managed User");
        List<AppRoleDto> appRoles = List.of(new AppRoleDto());
        List<Office> offices = List.of(Office.builder().build());

        when(userService.getEntraUserById(userId)).thenReturn(Optional.of(mockUser));
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(appRoles);
        when(officeService.getOffices()).thenReturn(offices);

        // Act
        String view = userController.manageUser(userId, model);

        // Assert
        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isEqualTo(mockUser);
        assertThat(model.getAttribute("userAppRoles")).isEqualTo(appRoles);
        assertThat(model.getAttribute("offices")).isEqualTo(offices);
        verify(userService).getEntraUserById(userId);
    }

    @Test
    void manageUser_whenUserNotFound_shouldAddNullUserAndReturnManageUserView() {
        // Arrange
        String userId = "notfound";
        when(userService.getEntraUserById(userId)).thenReturn(Optional.empty());
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(null);

        // Act
        String view = userController.manageUser(userId, model);

        // Assert
        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isNull();
        assertThat(model.getAttribute("lastLoggedIn")).isNull();
        assertThat(model.getAttribute("appRoles")).isNull();
        verify(userService).getEntraUserById(userId);
        verify(userService).getUserAppRolesByUserId(userId);
    }

    @Test
    void createNewUser() {
        when(session.getAttribute("user")).thenReturn(null);
        when(session.getAttribute("firm")).thenReturn(null);
        FirmDto firm1 = FirmDto.builder().build();
        FirmDto firm2 = FirmDto.builder().build();
        when(firmService.getFirms()).thenReturn(List.of(firm1, firm2));
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        String view = userController.createUser(userDetailsForm, session, model);
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(view).isEqualTo("add-user-details");
    }

    @Test
    void createUserFromSession() {
        User mockUser = new User();
        mockUser.setDisplayName("Test User");
        when(session.getAttribute("user")).thenReturn(mockUser);
        when(session.getAttribute("firm")).thenReturn(FirmDto.builder().name("Test firm").build());
        FirmDto firm1 = FirmDto.builder().build();
        FirmDto firm2 = FirmDto.builder().build();
        when(firmService.getFirms()).thenReturn(List.of(firm1, firm2));
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        String view = userController.createUser(userDetailsForm, session, model);
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("firms")).isNotNull();
        User sessionUser = (User) session.getAttribute("user");
        assertThat(sessionUser.getDisplayName()).isEqualTo("Test User");
        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
        assertThat(selectedFirm.getName()).isEqualTo("Test firm");
        assertThat(view).isEqualTo("add-user-details");
    }

    @Test
    void postNewUser() {
        when(firmService.getFirm(anyString())).thenReturn(FirmDto.builder().name("Test Firm").build());
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setFirstName("firstName");
        userDetailsForm.setLastName("lastName");
        userDetailsForm.setEmail("email");
        userDetailsForm.setFirmId("firmId");
        userDetailsForm.setIsFirmAdmin(false);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        HttpSession session = new MockHttpSession();
        String redirectUrl = userController.postUser(userDetailsForm, bindingResult, "firmId", session, model);
        User sessionUser = (User) session.getAttribute("user");
        assertThat(sessionUser.getGivenName()).isEqualTo("firstName");
        assertThat(sessionUser.getSurname()).isEqualTo("lastName");
        assertThat(sessionUser.getDisplayName()).isEqualTo("firstName lastName");
        assertThat(sessionUser.getMail()).isEqualTo("email");
        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
        assertThat(selectedFirm.getName()).isEqualTo("Test Firm");
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/services");
        assertThat((Boolean) session.getAttribute("isFirmAdmin")).isFalse();
    }

    @Test
    void postSessionUser() {
        User mockUser = new User();
        mockUser.setDisplayName("Test User");
        HttpSession session = new MockHttpSession();
        session.setAttribute("user", mockUser);
        session.setAttribute("firm", FirmDto.builder().name("oldFirm").build());
        when(firmService.getFirm(eq("newFirm"))).thenReturn(FirmDto.builder().name("Test Firm").build());
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setFirstName("firstName");
        userDetailsForm.setLastName("lastName");
        userDetailsForm.setEmail("email");
        userDetailsForm.setFirmId("newFirm");
        userDetailsForm.setIsFirmAdmin(true);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        String redirectUrl = userController.postUser(userDetailsForm, bindingResult, "newFirm", session, model);
        User sessionUser = (User) session.getAttribute("user");
        assertThat(sessionUser.getGivenName()).isEqualTo("firstName");
        assertThat(sessionUser.getSurname()).isEqualTo("lastName");
        assertThat(sessionUser.getDisplayName()).isEqualTo("firstName lastName");
        assertThat(sessionUser.getMail()).isEqualTo("email");
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/services");
        boolean firmAdmin = (Boolean) session.getAttribute("isFirmAdmin");
        assertThat(firmAdmin).isTrue();
        String selectedFirmName = ((FirmDto) session.getAttribute("firm")).getName();
        assertThat(selectedFirmName).isEqualTo("Test Firm");
    }

    @Test
    void selectUserAppsGet() {
        AppDto app = new AppDto();
        app.setId("1");
        when(userService.getApps()).thenReturn(List.of(app));
        List<String> ids = List.of("1");
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", ids);
        session.setAttribute("user", new User());
        ApplicationsForm applicationsForm = new ApplicationsForm();
        String view = userController.selectUserApps(applicationsForm, model, session);
        assertThat(view).isEqualTo("add-user-apps");
        assertThat(model.getAttribute("apps")).isNotNull();
        List<AppViewModel> modeApps = (List<AppViewModel>) model.getAttribute("apps");
        assertThat(modeApps.getFirst().getId()).isEqualTo("1");
        assertThat(modeApps.getFirst().isSelected()).isTrue();
    }

    @Test
    void selectUserAppsGetThrowsExceptionWhenNoUserPresent() {
        AppDto app = new AppDto();
        app.setId("1");
        when(userService.getApps()).thenReturn(List.of(app));
        HttpSession session = new MockHttpSession();
        ApplicationsForm applicationsForm = new ApplicationsForm();
        assertThrows(CreateUserDetailsIncompleteException.class, () -> userController.selectUserApps(applicationsForm, model, session));
    }

    @Test
    void setSelectedAppsPost() {
        HttpSession session = new MockHttpSession();
        List<String> ids = List.of("1");
        ApplicationsForm applicationsForm = new ApplicationsForm();
        applicationsForm.setApps(ids);
        Model model = new ExtendedModelMap();
        String redirectUrl = userController.setSelectedApps(applicationsForm, model, session);
        assertThat(session.getAttribute("apps")).isNotNull();
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/roles");
    }

    @Test
    void getSelectedRolesGet() {
        List<String> selectedApps = new ArrayList<>();
        selectedApps.add("app1");
        List<String> selectedRoles = new ArrayList<>();
        selectedRoles.add("dev");
        List<AppRoleDto> roles = new ArrayList<>();
        AppRoleDto userRole = new AppRoleDto();
        userRole.setId("tester");
        AppRoleDto userRole2 = new AppRoleDto();
        userRole2.setId("dev");
        roles.add(userRole);
        roles.add(userRole2);
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", selectedApps);
        session.setAttribute("roles", selectedRoles);
        session.setAttribute("user", new User());
        when(userService.getAppByAppId(any())).thenReturn(Optional.of(new AppDto()));
        when(userService.getAppRolesByAppId(any())).thenReturn(roles);
        String view = userController.getSelectedRoles(new RolesForm(), model, session);
        assertThat(view).isEqualTo("add-user-roles");
        assertThat(model.getAttribute("roles")).isNotNull();
        List<AppRoleViewModel> sessionRoles = (List<AppRoleViewModel>) model.getAttribute("roles");
        assertThat(sessionRoles.getFirst().isSelected()).isFalse();
        assertThat(sessionRoles.get(1).isSelected()).isTrue();
    }

    @Test
    void getSelectedRolesGetThrowsExceptionWhenNoAppsPresent() {
        assertThrows(CreateUserDetailsIncompleteException.class, () -> userController.getSelectedRoles(new RolesForm(), model, session));
    }

    @Test
    void getSelectedRolesGetThrowsExceptionWhenNoUserPresent() {
        List<String> selectedApps = new ArrayList<>();
        selectedApps.add("app1");
        List<String> selectedRoles = new ArrayList<>();
        selectedRoles.add("dev");
        List<AppRoleDto> roles = new ArrayList<>();
        AppRoleDto userRole = new AppRoleDto();
        userRole.setId("tester");
        AppRoleDto userRole2 = new AppRoleDto();
        userRole2.setId("dev");
        roles.add(userRole);
        roles.add(userRole2);
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", selectedApps);
        session.setAttribute("roles", selectedRoles);
        when(userService.getAppByAppId(any())).thenReturn(Optional.of(new AppDto()));
        when(userService.getAppRolesByAppId(any())).thenReturn(roles);
        assertThrows(CreateUserDetailsIncompleteException.class, () -> userController.getSelectedRoles(new RolesForm(), model, session));
    }

    @Test
    void setSelectedRolesPost() {
        HttpSession session = new MockHttpSession();
        List<String> roles = List.of("1");
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(roles);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        Model model = new ExtendedModelMap();
        model.addAttribute("createUserRolesSelectedAppIndex", 0);
        session.setAttribute("userCreateRolesModel", model);
        String redirectUrl = userController.setSelectedRoles(rolesForm, bindingResult, model, session);
        assertThat(session.getAttribute("roles")).isNotNull();
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/offices");
    }

    @Test
    void testSetSelectedRolesPostRedirectsWhenSessionModelIsNull() {
        HttpSession session = new MockHttpSession();
        List<String> roles = List.of("1");
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(roles);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        Model model = new ExtendedModelMap();
        String redirectUrl = userController.setSelectedRoles(rolesForm, bindingResult, model, session);
        assertThat(session.getAttribute("roles")).isNull();
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/roles");
    }

    @Test
    void testSetSelectedRolesPostRedirectsToNextPage() {
        HttpSession session = new MockHttpSession();
        List<String> roles = List.of("1");
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(roles);
        Model model = new ExtendedModelMap();
        model.addAttribute("createUserRolesSelectedAppIndex", 0);
        session.setAttribute("userCreateRolesModel", model);
        List<String> selectedApps = List.of("app1", "app2");
        session.setAttribute("apps", selectedApps);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        String redirectUrl = userController.setSelectedRoles(rolesForm, bindingResult, model, session);
        assertThat(model.getAttribute("createUserRolesSelectedAppIndex")).isNotNull();
        assertThat(model.getAttribute("createUserRolesSelectedAppIndex")).isEqualTo(1);
        assertThat(session.getAttribute("createUserAllSelectedRoles")).isNotNull();
        Map<Integer, List<String>> allSelectedRolesByPage = (Map<Integer, List<String>>) session
                .getAttribute("createUserAllSelectedRoles");
        assertThat(allSelectedRolesByPage.keySet().size()).isEqualTo(1);
        assertThat(allSelectedRolesByPage.get(0)).isEqualTo(List.of("1"));
        assertThat(session.getAttribute("userCreateRolesModel")).isNotNull();
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/roles");
    }

    @Test
    void offices() {
        HttpSession session = new MockHttpSession();
        UUID officeId = UUID.randomUUID();
        OfficeData officeData = new OfficeData();
        officeData.setSelectedOffices(List.of(officeId.toString()));
        session.setAttribute("officeData", officeData);
        session.setAttribute("user", new User());
        Office office1 = Office.builder().id(officeId).build();
        Office office2 = Office.builder().id(UUID.randomUUID()).build();
        List<Office> dbOffices = List.of(office1, office2);
        when(officeService.getOffices()).thenReturn(dbOffices);
        String view = userController.offices(new OfficesForm(), session, model);
        List<OfficeModel> modelOfficeData = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(modelOfficeData).isNotNull();
        assertThat(modelOfficeData.get(0).isSelected()).isTrue();
        assertThat(modelOfficeData.get(1).isSelected()).isFalse();
        assertThat(view).isEqualTo("add-user-offices");
    }

    @Test
    void postOffices() {
        MockHttpSession mockSession = new MockHttpSession();
        UUID officeId1 = UUID.randomUUID();
        UUID officeId2 = UUID.randomUUID();
        List<String> selectedOffices = List.of(officeId1.toString());
        Office office1 = Office.builder().id(officeId1).name("of1").build();
        Office office2 = Office.builder().id(officeId2).name("of2").build();
        List<Office> dbOffices = List.of(office1, office2);
        when(officeService.getOffices()).thenReturn(dbOffices);

        // Prepare OfficesForm and BindingResult
        OfficesForm officesForm = new OfficesForm();
        officesForm.setOffices(selectedOffices);
        BindingResult bindingResult = Mockito
                .mock(BindingResult.class);

        String redirectUrl = userController.postOffices(officesForm, bindingResult, model, mockSession);
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/check-answers");
        OfficeData modelOfficeData = (OfficeData) mockSession.getAttribute("officeData");
        assertThat(modelOfficeData.getSelectedOffices()).hasSize(1);
        assertThat(modelOfficeData.getSelectedOfficesDisplay()).hasSize(1);
        assertThat(modelOfficeData.getSelectedOffices().get(0)).isEqualTo(officeId1.toString());
        assertThat(modelOfficeData.getSelectedOfficesDisplay().get(0)).isEqualTo("of1");
    }

    @Test
    void addUserCheckAnswersGet() {
        HttpSession session = new MockHttpSession();
        List<String> selectedApps = List.of("app1");
        session.setAttribute("apps", selectedApps);
        AppRoleDto userRole = new AppRoleDto();
        userRole.setId("app1-tester");
        userRole.setName("tester");
        AppDto app1 = new AppDto();
        app1.setId("app1");
        userRole.setApp(app1);
        AppRoleDto userRole2 = new AppRoleDto();
        userRole2.setId("app1-dev");
        userRole2.setName("dev");
        userRole2.setApp(app1);
        Mockito.lenient().when(userService.getAllAvailableRolesForApps(eq(selectedApps)))
                .thenReturn(List.of(userRole, userRole2));
        List<String> selectedRoles = List.of("app1-dev");
        session.setAttribute("roles", selectedRoles);
        session.setAttribute("user", new User());
        session.setAttribute("officeData", new OfficeData());
        String view = userController.getUserCheckAnswers(model, session);
        assertThat(view).isEqualTo("add-user-check-answers");
        assertThat(model.getAttribute("roles")).isNotNull();
        Map<String, List<AppRoleViewModel>> cyaRoles = (Map<String, List<AppRoleViewModel>>) model
                .getAttribute("roles");

        assertThat(cyaRoles.get("app1").getFirst().getId()).isEqualTo("app1-dev");
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("officeData")).isNotNull();
        assertThat(session.getAttribute("displayRoles")).isNotNull();
    }

    @Test
    void addUserCheckAnswersGetThrowsExceptionWhenNoUserPresent() {
        HttpSession session = new MockHttpSession();
        assertThrows(CreateUserDetailsIncompleteException.class, () -> userController.getUserCheckAnswers(model, session));
    }

    @Test
    void addUserCheckAnswersGet_multipleRoles() {
        HttpSession session = new MockHttpSession();
        List<String> selectedApps = List.of("app1");
        session.setAttribute("apps", selectedApps);
        AppRoleDto userRole = new AppRoleDto();
        userRole.setId("app1-tester");
        userRole.setName("tester");
        AppDto app1 = new AppDto();
        app1.setId("app1");
        userRole.setApp(app1);
        AppRoleDto userRole2 = new AppRoleDto();
        userRole2.setId("app1-dev");
        userRole2.setName("dev");
        userRole2.setApp(app1);
        when(userService.getAllAvailableRolesForApps(eq(selectedApps))).thenReturn(List.of(userRole, userRole2));
        List<String> selectedRoles = List.of("app1-dev", "app1-tester");
        session.setAttribute("roles", selectedRoles);
        session.setAttribute("user", new User());
        session.setAttribute("officeData", new OfficeData());
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("tester");
        userController.getUserCheckAnswers(model, session);

        assertThat(session.getAttribute("displayRoles")).isNotNull();
    }

    @Test
    void addUserCheckAnswersGetFirmAdmin() {
        HttpSession session = new MockHttpSession();
        List<String> selectedApps = List.of("app1");
        session.setAttribute("apps", selectedApps);
        AppRoleDto userRole = new AppRoleDto();
        userRole.setId("app1-tester");
        AppDto app1 = new AppDto();
        app1.setId("app1");
        userRole.setApp(app1);
        AppRoleDto userRole2 = new AppRoleDto();
        userRole2.setId("app1-dev");
        userRole2.setApp(app1);
        Mockito.lenient().when(userService.getAllAvailableRolesForApps(eq(selectedApps)))
                .thenReturn(List.of(userRole, userRole2));
        List<String> selectedRoles = List.of("app1-dev");
        session.setAttribute("roles", selectedRoles);
        session.setAttribute("user", new User());
        session.setAttribute("officeData", new OfficeData());
        session.setAttribute("isFirmAdmin", true);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("tester");
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        String redirectUrl = userController.addUserCheckAnswers(session, authentication);
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/confirmation");
    }

    @Test
    void addUserCheckAnswersGet_NoAppsProvided() {
        UserRole userRole = new UserRole();
        userRole.setAppRoleId("app1-tester");
        userRole.setAppId("app1");
        UserRole userRole2 = new UserRole();
        userRole2.setAppRoleId("app1-dev");
        userRole2.setAppId("app1");
        List<String> selectedRoles = List.of("app1-dev");
        HttpSession session = new MockHttpSession();
        session.setAttribute("roles", selectedRoles);
        session.setAttribute("user", new User());
        session.setAttribute("officeData", new OfficeData());
        session.setAttribute("firm", FirmDto.builder().build());
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("tester");
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        String redirectUrl = userController.addUserCheckAnswers(session, authentication);
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/confirmation");
    }

    @Test
    void addUserCheckAnswersPost() {
        HttpSession session = new MockHttpSession();
        User user = new User();
        session.setAttribute("user", user);
        List<String> roles = List.of("dev", "tester");
        session.setAttribute("roles", roles);
        session.setAttribute("displayRoles", "dev, tester");
        List<String> selectedApps = List.of("app1");
        session.setAttribute("apps", selectedApps);
        OfficeData officeData = new OfficeData();
        officeData.setSelectedOfficesDisplay(List.of("of1"));
        officeData.setSelectedOffices(List.of("of1Id"));
        session.setAttribute("officeData", officeData);
        session.setAttribute("firm", FirmDto.builder().id(UUID.randomUUID()).name("test firm").build());
        session.setAttribute("isFirmAdmin", false);
        EntraUser entraUser = EntraUser.builder().build();
        when(userService.createUser(any(), any(), any(), any(), eq(false), any())).thenReturn(entraUser);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("tester");
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        String redirectUrl = userController.addUserCheckAnswers(session, authentication);
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/confirmation");
        assertThat(session.getAttribute("roles")).isNull();
        assertThat(session.getAttribute("apps")).isNull();
        assertThat(session.getAttribute("officeData")).isNull();
        assertThat(session.getAttribute("firm")).isNull();
        verify(eventService).auditUserCreate(currentUserDto, entraUser, "dev, tester", List.of("of1"), "test firm");
    }

    @Test
    void addUserCheckAnswersPost_NoUserProvided() {
        HttpSession session = new MockHttpSession();
        List<String> roles = List.of("app1");
        session.setAttribute("roles", roles);
        List<String> selectedApps = List.of("app1");
        session.setAttribute("apps", selectedApps);
        // Add list appender to logger to verify logs
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserController.class);
        String redirectUrl = userController.addUserCheckAnswers(session, authentication);
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/confirmation");
        assertThat(model.getAttribute("roles")).isNull();
        assertThat(model.getAttribute("apps")).isNull();
        List<ILoggingEvent> logEvents = LogMonitoring.getLogsByLevel(listAppender, Level.ERROR);
        assertThat(logEvents).hasSize(1);
    }

    @Test
    void addUserCreated() {
        HttpSession session = new MockHttpSession();
        User user = new User();
        session.setAttribute("user", user);
        String view = userController.addUserCreated(model, session);
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(view).isEqualTo("add-user-created");
    }

    @Test
    void addUserCreated_NoUserProvided() {
        HttpSession session = new MockHttpSession();
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserController.class);
        String view = userController.addUserCreated(model, session);
        assertThat(model.getAttribute("user")).isNull();
        assertThat(view).isEqualTo("add-user-created");
        List<ILoggingEvent> logEvents = LogMonitoring.getLogsByLevel(listAppender, Level.ERROR);
        assertThat(logEvents).hasSize(1);
    }

    @Test
    public void testEditUserRolesOutputMatchesInput() {
        // Given
        final String userId = "12345";
        // Setup test user call
        EntraUserDto testUser = new EntraUserDto();
        when(userService.getEntraUserById(userId)).thenReturn(Optional.of(testUser));
        // Setup test user roles
        AppRoleDto testUserRole = new AppRoleDto();
        testUserRole.setId("testUserAppRoleId");
        List<AppRoleDto> testUserRoles = List.of(testUserRole);
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(testUserRoles);
        // Setup all available roles
        AppRoleDto testRole1 = new AppRoleDto();
        testRole1.setId("testAppRoleId1");
        AppRoleDto testRole2 = new AppRoleDto();
        testRole2.setId("testAppRoleId2");
        AppRoleDto testRole3 = new AppRoleDto();
        testRole3.setId("testAppRoleId3");
        AppRoleDto testRole4 = new AppRoleDto();
        testRole4.setId("testUserAppRoleId");
        AppDto currentApp = new AppDto();
        currentApp.setId("testAppId");
        currentApp.setName("testAppName");
        List<String> selectedApps = List.of(currentApp.getId());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("selectedApps", selectedApps);
        when(userService.getAppByAppId(currentApp.getId())).thenReturn(Optional.of(currentApp));
        List<AppRoleDto> allRoles = List.of(testRole1, testRole2, testRole3, testRole4);
        when(userService.getAppRolesByAppId(any())).thenReturn(allRoles);

        // When
        String view = userController.editUserRoles(userId, 0, model, session);

        // Then
        Assertions.assertEquals("edit-user-roles", view);
        Assertions.assertSame(model.getAttribute("user"), testUser);
        Assertions.assertSame(allRoles, model.getAttribute("availableRoles"));
        Set<?> userAssignedRoles = (Set<?>) model.getAttribute("userAssignedRoles");
        Assertions.assertNotNull(userAssignedRoles);
        Assertions.assertTrue(userAssignedRoles.stream().findFirst().isPresent());
        String returnedAssignedAppRoleId = (String) userAssignedRoles.stream().findFirst().get();
        Assertions.assertEquals(testUserRole.getId(), returnedAssignedAppRoleId);
    }

    @Test
    public void testEditUserRolesThrowsExceptionWhenNoUserProvided() {
        // Given
        final String userId = "12345";
        when(userService.getEntraUserById(userId)).thenReturn(Optional.empty());
        // When
        assertThrows(NoSuchElementException.class, () -> userController.editUserRoles(userId, 0, model, session));
    }

    @Test
    public void testUpdateUserRolesReturnsCorrectView() {
        // Given
        final String userId = "12345";
        final List<String> selectedRoles = new ArrayList<>();
        MockHttpSession session = new MockHttpSession();

        // When
        RedirectView view = userController.updateUserRoles(userId, selectedRoles, 0, authentication, session);

        // Then
        verify(eventService).auditUpdateRole(any(), any(), any());
        verify(loginService).getCurrentUser(authentication);
        Assertions.assertEquals("/admin/users", view.getUrl());
    }

    @Test
    public void testEditUserAppsReturnsCorrectViewAndAttributes() throws ServletException {
        // Given
        final UUID userId = UUID.randomUUID();
        EntraUserDto testUser = new EntraUserDto();
        testUser.setId(userId.toString());
        testUser.setFullName("Test User");

        UUID appId = UUID.randomUUID();
        AppDto testApp = new AppDto();
        testApp.setId(appId.toString());
        testApp.setName("Test App");

        when(userService.getEntraUserById(userId.toString())).thenReturn(Optional.of(testUser));
        when(userService.getUserAppsByUserId(userId.toString())).thenReturn(Set.of(testApp));
        when(userService.getApps()).thenReturn(List.of(testApp));

        // When
        String view = userController.editUserApps(userId.toString(), model);

        // Then
        assertThat(view).isEqualTo("edit-user-apps");
        assertThat(model.getAttribute("user")).isNotNull();
        EntraUserDto returnedUser = (EntraUserDto) model.getAttribute("user");
        Assertions.assertEquals(testUser.getId(), returnedUser.getId());
        Assertions.assertEquals(testUser.getFullName(), returnedUser.getFullName());
        assertThat(model.getAttribute("userAssignedApps")).isNotNull();
        Set<AppDto> assignedApps = (Set<AppDto>) model.getAttribute("userAssignedApps");
        assertThat(assignedApps).hasSize(1);
        assertThat(model.getAttribute("availableApps")).isNotNull();
        List<AppDto> availableApps = (List<AppDto>) model.getAttribute("availableApps");
        assertThat(availableApps).hasSize(1);
    }

    @Test
    public void testSetSelectedAppsEditReturnsCorrectRedirectAndAttributes() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        List<String> apps = List.of(appId.toString());
        HttpSession session = new MockHttpSession();

        // When
        RedirectView redirectView = userController.setSelectedAppsEdit(userId.toString(), apps, session);

        // Then
        assertThat(redirectView.getUrl()).isEqualTo(String.format("/admin/users/edit/%s/roles", userId));
        assertThat(session.getAttribute("selectedApps")).isNotNull();
        List<String> returnedApps = (List<String>) session.getAttribute("selectedApps");
        assertThat(returnedApps).hasSize(1);
        assertThat(returnedApps.getFirst()).isEqualTo(appId.toString());
    }

    @Test
    public void testSetSelectedAppsEditThrowsExceptionWhenIdIsNotValidUuid() {
        // Given
        String userId = "testUserId";
        HttpSession session = new MockHttpSession();

        // When
        assertThrows(IllegalArgumentException.class,
                () -> userController.setSelectedAppsEdit(userId, List.of(), session));

    }

    @Test
    void postUser_whenEmailAlreadyExists_shouldRejectEmailAndReturnToForm() {
        // Arrange
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setFirstName("John");
        userDetailsForm.setLastName("Doe");
        userDetailsForm.setEmail("existing@email.com");
        userDetailsForm.setFirmId("firmId");
        userDetailsForm.setIsFirmAdmin(false);

        HttpSession session = new MockHttpSession();

        // Simulate user not in session
        session.setAttribute("user", null);

        // Simulate userService returns true for existing email
        when(userService.userExistsByEmail("existing@email.com")).thenReturn(true);

        // Simulate model in session for error handling
        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("firms", List.of());
        sessionModel.addAttribute("selectedFirm", null);
        sessionModel.addAttribute("user", new User());
        session.setAttribute("createUserDetailsModel", sessionModel);

        Model model = new ExtendedModelMap();
        BindingResult bindingResult = Mockito.mock(BindingResult.class);

        // Act
        String view = userController.postUser(userDetailsForm, bindingResult, null, session, model);

        // Assert
        assertThat(view).isEqualTo("redirect:/admin/user/create/services");
        verify(userService).userExistsByEmail("existing@email.com");
        // Should rejectValue on bindingResult for email
        Mockito.verify(bindingResult).rejectValue("email", "error.email",
                "Email address already exists");
    }

    @Test
    void postUser_whenValidationErrors_shouldReturnToFormWithModelAttributes() {
        // Arrange
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setFirstName("Jane");
        userDetailsForm.setLastName("Smith");
        userDetailsForm.setEmail("jane@email.com");
        userDetailsForm.setFirmId("firmId");
        userDetailsForm.setIsFirmAdmin(true);

        HttpSession session = new MockHttpSession();

        // Simulate user not in session
        session.setAttribute("user", null);

        // Simulate userService returns false for existing email
        when(userService.userExistsByEmail("jane@email.com")).thenReturn(false);

        // Simulate validation errors
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        // Simulate model in session for error handling
        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("firms", List.of());
        sessionModel.addAttribute("selectedFirm", null);
        sessionModel.addAttribute("user", new User());
        session.setAttribute("createUserDetailsModel", sessionModel);

        Model model = new ExtendedModelMap();

        // Act
        String view = userController.postUser(userDetailsForm, bindingResult, null, session, model);

        // Assert
        assertThat(view).isEqualTo("add-user-details");
        assertThat(model.getAttribute("firms")).isNotNull();
        assertThat(model.getAttribute("selectedFirm")).isNull();
        assertThat(model.getAttribute("user")).isNotNull();
    }

    @Test
    void postUser_whenNoModelInSessionAndValidationErrors_shouldRedirectToDetails() {
        // Arrange
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setFirstName("Jane");
        userDetailsForm.setLastName("Smith");
        userDetailsForm.setEmail("jane@email.com");
        userDetailsForm.setFirmId("firmId");
        userDetailsForm.setIsFirmAdmin(true);

        HttpSession session = new MockHttpSession();

        // Simulate user not in session
        session.setAttribute("user", null);

        // Simulate userService returns false for existing email
        when(userService.userExistsByEmail("jane@email.com")).thenReturn(false);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);

        // Simulate validation errors
        when(bindingResult.hasErrors()).thenReturn(true);

        // No model in session
        session.removeAttribute("createUserDetailsModel");

        Model model = new ExtendedModelMap();

        // Act
        String view = userController.postUser(userDetailsForm, bindingResult, null, session, model);

        // Assert
        assertThat(view).isEqualTo("redirect:/admin/user/create/details");
    }

    @Test
    void postUser_whenValid_shouldSetSessionAttributesAndRedirect() {
        // Arrange
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setFirstName("Jane");
        userDetailsForm.setLastName("Smith");
        userDetailsForm.setEmail("jane@email.com");
        userDetailsForm.setFirmId("firmId");
        userDetailsForm.setIsFirmAdmin(true);

        HttpSession session = new MockHttpSession();

        // Simulate user not in session
        session.setAttribute("user", null);

        // Simulate userService returns false for existing email
        when(userService.userExistsByEmail("jane@email.com")).thenReturn(false);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);

        // Simulate no validation errors
        when(bindingResult.hasErrors()).thenReturn(false);

        // Simulate firmService returns a firm
        FirmDto firm = FirmDto.builder().name("Test Firm").build();
        when(firmService.getFirm("firmId")).thenReturn(firm);

        // Simulate model in session for error handling (should be cleared)
        Model sessionModel = new ExtendedModelMap();
        session.setAttribute("createUserDetailsModel", sessionModel);

        Model model = new ExtendedModelMap();

        // Act
        String view = userController.postUser(userDetailsForm, bindingResult, null, session, model);

        // Assert
        assertThat(view).isEqualTo("redirect:/admin/user/create/services");
        User sessionUser = (User) session.getAttribute("user");
        assertThat(sessionUser.getGivenName()).isEqualTo("Jane");
        assertThat(sessionUser.getSurname()).isEqualTo("Smith");
        assertThat(sessionUser.getDisplayName()).isEqualTo("Jane Smith");
        assertThat(sessionUser.getMail()).isEqualTo("jane@email.com");
        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
        assertThat(selectedFirm.getName()).isEqualTo("Test Firm");
        assertThat(session.getAttribute("isFirmAdmin")).isEqualTo(true);
        assertThat(session.getAttribute("createUserDetailsModel")).isNull();
    }

    @Test
    void displayAllUsers_shouldAddSuccessMessageToModelAndRemoveFromSession() {
        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setUsers(new ArrayList<>());
        paginatedUsers.setTotalUsers(1);
        paginatedUsers.setTotalPages(1);

        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().build());
        when(userService.isInternal(any()))
                .thenReturn(true);
        when(userService.getPageOfUsersByNameOrEmail(any(), anyBoolean(), anyBoolean(), any(), anyInt(), anyInt())).thenReturn(paginatedUsers);
        when(session.getAttribute("successMessage")).thenReturn("User added successfully");

        String view = userController.displayAllUsers(10, 1, null, null, false, model, session, authentication);

        assertThat(view).isEqualTo("users");
        assertThat(model.getAttribute("successMessage")).isEqualTo("User added successfully");
        verify(session).removeAttribute("successMessage");
    }

    @Test
    void editUser_shouldAddUserAndRolesToModelIfPresent() {
        EntraUserDto user = new EntraUserDto();
        user.setId("id1");
        List<AppRoleDto> roles = List.of(new AppRoleDto());
        when(userService.getEntraUserById("id1")).thenReturn(Optional.of(user));
        when(userService.getUserAppRolesByUserId("id1")).thenReturn(roles);

        String view = userController.editUser("id1", model);

        assertThat(view).isEqualTo("edit-user");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("roles")).isEqualTo(roles);
    }

    @Test
    void editUser_shouldNotAddUserOrRolesIfNotPresent() {
        when(userService.getEntraUserById("id2")).thenReturn(Optional.empty());

        String view = userController.editUser("id2", model);

        assertThat(view).isEqualTo("edit-user");
        assertThat(model.getAttribute("user")).isNull();
        assertThat(model.getAttribute("roles")).isNull();
    }

    @Test
    void disableUsers_shouldCallServiceAndRedirect() throws IOException {
        List<String> ids = List.of("id1", "id2");
        String view = userController.disableUsers(ids);
        assertThat(view).isEqualTo("redirect:/users");
        verify(userService).disableUsers(ids);
    }

    @Test
    void manageUser_shouldAddUserAppRolesAndOfficesToModel() {
        EntraUserDto user = new EntraUserDto();
        user.setId("id1");
        List<AppRoleDto> roles = List.of(new AppRoleDto());
        List<Office> offices = List.of(Office.builder().build());
        when(userService.getEntraUserById("id1")).thenReturn(Optional.of(user));
        when(userService.getUserAppRolesByUserId("id1")).thenReturn(roles);
        when(officeService.getOffices()).thenReturn(offices);

        String view = userController.manageUser("id1", model);

        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("userAppRoles")).isEqualTo(roles);
        assertThat(model.getAttribute("offices")).isEqualTo(offices);
    }

    @Test
    void manageUser_shouldHandleUserNotPresent() {
        when(userService.getEntraUserById("id2")).thenReturn(Optional.empty());
        when(userService.getUserAppRolesByUserId("id2")).thenReturn(List.of());
        when(officeService.getOffices()).thenReturn(List.of());

        String view = userController.manageUser("id2", model);

        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isNull();
        assertThat(model.getAttribute("userAppRoles")).isEqualTo(List.of());
        assertThat(model.getAttribute("offices")).isEqualTo(List.of());
    }

    @Test
    void createUser_shouldPopulateModelWithFirmsAndSelectedFirm() {
        when(session.getAttribute("user")).thenReturn(null);
        when(session.getAttribute("firm")).thenReturn(null);
        List<FirmDto> firms = List.of(FirmDto.builder().id(UUID.randomUUID()).build());
        when(firmService.getFirms()).thenReturn(firms);

        UserDetailsForm form = new UserDetailsForm();
        String view = userController.createUser(form, session, model);

        assertThat(view).isEqualTo("add-user-details");
        assertThat(model.getAttribute("firms")).isEqualTo(firms);
        assertThat(model.getAttribute("selectedFirm")).isNull();
        assertThat(model.getAttribute("user")).isNotNull();
    }

    @Test
    void postUser_shouldHandleValidationErrors() {
        BindingResult result = Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("firms", List.of());
        sessionModel.addAttribute("selectedFirm", null);
        sessionModel.addAttribute("user", new User());
        Mockito.lenient().when(session.getAttribute("createUserDetailsModel")).thenReturn(sessionModel);

        final Model model = new ExtendedModelMap();
        UserDetailsForm form = new UserDetailsForm();
        String view = userController.postUser(form, result, null, session, model);

        assertThat(view).isEqualTo("add-user-details");
        assertThat(model.getAttribute("firms")).isNotNull();
        assertThat(model.getAttribute("user")).isNotNull();
    }

    @Test
    void postUser_shouldRedirectOnNoValidationErrors() {
        BindingResult result = Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);
        when(firmService.getFirm(anyString())).thenReturn(FirmDto.builder().id(UUID.randomUUID()).build());
        UserDetailsForm form = new UserDetailsForm();
        form.setFirstName("A");
        form.setLastName("B");
        form.setEmail("a@b.com");
        form.setFirmId("firmId");
        form.setIsFirmAdmin(true);

        final Model model = new ExtendedModelMap();
        HttpSession session = new MockHttpSession();

        String view = userController.postUser(form, result, "true", session, model);

        assertThat(view).isEqualTo("redirect:/admin/user/create/services");
        assertThat(session.getAttribute("firm")).isNotNull();
        assertThat(session.getAttribute("isFirmAdmin")).isEqualTo(true);
    }

    @Test
    void selectUserApps_shouldAddAppsAndUserToModel() {
        AppDto appDto = new AppDto();
        appDto.setId("app1");
        when(userService.getApps()).thenReturn(List.of(appDto));
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", List.of("app1"));
        session.setAttribute("user", new User());
        ApplicationsForm form = new ApplicationsForm();

        String view = userController.selectUserApps(form, model, session);

        assertThat(view).isEqualTo("add-user-apps");
        assertThat(model.getAttribute("apps")).isNotNull();
        assertThat(model.getAttribute("user")).isNotNull();
    }

    @Test
    void setSelectedApps_shouldRedirectToCheckAnswersIfNoApps() {
        ApplicationsForm form = new ApplicationsForm();
        form.setApps(new ArrayList<>());
        HttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = userController.setSelectedApps(form, model, session);

        assertThat(view).isEqualTo("redirect:/admin/user/create/check-answers");
    }

    @Test
    void setSelectedApps_shouldSetAppsAndRedirectToRoles() {
        ApplicationsForm form = new ApplicationsForm();
        form.setApps(List.of("app1"));
        HttpSession session = new MockHttpSession();
        final Model model = new ExtendedModelMap();

        String view = userController.setSelectedApps(form, model, session);

        assertThat(view).isEqualTo("redirect:/admin/user/create/roles");
        assertThat(session.getAttribute("apps")).isEqualTo(List.of("app1"));
    }

    @Test
    void setSelectedRoles_shouldHandleValidationErrors() {
        BindingResult result = Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);

        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("roles", List.of());
        sessionModel.addAttribute("user", new User());
        HttpSession session = new MockHttpSession();
        session.setAttribute("userCreateRolesModel", sessionModel);

        RolesForm form = new RolesForm();
        Model model = new ExtendedModelMap();
        String view = userController.setSelectedRoles(form, result, model, session);

        assertThat(view).isEqualTo("add-user-roles");
        assertThat(model.getAttribute("roles")).isNotNull();
        assertThat(model.getAttribute("user")).isNotNull();
    }

    @Test
    void setSelectedRoles_shouldSetRolesAndRedirect() {
        BindingResult result = Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);
        HttpSession session = new MockHttpSession();
        RolesForm form = new RolesForm();
        form.setRoles(List.of("role1"));
        final Model model = new ExtendedModelMap();
        model.addAttribute("createUserRolesSelectedAppIndex", 0);
        session.setAttribute("userCreateRolesModel", model);

        String view = userController.setSelectedRoles(form, result, model, session);

        assertThat(view).isEqualTo("redirect:/admin/user/create/offices");
        assertThat(session.getAttribute("roles")).isEqualTo(List.of("role1"));
    }

    @Test
    void offices_shouldAddOfficeDataAndUserToModel() {
        Office office = Office.builder().id(UUID.randomUUID()).name("Office1").build();
        when(officeService.getOffices()).thenReturn(List.of(office));
        HttpSession session = new MockHttpSession();
        session.setAttribute("user", new User());
        OfficesForm form = new OfficesForm();

        String view = userController.offices(form, session, model);

        assertThat(view).isEqualTo("add-user-offices");
        assertThat(model.getAttribute("officeData")).isNotNull();
        assertThat(model.getAttribute("user")).isNotNull();
    }

    @Test
    void offices_shouldThrowExceptionWhenUserIsNotPresent() {
        Office office = Office.builder().id(UUID.randomUUID()).name("Office1").build();
        when(officeService.getOffices()).thenReturn(List.of(office));
        HttpSession session = new MockHttpSession();
        OfficesForm form = new OfficesForm();

        assertThrows(CreateUserDetailsIncompleteException.class, () -> userController.offices(form, session, model));
    }

    @Test
    void postOffices_shouldHandleValidationErrors() {
        BindingResult result = Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("user", new User());
        sessionModel.addAttribute("officeData", List.of());
        HttpSession session = new MockHttpSession();
        session.setAttribute("createUserOfficesModel", sessionModel);

        final Model model = new ExtendedModelMap();
        OfficesForm form = new OfficesForm();

        String view = userController.postOffices(form, result, model, session);

        assertThat(view).isEqualTo("add-user-offices");
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("officeData")).isNotNull();
    }

    @Test
    void postOffices_shouldSetOfficeDataAndRedirect() {
        Office office = Office.builder().id(UUID.randomUUID()).name("Office1").build();
        when(officeService.getOffices()).thenReturn(List.of(office));
        BindingResult result = Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);
        OfficesForm form = new OfficesForm();
        form.setOffices(List.of(office.getId().toString()));
        HttpSession session = new MockHttpSession();
        final Model model = new ExtendedModelMap();

        String view = userController.postOffices(form, result, model, session);

        assertThat(view).isEqualTo("redirect:/admin/user/create/check-answers");
        OfficeData officeData = (OfficeData) session.getAttribute("officeData");
        assertThat(officeData.getSelectedOffices()).containsExactly(office.getId().toString());
        assertThat(officeData.getSelectedOfficesDisplay()).containsExactly("Office1");
    }

    @Test
    void getUserCheckAnswers_shouldAddAttributesToModel() {
        AppRoleDto role = new AppRoleDto();
        role.setId("role1");
        AppDto app = new AppDto();
        app.setId("app1");
        role.setApp(app);
        List<String> selectedApps = List.of("app1");
        when(userService.getAllAvailableRolesForApps(selectedApps)).thenReturn(List.of(role));
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", selectedApps);
        session.setAttribute("roles", List.of("role1"));
        session.setAttribute("user", new User());
        session.setAttribute("officeData", new OfficeData());
        session.setAttribute("firm", FirmDto.builder().build());
        session.setAttribute("isFirmAdmin", true);

        String view = userController.getUserCheckAnswers(model, session);

        assertThat(view).isEqualTo("add-user-check-answers");
        assertThat(model.getAttribute("roles")).isNotNull();
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("officeData")).isNotNull();
        assertThat(model.getAttribute("firm")).isNotNull();
        assertThat(model.getAttribute("isFirmAdmin")).isEqualTo(true);
    }

    @Test
    void addUserCheckAnswers_shouldCallCreateUserAndRedirect() {
        HttpSession session = new MockHttpSession();
        User user = new User();
        session.setAttribute("user", user);
        session.setAttribute("roles", List.of("role1"));
        session.setAttribute("firm", FirmDto.builder().build());
        session.setAttribute("isFirmAdmin", true);
        session.setAttribute("officeData", new OfficeData());
        EntraUser entraUser = EntraUser.builder().build();
        when(userService.createUser(any(), any(), any(), any(), eq(true), any())).thenReturn(entraUser);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("tester");
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        String redirectUrl = userController.addUserCheckAnswers(session, authentication);

        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/confirmation");
        assertThat(session.getAttribute("user")).isNotNull();
        assertThat(session.getAttribute("firm")).isNull();
        assertThat(session.getAttribute("roles")).isNull();
        assertThat(session.getAttribute("officeData")).isNull();
    }

    @Test
    void addUserCheckAnswers_shouldLogErrorIfNoUser() {
        HttpSession session = new MockHttpSession();
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserController.class);

        String redirectUrl = userController.addUserCheckAnswers(session, authentication);

        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/confirmation");
        List<ILoggingEvent> logEvents = LogMonitoring.getLogsByLevel(listAppender, Level.ERROR);
        assertThat(logEvents).hasSize(1);
        assertThat(logEvents.getFirst().getFormattedMessage()).contains("No user attribute was present in request");
    }

    @Test
    void addUserCreated_shouldAddUserToModelIfPresent() {
        HttpSession session = new MockHttpSession();
        User user = new User();
        user.setGivenName("Chris");
        user.setSurname("Newman");
        session.setAttribute("user", user);
        Model model = new ExtendedModelMap();
        String view = userController.addUserCreated(model, session);
        assertThat(view).isEqualTo("add-user-created");
        assertThat(model.getAttribute("user")).isNotNull();
        User modelUser = (User) model.getAttribute("user");
        assertThat(modelUser.getGivenName()).isEqualTo("Chris");
        assertThat(modelUser.getSurname()).isEqualTo("Newman");
    }

    @Test
    void addUserCreated_shouldLogErrorIfNoUser() {
        HttpSession session = new MockHttpSession();
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserController.class);

        String view = userController.addUserCreated(model, session);

        assertThat(view).isEqualTo("add-user-created");
        assertThat(model.getAttribute("user")).isNull();
        List<ILoggingEvent> logEvents = LogMonitoring.getLogsByLevel(listAppender, Level.ERROR);
        assertThat(logEvents).hasSize(1);
        assertThat(logEvents.getFirst().getFormattedMessage()).contains("No user attribute was present in request");
    }

    @Test
    void editUserRoles_shouldAddAttributesToModel() {
        String id = "id1";
        EntraUserDto user = new EntraUserDto();
        List<AppRoleDto> userRoles = List.of(new AppRoleDto());
        List<AppRoleDto> availableRoles = List.of(new AppRoleDto());
        when(userService.getEntraUserById(id)).thenReturn(Optional.of(user));
        when(userService.getUserAppRolesByUserId(id)).thenReturn(userRoles);
        when(userService.getAppRolesByAppId(any())).thenReturn(availableRoles);
        HttpSession session = new MockHttpSession();
        AppDto currentApp = new AppDto();
        currentApp.setId("testAppId");
        currentApp.setName("testAppName");
        List<String> selectedApps = List.of(currentApp.getId());
        session.setAttribute("selectedApps", selectedApps);
        when(userService.getAppByAppId(currentApp.getId())).thenReturn(Optional.of(currentApp));

        String view = userController.editUserRoles(id, 0, model, session);

        assertThat(view).isEqualTo("edit-user-roles");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("availableRoles")).isEqualTo(availableRoles);
        assertThat(model.getAttribute("userAssignedRoles")).isNotNull();
    }

    @Test
    void updateUserRoles_shouldCallServiceAndRedirect() {
        String id = "id1";
        List<String> roles = List.of("role1");
        MockHttpSession session = new MockHttpSession();

        RedirectView view = userController.updateUserRoles(id, roles, 0, authentication, session);

        assertThat(view.getUrl()).isEqualTo("/admin/users");
        verify(userService).updateUserRoles(id, roles);
    }

    @Test
    void editUserApps_shouldAddAttributesToModel() {
        String id = "id1";
        EntraUserDto user = new EntraUserDto();
        Set<AppDto> assignedApps = Set.of(new AppDto());
        List<AppDto> availableApps = List.of(new AppDto());
        when(userService.getEntraUserById(id)).thenReturn(Optional.of(user));
        when(userService.getUserAppsByUserId(id)).thenReturn(assignedApps);
        when(userService.getApps()).thenReturn(availableApps);

        String view = userController.editUserApps(id, model);

        assertThat(view).isEqualTo("edit-user-apps");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("userAssignedApps")).isEqualTo(assignedApps);
        assertThat(model.getAttribute("availableApps")).isEqualTo(availableApps);
    }

    @Test
    void setSelectedAppsEdit_shouldSetSelectedAppsAndRedirect() {
        UUID userId = UUID.randomUUID();
        List<String> apps = List.of("app1");
        HttpSession session = new MockHttpSession();

        RedirectView view = userController.setSelectedAppsEdit(userId.toString(), apps, session);

        assertThat(view.getUrl()).isEqualTo(String.format("/admin/users/edit/%s/roles", userId));
        assertThat(session.getAttribute("selectedApps")).isEqualTo(apps);
    }

    @Test
    void setSelectedAppsEdit_shouldThrowExceptionForInvalidUuid() {
        String invalidId = "not-a-uuid";
        HttpSession session = new MockHttpSession();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> userController.setSelectedAppsEdit(invalidId, List.of("app1"), session));
        assertThat(thrown).isNotNull();
    }

    @Test
    public void testThatUpdateRolesRedirectsToNextIndex() {
        UUID role1Id = UUID.randomUUID();
        UUID role2Id = UUID.randomUUID();
        AppRoleDto role1 = new AppRoleDto();
        role1.setId(role1Id.toString());
        role1.setName("role1");
        AppRoleDto role2 = new AppRoleDto();
        role2.setId(role2Id.toString());
        role2.setName("role2");
        List<String> selectedRoles = List.of(role1.getId(), role2.getId());
        List<String> selectedApps = List.of("app1", "app2");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("selectedApps", selectedApps);
        UUID userId = UUID.randomUUID();
        RedirectView view = userController.updateUserRoles(userId.toString(), selectedRoles, 0, authentication,
                session);
        Map<Integer, String> allSelectedRoles = (Map<Integer, String>) session.getAttribute("editUserAllSelectedRoles");
        assertThat(allSelectedRoles).isNotNull();
        assertThat(allSelectedRoles.keySet()).hasSize(1);
        assertThat(view.getUrl()).isEqualTo(String.format("/admin/users/edit/%s/roles?selectedAppIndex=%d", userId, 1));
    }

    @Test
    void cancelUserCreation_shouldClearSessionAttributesAndRedirect() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", new Object());
        session.setAttribute("firm", new Object());
        session.setAttribute("isFirmAdmin", true);
        session.setAttribute("apps", List.of("app1"));
        session.setAttribute("roles", List.of("role1"));
        session.setAttribute("officeData", new Object());

        String result = userController.cancelUserCreation(session);

        assertThat(result).isEqualTo("redirect:/admin/users");
        assertThat(session.getAttribute("user")).isNull();
        assertThat(session.getAttribute("firm")).isNull();
        assertThat(session.getAttribute("isFirmAdmin")).isNull();
        assertThat(session.getAttribute("apps")).isNull();
        assertThat(session.getAttribute("roles")).isNull();
        assertThat(session.getAttribute("officeData")).isNull();
    }

}