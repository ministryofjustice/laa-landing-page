package uk.gov.justice.laa.portal.landingpage.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import com.microsoft.graph.applications.ApplicationsRequestBuilder;
import com.microsoft.graph.core.content.BatchRequestContent;
import com.microsoft.graph.core.content.BatchResponseContent;
import com.microsoft.graph.core.requests.BatchRequestBuilder;
import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.ApplicationCollectionResponse;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.models.ServicePrincipalCollectionResponse;
import com.microsoft.graph.models.SignInActivity;
import com.microsoft.graph.models.DirectoryObjectCollectionResponse;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.serviceprincipals.ServicePrincipalsRequestBuilder;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.graph.users.item.approleassignments.AppRoleAssignmentsRequestBuilder;
import com.microsoft.graph.users.item.UserItemRequestBuilder;
import com.microsoft.graph.users.item.memberof.MemberOfRequestBuilder;
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.RequestAdapter;
import com.microsoft.kiota.RequestInformation;
import jakarta.servlet.http.HttpSession;
import okhttp3.Request;
import org.junit.jupiter.api.Nested;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.model.UserRole;
import uk.gov.justice.laa.portal.landingpage.repository.UserModelRepository;

import java.util.Collections;
import java.util.UUID;
import java.util.Stack;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private HttpSession session;

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
                mockUserModelRepository
        );
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
    void formatLastSignInDateTime_returnsFormattedString() {
        OffsetDateTime dateTime = OffsetDateTime.parse("2024-01-01T10:15:30+00:00");
        String formatted = userService.formatLastSignInDateTime(dateTime);
        assertThat(formatted).isEqualTo("1 January 2024, 10:15");
    }

    @Test
    void formatLastSignInDateTime_returnsNaIfNull() {
        String formatted = userService.formatLastSignInDateTime(null);
        assertThat(formatted).isEqualTo("N/A");
    }

    @Test
    void getLastLoggedInByUserId_returnsFormattedDate() {
        // Arrange
        User user = new User();
        SignInActivity signInActivity = new SignInActivity();
        OffsetDateTime dateTime = OffsetDateTime.parse("2024-01-01T10:15:30+00:00");
        signInActivity.setLastSignInDateTime(dateTime);
        user.setSignInActivity(signInActivity);
        user.setDisplayName("Test User");

        String userId = "user-123";
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.byUserId(userId).get(any())).thenReturn(user);

        // Act
        String result = userService.getLastLoggedInByUserId(userId);

        // Assert
        assertThat(result).isEqualTo("1 January 2024, 10:15");
    }

    @Test
    void getLastLoggedInByUserId_returnsMessageIfNeverLoggedIn() {
        User user = new User();
        user.setDisplayName("Test User");
        user.setSignInActivity(null);

        String userId = "user-123";
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.byUserId(userId).get(any())).thenReturn(user);

        String result = userService.getLastLoggedInByUserId(userId);

        assertThat(result).isEqualTo("Test User has not logged in yet.");
    }

    @Test
    void getUserById_returnsUser_whenUserExists() {
        String userId = "user-123";
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setDisplayName("Test User");

        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.byUserId(userId).get()).thenReturn(mockUser);

        User result = userService.getUserById(userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getDisplayName()).isEqualTo("Test User");
    }

    @Test
    void getUserById_returnsNull_whenExceptionThrown() {
        String userId = "user-123";
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.byUserId(userId).get()).thenThrow(new RuntimeException("Not found"));

        User result = userService.getUserById(userId);

        assertThat(result).isNull();
    }

    @Test
    void getServicePrincipals() {
        ServicePrincipalCollectionResponse servicePrincipals = new ServicePrincipalCollectionResponse();
        List<ServicePrincipal> value = new ArrayList<>();
        servicePrincipals.setValue(value);
        ServicePrincipalsRequestBuilder servicePrincipalsRequestBuilder = mock(ServicePrincipalsRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(servicePrincipalsRequestBuilder.get()).thenReturn(servicePrincipals);
        when(mockGraphServiceClient.servicePrincipals()).thenReturn(servicePrincipalsRequestBuilder);
        List<ServicePrincipal> result = userService.getServicePrincipals();
        assertThat(result).isNotNull();
    }

    @Test
    void getAllAvailableRolesForApps() {
        ServicePrincipal servicePrincipal = new ServicePrincipal();
        servicePrincipal.setId("sId");
        servicePrincipal.setAppId("appId");
        servicePrincipal.setDisplayName("appDisplayName");
        AppRole appRole = new AppRole();
        appRole.setId(UUID.randomUUID());
        appRole.setDisplayName("appRoleDisplayName");
        servicePrincipal.setAppRoles(List.of(appRole));
        List<ServicePrincipal> value = new ArrayList<>();
        value.add(servicePrincipal);
        ServicePrincipalCollectionResponse servicePrincipals = new ServicePrincipalCollectionResponse();
        servicePrincipals.setValue(value);
        ServicePrincipalsRequestBuilder servicePrincipalsRequestBuilder = mock(ServicePrincipalsRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(servicePrincipalsRequestBuilder.get()).thenReturn(servicePrincipals);
        when(mockGraphServiceClient.servicePrincipals()).thenReturn(servicePrincipalsRequestBuilder);
        List<UserRole> result = userService.getAllAvailableRolesForApps(List.of("appId"));
        assertThat(result).isNotNull();
        assertThat(result.get(0).getAppId()).isEqualTo("sId");
        assertThat(result.get(0).getAppName()).isEqualTo("appDisplayName");
        assertThat(result.get(0).getAppRoleName()).isEqualTo("appRoleDisplayName");
        assertThat(result.get(0).getRoleName()).isEqualTo("appRoleDisplayName");
    }

    @Test
    void assignAppRoleToUser() {
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
        AppRoleAssignmentsRequestBuilder appRoleAssignmentsRequestBuilder = mock(AppRoleAssignmentsRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users().byUserId(any()).appRoleAssignments()).thenReturn(appRoleAssignmentsRequestBuilder);
        AppRoleAssignment appRoleAssignment = mock(AppRoleAssignment.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users().byUserId(any()).appRoleAssignments().post(any())).thenReturn(appRoleAssignment);
        userService.assignAppRoleToUser(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
        verify(appRoleAssignmentsRequestBuilder, times(1)).post(any());
    }

    @Test
    void createUser() {
        //post user
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
        User user = new User();
        when(mockGraphServiceClient.users().post(any())).thenReturn(user);
        //get roles
        ServicePrincipal servicePrincipal = new ServicePrincipal();
        servicePrincipal.setId("sId");
        servicePrincipal.setAppId("appId");
        servicePrincipal.setDisplayName("appDisplayName");
        AppRole appRole = new AppRole();
        appRole.setId(UUID.randomUUID());
        appRole.setDisplayName("appRoleDisplayName");
        servicePrincipal.setAppRoles(List.of(appRole));
        ServicePrincipalCollectionResponse servicePrincipals = new ServicePrincipalCollectionResponse();
        List<ServicePrincipal> value = new ArrayList<>();
        value.add(servicePrincipal);
        servicePrincipals.setValue(value);
        ServicePrincipalsRequestBuilder servicePrincipalsRequestBuilder = mock(ServicePrincipalsRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(servicePrincipalsRequestBuilder.get()).thenReturn(servicePrincipals);
        when(mockGraphServiceClient.servicePrincipals()).thenReturn(servicePrincipalsRequestBuilder);
        //assign role
        UsersRequestBuilder usersRoleRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users()).thenReturn(usersRoleRequestBuilder);
        AppRoleAssignmentsRequestBuilder appRoleAssignmentsRequestBuilder = mock(AppRoleAssignmentsRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users().byUserId(any()).appRoleAssignments()).thenReturn(appRoleAssignmentsRequestBuilder);
        AppRoleAssignment appRoleAssignment = mock(AppRoleAssignment.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users().byUserId(any()).appRoleAssignments().post(any())).thenReturn(appRoleAssignment);
        userService.assignAppRoleToUser(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());

        List<String> roles = new ArrayList<>();
        roles.add("role1");
        org.springframework.test.util.ReflectionTestUtils.setField(userService, "defaultDomain", "testDomain");

        userService.createUser(user, "pw", roles);
        verify(appRoleAssignmentsRequestBuilder, times(1)).post(any());
        verify(mockUserModelRepository, times(1)).save(any());
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
    void nonExistentUserRetrievalByIdReturnsNull() {
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
    void userDirectoryRolesRetrieval() {
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

    @Nested
    class PaginationTests {

        private final GraphServiceClient mockGraph = mock(GraphServiceClient.class, RETURNS_DEEP_STUBS);
        private final UserService paginationSvc =
                new UserService(mockGraph, mockUserModelRepository);

        private static User graphUser(String id, String name) {
            User u = new User();
            u.setId(id);
            u.setUserPrincipalName(id + "@test");
            u.setDisplayName(name);
            return u;
        }

        // Helpers
        private UserCollectionResponse buildPage(List<User> users, String next) {
            UserCollectionResponse r = new UserCollectionResponse();
            r.setValue(users);
            r.setOdataNextLink(next);
            return r;
        }

        @Test
        void firstPage_returnsNextLink_noPrev() {
            // Arrange
            when(mockGraph.users().get(any()))
                    .thenReturn(buildPage(List.of(graphUser("1", "Alice")), "https://next"));
            when(mockGraph.users().count().get(any())).thenReturn(1);

            // Act
            PaginatedUsers result = paginationSvc.getPaginatedUsersWithHistory(new Stack<>(), 10, null);

            // Assert
            assertThat(result.getUsers()).hasSize(1);
            assertThat(result.getNextPageLink()).isEqualTo("https://next");
            assertThat(result.getPreviousPageLink()).isNull();
            assertThat(result.getTotalPages()).isEqualTo(1);
        }

        @Test
        void backNav_usesWithUrl_andPopsHistory() {
            // Arrange
            Stack<String> history = new Stack<>();
            history.push("https://first");
            when(mockGraph.users().get(any())).thenReturn(buildPage(List.of(), "ignored"));
            when(mockGraph.users().count().get(any())).thenReturn(2);
            when(mockGraph.users().withUrl("https://first").get())
                    .thenReturn(buildPage(List.of(graphUser("1", "Alice")), null));

            // Act
            PaginatedUsers result = paginationSvc.getPaginatedUsersWithHistory(history, 1, "https://ignored");

            // Assert
            assertThat(result.getUsers()).extracting(UserModel::getFullName).containsExactly("Alice");
            assertThat(history).doesNotContain("https://first");
        }

        @Test
        void zeroUsers_setsTotalPagesToOne() {
            // Arrange
            when(mockGraph.users().get(any()))
                    .thenReturn(buildPage(Collections.emptyList(), null));
            when(mockGraph.users().count().get(any())).thenReturn(0);

            // Act
            PaginatedUsers result = paginationSvc.getPaginatedUsersWithHistory(new Stack<>(), 5, null);

            // Assert
            assertThat(result.getTotalUsers()).isZero();
            assertThat(result.getTotalPages()).isEqualTo(1);
        }
    }

    @Nested
    class AssignAppRoleToUser {

        @Test
        void postsAssignmentToGraph() {
            // Arrange
            String userId = UUID.randomUUID().toString();
            UsersRequestBuilder users = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
            AppRoleAssignmentsRequestBuilder appRoles = mock(AppRoleAssignmentsRequestBuilder.class, RETURNS_DEEP_STUBS);
            when(mockGraphServiceClient.users()).thenReturn(users);
            when(users.byUserId(userId).appRoleAssignments()).thenReturn(appRoles);

            // Act
            userService.assignAppRoleToUser(
                    userId,
                    "00000000-0000-0000-0000-000000000222",
                    "00000000-0000-0000-0000-000000000333");

            // Assert
            verify(appRoles).post(any(AppRoleAssignment.class));
        }

        @Test
        void swallowsGraphExceptions_gracefully() {
            // Arrange
            String userId = UUID.randomUUID().toString();
            UsersRequestBuilder users = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
            AppRoleAssignmentsRequestBuilder appRoles = mock(AppRoleAssignmentsRequestBuilder.class, RETURNS_DEEP_STUBS);
            when(mockGraphServiceClient.users()).thenReturn(users);
            when(users.byUserId(userId).appRoleAssignments()).thenReturn(appRoles);
            doThrow(new RuntimeException("boom")).when(appRoles).post(any(AppRoleAssignment.class));

            // Act
            userService.assignAppRoleToUser(
                    userId,
                    "00000000-0000-0000-0000-000000000222",
                    "00000000-0000-0000-0000-000000000333");

            // Assert
            verify(appRoles).post(any(AppRoleAssignment.class));
        }
    }

    @Nested
    class PartitionBasedOnSize {

        @Test
        void partitionsEvenly_whenDivisible() {
            // Arrange
            List<Integer> input = List.of(1, 2, 3, 4);

            // Act
            List<List<Integer>> result = UserService.partitionBasedOnSize(input, 2);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).containsExactly(1, 2);
            assertThat(result.get(1)).containsExactly(3, 4);
        }

        @Test
        void keepsRemainder_inFinalChunk() {
            // Arrange
            List<Integer> input = List.of(1, 2, 3, 4, 5);

            // Act
            List<List<Integer>> result = UserService.partitionBasedOnSize(input, 2);

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result.get(2)).containsExactly(5);
        }
    }
}
