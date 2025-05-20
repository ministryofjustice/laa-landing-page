package uk.gov.justice.laa.portal.landingpage.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.graph.models.ServicePrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.microsoft.graph.models.User;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeData;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.ServicePrincipalModel;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.model.UserRole;
import uk.gov.justice.laa.portal.landingpage.service.CreateUserNotificationService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.io.IOException;


@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;
    @Mock
    private HttpSession session;
    @Mock
    private CreateUserNotificationService createUserNotificationService;
    private Model model;

    @BeforeEach
    void setUp() {
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

        Stack<String> pageHistory = new Stack<>();
        when(userService.getPageHistory(session)).thenReturn(pageHistory);
        when(userService.getPaginatedUsersWithHistory(any(), anyInt(), anyString())).thenReturn(paginatedUsers);

        String view = userController.displayAllUsers(10, 1, "nextPageLink", model, session);

        assertThat(view).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(paginatedUsers.getUsers());
        assertThat(model.getAttribute("nextPageLink")).isEqualTo("nextPageLink");
        assertThat(model.getAttribute("previousPageLink")).isEqualTo("previousPageLink");
        assertThat(model.getAttribute("pageSize")).isEqualTo(10);
        assertThat(model.getAttribute("pageHistory")).isEqualTo(pageHistory);
        assertThat(model.getAttribute("page")).isEqualTo(1);
        assertThat(model.getAttribute("totalUsers")).isEqualTo(100);
        assertThat(model.getAttribute("totalPages")).isEqualTo(10);
    }

    @Test
    void editUser() {
        User user = new User();
        user.setDisplayName("Test User");

        when(userService.getUserById(anyString())).thenReturn(user);

        String view = userController.editUser("userId", model);

        assertThat(view).isEqualTo("edit-user");
        assertThat(model.getAttribute("user")).isEqualTo(user);
    }

    @Test
    void givenUsersExist_whenDisplayAllUsers_thenPopulatesModelAndReturnsUsersView() {
        // Arrange
        Stack<String> history = new Stack<>();
        history.push("prevLink456");
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(List.of(new UserModel(), new UserModel()));
        mockPaginatedUsers.setNextPageLink("nextLink123");
        mockPaginatedUsers.setPreviousPageLink("prevLink456");
        when(userService.getPageHistory(session)).thenReturn(history);
        when(userService.getPaginatedUsersWithHistory(eq(history), eq(10), isNull())).thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(10, 1, null, model, session);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(mockPaginatedUsers.getUsers());
        assertThat(model.getAttribute("nextPageLink")).isEqualTo("nextLink123");
        assertThat(model.getAttribute("previousPageLink")).isEqualTo("prevLink456");
        assertThat(model.getAttribute("pageSize")).isEqualTo(10);
        assertThat(model.getAttribute("pageHistory")).isEqualTo(history);
        verify(userService).getPageHistory(session);
        verify(userService).getPaginatedUsersWithHistory(history, 10, null);
    }

    @Test
    void givenNextPageLink_whenDisplayAllUsers_thenUsesLinkAndUpdatesHistory() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(List.of(new UserModel()));
        mockPaginatedUsers.setNextPageLink("nextPageLinkFromServer");
        mockPaginatedUsers.setPreviousPageLink("somePrevLink");
        Stack<String> history = new Stack<>();
        when(userService.getPageHistory(session)).thenReturn(history);
        when(userService.getPaginatedUsersWithHistory(eq(history), eq(10), eq("pageLink")))
                .thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(10, 1, "pageLink", model, session);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("nextPageLink")).isEqualTo("nextPageLinkFromServer");
        verify(userService).getPageHistory(session);
        verify(userService).getPaginatedUsersWithHistory(history, 10, "pageLink");
    }

    @Test
    void givenNoUsers_whenDisplayAllUsers_thenReturnsEmptyListInModel() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(new ArrayList<>());
        mockPaginatedUsers.setNextPageLink(null);
        mockPaginatedUsers.setPreviousPageLink(null);
        Stack<String> history = new Stack<>();

        when(userService.getPageHistory(session)).thenReturn(history);
        when(userService.getPaginatedUsersWithHistory(any(), anyInt(), isNull()))
                .thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(10, 1, null, model, session);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(new ArrayList<>());
        assertThat(model.getAttribute("nextPageLink")).isNull();
        verify(userService).getPaginatedUsersWithHistory(history, 10, null);
    }

    @Test
    void givenValidUserId_whenEditUser_thenFetchesUserAndReturnsEditView() {

        // Arrange
        String userId = "user123";
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setDisplayName("Test User");
        when(userService.getUserById(userId)).thenReturn(mockUser);

        // Act
        String viewName = userController.editUser(userId, model);

        // Assert
        assertThat(viewName).isEqualTo("edit-user");
        assertThat(model.getAttribute("user")).isEqualTo(mockUser);
        verify(userService).getUserById(userId);
    }

    @Test
    void givenInvalidUserId_whenEditUser_thenReturnsEditViewWithErrorOrRedirect() {

        // Arrange
        String userId = "invalid-user";
        when(userService.getUserById(userId)).thenReturn(null);

        // Act
        String viewName = userController.editUser(userId, model);

        // Assert
        assertThat(viewName).isEqualTo("edit-user");
        assertThat(model.getAttribute("user")).isNull();
        verify(userService).getUserById(userId);
    }

    @Test
    void displaySavedUsers() {

        when(userService.getSavedUsers()).thenReturn(new ArrayList<>());
        String view = userController.displaySavedUsers(model);

        assertThat(view).isEqualTo("users");
        assertThat(model.getAttribute("users")).isNotNull();

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
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setDisplayName("Managed User");
        String lastLoggedIn = "2024-06-01T12:00:00Z";
        when(userService.getUserById(userId)).thenReturn(mockUser);
        when(userService.getLastLoggedInByUserId(userId)).thenReturn(lastLoggedIn);

        // Act
        String view = userController.manageUser(userId, model);

        // Assert
        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isEqualTo(mockUser);
        assertThat(model.getAttribute("lastLoggedIn")).isEqualTo(lastLoggedIn);
        verify(userService).getUserById(userId);
        verify(userService).getLastLoggedInByUserId(userId);
    }

    @Test
    void manageUser_whenUserNotFound_shouldAddNullUserAndReturnManageUserView() {
        // Arrange
        String userId = "notfound";
        when(userService.getUserById(userId)).thenReturn(null);
        when(userService.getLastLoggedInByUserId(userId)).thenReturn(null);

        // Act
        String view = userController.manageUser(userId, model);

        // Assert
        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isNull();
        assertThat(model.getAttribute("lastLoggedIn")).isNull();
        verify(userService).getUserById(userId);
        verify(userService).getLastLoggedInByUserId(userId);
    }

    @Test
    void createNewUser() {
        when(session.getAttribute("user")).thenReturn(null);
        String view = userController.createUser(session, model);
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(view).isEqualTo("user/user-details");
    }

    @Test
    void createUserFromSession() {
        User mockUser = new User();
        mockUser.setDisplayName("Test User");
        when(session.getAttribute("user")).thenReturn(mockUser);
        String view = userController.createUser(session, model);
        assertThat(model.getAttribute("user")).isNotNull();
        User sessionUser = (User) session.getAttribute("user");
        assertThat(sessionUser.getDisplayName()).isEqualTo("Test User");
        assertThat(view).isEqualTo("user/user-details");
    }

    @Test
    void postNewUser() {
        HttpSession session = new MockHttpSession();
        RedirectView view = userController.postUser("firstName", "lastName", "email", session);
        User sessionUser = (User) session.getAttribute("user");
        assertThat(sessionUser.getGivenName()).isEqualTo("firstName");
        assertThat(sessionUser.getSurname()).isEqualTo("lastName");
        assertThat(sessionUser.getDisplayName()).isEqualTo("firstName lastName");
        assertThat(sessionUser.getMail()).isEqualTo("email");
        assertThat(view.getUrl()).isEqualTo("/user/create/services");
    }

    @Test
    void postSessionUser() {
        User mockUser = new User();
        mockUser.setDisplayName("Test User");
        HttpSession session = new MockHttpSession();
        session.setAttribute("user", mockUser);
        User sessionUser = (User) session.getAttribute("user");
        RedirectView view = userController.postUser("firstName", "lastName", "email", session);
        assertThat(sessionUser.getGivenName()).isEqualTo("firstName");
        assertThat(sessionUser.getSurname()).isEqualTo("lastName");
        assertThat(sessionUser.getDisplayName()).isEqualTo("firstName lastName");
        assertThat(sessionUser.getMail()).isEqualTo("email");
        assertThat(view.getUrl()).isEqualTo("/user/create/services");
    }

    @Test
    void addUserTwoGet() {
        ServicePrincipal servicePrincipal = new ServicePrincipal();
        servicePrincipal.setAppId("1");
        when(userService.getServicePrincipals()).thenReturn(List.of(servicePrincipal));
        List<String> ids = List.of("1");
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", ids);
        String view = userController.addUserTwo(model, session);
        assertThat(view).isEqualTo("add-user-apps");
        assertThat(model.getAttribute("apps")).isNotNull();
        List<ServicePrincipalModel> modeApps = (List<ServicePrincipalModel>) model.getAttribute("apps");
        assertThat(modeApps.get(0).getServicePrincipal().getAppId()).isEqualTo("1");
        assertThat(modeApps.get(0).isSelected()).isTrue();
    }

    @Test
    void addUserTwoPost() {
        HttpSession session = new MockHttpSession();
        List<String> ids = List.of("1");
        RedirectView view = userController.addUserTwo(ids, session);
        assertThat(session.getAttribute("apps")).isNotNull();
        assertThat(view.getUrl()).isEqualTo("/user/create/roles");
    }

    @Test
    void addUserThreeGet() {
        List<String> selectedApps = new ArrayList<>();
        selectedApps.add("app1");
        List<String> selectedRoles = new ArrayList<>();
        selectedRoles.add("dev");
        List<UserRole> roles = new ArrayList<>();
        UserRole userRole = new UserRole();
        userRole.setAppRoleId("tester");
        UserRole userRole2 = new UserRole();
        userRole2.setAppRoleId("dev");
        roles.add(userRole);
        roles.add(userRole2);
        when(userService.getAllAvailableRolesForApps(eq(selectedApps))).thenReturn(roles);
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", selectedApps);
        session.setAttribute("roles", selectedRoles);
        String view = userController.addUserThree(model, session);
        assertThat(view).isEqualTo("add-user-roles");
        assertThat(model.getAttribute("roles")).isNotNull();
        List<UserRole> sessionRoles = (List<UserRole>) model.getAttribute("roles");
        assertThat(sessionRoles.get(0).isSelected()).isFalse();
        assertThat(sessionRoles.get(1).isSelected()).isTrue();
    }

    @Test
    void addUserThreePost() {
        HttpSession session = new MockHttpSession();
        List<String> roles = List.of("1");
        RedirectView view = userController.addUserThree(roles, session);
        assertThat(session.getAttribute("roles")).isNotNull();
        assertThat(view.getUrl()).isEqualTo("/user/create/offices");
    }

    @Test
    void offices() {
        //TODO
    }

    @Test
    void postOffices() {
        //TODO
    }

    @Test
    void addUserCyaGet() {
        HttpSession session = new MockHttpSession();
        List<String> selectedApps = List.of("app1");
        session.setAttribute("apps", selectedApps);
        UserRole userRole = new UserRole();
        userRole.setAppRoleId("app1-tester");
        userRole.setAppId("app1");
        UserRole userRole2 = new UserRole();
        userRole2.setAppRoleId("app1-dev");
        userRole2.setAppId("app1");
        when(userService.getAllAvailableRolesForApps(eq(selectedApps))).thenReturn(List.of(userRole, userRole2));
        List<String> selectedRoles = List.of("app1-dev");
        session.setAttribute("roles", selectedRoles);
        session.setAttribute("user", new User());
        session.setAttribute("officeData", new OfficeData());
        String view = userController.addUserCya(model, session);
        assertThat(view).isEqualTo("add-user-cya");
        assertThat(model.getAttribute("roles")).isNotNull();
        Map<String, List<UserRole>> cyaRoles =  (Map<String, List<UserRole>>) model.getAttribute("roles");

        assertThat(cyaRoles.get("app1").get(0).getAppRoleId()).isEqualTo("app1-dev");
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("officeData")).isNotNull();
    }

    @Test
    void addUserCyaPost() {
        HttpSession session = new MockHttpSession();
        User user = new User();
        session.setAttribute("user", user);
        List<String> roles = List.of("app1");
        session.setAttribute("roles", roles);
        when(userService.createUser(any(), any(), any())).thenReturn(user);
        List<String> selectedApps = List.of("app1");
        session.setAttribute("apps", selectedApps);
        RedirectView view = userController.addUserCya(session);
        assertThat(view.getUrl()).isEqualTo("/users");
        assertThat(model.getAttribute("roles")).isNull();
        assertThat(model.getAttribute("apps")).isNull();
    }

    @Test
    void addUsercreated() {
        //TODO
    }

}
