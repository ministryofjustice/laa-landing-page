package uk.gov.justice.laa.portal.landingpage.controller;

import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class UserControllerTest extends BaseIntegrationTest {
    private static final String ADD_USER_API_ENDPOINT = "/register";

    @MockitoBean
    private GraphServiceClient graphServiceClient;

    @Test
    void shouldRedirectAnonymousUser() throws Exception {
        this.mockMvc
                .perform(get("/users"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Happy Path Test: addUserToGraph and display user list")
    void addUserToGraph() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("username", "john");
        requestParams.add("password", "pw123");

        var users = Mockito.mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(graphServiceClient.users()).thenReturn(users);
        when(graphServiceClient.users().post(any())).thenReturn(new User());

        // when
        Map<String, Object> model =
                performPostRequestWithParams(ADD_USER_API_ENDPOINT, requestParams, status().is2xxSuccessful(), "register");

        //then
        assertThat(model).isNotNull();
        //display
        this.mockMvc.perform(get("/userlist"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"));
    }

    @Test
    @DisplayName("Happy Path Test: register get")
    void register() throws Exception {
        this.mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("Happy Path Test: displaySavedUsers get")
    void displaySavedUsers() throws Exception {
        this.mockMvc.perform(get("/userlist"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"));
    }
}