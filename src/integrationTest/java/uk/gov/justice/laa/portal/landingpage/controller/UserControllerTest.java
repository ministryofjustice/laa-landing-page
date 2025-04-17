package uk.gov.justice.laa.portal.landingpage.controller;

import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.util.LinkedMultiValueMap;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends BaseIntegrationTest {
    private static final String ADD_USER_API_ENDPOINT = "/register";
    @Mock
    private GraphServiceClient graphServiceClient;

    @Test
    @DisplayName("Happy Path Test: addUserToGraph")
    void addUserToGraph() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap();
        requestParams.add("username", "john");
        requestParams.add("password", "pw123");

        mockStatic(UserService.class);
        when(UserService.getGraphClient()).thenReturn(graphServiceClient);
        var users = Mockito.mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(graphServiceClient.users()).thenReturn(users);
        when(graphServiceClient.users().post(any())).thenReturn(new User());
        // when
        Map<String, Object> model =
                performPostRequestWithParams(ADD_USER_API_ENDPOINT, requestParams, status().is2xxSuccessful(), "register");

        //then
        assertThat(model).isNull();
    }
}