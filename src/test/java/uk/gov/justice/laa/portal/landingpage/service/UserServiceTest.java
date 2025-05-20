package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.applications.ApplicationsRequestBuilder;
import com.microsoft.graph.core.content.BatchRequestContent;
import com.microsoft.graph.core.content.BatchResponseContent;
import com.microsoft.graph.core.requests.BatchRequestBuilder;
import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.ApplicationCollectionResponse;
import com.microsoft.graph.models.DirectoryObjectCollectionResponse;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.graph.users.item.UserItemRequestBuilder;
import com.microsoft.graph.users.item.approleassignments.AppRoleAssignmentsRequestBuilder;
import com.microsoft.graph.users.item.memberof.MemberOfRequestBuilder; // EXTRA_IMPORT
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.RequestAdapter;
import com.microsoft.kiota.RequestInformation;
import jakarta.servlet.http.HttpSession;
import okhttp3.Request;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers; // EXTRA_IMPORT
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.repository.UserModelRepository;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;
    @Mock
    private GraphServiceClient mockGraphServiceClient;
    @Mock
    private UserModelRepository mockUserModelRepository;
    @Mock
    private ApplicationCollectionResponse mockApplicationCollectionResponse;
    @Mock
    private CreateUserNotificationService mockCreateUserNotificationService;
    @Mock
    private HttpSession session;
    @Mock
    private GraphApiService graphApiService;


    @BeforeAll
    public static void init() {
        // Test data for app registrations in local store
        LaaApplication laaApp1 = LaaApplication.builder().id("4efb3caa44d53b15ef398fa622110166f63eadc9ad68f6f8954529c39b901889").title("App One").build();
        LaaApplication laaApp2 = LaaApplication.builder().id("b21b9c1a0611a09a0158d831b765ffe6ded9103a1ecdbc87c706c4ce44d07be7").title("App Two").build();
        LaaApplication laaApp3 = LaaApplication.builder().id("a32d05f19e64840bf256a7128483db941410e4f86bae5c1d4a03c9514c2266a4").title("App Three").build();
        List<LaaApplication> laaApplications = List.of(laaApp1, laaApp2, laaApp3);
        ReflectionTestUtils.setField(LaaAppDetailsStore.class, "laaApplications", laaApplications);
    }

    @AfterAll
    public static void tearDown() {
        ReflectionTestUtils.setField(LaaAppDetailsStore.class, "laaApplications", null);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(
                mockGraphServiceClient,
                mockUserModelRepository,
                mockCreateUserNotificationService
        );
    }

    @Test
    void createUser() {
        // Mocking call chain
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.post(any(User.class))).thenReturn(new User());

        // Call createUser method
        User createdUser = userService.createUser("user", "pw");

        // Assert the result
        assertThat(createdUser).isNotNull();
    }

    @Test
    void getManagedAppRegistrations() {
        //Setup
        Application app1 = new Application();
        app1.setAppId("698815d2-5760-4fd0-bdef-54c683e91b26");
        app1.setDisplayName("App One");

        Application app2 = new Application();
        app2.setAppId("f27a5c75-a33b-4290-becf-9e4f0c14a1eb");
        app2.setDisplayName("App Two");

        // Mocked response from Graph API
        when(mockApplicationCollectionResponse.getValue()).thenReturn(List.of(app1, app2));
        ApplicationsRequestBuilder applicationsRequestBuilder = mock(ApplicationsRequestBuilder.class);
        when(mockGraphServiceClient.applications()).thenReturn(applicationsRequestBuilder);
        when(applicationsRequestBuilder.get()).thenReturn(mockApplicationCollectionResponse);

        // Act
        List<LaaApplication> result = userService.getManagedAppRegistrations();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("App One");
        assertThat(result.get(1).getTitle()).isEqualTo("App Two");
    }

    @Test
    void getManagedAppRegistrationsReturnNull() {
        // Arrange
        ApplicationsRequestBuilder applicationsRequestBuilder = mock(ApplicationsRequestBuilder.class);
        when(mockGraphServiceClient.applications()).thenReturn(applicationsRequestBuilder);
        when(applicationsRequestBuilder.get()).thenReturn(null);

        // Act
        List<LaaApplication> result = userService.getManagedAppRegistrations();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getManagedAppRegistrationsReturnEmpty() {
        // Arrange
        ApplicationsRequestBuilder applicationsRequestBuilder = mock(ApplicationsRequestBuilder.class);
        when(mockGraphServiceClient.applications()).thenReturn(applicationsRequestBuilder);
        when(applicationsRequestBuilder.get()).thenThrow(new RuntimeException("Failed to fetch managed app registrations"));

        // Act
        List<LaaApplication> result = userService.getManagedAppRegistrations();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getSavedUsers() {
        // Arrange
        UserModel user1 = new UserModel();
        user1.setFullName("alice");
        user1.setEmail("alice@test.com");
        user1.setUid(1);

        UserModel user2 = new UserModel();
        user2.setFullName("bob");
        user2.setEmail("bob@test.com");
        user2.setUid(2);

        List<UserModel> mockUsers = List.of(user1, user2);
        when(mockUserModelRepository.findAll()).thenReturn(mockUsers);

        // Act
        List<UserModel> result = userService.getSavedUsers();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFullName()).isEqualTo("alice");
        assertThat(result.get(1).getFullName()).isEqualTo("bob");
    }

    @Test
    void disableUsers() throws IOException {
        BatchRequestBuilder batchRequestBuilder = mock(BatchRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.getBatchRequestBuilder()).thenReturn(batchRequestBuilder);

        RequestAdapter requestAdapter = mock(RequestAdapter.class, RETURNS_DEEP_STUBS);
        Request request = mock(Request.class, RETURNS_DEEP_STUBS);
        when(requestAdapter.convertToNativeRequest(any())).thenReturn(request);
        when(mockGraphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);

        BatchResponseContent responseContent = mock(BatchResponseContent.class, RETURNS_DEEP_STUBS);
        RequestInformation requestInformation = mock(RequestInformation.class, RETURNS_DEEP_STUBS);
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.byUserId(any()).toPatchRequestInformation(any())).thenReturn(requestInformation);
        when(batchRequestBuilder.post(any(BatchRequestContent.class), any())).thenReturn(responseContent);

        userService.disableUsers(List.of("user1", "user2"));

        verify(mockGraphServiceClient, times(1)).getBatchRequestBuilder();
    }

    @Test
    void partitionBasedOnSize() {
        List<Character> characters = List.of('a', 'b', 'c');
        Collection<List<Character>> subsets = UserService.partitionBasedOnSize(characters, 2);
        List<List<Character>> subList = new ArrayList(subsets);

        assertThat(subList).hasSize(2);
        assertThat(subList.get(0)).hasSize(2);
        assertThat(subList.get(1)).hasSize(1);
    }

    @Test
    void getAllUsersWhenGraphApiReturnsNullResponseReturnsEmptyList() {
        // Arrange
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);

        // Act
        List<User> users = userService.getAllUsers();

        // Assert
        assertThat(users).isNotNull().isEmpty();
    }

    @Test
    void getPageHistoryWhenNoHistoryInSessionInitialisesAndReturnsEmptyStack() {
        // Arrange
        when(session.getAttribute("pageHistory")).thenReturn(null);

        // Act
        Stack<String> history = userService.getPageHistory(session);

        // Assert
        assertThat(history).isNotNull().isEmpty();
        verify(session).setAttribute(eq("pageHistory"), any(Stack.class));
    }

    // TODO: This test works but I intend to simplify or condense it
    @Test
    void retrieveUserAppRolesWhenServicePrincipalFound() {
        // Arrange
        String userId = "test-user-id";
        String resourceIdStr = UUID.randomUUID().toString();
        String appRoleIdStr = UUID.randomUUID().toString();
        String servicePrincipalDisplayName = "Test App"; // may need to remove or hardcode for brevity?
        String appRoleDisplayName = "Test Role"; // may need to remove or hardcode for brevity?

        AppRoleAssignment appRoleAssignment = new AppRoleAssignment();
        appRoleAssignment.setResourceId(UUID.fromString(resourceIdStr));
        appRoleAssignment.setAppRoleId(UUID.fromString(appRoleIdStr));

        ServicePrincipal servicePrincipal = new ServicePrincipal();
        servicePrincipal.setDisplayName(servicePrincipalDisplayName);
        AppRole appRole = new AppRole();
        appRole.setId(UUID.fromString(appRoleIdStr));
        appRole.setDisplayName(appRoleDisplayName);
        servicePrincipal.setAppRoles(List.of(appRole));

        AppRoleAssignmentsRequestBuilder appRoleAssignmentsRequestBuilder = mock(AppRoleAssignmentsRequestBuilder.class, RETURNS_DEEP_STUBS);
        UserItemRequestBuilder userItemRequestBuilder = mock(UserItemRequestBuilder.class);
        UsersRequestBuilder usersRequestBuilderMock = mock(UsersRequestBuilder.class);

        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilderMock);
        when(usersRequestBuilderMock.byUserId(userId)).thenReturn(userItemRequestBuilder);
        when(userItemRequestBuilder.appRoleAssignments()).thenReturn(appRoleAssignmentsRequestBuilder);
        when(appRoleAssignmentsRequestBuilder.get().getValue()).thenReturn(List.of(appRoleAssignment));

        com.microsoft.graph.serviceprincipals.item.ServicePrincipalItemRequestBuilder servicePrincipalItemRequestBuilderMock = mock(com.microsoft.graph.serviceprincipals.item.ServicePrincipalItemRequestBuilder.class); // Explicit mock, need to refactor
        com.microsoft.graph.serviceprincipals.ServicePrincipalsRequestBuilder servicePrincipalsRequestBuilderMock = mock(com.microsoft.graph.serviceprincipals.ServicePrincipalsRequestBuilder.class); // Explicit mock, need to refactor
        when(mockGraphServiceClient.servicePrincipals()).thenReturn(servicePrincipalsRequestBuilderMock);
        when(servicePrincipalsRequestBuilderMock.byServicePrincipalId(resourceIdStr)).thenReturn(servicePrincipalItemRequestBuilderMock);
        when(servicePrincipalItemRequestBuilderMock.get()).thenReturn(servicePrincipal);

        // Act
        List<Map<String, Object>> roleDetails = userService.getUserAppRolesByUserId(userId);

        // Assert
        assertThat(roleDetails).hasSize(1);
        Map<String, Object> detail = roleDetails.get(0);
        assertThat(detail.get("appId").toString()).isEqualTo(resourceIdStr);
        assertThat(detail.get("appName")).isEqualTo("Test App");
        assertThat(detail.get("roleName")).isEqualTo("Test Role");
    }

    @Test
    void applicationRoleAssignmentToUser() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        String appId = UUID.randomUUID().toString();
        String appRoleId = UUID.randomUUID().toString();

        UsersRequestBuilder usersRb = mock(UsersRequestBuilder.class);
        UserItemRequestBuilder userItemRb = mock(UserItemRequestBuilder.class);
        AppRoleAssignmentsRequestBuilder appAssignmentsRb = mock(AppRoleAssignmentsRequestBuilder.class);
        when(mockGraphServiceClient.users()).thenReturn(usersRb);
        when(usersRb.byUserId(userId)).thenReturn(userItemRb);
        when(userItemRb.appRoleAssignments()).thenReturn(appAssignmentsRb);
        when(appAssignmentsRb.post(any(AppRoleAssignment.class))).thenReturn(new AppRoleAssignment());

        // Act
        userService.assignAppRoleToUser(userId, appId, appRoleId);

        // Assert
        verify(appAssignmentsRb).post(any(AppRoleAssignment.class));
    }

    @Test
    void existingUserRetrievalById() {
        // Arrange
        String userId = "existing-user-id";
        User expectedUser = new User();
        expectedUser.setId(userId);

        UsersRequestBuilder usersRb = mock(UsersRequestBuilder.class);
        UserItemRequestBuilder userItemRb = mock(UserItemRequestBuilder.class);
        when(mockGraphServiceClient.users()).thenReturn(usersRb);
        when(usersRb.byUserId(userId)).thenReturn(userItemRb);
        when(userItemRb.get()).thenReturn(expectedUser);

        // Act
        User actualUser = userService.getUserById(userId);

        // Assert
        assertThat(actualUser).isEqualTo(expectedUser);
    }

    @Test
    void nonExistentUserRetrievalByIdReturnsNull() { // EXTRA_TEST
        // Arrange
        String userId = "non-existent-user-id";
        UsersRequestBuilder usersRb = mock(UsersRequestBuilder.class);
        UserItemRequestBuilder userItemRb = mock(UserItemRequestBuilder.class);
        when(mockGraphServiceClient.users()).thenReturn(usersRb);
        when(usersRb.byUserId(userId)).thenReturn(userItemRb);
        when(userItemRb.get()).thenThrow(new ApiException("User not found"));

        // Act
        User actualUser = userService.getUserById(userId);

        // Assert
        assertThat(actualUser).isNull();
    }

    @Test
    void signInDateTimeFormatting() {
        // Arrange
        OffsetDateTime signInTime = OffsetDateTime.parse("2023-05-15T10:30:00Z");
        // Act
        String formattedDate = userService.formatLastSignInDateTime(signInTime);
        // Assert
        assertThat(formattedDate).isEqualTo("15 May 2023, 10:30");
    }

    @Test
    void nullSignInDateTimeFormatting() {
        // Arrange & Act
        String formattedDate = userService.formatLastSignInDateTime(null);
        // Assert
        assertThat(formattedDate).isEqualTo("N/A");
    }

    @Test
    void userCreationNotification() {
        // Arrange
        String username = "newnotifyuser";
        String password = "password123";
        User graphUser = new User();
        graphUser.setId("notify-user-id");
        graphUser.setDisplayName(username);

        UsersRequestBuilder usersRb = mock(UsersRequestBuilder.class);
        when(mockGraphServiceClient.users()).thenReturn(usersRb);
        when(usersRb.post(any(User.class))).thenReturn(graphUser);

        // Act
        userService.createUser(username, password);

        // Assert
        verify(mockCreateUserNotificationService).notifyCreateUser(username, null, password, "notify-user-id");
    }

    @Test
    void userDirectoryRolesRetrieval() { // EXTRA_TEST
        // Arrange
        String userId = "test-user-id";
        DirectoryRole directoryRole = new DirectoryRole();
        directoryRole.setDisplayName("AdminRole");

        DirectoryObjectCollectionResponse dirObjectCollectionResponse = mock(DirectoryObjectCollectionResponse.class);
        when(dirObjectCollectionResponse.getValue()).thenReturn(List.of(directoryRole));

        UsersRequestBuilder usersRb = mock(UsersRequestBuilder.class);
        UserItemRequestBuilder userItemRb = mock(UserItemRequestBuilder.class);
        MemberOfRequestBuilder memberOfRb = mock(MemberOfRequestBuilder.class);
        when(mockGraphServiceClient.users()).thenReturn(usersRb);
        when(usersRb.byUserId(userId)).thenReturn(userItemRb);
        when(userItemRb.memberOf()).thenReturn(memberOfRb);
        when(memberOfRb.get()).thenReturn(dirObjectCollectionResponse);

        // Act
        List<DirectoryRole> roles = userService.getDirectoryRolesByUserId(userId);

        // Assert
        assertThat(roles).hasSize(1);
        assertThat(roles.getFirst().getDisplayName()).isEqualTo("AdminRole");
    }

}