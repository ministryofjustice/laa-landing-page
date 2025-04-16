package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;
    @Mock
    private GraphServiceClient graphServiceClient;

    @Test
    void createUser() {
        mockStatic(UserService.class);
        when(UserService.getGraphClient()).thenReturn(graphServiceClient);
        var users = Mockito.mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(graphServiceClient.users()).thenReturn(users);
        when(graphServiceClient.users().post(any())).thenReturn(new User());
        assertThat(userService.createUser("user", "pw")).isNotNull();
    }
}