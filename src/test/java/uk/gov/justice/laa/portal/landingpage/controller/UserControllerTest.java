package uk.gov.justice.laa.portal.landingpage.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.models.User;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeData;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.model.OfficeModel;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.ServicePrincipalModel;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.model.UserRole;
import uk.gov.justice.laa.portal.landingpage.service.NotificationService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppViewModel;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private UserController userController;

    @Mock
    private UserService userService;
    @Mock
    private OfficeService officeService;
    @Mock
    private HttpSession session;

    private Model model;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService, officeService, new MapperConfig().modelMapper());
        model = new ExtendedModelMap();
    }

    @Test
    void displayAllUsers() {
        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setUsers(new ArrayList<>());
        paginatedUsers.setNextPageLink("nextPageLink");
        paginatedUsers.setPreviousPageLink("previousPageLink");
        paginatedUsers.setTotalUsers(100);
        HttpSession session = new MockHttpSession();

        when(userService.getPaginatedUsers(anyInt(), anyInt(), any(HttpSession.class))).thenReturn(paginatedUsers);

        String view = userController.displayAllUsers(10, 1, model, session);

        assertThat(view).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(paginatedUsers.getUsers());
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(10);
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
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(List.of(new UserModel(), new UserModel()));
        mockPaginatedUsers.setNextPageLink("nextLink123");
        mockPaginatedUsers.setPreviousPageLink("prevLink456");
        HttpSession session = new MockHttpSession();
        when(userService.getPaginatedUsers(eq(1), eq(10), any(HttpSession.class))).thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(10, 1, model, session);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(mockPaginatedUsers.getUsers());
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(10);
        verify(userService).getPaginatedUsers(1, 10, session);
    }

    @Test
    void givenNoUsers_whenDisplayAllUsers_thenReturnsEmptyListInModel() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(new ArrayList<>());
        mockPaginatedUsers.setNextPageLink(null);
        mockPaginatedUsers.setPreviousPageLink(null);
        HttpSession session = new MockHttpSession();
        when(userService.getPaginatedUsers(anyInt(), anyInt(), any(HttpSession.class))).thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(10, 1, model, session);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(new ArrayList<>());
        verify(userService).getPaginatedUsers(1, 10, session);
    }

    @Test
    void testDisplayAllUsersDefaultsToPageOneWhenNoPageIsGiven() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(List.of(new UserModel(), new UserModel()));
        HttpSession session = new MockHttpSession();
        when(userService.getPaginatedUsers(eq(1), eq(10), any(HttpSession.class))).thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(10, null, model, session);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(mockPaginatedUsers.getUsers());
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(10);
        verify(userService).getPaginatedUsers(1, 10, session);
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
        List<AppRoleDto> appRoles = List.of(new AppRoleDto());
        List<Office> offices = List.of(Office.builder().build());

        when(userService.getUserById(userId)).thenReturn(mockUser);
        when(userService.getLastLoggedInByUserId(userId)).thenReturn(lastLoggedIn);
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(appRoles);
        when(officeService.getOffices()).thenReturn(offices);

        // Act
        String view = userController.manageUser(userId, model);

        // Assert
        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isEqualTo(mockUser);
        assertThat(model.getAttribute("lastLoggedIn")).isEqualTo(lastLoggedIn);
        assertThat(model.getAttribute("userAppRoles")).isEqualTo(appRoles);
        assertThat(model.getAttribute("offices")).isEqualTo(offices);
        verify(userService).getUserById(userId);
        verify(userService).getLastLoggedInByUserId(userId);
    }

    @Test
    void manageUser_whenUserNotFound_shouldAddNullUserAndReturnManageUserView() {
        // Arrange
        String userId = "notfound";
        when(userService.getUserById(userId)).thenReturn(null);
        when(userService.getLastLoggedInByUserId(userId)).thenReturn(null);
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(null);

        // Act
        String view = userController.manageUser(userId, model);

        // Assert
        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isNull();
        assertThat(model.getAttribute("lastLoggedIn")).isNull();
        assertThat(model.getAttribute("appRoles")).isNull();
        verify(userService).getUserById(userId);
        verify(userService).getLastLoggedInByUserId(userId);
        verify(userService).getUserAppRolesByUserId(userId);
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
    void selectUserAppsGet() {
        AppDto app = new AppDto();
        app.setId("1");
        when(userService.getApps()).thenReturn(List.of(app));
        List<String> ids = List.of("1");
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", ids);
        String view = userController.selectUserApps(model, session);
        assertThat(view).isEqualTo("add-user-apps");
        assertThat(model.getAttribute("apps")).isNotNull();
        List<AppViewModel> modeApps = (List<AppViewModel>) model.getAttribute("apps");
        assertThat(modeApps.getFirst().getId()).isEqualTo("1");
        assertThat(modeApps.getFirst().isSelected()).isTrue();
    }

    @Test
    void setSelectedAppsPost() {
        HttpSession session = new MockHttpSession();
        List<String> ids = List.of("1");
        RedirectView view = userController.setSelectedApps(ids, session);
        assertThat(session.getAttribute("apps")).isNotNull();
        assertThat(view.getUrl()).isEqualTo("/user/create/roles");
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
        when(userService.getAllAvailableRolesForApps(eq(selectedApps))).thenReturn(roles);
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", selectedApps);
        session.setAttribute("roles", selectedRoles);
        String view = userController.getSelectedRoles(model, session);
        assertThat(view).isEqualTo("add-user-roles");
        assertThat(model.getAttribute("roles")).isNotNull();
        List<AppRoleViewModel> sessionRoles = (List<AppRoleViewModel>) model.getAttribute("roles");
        assertThat(sessionRoles.getFirst().isSelected()).isFalse();
        assertThat(sessionRoles.get(1).isSelected()).isTrue();
    }

    @Test
    void setSelectedRolesPost() {
        HttpSession session = new MockHttpSession();
        List<String> roles = List.of("1");
        RedirectView view = userController.setSelectedRoles(roles, session);
        assertThat(session.getAttribute("roles")).isNotNull();
        assertThat(view.getUrl()).isEqualTo("/user/create/offices");
    }

    @Test
    void offices() {
        HttpSession session = new MockHttpSession();
        UUID officeId = UUID.randomUUID();
        OfficeData officeData = new OfficeData();
        officeData.setSelectedOffices(List.of(officeId.toString()));
        session.setAttribute("officeData", officeData);
        Office office1 = Office.builder().id(officeId).build();
        Office office2 = Office.builder().id(UUID.randomUUID()).build();
        List<Office> dbOffices = List.of(office1, office2);
        when(officeService.getOffices()).thenReturn(dbOffices);
        String view = userController.offices(session, model);
        List<OfficeModel> modelOfficeData = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(modelOfficeData).isNotNull();
        assertThat(modelOfficeData.get(0).isSelected()).isTrue();
        assertThat(modelOfficeData.get(1).isSelected()).isFalse();
        assertThat(view).isEqualTo("user/offices");
    }

    @Test
    void postOffices() {
        HttpSession session = new MockHttpSession();
        UUID officeId1 = UUID.randomUUID();
        UUID officeId2 = UUID.randomUUID();
        List<String> selectedOffices = List.of(officeId1.toString());
        Office office1 = Office.builder().id(officeId1).name("of1").build();
        Office office2 = Office.builder().id(officeId2).name("of2").build();
        List<Office> dbOffices = List.of(office1, office2);
        when(officeService.getOffices()).thenReturn(dbOffices);
        RedirectView view = userController.postOffices(session, selectedOffices);
        assertThat(view.getUrl()).isEqualTo("/user/create/check-answers");
        OfficeData modelOfficeData = (OfficeData) session.getAttribute("officeData");
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
        AppDto app1 = new AppDto();
        app1.setId("app1");
        userRole.setApp(app1);
        AppRoleDto userRole2 = new AppRoleDto();
        userRole2.setId("app1-dev");
        userRole2.setApp(app1);
        when(userService.getAllAvailableRolesForApps(eq(selectedApps))).thenReturn(List.of(userRole, userRole2));
        List<String> selectedRoles = List.of("app1-dev");
        session.setAttribute("roles", selectedRoles);
        session.setAttribute("user", new User());
        session.setAttribute("officeData", new OfficeData());
        String view = userController.addUserCheckAnswers(model, session);
        assertThat(view).isEqualTo("add-user-check-answers");
        assertThat(model.getAttribute("roles")).isNotNull();
        Map<String, List<AppRoleViewModel>> cyaRoles = (Map<String, List<AppRoleViewModel>>) model.getAttribute("roles");

        assertThat(cyaRoles.get("app1").getFirst().getId()).isEqualTo("app1-dev");
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("officeData")).isNotNull();
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
        String view = userController.addUserCheckAnswers(model, session);
        assertThat(view).isEqualTo("add-user-check-answers");
        assertThat(model.getAttribute("roles")).isNull();
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("officeData")).isNotNull();
    }

    @Test
    void addUserCheckAnswersPost() {
        HttpSession session = new MockHttpSession();
        User user = new User();
        session.setAttribute("user", user);
        List<String> roles = List.of("app1");
        session.setAttribute("roles", roles);
        List<String> selectedApps = List.of("app1");
        session.setAttribute("apps", selectedApps);
        session.setAttribute("officeData", new OfficeData());
        when(userService.createUser(any(), any(), any())).thenReturn(user);
        RedirectView view = userController.addUserCheckAnswers(session);
        assertThat(view.getUrl()).isEqualTo("/users");
        assertThat(model.getAttribute("roles")).isNull();
        assertThat(model.getAttribute("apps")).isNull();
        assertThat(model.getAttribute("officeData")).isNull();
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
        RedirectView view = userController.addUserCheckAnswers(session);
        assertThat(view.getUrl()).isEqualTo("/users");
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
    public void testGetUserRolesOutputMatchesInput() {
        // Given
        final String userId = "12345";
        // Setup test user call
        User testUser = new User();
        when(userService.getUserById(userId)).thenReturn(testUser);
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
        List<AppRoleDto> allRoles = List.of(testRole1, testRole2, testRole3);
        when(userService.getAllAvailableRoles()).thenReturn(allRoles);

        // When
        String view = userController.getUserRoles(userId, model);

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
    public void testUpdateUserRolesReturnsCorrectView() {
        // Given
        final String userId = "12345";
        final List<String> selectedRoles = new ArrayList<>();

        // When
        RedirectView view = userController.updateUserRoles(userId, selectedRoles);

        // Then
        Assertions.assertEquals("/users", view.getUrl());
    }

}
