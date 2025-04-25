package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.applications.ApplicationsRequestBuilder;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.ApplicationCollectionResponse;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.config.GraphClientConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;
    @Mock
    private GraphServiceClient graphServiceClient;
    @Mock
    private ApplicationCollectionResponse mockResponse;

    @Test
    void createUser() {
        try (MockedStatic<GraphClientConfig> mockedStatic = mockStatic(GraphClientConfig.class)) {
            mockedStatic.when(GraphClientConfig::getGraphClient).thenReturn(graphServiceClient);

            UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
            when(graphServiceClient.users()).thenReturn(usersRequestBuilder);
            when(usersRequestBuilder.post(any(User.class))).thenReturn(new User());

            assertThat(userService.createUser("user", "pw")).isNotNull();
        }
    }

    @Test
    void getManagedAppRegistrations() {
        // Arrange
        Application app1 = new Application();
        app1.setDisplayName("App One");

        Application app2 = new Application();
        app2.setDisplayName("App Two");

        when(mockResponse.getValue()).thenReturn(List.of(app1, app2));

        try (MockedStatic<GraphClientConfig> mockedStatic = mockStatic(GraphClientConfig.class)) {
            mockedStatic.when(GraphClientConfig::getGraphClient).thenReturn(graphServiceClient);

            ApplicationsRequestBuilder applicationsRequestBuilder = mock(ApplicationsRequestBuilder.class);
            when(graphServiceClient.applications()).thenReturn(applicationsRequestBuilder);
            when(applicationsRequestBuilder.get()).thenReturn(mockResponse);

            // Act
            List<Application> result = userService.getManagedAppRegistrations();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.getFirst().getDisplayName()).isEqualTo("App One");
        }
    }

    @Test
    void getManagedAppRegistrationsReturnNull() {
        // Arrange
        GraphServiceClient mockClient = mock(GraphServiceClient.class, RETURNS_DEEP_STUBS);
        when(mockClient.applications().get()).thenReturn(null);

        try (MockedStatic<GraphClientConfig> mockedStatic = mockStatic(GraphClientConfig.class)) {
            mockedStatic.when(GraphClientConfig::getGraphClient).thenReturn(mockClient);

            // Act
            List<Application> result = userService.getManagedAppRegistrations();

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Test
    void getManagedAppRegistrationsReturnEmpty() {
        // Arrange
        GraphServiceClient mockClient = mock(GraphServiceClient.class, RETURNS_DEEP_STUBS);
        when(mockClient.applications().get()).thenThrow(new RuntimeException("Failed to fetch managed app registrations"));

        try (MockedStatic<GraphClientConfig> mockedStatic = mockStatic(GraphClientConfig.class)) {
            mockedStatic.when(GraphClientConfig::getGraphClient).thenReturn(mockClient);

            // Act
            List<Application> result = userService.getManagedAppRegistrations();

            // Assert
            assertThat(result).isEmpty();
        }
    }
}