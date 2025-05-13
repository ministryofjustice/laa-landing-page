package uk.gov.justice.laa.portal.landingpage.controller;

import com.microsoft.graph.models.User;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @InjectMocks
    private UserController userController;
    @Mock
    private UserService userService;
    @Mock
    private HttpSession session;

    @Test
    void addUserToGraph() {
        User created = new User();
        when(userService.createUser(anyString(), anyString())).thenReturn(created);
        String view = userController.addUserToGraph("username", "password");
        assertThat(view).isEqualTo("register");
    }

    @Test
    void register() {
        assertThat(userController.register()).isEqualTo("register");
    }

    @Test
    void displayAllUsers() {
    }

    @Test
    void editUser() {
    }

    @Test
    void givenUsersExist_whenDisplayAllUsers_thenPopulatesModelAndReturnsUsersView() {
        // Arrange
        Model model = new ExtendedModelMap();
        Stack<String> history = new Stack<>();
        history.push("prevLink456");
        int size = 15;
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(List.of(new UserModel(), new UserModel()));
        mockPaginatedUsers.setNextPageLink("nextLink123");
        mockPaginatedUsers.setPreviousPageLink("prevLink456");
        when(userService.getPageHistory(session)).thenReturn(history);
        when(userService.getPaginatedUsersWithHistory(eq(history), eq(size), isNull())).thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(size, null, model, session);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(mockPaginatedUsers.getUsers());
        assertThat(model.getAttribute("nextPageLink")).isEqualTo("nextLink123");
        assertThat(model.getAttribute("previousPageLink")).isEqualTo("prevLink456");
        assertThat(model.getAttribute("pageSize")).isEqualTo(size);
        assertThat(model.getAttribute("pageHistory")).isEqualTo(history);
        verify(userService).getPageHistory(session);
        verify(userService).getPaginatedUsersWithHistory(history, size, null);
    }

    @Test
    void givenNextPageLink_whenDisplayAllUsers_thenUsesLinkAndUpdatesHistory() {
        // Arrange
        Model model = new ExtendedModelMap();
        int size = 10;
        String currentPageLink = "currentPageLink";

        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(List.of(new UserModel()));
        mockPaginatedUsers.setNextPageLink("nextPageLinkFromServer");
        mockPaginatedUsers.setPreviousPageLink("somePrevLink");

        Stack<String> history = new Stack<>();

        when(userService.getPageHistory(session)).thenReturn(history);
        when(userService.getPaginatedUsersWithHistory(eq(history), eq(size), eq(currentPageLink)))
                .thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(size, currentPageLink, model, session);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("nextPageLink")).isEqualTo("nextPageLinkFromServer");
        verify(userService).getPageHistory(session);
        verify(userService).getPaginatedUsersWithHistory(history, size, currentPageLink);
    }

    @Test
    void givenNoUsers_whenDisplayAllUsers_thenReturnsEmptyListInModel() {

        // Arrange
        Model model = new ExtendedModelMap();
        int size = 10;
        Stack<String> history = new Stack<>();

        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(new ArrayList<>());
        mockPaginatedUsers.setNextPageLink(null);
        mockPaginatedUsers.setPreviousPageLink(null);

        when(userService.getPageHistory(session)).thenReturn(history);
        when(userService.getPaginatedUsersWithHistory(any(), anyInt(), isNull()))
                .thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(size, null, model, session);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(new ArrayList<>());
        assertThat(model.getAttribute("nextPageLink")).isNull();
        verify(userService).getPaginatedUsersWithHistory(history, size, null);
    }

    @Test
    void givenValidUserId_whenEditUser_thenFetchesUserAndReturnsEditView() {

        // Arrange
        Model model = new ExtendedModelMap();
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
        Model model = new ExtendedModelMap();
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
        Model model = new ExtendedModelMap();
        when(userService.getSavedUsers()).thenReturn(new ArrayList<>());
        String view = userController.displaySavedUsers(model);
        assertThat(view).isEqualTo("users");
        assertThat(model.getAttribute("users")).isNotNull();

    }

}