package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.microsoft.graph.models.User;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

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
        String view = userController.register();

        assertThat(view).isEqualTo("register");
    }

    @Test
    void displayAllUsers() {
        Model model = new ExtendedModelMap();
        Stack<String> pageHistory = new Stack<>();
        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setUsers(new ArrayList<>());
        paginatedUsers.setNextPageLink("nextPageLink");
        paginatedUsers.setPreviousPageLink("previousPageLink");
        paginatedUsers.setTotalUsers(100);
        paginatedUsers.setTotalPages(10);

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
        Model model = new ExtendedModelMap();
        User user = new User();
        user.setDisplayName("Test User");

        when(userService.getUserById(anyString())).thenReturn(user);

        String view = userController.editUser("userId", model);

        assertThat(view).isEqualTo("edit-user");
        assertThat(model.getAttribute("user")).isEqualTo(user);
    }

    @Test
    void displaySavedUsers() {
        Model model = new ExtendedModelMap();
        List<UserModel> users = new ArrayList<>();
        users.add(new UserModel());

        when(userService.getSavedUsers()).thenReturn(users);

        String view = userController.displaySavedUsers(model);

        assertThat(view).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(users);
    }
}
