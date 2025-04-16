package uk.gov.justice.laa.portal.landingpage.controller;

import com.microsoft.graph.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

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
        User user = userController.addUserToGraph("username", "password");
        assertThat(user).isNotNull();
    }

    @Test
    void register() {
    }

    @Test
    void displayAllUsers() {
    }

    @Test
    void editUser() {
    }
}