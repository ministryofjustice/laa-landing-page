package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.applications.ApplicationsRequestBuilder;
import com.microsoft.graph.core.content.BatchRequestContent;
import com.microsoft.graph.core.content.BatchResponseContent;
import com.microsoft.graph.core.requests.BatchRequestBuilder;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.serviceprincipals.ServicePrincipalsRequestBuilder;
import com.microsoft.graph.serviceprincipals.item.ServicePrincipalItemRequestBuilder;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.graph.users.item.UserItemRequestBuilder;
import com.microsoft.graph.users.item.approleassignments.AppRoleAssignmentsRequestBuilder;
import com.microsoft.graph.users.item.approleassignments.item.AppRoleAssignmentItemRequestBuilder;
import com.microsoft.kiota.RequestAdapter;
import com.microsoft.kiota.RequestInformation;
import okhttp3.Request;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.model.UserRole;
import uk.gov.justice.laa.portal.landingpage.repository.UserModelRepository;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

    private static String LAST_QUERIED_APP_ROLE_ASSIGNMENT_ID;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(
                mockGraphServiceClient,
                mockUserModelRepository
        );
    }

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
    public void testUpdateUserRolesByAddingAndRemovingRoles() {
        // Given
        // Setup user Id
        final String userId = UUID.randomUUID().toString();

        // Setup test App
        ServicePrincipal app = new ServicePrincipal();
        UUID appId = UUID.randomUUID();
        app.setId(appId.toString());
        app.setDisplayName("testApp");
        app.setAppId(UserService.APPLICATION_ID);

        // Set test roles for app
        AppRole appRole1 = new AppRole();
        UUID appRole1Id = UUID.randomUUID();
        appRole1.setId(appRole1Id);
        appRole1.setDisplayName("testRole1");

        AppRole appRole2 = new AppRole();
        UUID appRole2Id = UUID.randomUUID();
        appRole2.setId(appRole2Id);
        appRole2.setDisplayName("testRole2");

        AppRole appRole3 = new AppRole();
        UUID appRole3Id = UUID.randomUUID();
        appRole3.setId(appRole3Id);
        appRole3.setDisplayName("testRole3");

        // Set roles on app
        List<AppRole> roles = new ArrayList<>();
        roles.add(appRole1);
        roles.add(appRole2);
        roles.add(appRole3);
        app.setAppRoles(roles);

        // Build role assignments and assign user to test role 1
        String appRoleAssignmentId = UUID.randomUUID().toString();
        AppRoleAssignment appRoleAssignment = new AppRoleAssignment();
        appRoleAssignment.setResourceId(appId);
        appRoleAssignment.setAppRoleId(appRole1Id);
        appRoleAssignment.setId(appRoleAssignmentId);
        List<AppRoleAssignment> testAppRoleAssignments = new ArrayList<>();
        testAppRoleAssignments.add(appRoleAssignment);

        // Mock response so the above app role assignments are associated with the user.
        AppRoleAssignmentCollectionResponse appRoleAssignmentCollectionResponse = new AppRoleAssignmentCollectionResponse();
        appRoleAssignmentCollectionResponse.getBackingStore().set("value", testAppRoleAssignments);
        AppRoleAssignmentsRequestBuilder appRoleAssignmentsRequestBuilder = mock(AppRoleAssignmentsRequestBuilder.class);
        when(appRoleAssignmentsRequestBuilder.get()).thenReturn(appRoleAssignmentCollectionResponse);
        UserItemRequestBuilder userItemRequestBuilder = mock(UserItemRequestBuilder.class);
        when(userItemRequestBuilder.appRoleAssignments()).thenReturn(appRoleAssignmentsRequestBuilder);
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class);
        when(usersRequestBuilder.byUserId(userId)).thenReturn(userItemRequestBuilder);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);

        // Setup service principal all response
        ServicePrincipalCollectionResponse servicePrincipalCollectionResponse = new ServicePrincipalCollectionResponse();
        servicePrincipalCollectionResponse.getBackingStore().set("value", List.of(app));
        ServicePrincipalsRequestBuilder servicePrincipalsRequestBuilder = mock(ServicePrincipalsRequestBuilder.class);
        when(servicePrincipalsRequestBuilder.get()).thenReturn(servicePrincipalCollectionResponse);

        // Setup service by Id response
        ServicePrincipalItemRequestBuilder servicePrincipalItemRequestBuilder = mock(ServicePrincipalItemRequestBuilder.class);
        when(servicePrincipalItemRequestBuilder.get()).thenReturn(app);
        when(servicePrincipalsRequestBuilder.byServicePrincipalId(appId.toString())).thenReturn(servicePrincipalItemRequestBuilder);
        when(mockGraphServiceClient.servicePrincipals()).thenReturn(servicePrincipalsRequestBuilder);

        // Added the new app role assignment to our list of app role assignment when we post using the mock graph client.
        when(appRoleAssignmentsRequestBuilder.post(any(AppRoleAssignment.class))).then(invocation -> {
            AppRoleAssignment newAppRoleAssignment = invocation.getArgument(0);
            testAppRoleAssignments.add(newAppRoleAssignment);
            return newAppRoleAssignment;
        });


        // Remember the last app role assignment we queried so we can use it for the delete behaviour later.
        AppRoleAssignmentItemRequestBuilder appRoleAssignmentItemRequestBuilder = mock(AppRoleAssignmentItemRequestBuilder.class);
        when(appRoleAssignmentsRequestBuilder.byAppRoleAssignmentId(anyString())).then(invocation -> {
            LAST_QUERIED_APP_ROLE_ASSIGNMENT_ID = invocation.getArgument(0);
            return appRoleAssignmentItemRequestBuilder;
        });

        // Remove the given app role assignment from our list of app role assignment when we call delete on the mock graph client.
        doAnswer(invocation -> {
            AppRoleAssignment returnedAppRoleAssignment = null;
            for (AppRoleAssignment assignment : testAppRoleAssignments) {
                if (LAST_QUERIED_APP_ROLE_ASSIGNMENT_ID.equals(assignment.getId())) {
                    returnedAppRoleAssignment = assignment;
                }
            }
            if(returnedAppRoleAssignment != null) {
                testAppRoleAssignments.remove(returnedAppRoleAssignment);
            }
            return null;
        }).when(appRoleAssignmentItemRequestBuilder).delete();

        // When
        // We add an extra ID for the user
        userService.updateUserRoles(userId, List.of(appRole2.getId().toString()));
        // Then
        Assertions.assertEquals(1, testAppRoleAssignments.size());
    }
}
