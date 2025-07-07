package uk.gov.justice.laa.portal.landingpage.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
import static org.mockito.Mockito.lenient;
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
import uk.gov.justice.laa.portal.landingpage.forms.EditUserDetailsForm;
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
        List<Office> userOffices = List.of(Office.builder().build());

        when(userService.getEntraUserById(userId)).thenReturn(Optional.of(mockUser));
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(appRoles);
        when(userService.getUserOfficesByUserId(userId)).thenReturn(userOffices);

        // Act
        String view = userController.manageUser(userId, model);

        // Assert
        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isEqualTo(mockUser);
        assertThat(model.getAttribute("userAppRoles")).isEqualTo(appRoles);
        assertThat(model.getAttribute("userOffices")).isEqualTo(userOffices);
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
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("selectedApps", selectedApps);
        when(userService.getAppByAppId(currentApp.getId())).thenReturn(Optional.of(currentApp));
        List<AppRoleDto> allRoles = List.of(testRole1, testRole2, testRole3, testRole4);
        when(userService.getAppRolesByAppId(any())).thenReturn(allRoles);

        // When
        String view = userController.editUserRoles(userId, 0, new RolesForm(), model, testSession);

        // Then
        Assertions.assertEquals("edit-user-roles", view);
        Assertions.assertSame(model.getAttribute("user"), testUser);
    }

    @Test
    public void testEditUserRolesThrowsExceptionWhenNoUserProvided() {
        // Given
        final String userId = "12345";
        when(userService.getEntraUserById(userId)).thenReturn(Optional.empty());
        // When
        Assertions.assertThrows(NoSuchElementException.class, 
            () -> userController.editUserRoles(userId, 0, new RolesForm(), model, session));
    }

    @Test
    public void testUpdateUserRolesReturnsCorrectView() {
        // Given
        final String userId = "12345";
        final RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(List.of("role1"));
        MockHttpSession testSession = new MockHttpSession();
        Model sessionModel = new ExtendedModelMap();
        testSession.setAttribute("userEditRolesModel", sessionModel);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        // When
        String view = userController.updateUserRoles(userId, rolesForm, bindingResult, 0, authentication, model, testSession);

        // Then
        verify(eventService).auditUpdateRole(any(), any(), any());
        verify(loginService).getCurrentUser(authentication);
        Assertions.assertEquals("redirect:/admin/users/manage/" + userId, view);
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
        
        // Check the correct model attribute name "apps"
        assertThat(model.getAttribute("apps")).isNotNull();
        @SuppressWarnings("unchecked")
        List<AppDto> apps = (List<AppDto>) model.getAttribute("apps");
        assertThat(apps).hasSize(1);
        assertThat(apps.get(0).isSelected()).isTrue(); // Should be selected because user has this app
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

    // ===== NEW EDIT USER FUNCTIONALITY TESTS =====

    @Test
    void editUserDetails_shouldPopulateFormAndReturnView() {
        // Given
        String userId = "user123";
        EntraUserDto user = new EntraUserDto();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        
        when(userService.getEntraUserById(userId)).thenReturn(Optional.of(user));
        
        // When
        String view = userController.editUserDetails(userId, model);
        
        // Then
        assertThat(view).isEqualTo("edit-user-details");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        
        EditUserDetailsForm form = (EditUserDetailsForm) model.getAttribute("editUserDetailsForm");
        assertThat(form).isNotNull();
        assertThat(form.getFirstName()).isEqualTo("John");
        assertThat(form.getLastName()).isEqualTo("Doe");
        assertThat(form.getEmail()).isEqualTo("john.doe@example.com");
        
        verify(userService).getEntraUserById(userId);
    }

    @Test
    void editUserDetails_shouldThrowExceptionWhenUserNotFound() {
        // Given
        String userId = "nonexistent";
        when(userService.getEntraUserById(userId)).thenReturn(Optional.empty());
        
        // When & Then
        Assertions.assertThrows(NoSuchElementException.class, 
            () -> userController.editUserDetails(userId, model));
    }

    @Test
    void updateUserDetails_shouldUpdateUserAndRedirect() throws IOException {
        // Given
        final String userId = "user123";
        EditUserDetailsForm form = new EditUserDetailsForm();
        form.setFirstName("Jane");
        form.setLastName("Smith");
        form.setEmail("jane.smith@example.com");
        
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        MockHttpSession testSession = new MockHttpSession();
        
        // When
        String view = userController.updateUserDetails(userId, form, bindingResult, testSession);
        
        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);
        verify(userService).updateUserDetails(userId, "Jane", "Smith", "jane.smith@example.com");
    }

    @Test
    void updateUserDetails_shouldReturnToFormOnValidationErrors() throws IOException {
        // Given
        final String userId = "user123";
        EditUserDetailsForm form = new EditUserDetailsForm();
        form.setFirstName("Jane");
        form.setLastName("Smith");
        form.setEmail("jane.smith@example.com");
        
        EntraUserDto user = new EntraUserDto();
        when(userService.getEntraUserById(userId)).thenReturn(Optional.of(user));
        
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);
        MockHttpSession testSession = new MockHttpSession();
        
        // When
        String view = userController.updateUserDetails(userId, form, bindingResult, testSession);
        
        // Then
        assertThat(view).isEqualTo("edit-user-details");
        assertThat(testSession.getAttribute("user")).isEqualTo(user);
        assertThat(testSession.getAttribute("editUserDetailsForm")).isEqualTo(form);
        verify(userService, Mockito.never()).updateUserDetails(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void editUserApps_shouldPopulateAppsAndReturnView() {
        // Given
        String userId = "user123";
        EntraUserDto user = new EntraUserDto();
        user.setId(userId);
        
        AppDto app1 = new AppDto();
        app1.setId("app1");
        app1.setName("App 1");
        
        AppDto app2 = new AppDto();
        app2.setId("app2");
        app2.setName("App 2");
        
        Set<AppDto> userApps = Set.of(app1);
        List<AppDto> allApps = List.of(app1, app2);
        
        when(userService.getEntraUserById(userId)).thenReturn(Optional.of(user));
        when(userService.getUserAppsByUserId(userId)).thenReturn(userApps);
        when(userService.getApps()).thenReturn(allApps);
        
        // When
        String view = userController.editUserApps(userId, model);
        
        // Then
        assertThat(view).isEqualTo("edit-user-apps");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        
        @SuppressWarnings("unchecked")
        List<AppDto> apps = (List<AppDto>) model.getAttribute("apps");
        assertThat(apps).hasSize(2);
        assertThat(apps.get(0).isSelected()).isTrue(); // app1 should be selected
        assertThat(apps.get(1).isSelected()).isFalse(); // app2 should not be selected
    }

    @Test
    void setSelectedAppsEdit_shouldStoreAppsInSessionAndRedirect() {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440000"; // Valid UUID
        List<String> apps = List.of("app1", "app2");
        MockHttpSession testSession = new MockHttpSession();
        
        // When
        RedirectView redirectView = userController.setSelectedAppsEdit(userId, apps, testSession);
        
        // Then
        assertThat(redirectView.getUrl()).isEqualTo("/admin/users/edit/" + userId + "/roles");
        assertThat(testSession.getAttribute("selectedApps")).isEqualTo(apps);
    }

    @Test
    void editUserOffices_shouldPopulateOfficesAndReturnView() {
        // Given
        String userId = "user123";
        EntraUserDto user = new EntraUserDto();
        user.setId(userId);
        
        Office office1 = Office.builder().id(UUID.randomUUID()).name("Office 1").build();
        Office office2 = Office.builder().id(UUID.randomUUID()).name("Office 2").build();
        List<Office> allOffices = List.of(office1, office2);
        
        List<Office> userOffices = List.of(office1); // User has access to office1 only
        
        when(userService.getEntraUserById(userId)).thenReturn(Optional.of(user));
        when(officeService.getOffices()).thenReturn(allOffices);
        when(userService.getUserOfficesByUserId(userId)).thenReturn(userOffices);
        MockHttpSession testSession = new MockHttpSession();
        
        // When
        String view = userController.editUserOffices(userId, model, testSession);
        
        // Then
        assertThat(view).isEqualTo("edit-user-offices");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("hasAllOffices")).isEqualTo(false);
        
        @SuppressWarnings("unchecked")
        List<OfficeModel> offices = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(offices).hasSize(2);
        assertThat(offices.get(0).isSelected()).isTrue(); // office1 should be selected
        assertThat(offices.get(1).isSelected()).isFalse(); // office2 should not be selected
    }

    @Test
    void editUserOffices_shouldHandleAccessToAllOffices() {
        // Given
        String userId = "user123";
        EntraUserDto user = new EntraUserDto();
        user.setId(userId);
        
        Office office1 = Office.builder().id(UUID.randomUUID()).name("Office 1").build();
        Office office2 = Office.builder().id(UUID.randomUUID()).name("Office 2").build();
        List<Office> allOffices = List.of(office1, office2);
        
        List<Office> userOffices = List.of(office1, office2); // User has access to all offices
        
        when(userService.getEntraUserById(userId)).thenReturn(Optional.of(user));
        when(officeService.getOffices()).thenReturn(allOffices);
        when(userService.getUserOfficesByUserId(userId)).thenReturn(userOffices);
        MockHttpSession testSession = new MockHttpSession();
        
        // When
        String view = userController.editUserOffices(userId, model, testSession);
        
        // Then
        assertThat(view).isEqualTo("edit-user-offices");
        assertThat(model.getAttribute("hasAllOffices")).isEqualTo(true);
        
        @SuppressWarnings("unchecked")
        List<OfficeModel> offices = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(offices).hasSize(2);
        // All offices should be selected when user has access to all
        assertThat(offices.get(0).isSelected()).isTrue();
        assertThat(offices.get(1).isSelected()).isTrue();
    }

    @Test
    void updateUserOffices_shouldUpdateOfficesAndRedirect() throws IOException {
        // Given
        String userId = "user123";
        OfficesForm form = new OfficesForm();
        form.setOffices(List.of("office1", "office2"));
        
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        MockHttpSession testSession = new MockHttpSession();
        
        // When
        String view = userController.updateUserOffices(userId, form, bindingResult, model, testSession);
        
        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);
        verify(userService).updateUserOffices(userId, List.of("office1", "office2"));
    }

    @Test
    void updateUserOffices_shouldHandleAccessToAllOffices() throws IOException {
        // Given
        final String userId = "user123";
        OfficesForm form = new OfficesForm();
        form.setOffices(List.of("ALL")); // Special value for "Access to all offices"
        
        Office office1 = Office.builder().id(UUID.randomUUID()).name("Office 1").build();
        Office office2 = Office.builder().id(UUID.randomUUID()).name("Office 2").build();
        when(officeService.getOffices()).thenReturn(List.of(office1, office2));
        
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        MockHttpSession testSession = new MockHttpSession();
        
        // When
        String view = userController.updateUserOffices(userId, form, bindingResult, model, testSession);
        
        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);
        // Should pass all office IDs to the service
        List<String> expectedOfficeIds = List.of(office1.getId().toString(), office2.getId().toString());
        verify(userService).updateUserOffices(userId, expectedOfficeIds);
    }

    @Test
    void updateUserOffices_shouldReturnToFormOnValidationErrors() throws IOException {
        // Given
        final String userId = "user123";
        OfficesForm form = new OfficesForm();
        form.setOffices(List.of("office1"));
        
        EntraUserDto user = new EntraUserDto();
        List<Office> offices = List.of(Office.builder().build());
        
        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("user", user);
        sessionModel.addAttribute("officeData", offices);
        
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("editUserOfficesModel", sessionModel);
        
        // When
        String view = userController.updateUserOffices(userId, form, bindingResult, model, testSession);
        
        // Then
        assertThat(view).isEqualTo("edit-user-offices");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("officeData")).isEqualTo(offices);
        verify(userService, Mockito.never()).updateUserOffices(anyString(), anyList());
    }

    @Test
    void cancelUserEdit_shouldClearSessionAndRedirect() {
        // Given
        String userId = "user123";
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("selectedApps", List.of("app1"));
        testSession.setAttribute("editUserRoles", List.of("role1"));
        testSession.setAttribute("userEditRolesModel", new ExtendedModelMap());
        testSession.setAttribute("editUserRolesCurrentApp", new AppDto());
        testSession.setAttribute("editUserRolesSelectedAppIndex", 0);
        
        // When
        String view = userController.cancelUserEdit(userId, testSession);
        
        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);
        assertThat(testSession.getAttribute("selectedApps")).isNull();
        assertThat(testSession.getAttribute("editUserRoles")).isNull();
        assertThat(testSession.getAttribute("userEditRolesModel")).isNull();
        assertThat(testSession.getAttribute("editUserRolesCurrentApp")).isNull();
        assertThat(testSession.getAttribute("editUserRolesSelectedAppIndex")).isNull();
    }

    @Test
    void editUserRoles_shouldAutoPopulateSelectedAppsFromUserApps() {
        // Given
        String userId = "user123";
        EntraUserDto user = new EntraUserDto();
        user.setId(userId);
        
        AppDto userApp1 = new AppDto();
        userApp1.setId("app1");
        AppDto userApp2 = new AppDto();
        userApp2.setId("app2");
        Set<AppDto> userApps = new LinkedHashSet<>();
        userApps.add(userApp1);
        userApps.add(userApp2);
        
        AppDto currentApp = new AppDto();
        currentApp.setId("app1");
        currentApp.setName("App 1");
        
        List<AppRoleDto> appRoles = List.of(new AppRoleDto());
        
        when(userService.getEntraUserById(userId)).thenReturn(Optional.of(user));
        when(userService.getUserAppsByUserId(userId)).thenReturn(userApps);
        when(userService.getAppByAppId("app1")).thenReturn(Optional.of(currentApp));
        lenient().when(userService.getAppByAppId("app2")).thenReturn(Optional.of(userApp2));
        when(userService.getAppRolesByAppId("app1")).thenReturn(appRoles);
        lenient().when(userService.getAppRolesByAppId("app2")).thenReturn(List.of());
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(List.of());
        
        MockHttpSession testSession = new MockHttpSession(); // No selectedApps in session
        
        // When
        String view = userController.editUserRoles(userId, 0, new RolesForm(), model, testSession);
        
        // Then
        assertThat(view).isEqualTo("edit-user-roles");
        @SuppressWarnings("unchecked")
        List<String> selectedApps = (List<String>) testSession.getAttribute("selectedApps");
        assertThat(selectedApps).containsExactlyInAnyOrder("app1", "app2");
    }

    @Test
    void updateUserRoles_shouldHandleMultipleAppsNavigation() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440000"; // Valid UUID
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(List.of("role1", "role2"));
        
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("userEditRolesModel", new ExtendedModelMap());
        testSession.setAttribute("selectedApps", List.of("app1", "app2"));
        
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // When - updating roles for first app (index 0)
        String view = userController.updateUserRoles(userId, rolesForm, bindingResult, 0, authentication, model, testSession);
        
        // Then - should redirect to next app (index 1)
        assertThat(view).isEqualTo("redirect:/admin/users/edit/" + userId + "/roles?selectedAppIndex=1");
        
        @SuppressWarnings("unchecked")
        Map<Integer, List<String>> allRoles = (Map<Integer, List<String>>) testSession.getAttribute("editUserAllSelectedRoles");
        assertThat(allRoles).isNotNull();
        assertThat(allRoles.get(0)).containsExactlyInAnyOrder("role1", "role2");
    }

    @Test
    void updateUserRoles_shouldCompleteEditingOnLastApp() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440000"; // Valid UUID
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(List.of("role3"));
        
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("userEditRolesModel", new ExtendedModelMap());
        testSession.setAttribute("selectedApps", List.of("app1", "app2"));
        
        // Simulate roles for previous apps already selected
        Map<Integer, List<String>> existingRoles = new HashMap<>();
        existingRoles.put(0, List.of("role1", "role2"));
        testSession.setAttribute("editUserAllSelectedRoles", existingRoles);
        
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // When - updating roles for last app (index 1)
        String view = userController.updateUserRoles(userId, rolesForm, bindingResult, 1, authentication, model, testSession);
        
        // Then - should complete editing and redirect to manage user
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);
        
        List<String> allSelectedRoles = List.of("role1", "role2", "role3");
        verify(userService).updateUserRoles(userId, allSelectedRoles);
        verify(eventService).auditUpdateRole(any(), any(), eq(allSelectedRoles));
    }

    @Test
    void updateUserRoles_shouldRedirectWhenSessionModelMissing() {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440000"; // Valid UUID
        RolesForm rolesForm = new RolesForm();
        MockHttpSession testSession = new MockHttpSession(); // No session model
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        
        // When
        String view = userController.updateUserRoles(userId, rolesForm, bindingResult, 0, authentication, model, testSession);
        
        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/edit/" + userId + "/roles");
    }
}