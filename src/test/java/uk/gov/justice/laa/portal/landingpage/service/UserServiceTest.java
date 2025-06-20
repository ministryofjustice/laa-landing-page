package uk.gov.justice.laa.portal.landingpage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.microsoft.graph.applications.ApplicationsRequestBuilder;
import com.microsoft.graph.core.content.BatchRequestContent;
import com.microsoft.graph.core.content.BatchResponseContent;
import com.microsoft.graph.core.requests.BatchRequestBuilder;
import com.microsoft.graph.invitations.InvitationsRequestBuilder;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.ApplicationCollectionResponse;
import com.microsoft.graph.models.DirectoryObjectCollectionResponse;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.graph.users.item.UserItemRequestBuilder;
import com.microsoft.graph.users.item.memberof.MemberOfRequestBuilder;
import com.microsoft.kiota.RequestAdapter;
import com.microsoft.kiota.RequestInformation;
import jakarta.servlet.http.HttpSession;
import okhttp3.Request;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    private EntraUserRepository mockEntraUserRepository;
    @Mock
    private NotificationService mockNotificationService;
    @Mock
    private ApplicationCollectionResponse mockApplicationCollectionResponse;
    @Mock
    private HttpSession session;
    @Mock
    private AppRepository mockAppRepository;
    @Mock
    private AppRoleRepository mockAppRoleRepository;
    @Mock
    private OfficeRepository mockOfficeRepository;
    @Mock
    private InvitationsRequestBuilder invitationsRequestBuilder;

    private static String LAST_QUERIED_APP_ROLE_ASSIGNMENT_ID;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                mockGraphServiceClient,
                mockEntraUserRepository,
                mockAppRepository,
                mockAppRoleRepository,
                new MapperConfig().modelMapper(),
                mockNotificationService,
                mockOfficeRepository
        );
    }

    @BeforeAll
    public static void init() {
        // Test data for app registrations in local store
        LaaApplication laaApp1 = LaaApplication.builder().id("4efb3caa44d53b15ef398fa622110166f63eadc9ad68f6f8954529c39b901889").title("App One").build();
        LaaApplication laaApp2 = LaaApplication.builder().id("b21b9c1a0611a09a0158d831b765ffe6ded9103a1ecdbc87c706c4ce44d07be7").title("App Two").build();
        List<LaaApplication> laaApplications = List.of(laaApp1, laaApp2);
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
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        EntraUser user1 = EntraUser.builder()
                .firstName("alice")
                .email("alice@test.com")
                .id(user1Id)
                .build();

        EntraUser user2 = EntraUser.builder()
                .firstName("bob")
                .email("bob@test.com")
                .id(user2Id)
                .build();

        List<EntraUser> mockUsers = List.of(user1, user2);
        when(mockEntraUserRepository.findAll()).thenReturn(mockUsers);

        // Act
        List<EntraUserDto> result = userService.getSavedUsers();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFullName()).isEqualTo("alice");
        assertThat(result.get(1).getFullName()).isEqualTo("bob");
    }

    @Test
    void getSavedUsersWhereFirstAndLastNameAreNull() {
        // Arrange
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        EntraUser user1 = EntraUser.builder()
                .email("alice@test.com")
                .id(user1Id)
                .build();

        EntraUser user2 = EntraUser.builder()
                .email("bob@test.com")
                .id(user2Id)
                .build();

        List<EntraUser> mockUsers = List.of(user1, user2);
        when(mockEntraUserRepository.findAll()).thenReturn(mockUsers);

        // Act
        List<EntraUserDto> result = userService.getSavedUsers();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFullName()).isNull();
        assertThat(result.get(1).getFullName()).isNull();
    }

    @Test
    void getSavedUsersWhereFirstNameIsNullButLastNameIsPopulated() {
        // Arrange
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        EntraUser user1 = EntraUser.builder()
                .lastName("alison")
                .email("alice@test.com")
                .id(user1Id)
                .build();

        EntraUser user2 = EntraUser.builder()
                .lastName("robertson")
                .email("bob@test.com")
                .id(user2Id)
                .build();

        List<EntraUser> mockUsers = List.of(user1, user2);
        when(mockEntraUserRepository.findAll()).thenReturn(mockUsers);

        // Act
        List<EntraUserDto> result = userService.getSavedUsers();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFullName()).isNull();
        assertThat(result.get(1).getFullName()).isNull();
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
        List<List<Character>> subList = new ArrayList<>(subsets);

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
    void getApps() {
        when(mockAppRepository.findAll()).thenReturn(List.of(App.builder().build()));
        List<AppDto> result = userService.getApps();
        assertThat(result).isNotNull();
    }

    @Test
    void getAllAvailableRolesForApps() {
        UUID appId = UUID.randomUUID();
        UUID appRoleId = UUID.randomUUID();
        App app = App.builder()
                .id(appId)
                .name("appDisplayName")
                .build();
        AppRole appRole = AppRole.builder()
                .id(appRoleId)
                .name("appRoleDisplayName")
                .build();
        app.setAppRoles(Set.of(appRole));
        appRole.setApp(app);
        List<App> apps = new ArrayList<>();
        apps.add(app);
        List<String> selectedApps = List.of(appId.toString());
        when(mockAppRepository.findAllById(selectedApps.stream().map(UUID::fromString).toList())).thenReturn(apps);

        List<AppRoleDto> result = userService.getAllAvailableRolesForApps(selectedApps);
        assertThat(result).isNotNull();
        assertThat(result.getFirst().getApp().getId()).isEqualTo(appId.toString());
        assertThat(result.getFirst().getApp().getName()).isEqualTo("appDisplayName");
        assertThat(result.getFirst().getName()).isEqualTo("appRoleDisplayName");
    }

    @Test
    void createUser() {
        //post user
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
        User user = new User();
        when(mockGraphServiceClient.users().post(any())).thenReturn(user);
        //get roles
        UUID appId = UUID.randomUUID();
        UUID appRoleId = UUID.randomUUID();
        App app = App.builder()
                .id(appId)
                .name("appDisplayName")
                .build();
        AppRole appRole = AppRole.builder()
                .id(appRoleId)
                .name("appRoleDisplayName")
                .app(app)
                .build();
        app.setAppRoles(Set.of(appRole));
        //assign role
        UUID userId = UUID.randomUUID();
        UserProfile userProfile = UserProfile.builder().defaultProfile(true).build();
        EntraUser entraUser = EntraUser.builder().id(userId).userProfiles(Set.of(userProfile)).build();
        userProfile.setEntraUser(entraUser);
        when(mockAppRoleRepository.findAllById(any())).thenReturn(List.of(appRole));
        when(mockEntraUserRepository.findById(userId)).thenReturn(Optional.of(entraUser));
        userService.updateUserRoles(userId.toString(), List.of(appRole.getId().toString()));
        when(mockGraphServiceClient.invitations()).thenReturn(invitationsRequestBuilder);
        Invitation invitation = mock(Invitation.class, RETURNS_DEEP_STUBS);
        when(invitationsRequestBuilder.post(any())).thenReturn(invitation);

        List<String> roles = new ArrayList<>();
        roles.add(UUID.randomUUID().toString());
        FirmDto firm = FirmDto.builder().name("Firm").build();
        userService.createUser(user, roles, new ArrayList<>(), firm, false, "admin");
        verify(mockEntraUserRepository, times(2)).saveAndFlush(any());
    }

    @Test
    void createUserWithPopulatedDisplayName() {
        //post user
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
        User user = new User();
        user.setDisplayName("Test User");
        when(mockGraphServiceClient.users().post(any())).thenReturn(user);
        //get roles
        UUID appId = UUID.randomUUID();
        UUID appRoleId = UUID.randomUUID();
        App app = App.builder()
                .id(appId)
                .name("appDisplayName")
                .build();
        AppRole appRole = AppRole.builder()
                .id(appRoleId)
                .name("appRoleDisplayName")
                .app(app)
                .build();
        app.setAppRoles(Set.of(appRole));
        //assign role
        UUID userId = UUID.randomUUID();
        UserProfile userProfile = UserProfile.builder().defaultProfile(true).build();
        EntraUser entraUser = EntraUser.builder().id(userId).userProfiles(Set.of(userProfile)).build();
        userProfile.setEntraUser(entraUser);
        when(mockAppRoleRepository.findAllById(any())).thenReturn(List.of(appRole));
        List<EntraUser> savedUsers = new ArrayList<>();
        when(mockEntraUserRepository.saveAndFlush(any())).then(invocation -> {
            savedUsers.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        when(mockGraphServiceClient.invitations()).thenReturn(invitationsRequestBuilder);
        Invitation invitation = mock(Invitation.class, RETURNS_DEEP_STUBS);
        when(invitationsRequestBuilder.post(any())).thenReturn(invitation);

        List<String> roles = new ArrayList<>();
        roles.add(UUID.randomUUID().toString());
        FirmDto firm = FirmDto.builder().name("Firm").build();
        userService.createUser(user, roles, new ArrayList<>(), firm, false, "admin");
        verify(mockEntraUserRepository, times(1)).saveAndFlush(any());
        assertThat(savedUsers.size()).isEqualTo(1);
        EntraUser savedUser = savedUsers.getFirst();
        assertThat(savedUser.getFirstName()).isEqualTo("Test");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.getUserProfiles().iterator().next().getFirm().getName()).isEqualTo("Firm");
    }

    @Test
    void getUserAppRolesByUserId_returnsRoleDetails_whenServicePrincipalAndRoleExist() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        UUID appRoleId = UUID.randomUUID();

        // Mock ServicePrincipal and AppRole
        AppRole appRole = AppRole.builder()
                .id(appRoleId)
                .name("Test Role")
                .build();

        App app = App.builder()
                .id(appId)
                .name("Test App")
                .appRoles(Set.of(appRole))
                .build();

        appRole.setApp(app);

        UserProfile userProfile = UserProfile.builder().appRoles(Set.of(appRole)).build();
        EntraUser entraUser = EntraUser.builder().id(userId).userProfiles(Set.of(userProfile)).build();

        when(mockEntraUserRepository.findById(userId)).thenReturn(Optional.of(entraUser));

        // Act
        List<AppRoleDto> result = userService.getUserAppRolesByUserId(userId.toString());

        // Assert
        assertThat(result).hasSize(1);
        AppRoleDto roleInfo = result.getFirst();
        assertThat(roleInfo.getApp().getId()).isEqualTo(appId.toString());
        assertThat(roleInfo.getApp().getName()).isEqualTo("Test App");
        assertThat(roleInfo.getName()).isEqualTo("Test Role");
    }

    @Test
    void getUserAppRolesByUserId_returnsUnknown_whenServicePrincipalIsNull() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID appRoleId = UUID.randomUUID();

        // Mock ServicePrincipal and AppRole
        AppRole appRole = AppRole.builder()
                .id(appRoleId)
                .name("Test Role")
                .build();

        UserProfile userProfile = UserProfile.builder().appRoles(Set.of(appRole)).build();
        EntraUser entraUser = EntraUser.builder().id(userId).userProfiles(Set.of(userProfile)).build();

        when(mockEntraUserRepository.findById(userId)).thenReturn(Optional.of(entraUser));

        // Act
        List<AppRoleDto> result = userService.getUserAppRolesByUserId(userId.toString());

        // Assert
        assertThat(result).hasSize(1);
        AppRoleDto roleInfo = result.getFirst();
        assertThat(roleInfo.getApp()).isNull();
    }

    @Test
    void getUserAppRolesByUserId_returnsEmptyList_whenNoAssignments() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile userProfile = UserProfile.builder().appRoles(new HashSet<>()).build();
        EntraUser entraUser = EntraUser.builder().id(userId).userProfiles(Set.of(userProfile)).build();

        when(mockEntraUserRepository.findById(userId)).thenReturn(Optional.of(entraUser));

        // Act
        List<AppRoleDto> result = userService.getUserAppRolesByUserId(userId.toString());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getUserAppRolesByUserId_returnsEmptyList_whenNoUser() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(mockEntraUserRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        List<AppRoleDto> result = userService.getUserAppRolesByUserId(userId.toString());

        // Assert
        assertThat(result).isEmpty();
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
        DirectoryRole directoryRole = new DirectoryRole();
        directoryRole.setDisplayName("AdminRole");

        DirectoryObjectCollectionResponse dirObjectCollectionResponse = mock(DirectoryObjectCollectionResponse.class);
        when(dirObjectCollectionResponse.getValue()).thenReturn(List.of(directoryRole));

        UsersRequestBuilder usersRb = mock(UsersRequestBuilder.class);
        UserItemRequestBuilder userItemRb = mock(UserItemRequestBuilder.class);
        MemberOfRequestBuilder memberOfRb = mock(MemberOfRequestBuilder.class);
        when(mockGraphServiceClient.users()).thenReturn(usersRb);
        when(usersRb.byUserId("test-user-id")).thenReturn(userItemRb);
        when(userItemRb.memberOf()).thenReturn(memberOfRb);
        when(memberOfRb.get()).thenReturn(dirObjectCollectionResponse);

        // Act
        List<DirectoryRole> roles = userService.getDirectoryRolesByUserId("test-user-id");

        // Assert
        assertThat(roles).hasSize(1);
        assertThat(roles.getFirst().getDisplayName()).isEqualTo("AdminRole");
    }

    @Test
    public void testGetAllAvailableRolesReturnsListOfRoles() {
        // Given
        UUID appId = UUID.randomUUID();
        UUID role1Id = UUID.randomUUID();
        UUID role2Id = UUID.randomUUID();
        App app = App.builder()
                .id(appId)
                .name("Test App")
                .build();
        AppRole role1 = AppRole.builder()
                .app(app)
                .name("Test Role 1")
                .id(role1Id)
                .build();
        AppRole role2 = AppRole.builder()
                .app(app)
                .name("Test Role 2")
                .id(role2Id)
                .build();
        app.setAppRoles(Set.of(role1, role2));
        when(mockAppRoleRepository.findAll()).thenReturn(List.of(role1, role2));
        // When
        List<AppRoleDto> roles = userService.getAllAvailableRoles();

        // Then
        assertThat(roles).hasSize(2);
        AppRoleDto returnedRole1 = roles.getFirst();
        assertThat(returnedRole1.getId()).isEqualTo(role1Id.toString());
        assertThat(returnedRole1.getName()).isEqualTo(role1.getName());
        assertThat(returnedRole1.getApp()).isNotNull();
        assertThat(returnedRole1.getApp().getName()).isEqualTo(app.getName());
        assertThat(returnedRole1.getApp().getId()).isEqualTo(app.getId().toString());

        AppRoleDto returnedRole2 = roles.getLast();
        assertThat(returnedRole2.getId()).isEqualTo(role2Id.toString());
        assertThat(returnedRole2.getName()).isEqualTo(role2.getName());
        assertThat(returnedRole2.getApp()).isNotNull();
        assertThat(returnedRole2.getApp().getName()).isEqualTo(app.getName());
        assertThat(returnedRole2.getApp().getId()).isEqualTo(app.getId().toString());
    }

    @Test
    void testFindUserTypeByUsernameUserNotFound() {
        // Act
        RuntimeException rtEx = Assertions.assertThrows(RuntimeException.class,
                () -> userService.findUserTypeByUserEntraId("non-existent-username"));
        // Assert
        assertThat(rtEx.getMessage()).isEqualTo("User not found for the given user entra id: non-existent-username");
    }

    @Test
    void testFindUserTypeByUsernameUserProfileNotFound() {
        // Arrange
        Optional<EntraUser> entraUser = Optional.of(EntraUser.builder().firstName("Test1").build());
        when(mockEntraUserRepository.findByEntraId(anyString())).thenReturn(entraUser);
        // Act
        RuntimeException rtEx = Assertions.assertThrows(RuntimeException.class,
                () -> userService.findUserTypeByUserEntraId("no-profile-username"));
        // Assert
        assertThat(rtEx.getMessage()).isEqualTo("User profile not found for the given entra id: no-profile-username");
    }

    @Test
    void testFindUserTypeByUsername() {
        // Arrange
        EntraUser entraUser = EntraUser.builder().firstName("Test1").userStatus(UserStatus.ACTIVE).build();
        UserProfile userProfile = UserProfile.builder().defaultProfile(true).entraUser(entraUser).userType(UserType.EXTERNAL_MULTI_FIRM).build();
        entraUser.setUserProfiles(Set.of(userProfile));
        when(mockEntraUserRepository.findByEntraId(anyString())).thenReturn(Optional.of(entraUser));
        // Act
        List<UserType> userTypeByUsername = userService.findUserTypeByUserEntraId("no-profile-username");
        // Assert
        assertThat(userTypeByUsername).isNotNull();
        assertThat(userTypeByUsername).hasSize(1);
        assertThat(userTypeByUsername.getFirst()).isEqualTo(UserType.EXTERNAL_MULTI_FIRM);

    }

    @Test
    void testFindUserTypeByUsernameMultiProfile() {
        // Arrange
        EntraUser entraUser = EntraUser.builder().firstName("Test1").userStatus(UserStatus.ACTIVE).build();
        UserProfile userProfile1 = UserProfile.builder().defaultProfile(true).entraUser(entraUser).userType(UserType.EXTERNAL_MULTI_FIRM).build();
        UserProfile userProfile2 = UserProfile.builder().defaultProfile(true).entraUser(entraUser).userType(UserType.EXTERNAL_SINGLE_FIRM_ADMIN).build();
        entraUser.setUserProfiles(Set.of(userProfile1, userProfile2));
        when(mockEntraUserRepository.findByEntraId(anyString())).thenReturn(Optional.of(entraUser));
        // Act
        List<UserType> userTypeByUsername = userService.findUserTypeByUserEntraId("no-profile-username");
        // Assert
        assertThat(userTypeByUsername).isNotNull();
        assertThat(userTypeByUsername).hasSize(2);
        assertThat(userTypeByUsername).contains(UserType.EXTERNAL_MULTI_FIRM, UserType.EXTERNAL_SINGLE_FIRM_ADMIN);

    }

    @Test
    void testGetUserAuthoritiesEmpty() {
        // Arrange
        EntraUser entraUser = EntraUser.builder().firstName("Test1").build();
        when(mockEntraUserRepository.findByEntraId(anyString())).thenReturn(Optional.of(entraUser));
        // Act
        List<String> result = userService.getUserAuthorities("test");
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void testGetUserAuthorities() {
        // Arrange
        EntraUser entraUser = EntraUser.builder().firstName("Test1").userStatus(UserStatus.ACTIVE).build();
        UserProfile userProfile = UserProfile.builder().defaultProfile(true).entraUser(entraUser).userType(UserType.EXTERNAL_MULTI_FIRM).build();
        entraUser.setUserProfiles(Set.of(userProfile));
        when(mockEntraUserRepository.findByEntraId(anyString())).thenReturn(Optional.of(entraUser));
        // Act
        List<String> result = userService.getUserAuthorities("test");
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo("EXTERNAL_MULTI_FIRM");
    }

    @Nested
    class PaginationTests {

        @Test
        void zeroUsers_setsTotalPagesToOne() {
            // Arrange
            Page<EntraUser> userPage = new PageImpl<>(List.of());
            when(mockEntraUserRepository.findAll(any(Pageable.class))).thenReturn(userPage);

            // Act
            PaginatedUsers result = userService.getPageOfUsers(1, 10);

            // Assert
            assertThat(result.getTotalUsers()).isEqualTo(0);
            assertThat(result.getTotalPages()).isEqualTo(1);
        }

        @Test
        void testRequestingPage2ReturnsThirdPageOfUsers() {
            // Arrange
            List<EntraUser> allUsers = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                allUsers.add(EntraUser.builder().firstName("Test" + i).build());
            }
            Page<EntraUser> userPage = new PageImpl<>(allUsers.subList(20, 25), PageRequest.of(2, 10), allUsers.size());
            when(mockEntraUserRepository.findAll(any(Pageable.class))).thenReturn(userPage);
            // Arrange

            // Act
            PaginatedUsers result = userService.getPageOfUsers(2, 10);

            // Assert
            assertThat(result.getTotalUsers()).isEqualTo(25);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.getUsers().size()).isEqualTo(5);
            assertThat(result.getUsers().getFirst().getFullName()).isEqualTo("Test20");
        }

        @Test
        void testSearchThatReturnsZeroUsersSetsTotalPagesToZero() {
            // Arrange
            Page<EntraUser> userPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(mockEntraUserRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    any(), any(), any(), any(Pageable.class))).thenReturn(userPage);

            // Act
            PaginatedUsers result = userService.getPageOfUsersByNameOrEmail(1, 10, "testSearch");

            // Assert
            assertThat(result.getTotalUsers()).isEqualTo(0);
            assertThat(result.getTotalPages()).isEqualTo(0);
        }

        @Test
        void testSearchThatReturnsOneUserSetsTotalPagesToOne() {
            // Arrange
            EntraUser entraUser = EntraUser.builder().firstName("Test1").build();
            Page<EntraUser> userPage = new PageImpl<>(List.of(entraUser), PageRequest.of(0, 10), 1);
            when(mockEntraUserRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    any(), any(), any(), any(Pageable.class))).thenReturn(userPage);

            // Act
            PaginatedUsers result = userService.getPageOfUsersByNameOrEmail(1, 10, "testSearch");

            // Assert
            assertThat(result.getTotalUsers()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
        }

    }

    @Test
    public void testGetEntraUserByIdReturnsUserWhenOneIsPresent() {
        // Given
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder()
                .id(userId)
                .firstName("Test")
                .lastName("User")
                .email("test@test.com")
                .build();
        when(mockEntraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        // When
        Optional<EntraUserDto> optionalReturnedUser = userService.getEntraUserById(userId.toString());
        // Then
        assertThat(optionalReturnedUser.isPresent()).isTrue();
        EntraUserDto returnedUserDto = optionalReturnedUser.get();
        assertThat(returnedUserDto.getFullName()).isEqualTo("Test User");
        assertThat(returnedUserDto.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    public void testGetEntraUserByIdReturnsNothingWhenNoUserIsPresent() {
        // Given
        when(mockEntraUserRepository.findById(any())).thenReturn(Optional.empty());
        // When
        Optional<EntraUserDto> optionalReturnedUser = userService.getEntraUserById(UUID.randomUUID().toString());
        // Then
        assertThat(optionalReturnedUser.isEmpty()).isTrue();
    }

    @Test
    public void testGetUserAppsByUserIdReturnsAppsWhenUserHasAppsAssigned() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        UUID appRoleId = UUID.randomUUID();
        App app = App.builder()
                .id(appId)
                .name("Test App")
                .build();
        AppRole appRole = AppRole.builder()
                .id(appRoleId)
                .name("Test App Role")
                .app(app)
                .build();
        UserProfile userProfile = UserProfile.builder()
                .appRoles(Set.of(appRole))
                .build();
        EntraUser user = EntraUser.builder()
                .id(userId)
                .firstName("Test")
                .lastName("User")
                .email("test@test.com")
                .userProfiles(Set.of(userProfile))
                .build();
        when(mockEntraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        // When
        Set<AppDto> returnedApps = userService.getUserAppsByUserId(userId.toString());

        // Then
        assertThat(returnedApps.size()).isEqualTo(1);
        AppDto returnedAppDto = returnedApps.iterator().next();
        assertThat(returnedAppDto.getId()).isEqualTo(appId.toString());
        assertThat(returnedAppDto.getName()).isEqualTo("Test App");
    }

    @Test
    public void testGetUserAppsByUserIdReturnsEmptySetWhenNoUserIsPresent() {
        // Given
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserService.class);
        when(mockEntraUserRepository.findById(any())).thenReturn(Optional.empty());
        // When
        Set<AppDto> returnedApps = userService.getUserAppsByUserId(UUID.randomUUID().toString());
        // Then
        assertThat(returnedApps.isEmpty()).isTrue();
        List<ILoggingEvent> warningLogs = LogMonitoring.getLogsByLevel(listAppender, Level.WARN);
        assertThat(warningLogs.size()).isEqualTo(1);
    }

    @Test
    public void testGetAppRolesByAppIdsReturnsRolesWhenAppsArePresent() {
        // Given
        UUID appRoleId1 = UUID.randomUUID();
        UUID appRoleId2 = UUID.randomUUID();
        UUID appId1 = UUID.randomUUID();
        UUID appId2 = UUID.randomUUID();
        AppRole appRole1 = AppRole.builder()
                .id(appRoleId1)
                .name("Test App Role 1")
                .build();
        AppRole appRole2 = AppRole.builder()
                .id(appRoleId2)
                .name("Test App Role 2")
                .build();
        App app1 = App.builder()
                .id(appId1)
                .name("Test App 1")
                .appRoles(Set.of(appRole1))
                .build();
        App app2 = App.builder()
                .id(appId2)
                .name("Test App 2")
                .appRoles(Set.of(appRole2))
                .build();

        List<UUID> appIds = List.of(appId1, appId2);
        when(mockAppRepository.findAllById(appIds)).thenReturn(List.of(app1, app2));

        // When
        List<AppRoleDto> returnedAppRoles = userService.getAppRolesByAppIds(appIds.stream().map(UUID::toString).collect(Collectors.toList()));

        // Then
        assertThat(returnedAppRoles.size()).isEqualTo(2);
        AppRoleDto returnedAppRole1 = returnedAppRoles.getFirst();
        assertThat(returnedAppRole1.getId()).isEqualTo(appRoleId1.toString());
        assertThat(returnedAppRole1.getName()).isEqualTo("Test App Role 1");
        AppRoleDto returnedAppRole2 = returnedAppRoles.getLast();
        assertThat(returnedAppRole2.getId()).isEqualTo(appRoleId2.toString());
        assertThat(returnedAppRole2.getName()).isEqualTo("Test App Role 2");
    }

    @Test
    public void testGetAppRolesByAppIdsReturnsEmptyListWhenNoAppsArePresent() {
        // Given
        when(mockAppRepository.findAllById(any())).thenReturn(new ArrayList<>());

        // When
        List<AppRoleDto> returnedAppRoles = userService.getAppRolesByAppIds(List.of(UUID.randomUUID().toString()));

        // Then
        assertThat(returnedAppRoles.isEmpty()).isTrue();
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
