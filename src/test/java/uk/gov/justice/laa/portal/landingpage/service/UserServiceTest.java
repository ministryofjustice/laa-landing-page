package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.applications.ApplicationsRequestBuilder;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.ApplicationCollectionResponse;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.config.GraphClientConfig;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.repository.UserModelRepository;
import uk.gov.service.notify.NotificationClientException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;
    @Mock
    private GraphServiceClient graphServiceClient;
    @Mock
    private UserModelRepository userModelRepository;
    @Mock
    private ApplicationCollectionResponse mockResponse;
    @Mock
    private CreateUserNotificationService createUserNotificationService;

    @BeforeAll
    public static void init() {
        // Test data for app registrations in local store
        LaaApplication laaApp1 = LaaApplication.builder().id("4efb3caa44d53b15ef398fa622110166f63eadc9ad68f6f8954529c39b901889").title("App One").build();
        LaaApplication laaApp2 = LaaApplication.builder().id("b21b9c1a0611a09a0158d831b765ffe6ded9103a1ecdbc87c706c4ce44d07be7").title("App Two").build();
        LaaApplication laaApp3 = LaaApplication.builder().id("a32d05f19e64840bf256a7128483db941410e4f86bae5c1d4a03c9514c2266a4").title("App Two").build();
        List<LaaApplication> laaApplications = List.of(laaApp1, laaApp2, laaApp3);
        ReflectionTestUtils.setField(LaaAppDetailsStore.class, "laaApplications", laaApplications);
    }

    @AfterAll
    public static void tearDown() {
        ReflectionTestUtils.setField(LaaAppDetailsStore.class, "laaApplications", null);
    }

    @Test
    void createUser() throws NotificationClientException {
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
        app1.setAppId("698815d2-5760-4fd0-bdef-54c683e91b26");
        app1.setDisplayName("App One");

        Application app2 = new Application();
        app2.setAppId("f27a5c75-a33b-4290-becf-9e4f0c14a1eb");
        app2.setDisplayName("App Two");

        when(mockResponse.getValue()).thenReturn(List.of(app1, app2));

        try (MockedStatic<GraphClientConfig> mockedStatic = mockStatic(GraphClientConfig.class)) {
            mockedStatic.when(GraphClientConfig::getGraphClient).thenReturn(graphServiceClient);

            ApplicationsRequestBuilder applicationsRequestBuilder = mock(ApplicationsRequestBuilder.class);
            when(graphServiceClient.applications()).thenReturn(applicationsRequestBuilder);
            when(applicationsRequestBuilder.get()).thenReturn(mockResponse);

            // Act
            List<LaaApplication> result = userService.getManagedAppRegistrations();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.stream().findFirst().get().getTitle()).isEqualTo("App One");
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
            List<LaaApplication> result = userService.getManagedAppRegistrations();

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
            List<LaaApplication> result = userService.getManagedAppRegistrations();

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Test
    void getSavedUsers() {
        when(userModelRepository.findAll()).thenReturn(List.of());
        // Act
        List<UserModel> result = userService.getSavedUsers();

        // Assert
        assertThat(result).isNotNull();
    }
}