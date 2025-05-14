package uk.gov.justice.laa.portal.landingpage.controller;

import com.microsoft.graph.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @InjectMocks
    private UserController userController;
    @Mock
    private UserService userService;

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
    void displaySavedUsers() {
        Model model = new ExtendedModelMap();
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
}