package uk.gov.justice.laa.portal.landingpage.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.microsoft.graph.core.content.BatchRequestContent;
import com.microsoft.graph.core.content.BatchResponseContent;
import com.microsoft.graph.core.requests.BatchRequestBuilder;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.DirectoryObjectCollectionResponse;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.graph.users.item.UserItemRequestBuilder;
import com.microsoft.graph.users.item.memberof.MemberOfRequestBuilder;
import com.microsoft.kiota.RequestAdapter;
import com.microsoft.kiota.RequestInformation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import okhttp3.Request;
import uk.gov.justice.laa.portal.landingpage.config.LaaAppsConfig;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.RoleType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;
    @Mock
    private LaaAppsConfig.LaaApplicationsList laaApplicationsList;
    @Mock
    private GraphServiceClient mockGraphServiceClient;
    @Mock
    private EntraUserRepository mockEntraUserRepository;
    @Mock
    private AppRepository mockAppRepository;
    @Mock
    private AppRoleRepository mockAppRoleRepository;
    @Mock
    private OfficeRepository mockOfficeRepository;
    @Mock
    private TechServicesClient techServicesClient;
    @Mock
    private UserProfileRepository mockUserProfileRepository;
    @Mock
    private RoleChangeNotificationService mockRoleChangeNotificationService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                mockGraphServiceClient,
                mockEntraUserRepository,
                mockAppRepository,
                mockAppRoleRepository,
                new MapperConfig().modelMapper(),
                mockOfficeRepository,
                laaApplicationsList,
                techServicesClient,
                mockUserProfileRepository,
                mockRoleChangeNotificationService);
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
        // assign role
        UUID userId = UUID.randomUUID();
        UserProfile userProfile = UserProfile.builder().activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE).build();
        EntraUser entraUser = EntraUser.builder().id(userId).userProfiles(Set.of(userProfile)).build();
        userProfile.setEntraUser(entraUser);
        RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
        createdUser.setId("id");
        createdUser.setMail("test.user@email.com");
        RegisterUserResponse registerUserResponse = RegisterUserResponse.builder().createdUser(createdUser).build();
        when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);
        when(mockEntraUserRepository.saveAndFlush(any())).thenReturn(entraUser);

        List<String> roles = new ArrayList<>();
        roles.add(UUID.randomUUID().toString());
        EntraUserDto entraUserDto = new EntraUserDto();
        entraUserDto.setFirstName("Test");
        entraUserDto.setLastName("User");
        entraUserDto.setEmail("test.user@email.com");
        FirmDto firm = FirmDto.builder().name("Firm").build();
        userService.createUser(entraUserDto, firm, UserType.EXTERNAL_SINGLE_FIRM, "admin");
        verify(mockEntraUserRepository, times(1)).saveAndFlush(any());
        verify(techServicesClient, times(1)).registerNewUser(any(EntraUserDto.class));
    }

    @Test
    void createUserWithPopulatedDisplayName() {
        // assign role
        UUID userId = UUID.randomUUID();
        UserProfile userProfile = UserProfile.builder().activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE).build();
        EntraUser entraUser = EntraUser.builder().id(userId).userProfiles(Set.of(userProfile)).build();
        userProfile.setEntraUser(entraUser);
        List<EntraUser> savedUsers = new ArrayList<>();
        when(mockEntraUserRepository.saveAndFlush(any())).then(invocation -> {
            savedUsers.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
        createdUser.setId("id");
        createdUser.setMail("test.user@email.com");
        RegisterUserResponse registerUserResponse = RegisterUserResponse.builder().createdUser(createdUser).build();
        when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);

        List<String> roles = new ArrayList<>();
        roles.add(UUID.randomUUID().toString());
        EntraUserDto entraUserDto = new EntraUserDto();
        entraUserDto.setFirstName("Test");
        entraUserDto.setLastName("User");
        entraUserDto.setEmail("test.user@email.com");
        FirmDto firm = FirmDto.builder().name("Firm").build();
        userService.createUser(entraUserDto, firm, UserType.EXTERNAL_SINGLE_FIRM, "admin");
        verify(mockEntraUserRepository, times(1)).saveAndFlush(any());
        assertThat(savedUsers.size()).isEqualTo(1);
        EntraUser savedUser = savedUsers.getFirst();
        assertThat(savedUser.getFirstName()).isEqualTo("Test");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.getUserProfiles().iterator().next().getFirm().getName()).isEqualTo("Firm");
        assertThat(savedUser.getUserProfiles().iterator().next().getUserType()).isEqualTo(UserType.EXTERNAL_SINGLE_FIRM);
        verify(techServicesClient, times(1)).registerNewUser(any(EntraUserDto.class));
    }

    @Test
    void getUserAppRolesByUserId_returnsRoleDetails_whenServicePrincipalAndRoleExist() {
        // Arrange
        UUID userProfileId = UUID.randomUUID();
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

        UserProfile userProfile = UserProfile.builder()
                .id(userProfileId)
                .activeProfile(true)
                .appRoles(Set.of(appRole))
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .build();

        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

        // Act
        List<AppRoleDto> result = userService.getUserAppRolesByUserId(userProfileId.toString());

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
        UUID userProfileId = UUID.randomUUID();
        UUID appRoleId = UUID.randomUUID();

        // Mock ServicePrincipal and AppRole
        AppRole appRole = AppRole.builder()
                .id(appRoleId)
                .name("Test Role")
                .build();

        UserProfile userProfile = UserProfile.builder()
                .id(userProfileId)
                .activeProfile(true)
                .appRoles(Set.of(appRole))
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .build();

        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

        // Act
        List<AppRoleDto> result = userService.getUserAppRolesByUserId(userProfileId.toString());

        // Assert
        assertThat(result).hasSize(1);
        AppRoleDto roleInfo = result.getFirst();
        assertThat(roleInfo.getApp()).isNull();
    }

    @Test
    void getUserAppRolesByUserId_returnsEmptyList_whenNoAssignments() {
        // Arrange
        UUID userProfileId = UUID.randomUUID();
        UserProfile userProfile = UserProfile.builder()
                .id(userProfileId)
                .activeProfile(true)
                .appRoles(new HashSet<>())
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .build();

        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

        // Act
        List<AppRoleDto> result = userService.getUserAppRolesByUserId(userProfileId.toString());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getUserAppRolesByUserId_returnsEmptyList_whenNoUser() {
        // Arrange
        UUID userProfileId = UUID.randomUUID();
        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.empty());

        // Act
        List<AppRoleDto> result = userService.getUserAppRolesByUserId(userProfileId.toString());

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
                .id(role1Id)
                .name("Test Role 1")
                .build();
        AppRole role2 = AppRole.builder()
                .app(app)
                .id(role2Id)
                .name("Test Role 2")
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
        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> userService.findUserTypeByUserEntraId("non-existent-username"));
        // Assert
        assertThat(rtEx.getMessage()).isEqualTo("User not found for the given user entra id: non-existent-username");
    }

    @Test
    void testFindUserTypeByUsernameUserProfileNotFound() {
        // Arrange
        Optional<EntraUser> entraUser = Optional.of(EntraUser.builder().firstName("Test1").build());
        when(mockEntraUserRepository.findByEntraOid(anyString())).thenReturn(entraUser);
        // Act
        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> userService.findUserTypeByUserEntraId("no-profile-username"));
        // Assert
        assertThat(rtEx.getMessage()).isEqualTo("User profile not found for the given entra id: no-profile-username");
    }

    @Test
    void testFindUserTypeByUsername() {
        // Arrange
        EntraUser entraUser = EntraUser.builder().firstName("Test1").userStatus(UserStatus.ACTIVE).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true).entraUser(entraUser)
                .userType(UserType.EXTERNAL_MULTI_FIRM).userProfileStatus(UserProfileStatus.COMPLETE).build();
        entraUser.setUserProfiles(Set.of(userProfile));
        when(mockEntraUserRepository.findByEntraOid(anyString())).thenReturn(Optional.of(entraUser));
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
        UserProfile userProfile1 = UserProfile.builder().activeProfile(true).entraUser(entraUser)
                .userType(UserType.EXTERNAL_MULTI_FIRM).userProfileStatus(UserProfileStatus.COMPLETE).build();
        UserProfile userProfile2 = UserProfile.builder().activeProfile(true).entraUser(entraUser)
                .userType(UserType.EXTERNAL_SINGLE_FIRM_ADMIN).userProfileStatus(UserProfileStatus.COMPLETE).build();
        entraUser.setUserProfiles(Set.of(userProfile1, userProfile2));
        when(mockEntraUserRepository.findByEntraOid(anyString())).thenReturn(Optional.of(entraUser));
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
        when(mockEntraUserRepository.findByEntraOid(anyString())).thenReturn(Optional.of(entraUser));
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
        Permission userPermission = Permission.VIEW_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        UserProfile userProfile = UserProfile.builder().appRoles(Set.of(appRole)).activeProfile(true).entraUser(entraUser)
                .userType(UserType.EXTERNAL_MULTI_FIRM).userProfileStatus(UserProfileStatus.COMPLETE).build();
        entraUser.setUserProfiles(Set.of(userProfile));
        when(mockEntraUserRepository.findByEntraOid(anyString())).thenReturn(Optional.of(entraUser));
        // Act
        List<String> result = userService.getUserAuthorities("test");
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo("VIEW_EXTERNAL_USER");
    }

    @Test
    void testFindUserByUserEntraIdNotFound() {
        // Act
        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> userService.findUserByUserEntraId("non-existent-entra-id"));
        // Assert
        assertThat(rtEx.getMessage()).isEqualTo("User not found for the given user entra id: non-existent-entra-id");
    }

    @Test
    void testFindUserByUserEntraUserId() {
        // Arrange
        EntraUser entraUser = EntraUser.builder().entraOid("entra-oid").firstName("Test1").userStatus(UserStatus.ACTIVE)
                .build();
        when(mockEntraUserRepository.findByEntraOid(anyString())).thenReturn(Optional.of(entraUser));
        // Act
        EntraUserDto result = userService.findUserByUserEntraId("entra-oid");
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEntraOid()).isEqualTo("entra-oid");
        assertThat(result.getFullName()).isEqualTo("Test1");

    }

    @Test
    public void testGetUserAssignedAppsforLandingPageWhenNoUserIsPresent() {
        // Given
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserService.class);
        UUID userId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(userId).entraOid(userId.toString()).userProfiles(HashSet.newHashSet(1)).build();
        UserProfile userProfile = UserProfile.builder().id(userId).activeProfile(true).entraUser(entraUser).build();
        entraUser.getUserProfiles().add(userProfile);

        when(mockEntraUserRepository.findById(any(UUID.class))).thenReturn(Optional.of(entraUser));
        when(mockUserProfileRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        // When
        Set<LaaApplication> returnedApps = userService.getUserAssignedAppsforLandingPage(userId.toString());
        // Then
        assertThat(returnedApps.isEmpty()).isTrue();
        List<ILoggingEvent> warningLogs = LogMonitoring.getLogsByLevel(listAppender, Level.WARN);
        assertThat(warningLogs.size()).isEqualTo(1);
    }

    @Test
    public void testGetUserAssignedAppsforLandingPageWhenNoActiveProfile() {
        // Given
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserService.class);
        UUID userId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(userId).entraOid(userId.toString()).userProfiles(HashSet.newHashSet(1)).build();
        UserProfile userProfile = UserProfile.builder().id(userId).activeProfile(false).entraUser(entraUser).build();
        entraUser.getUserProfiles().add(userProfile);

        when(mockEntraUserRepository.findById(any(UUID.class))).thenReturn(Optional.of(entraUser));

        // When & Then
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> userService.getUserAssignedAppsforLandingPage(userId.toString()));
        assertThat(exception.getMessage()).contains("User profile not found for the given user id");
        List<ILoggingEvent> warningLogs = LogMonitoring.getLogsByLevel(listAppender, Level.ERROR);
        assertThat(warningLogs.size()).isEqualTo(1);
    }

    @Test
    public void testGetUserAssignedAppsforLandingPageWhenUserHasAppsAssigned() {
        // Given
        UUID entraUserId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID appId1 = UUID.randomUUID();
        UUID appId2 = UUID.randomUUID();
        UUID appRoleId1 = UUID.randomUUID();
        UUID appRoleId2 = UUID.randomUUID();
        App app1 = App.builder()
                .id(appId1)
                .name("Test App 1")
                .build();
        App app2 = App.builder()
                .id(appId2)
                .name("Test App 2")
                .build();
        AppRole appRole1 = AppRole.builder()
                .id(appRoleId1)
                .name("Test App Role 1")
                .app(app1)
                .build();
        AppRole appRole2 = AppRole.builder()
                .id(appRoleId2)
                .name("Test App Role 2")
                .app(app2)
                .build();
        UserProfile userProfile = UserProfile.builder()
                .id(userProfileId)
                .activeProfile(true)
                .appRoles(Set.of(appRole1, appRole2))
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .build();
        EntraUser user = EntraUser.builder()
                .id(entraUserId)
                .firstName("Test")
                .lastName("User")
                .email("test@test.com")
                .userProfiles(Set.of(userProfile))
                .build();
        when(mockEntraUserRepository.findById(entraUserId)).thenReturn(Optional.of(user));
        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));
        List<LaaApplication> applications = Arrays.asList(
                LaaApplication.builder().name("Test App 1").laaApplicationDetails("a//b//c").ordinal(0).build(),
                LaaApplication.builder().name("Test App 2").laaApplicationDetails("d//e//f").ordinal(1).build(),
                LaaApplication.builder().name("Test App 3").laaApplicationDetails("g//h//i").ordinal(2).build()
        );
        when(laaApplicationsList.getApplications()).thenReturn(applications);
        // When
        Set<LaaApplication> returnedApps = userService.getUserAssignedAppsforLandingPage(entraUserId.toString());

        // Then
        assertThat(returnedApps).isNotNull();
        assertThat(returnedApps.size()).isEqualTo(2);
        Iterator<LaaApplication> iterator = returnedApps.iterator();
        LaaApplication resultApp1 = iterator.next();
        assertThat(resultApp1.getName()).isEqualTo("Test App 1");
        LaaApplication resultApp2 = iterator.next();
        assertThat(resultApp2.getName()).isEqualTo("Test App 2");
    }

    @Test
    public void getActiveProfileByUserId() {
        // Given
        UUID userId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(userId).entraOid(userId.toString()).userProfiles(HashSet.newHashSet(1)).build();
        UserProfile userProfile = UserProfile.builder().id(userId).activeProfile(true).entraUser(entraUser).build();
        entraUser.getUserProfiles().add(userProfile);

        when(mockEntraUserRepository.findById(any(UUID.class))).thenReturn(Optional.of(entraUser));
        // When
        Optional<UserProfileDto> result = userService.getActiveProfileByUserId(userId.toString());
        // Then
        assertThat(result.isEmpty()).isFalse();
        UserProfileDto userProfileDto = result.get();
        assertThat(userProfileDto.getId()).isEqualTo(userId);
    }

    @Test
    public void getActiveProfileByUserIdNoEntraUser() {
        // Given
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserService.class);
        UUID userId = UUID.randomUUID();

        when(mockEntraUserRepository.findById(userId)).thenReturn(Optional.empty());
        // When & Then
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> userService.getActiveProfileByUserId(userId.toString()));
        assertThat(exception.getMessage()).contains("User not found for the given user user id:");

        List<ILoggingEvent> warningLogs = LogMonitoring.getLogsByLevel(listAppender, Level.ERROR);
        assertThat(warningLogs.size()).isEqualTo(1);
    }

    @Test
    public void getActiveProfileByUserIdNoUserProfile() {
        // Given
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserService.class);
        UUID userId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(userId).entraOid(userId.toString()).userProfiles(HashSet.newHashSet(1)).build();

        when(mockEntraUserRepository.findById(any(UUID.class))).thenReturn(Optional.of(entraUser));

        // When & Then
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> userService.getActiveProfileByUserId(userId.toString()));
        assertThat(exception.getMessage()).contains("User profile not found for the given user id:");
        List<ILoggingEvent> warningLogs = LogMonitoring.getLogsByLevel(listAppender, Level.ERROR);
        assertThat(warningLogs.size()).isEqualTo(1);
    }

    @Nested
    class PaginationTests {

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
        UUID entraUserId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
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
                .id(userProfileId)
                .activeProfile(true)
                .appRoles(Set.of(appRole))
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .build();
        EntraUser user = EntraUser.builder()
                .id(entraUserId)
                .firstName("Test")
                .lastName("User")
                .email("test@test.com")
                .userProfiles(Set.of(userProfile))
                .build();
        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));
        // When
        Set<AppDto> returnedApps = userService.getUserAppsByUserId(userProfileId.toString());

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
        UUID userProfileId = UUID.randomUUID();
        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.empty());
        // When
        Set<AppDto> returnedApps = userService.getUserAppsByUserId(userProfileId.toString());
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
        List<AppRoleDto> returnedAppRoles = userService
                .getAppRolesByAppIds(appIds.stream().map(UUID::toString).collect(Collectors.toList()));

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

    @Test
    public void testGetAppRolesByAppIdReturnsRolesWhenAppIsPresent() {
        UUID appId = UUID.randomUUID();
        UUID appRoleId = UUID.randomUUID();
        AppRole appRole = AppRole.builder()
                .name("Test App Role")
                .id(appRoleId)
                .build();
        App returnedApp = App.builder()
                .id(appId)
                .name("Test App")
                .appRoles(Set.of(appRole))
                .build();
        when(mockAppRepository.findById(appId)).thenReturn(Optional.of(returnedApp));
        List<AppRoleDto> returnedAppRoles = userService.getAppRolesByAppId(appId.toString());
        assertThat(returnedAppRoles.size()).isEqualTo(1);
        AppRoleDto returnedAppRole = returnedAppRoles.getFirst();
        assertThat(returnedAppRole.getId()).isEqualTo(appRoleId.toString());
        assertThat(returnedAppRole.getName()).isEqualTo("Test App Role");
    }

    @Test
    public void testGetAppRolesByAppIdReturnsEmptyListWhenAppNotFound() {
        when(mockAppRepository.findById(any())).thenReturn(Optional.empty());
        List<AppRoleDto> returnedAppRoles = userService.getAppRolesByAppId(UUID.randomUUID().toString());
        assertThat(returnedAppRoles.size()).isEqualTo(0);
    }

    @Test
    public void testGetAppByAppIdReturnsAppWhenAppIsPresent() {
        UUID appId = UUID.randomUUID();
        App app = App.builder()
                .id(appId)
                .name("Test App")
                .build();
        when(mockAppRepository.findById(appId)).thenReturn(Optional.of(app));
        Optional<AppDto> returnedApp = userService.getAppByAppId(appId.toString());
        assertThat(returnedApp.isPresent()).isTrue();
        AppDto returnedAppDto = returnedApp.get();
        assertThat(returnedAppDto.getId()).isEqualTo(appId.toString());
        assertThat(returnedAppDto.getName()).isEqualTo("Test App");
    }

    @Test
    public void testGetAppByAppIdReturnsEmptyWhenAppIsNotPresent() {
        when(mockAppRepository.findById(any())).thenReturn(Optional.empty());
        Optional<AppDto> returnedApp = userService.getAppByAppId(UUID.randomUUID().toString());
        assertThat(returnedApp.isEmpty()).isTrue();
    }

    @Test
    void updateUserRoles_updatesRoles_whenUserAndProfileExist_externalRole1() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        AppRole appRole = AppRole.builder().id(roleId).roleType(RoleType.INTERNAL_AND_EXTERNAL).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE).userType(UserType.EXTERNAL_SINGLE_FIRM).build();
        EntraUser user = EntraUser.builder().id(userId).userProfiles(Set.of(userProfile)).build();
        userProfile.setEntraUser(user);

        when(mockAppRoleRepository.findAllById(any())).thenReturn(List.of(appRole));
        when(mockUserProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));

        // Act
        userService.updateUserRoles(userId.toString(), List.of(roleId.toString()));

        // Assert
        assertThat(userProfile.getAppRoles()).containsExactly(appRole);
        verify(mockUserProfileRepository, times(1)).save(userProfile);
        verify(techServicesClient, times(1)).updateRoleAssignment(userId);
    }

    @Test
    void updateUserRoles_updatesRoles_whenUserAndProfileExist_externalRole2() {
        // Arrange
        UUID userProfileId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        AppRole appRole = AppRole.builder().id(roleId).roleType(RoleType.EXTERNAL).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true).userType(UserType.EXTERNAL_MULTI_FIRM).build();
        EntraUser user = EntraUser.builder().id(userProfileId).userProfiles(Set.of(userProfile)).build();
        userProfile.setEntraUser(user);

        when(mockAppRoleRepository.findAllById(any())).thenReturn(List.of(appRole));
        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

        // Act
        userService.updateUserRoles(userProfileId.toString(), List.of(roleId.toString()));

        // Assert
        assertThat(userProfile.getAppRoles()).containsExactly(appRole);
    }

    @Test
    void updateUserRoles_updatesRoles_whenUserAndProfileExist_internalRole() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        AppRole appRole = AppRole.builder().id(roleId).roleType(RoleType.INTERNAL).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true).userType(UserType.INTERNAL).build();
        EntraUser user = EntraUser.builder().id(userId).userProfiles(Set.of(userProfile)).build();
        userProfile.setEntraUser(user);

        when(mockAppRoleRepository.findAllById(any())).thenReturn(List.of(appRole));
        when(mockUserProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));

        // Act
        userService.updateUserRoles(userId.toString(), List.of(roleId.toString()));

        // Assert
        assertThat(userProfile.getAppRoles()).containsExactly(appRole);
    }

    @Test
    void updateUserRoles_updatesRoles_whenUserAndProfileExist_error() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        AppRole appRole = AppRole.builder().id(roleId).roleType(RoleType.INTERNAL).build();
        UserProfile userProfile = UserProfile.builder().activeProfile(true).userType(UserType.EXTERNAL_SINGLE_FIRM_ADMIN).build();
        EntraUser user = EntraUser.builder().id(userId).userProfiles(Set.of(userProfile)).build();
        userProfile.setEntraUser(user);

        when(mockAppRoleRepository.findAllById(any())).thenReturn(List.of(appRole));
        when(mockUserProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));

        // Act
        userService.updateUserRoles(userId.toString(), List.of(roleId.toString()));

        // Assert
        assertThat(userProfile.getAppRoles()).isEmpty();
    }

    @Test
    void updateUserRoles_logsWarning_whenUserNotFound() {
        // Arrange
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserService.class);
        UUID userProfileId = UUID.randomUUID();
        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.empty());

        // Act
        userService.updateUserRoles(userProfileId.toString(), List.of(UUID.randomUUID().toString()));

        // Assert
        List<ILoggingEvent> warningLogs = LogMonitoring.getLogsByLevel(listAppender, Level.WARN);
        assertThat(warningLogs).isNotEmpty();
        assertThat(warningLogs.getFirst().getFormattedMessage()).contains("User profile with id");
    }

    @Test
    void userExistsByEmail_returnsFalse_whenEmailIsNullOrBlank() {
        assertThat(userService.userExistsByEmail(null)).isFalse();
        assertThat(userService.userExistsByEmail("")).isFalse();
        assertThat(userService.userExistsByEmail("   ")).isFalse();
    }

    @Test
    void userExistsByEmail_returnsTrue_whenUserFoundInRepository() {
        String email = "test@example.com";
        EntraUser user = EntraUser.builder().email(email).build();
        when(mockEntraUserRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        assertThat(userService.userExistsByEmail(email)).isTrue();
    }

    @Test
    void userExistsByEmail_returnsTrue_whenUserFoundInGraph() {
        String email = "test@example.com";
        when(mockEntraUserRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        User graphUser = new User();
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.byUserId(email).get()).thenReturn(graphUser);
        assertThat(userService.userExistsByEmail(email)).isTrue();
    }

    @Test
    void userExistsByEmail_returnsFalse_whenUserNotFoundAnywhere() {
        String email = "test@example.com";
        when(mockEntraUserRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.byUserId(email).get()).thenReturn(null);
        assertThat(userService.userExistsByEmail(email)).isFalse();
    }

    @Test
    void userExistsByEmail_logsWarning_whenGraphThrowsException() {
        String email = "test@example.com";
        when(mockEntraUserRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.byUserId(email).get()).thenThrow(new RuntimeException("Not found"));
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserService.class);
        assertThat(userService.userExistsByEmail(email)).isFalse();
        List<ILoggingEvent> warningLogs = LogMonitoring.getLogsByLevel(listAppender, Level.WARN);
        assertThat(warningLogs).isNotEmpty();
        assertThat(warningLogs.getFirst().getFormattedMessage())
                .contains("No user found in Entra with matching email. Catching error and moving on");
    }

    @Test
    public void testGetAppsByUserTypeQueriesInternalUsersWhenUserTypeIsInternal() {
        App testApp = App.builder()
                .name("Test App")
                .build();
        AppRole testAppRole = AppRole.builder()
                .name("Test Role")
                .app(testApp)
                .build();
        when(mockAppRoleRepository.findByRoleTypeIn(anyList())).thenReturn(List.of(testAppRole));
        List<AppDto> apps = userService.getAppsByUserType(UserType.INTERNAL);
        Assertions.assertEquals(1, apps.size());
        Assertions.assertEquals(testApp.getName(), apps.getFirst().getName());
        ArgumentCaptor<List<RoleType>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockAppRoleRepository).findByRoleTypeIn(captor.capture());
        List<RoleType> roleTypes = captor.getValue();
        Assertions.assertEquals(2, roleTypes.size());
        Assertions.assertEquals(RoleType.INTERNAL, roleTypes.getFirst());
        Assertions.assertEquals(RoleType.INTERNAL_AND_EXTERNAL, roleTypes.get(1));
    }

    @Test
    public void testGetAppsByUserTypeQueriesExternalUsersWhenUserTypeIsExternal() {
        App testApp = App.builder()
                .name("Test App")
                .build();
        AppRole testAppRole = AppRole.builder()
                .name("Test Role")
                .app(testApp)
                .build();
        when(mockAppRoleRepository.findByRoleTypeIn(anyList())).thenReturn(List.of(testAppRole));
        List<AppDto> apps = userService.getAppsByUserType(UserType.EXTERNAL_SINGLE_FIRM);
        Assertions.assertEquals(1, apps.size());
        Assertions.assertEquals(testApp.getName(), apps.getFirst().getName());
        ArgumentCaptor<List<RoleType>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockAppRoleRepository).findByRoleTypeIn(captor.capture());
        List<RoleType> roleTypes = captor.getValue();
        Assertions.assertEquals(2, roleTypes.size());
        Assertions.assertEquals(RoleType.EXTERNAL, roleTypes.getFirst());
        Assertions.assertEquals(RoleType.INTERNAL_AND_EXTERNAL, roleTypes.get(1));
    }

    @Test
    public void testGetAppRolesByAppIdAndUserTypeReturnsInternalRolesWhenUserTypeIsInternal() {
        App testApp = App.builder()
                .name("Test App")
                .build();
        AppRole internalRole = AppRole.builder()
                .name("Test Internal Role")
                .roleType(RoleType.INTERNAL)
                .build();
        AppRole externalRole = AppRole.builder()
                .name("Test External Role")
                .roleType(RoleType.EXTERNAL)
                .build();
        AppRole internalAndExternalRole = AppRole.builder()
                .name("Test Internal And External Role")
                .roleType(RoleType.INTERNAL_AND_EXTERNAL)
                .build();

        testApp.setAppRoles(Set.of(internalRole, externalRole, internalAndExternalRole));
        when(mockAppRepository.findById(any())).thenReturn(Optional.of(testApp));

        List<AppRoleDto> returnedAppRoles = userService.getAppRolesByAppIdAndUserType(UUID.randomUUID().toString(),
                UserType.INTERNAL);
        Assertions.assertEquals(2, returnedAppRoles.size());
        // Check no external app roles in response
        Assertions
                .assertTrue(returnedAppRoles.stream().noneMatch(role -> role.getRoleType().equals(RoleType.EXTERNAL)));
    }

    @Test
    public void testGetAppRolesByAppIdAndUserTypeReturnsExternalRolesWhenUserTypeIsExternal() {
        App testApp = App.builder()
                .name("Test App")
                .build();
        AppRole internalRole = AppRole.builder()
                .name("Test Internal Role")
                .roleType(RoleType.INTERNAL)
                .build();
        AppRole externalRole = AppRole.builder()
                .name("Test External Role")
                .roleType(RoleType.EXTERNAL)
                .build();
        AppRole internalAndExternalRole = AppRole.builder()
                .name("Test Internal And External Role")
                .roleType(RoleType.INTERNAL_AND_EXTERNAL)
                .build();

        testApp.setAppRoles(Set.of(internalRole, externalRole, internalAndExternalRole));
        when(mockAppRepository.findById(any())).thenReturn(Optional.of(testApp));

        List<AppRoleDto> returnedAppRoles = userService.getAppRolesByAppIdAndUserType(UUID.randomUUID().toString(),
                UserType.EXTERNAL_SINGLE_FIRM);
        Assertions.assertEquals(2, returnedAppRoles.size());
        // Check no external app roles in response
        Assertions
                .assertTrue(returnedAppRoles.stream().noneMatch(role -> role.getRoleType().equals(RoleType.INTERNAL)));
    }

    @Test
    public void testGetAppRolesByAppIdAndUserTypeReturnsEmptyListWhenAppIdIsNotFound() {
        when(mockAppRepository.findById(any())).thenReturn(Optional.empty());
        List<AppRoleDto> returnedAppRoles = userService.getAppRolesByAppIdAndUserType(UUID.randomUUID().toString(),
                UserType.EXTERNAL_SINGLE_FIRM);
        Assertions.assertEquals(0, returnedAppRoles.size());
    }

    @Test
    public void testGetUserTypeByUserIdReturnsUserTypeWhenUserIsPresent() {
        UserProfile testUserProfile = UserProfile.builder()
                .userType(UserType.INTERNAL)
                .activeProfile(true)
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .build();
        EntraUser testUser = EntraUser.builder()
                .userProfiles(Set.of(testUserProfile))
                .build();
        when(mockEntraUserRepository.findById(any())).thenReturn(Optional.of(testUser));
        Optional<UserType> returnedUserType = userService.getUserTypeByUserId(UUID.randomUUID().toString());
        Assertions.assertTrue(returnedUserType.isPresent());
        Assertions.assertEquals(UserType.INTERNAL, returnedUserType.get());
    }

    @Test
    public void testGetUserTypeByUserIdReturnsEmptyWhenUserIsNotPresent() {
        when(mockEntraUserRepository.findById(any())).thenReturn(Optional.empty());
        Optional<UserType> returnedUserType = userService.getUserTypeByUserId(UUID.randomUUID().toString());
        Assertions.assertTrue(returnedUserType.isEmpty());
    }

    @Test
    public void testGetUserTypeByUserIdReturnsEmptyWhenUserHasNoActiveProfile() {
        UserProfile testUserProfile = UserProfile.builder()
                .userType(UserType.INTERNAL)
                .activeProfile(false)
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .build();
        EntraUser testUser = EntraUser.builder()
                .userProfiles(Set.of(testUserProfile))
                .build();
        when(mockEntraUserRepository.findById(any())).thenReturn(Optional.of(testUser));
        Optional<UserType> returnedUserType = userService.getUserTypeByUserId(UUID.randomUUID().toString());
        Assertions.assertTrue(returnedUserType.isEmpty());
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

    @Test
    public void getUserByEntraIdWhenUserIsPresent() {
        // Given
        when(mockEntraUserRepository.findByEntraOid(any())).thenReturn(Optional.of(EntraUser.builder().build()));
        // When
        EntraUser entraUser = userService.getUserByEntraId(UUID.randomUUID());
        // Then
        assertThat(entraUser).isNotNull();
    }

    @Test
    public void getUserByEntraIdWhenUserIsNotPresent() {
        // Given
        when(mockEntraUserRepository.findByEntraOid(any())).thenReturn(Optional.empty());
        // When
        EntraUser entraUser = userService.getUserByEntraId(UUID.randomUUID());
        // Then
        assertThat(entraUser).isNull();
    }

    @Test
    void isInternal_Ok() {
        UUID userId = UUID.randomUUID();
        Permission userPermission = Permission.VIEW_INTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        Set<UserProfile> userProfiles = Set.of(UserProfile.builder().activeProfile(true).userType(UserType.INTERNAL).userProfileStatus(UserProfileStatus.COMPLETE).appRoles(Set.of(appRole)).build());
        EntraUser entraUser = EntraUser.builder().id(userId).userProfiles(userProfiles).build();
        when(mockEntraUserRepository.findById(any())).thenReturn(Optional.of(entraUser));
        assertThat(userService.isInternal(entraUser.getId())).isTrue();
    }

    @Test
    void isInternal_Failed() {
        UUID userId = UUID.randomUUID();
        Permission userPermission = Permission.VIEW_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        Set<UserProfile> userProfiles = Set.of(UserProfile.builder().activeProfile(true).userType(UserType.EXTERNAL_SINGLE_FIRM)
                .appRoles(Set.of(appRole)).userProfileStatus(UserProfileStatus.COMPLETE).build());
        EntraUser entraUser = EntraUser.builder().id(userId).userProfiles(userProfiles).build();
        when(mockEntraUserRepository.findById(any())).thenReturn(Optional.of(entraUser));
        assertThat(userService.isInternal(entraUser.getId())).isFalse();
    }

    @Test
    void isUserCreationAllowed_Ok() {
        Set<UserProfile> userProfiles = Set.of(UserProfile.builder().activeProfile(true).userType(UserType.INTERNAL).userProfileStatus(UserProfileStatus.COMPLETE).build());
        EntraUser entraUser = EntraUser.builder().userProfiles(userProfiles).build();
        assertThat(userService.isUserCreationAllowed(entraUser)).isTrue();
    }

    @Test
    void isUserCreationAllowed_Failed() {
        Set<UserProfile> userProfiles = Set
                .of(UserProfile.builder().activeProfile(true).userType(UserType.EXTERNAL_SINGLE_FIRM_ADMIN).userProfileStatus(UserProfileStatus.COMPLETE).build());
        EntraUser entraUser = EntraUser.builder().userProfiles(userProfiles).build();
        assertThat(userService.isUserCreationAllowed(entraUser)).isFalse();
    }

    @Nested
    class UpdateUserDetailsTests {

        @Test
        void updateUserDetails_updatesDatabase_whenUserExists() throws IOException {
            // Arrange
            UUID userId = UUID.randomUUID();
            String firstName = "John";
            String lastName = "Doe";

            EntraUser entraUser = EntraUser.builder()
                    .id(userId)
                    .firstName("OldFirst")
                    .lastName("OldLast")
                    .email("old@example.com")
                    .build();
            when(mockEntraUserRepository.findById(userId)).thenReturn(Optional.of(entraUser));
            when(mockEntraUserRepository.saveAndFlush(any())).thenReturn(entraUser);

            // Act
            userService.updateUserDetails(userId.toString(), firstName, lastName);

            // Assert
            assertThat(entraUser.getFirstName()).isEqualTo(firstName);
            assertThat(entraUser.getLastName()).isEqualTo(lastName);
            verify(mockEntraUserRepository).saveAndFlush(entraUser);
        }

        @Test
        void updateUserDetails_throwsIoException_whenDatabaseUpdateFails() {
            // Arrange
            UUID userId = UUID.randomUUID();
            EntraUser entraUser = EntraUser.builder().id(userId).build();
            when(mockEntraUserRepository.findById(userId)).thenReturn(Optional.of(entraUser));
            when(mockEntraUserRepository.saveAndFlush(any())).thenThrow(new RuntimeException("DB error"));

            // Act & Assert
            IOException exception = Assertions.assertThrows(IOException.class,
                    () -> userService.updateUserDetails(userId.toString(), "John", "Doe"));
            assertThat(exception.getMessage()).contains("Failed to update user details in database");
        }

        @Test
        void updateUserDetails_logsWarning_whenUserNotFoundInDatabase() throws IOException {
            // Arrange
            UUID userId = UUID.randomUUID();
            when(mockEntraUserRepository.findById(userId)).thenReturn(Optional.empty());

            ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserService.class);

            // Act
            userService.updateUserDetails(userId.toString(), "John", "Doe");

            // Assert
            List<ILoggingEvent> warningLogs = LogMonitoring.getLogsByLevel(listAppender, Level.WARN);
            assertThat(warningLogs).isNotEmpty();
            assertThat(warningLogs.getFirst().getFormattedMessage())
                    .contains("User with id " + userId + " not found in database");
        }

        @Test
        void updateUserDetails_handlesRepositoryException_gracefully() throws IOException {
            // Arrange
            UUID userId = UUID.randomUUID();
            EntraUser entraUser = EntraUser.builder().id(userId).build();
            when(mockEntraUserRepository.findById(userId)).thenReturn(Optional.of(entraUser));
            when(mockEntraUserRepository.saveAndFlush(any())).thenReturn(entraUser);

            // Act - should not throw exception
            userService.updateUserDetails(userId.toString(), "John", "Doe");

            // Assert - database update should occur
            verify(mockEntraUserRepository).saveAndFlush(entraUser);
        }
    }

    @Nested
    class GetUserOfficesByUserIdTests {

        @Test
        void getUserOfficesByUserId_returnsOffices_whenUserExists() {
            // Arrange
            UUID entraUserId = UUID.randomUUID();
            UUID userProfileId = UUID.randomUUID();
            UUID officeId1 = UUID.randomUUID();
            UUID officeId2 = UUID.randomUUID();

            Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
            Office office1 = Office.builder().id(officeId1).code("Office 1").address(address).build();
            Office office2 = Office.builder().id(officeId2).code("Office 2").address(address).build();

            UserProfile userProfile = UserProfile.builder()
                    .id(userProfileId)
                    .offices(Set.of(office1, office2))
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

            // Act
            List<OfficeDto> result = userService.getUserOfficesByUserId(userProfileId.toString());

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(OfficeDto::getCode).containsExactlyInAnyOrder("Office 1", "Office 2");
        }

        @Test
        void getUserOfficesByUserId_returnsEmptyList_whenUserNotFound() {
            // Arrange
            UUID userProfileId = UUID.randomUUID();
            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.empty());

            // Act
            List<OfficeDto> result = userService.getUserOfficesByUserId(userProfileId.toString());

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        void getUserOfficesByUserId_returnsEmptyList_whenUserHasNoOffices() {
            // Arrange
            UUID userProfileId = UUID.randomUUID();
            UserProfile userProfile = UserProfile.builder().id(userProfileId).activeProfile(true).offices(new HashSet<>()).userProfileStatus(UserProfileStatus.COMPLETE).build();

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

            // Act
            List<OfficeDto> result = userService.getUserOfficesByUserId(userProfileId.toString());

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        void getUserOfficesByUserId_handlesMultipleProfiles() {
            // Arrange
            UUID userProfileId = UUID.randomUUID();
            UUID officeId1 = UUID.randomUUID();
            UUID officeId2 = UUID.randomUUID();
            UUID officeId3 = UUID.randomUUID();

            Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
            Office office1 = Office.builder().id(officeId1).code("Office 1").address(address).build();
            Office office2 = Office.builder().id(officeId2).code("Office 2").address(address).build();
            Office office3 = Office.builder().id(officeId3).code("Office 3").address(address).build();

            UserProfile userProfile = UserProfile.builder()
                    .id(userProfileId)
                    .offices(Set.of(office1, office2, office3))
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

            // Act
            List<OfficeDto> result = userService.getUserOfficesByUserId(userProfileId.toString());

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result).extracting(OfficeDto::getCode).containsExactlyInAnyOrder("Office 1", "Office 2", "Office 3");
        }
    }

    @Nested
    class UpdateUserOfficesTests {

        @Test
        void updateUserOffices_saveOrRemoveOffices_whenUserAndProfileExistAndAllOfficesChosen() throws IOException {
            // Arrange
            UUID entraUserId = UUID.randomUUID();
            UUID userProfileId = UUID.randomUUID();

            UserProfile userProfile = UserProfile.builder()
                    .id(userProfileId)
                    .activeProfile(true)
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();
            EntraUser entraUser = EntraUser.builder()
                    .id(entraUserId)
                    .userProfiles(Set.of(userProfile))
                    .build();
            userProfile.setEntraUser(entraUser);

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));
            when(mockUserProfileRepository.saveAndFlush(any())).thenReturn(userProfile);

            List<String> selectedOffices = List.of("ALL");

            // Act
            userService.updateUserOffices(userProfileId.toString(), selectedOffices);

            // Assert
            assertThat(userProfile.getOffices()).isNull();
            verify(mockUserProfileRepository).saveAndFlush(userProfile);
        }

        @Test
        void updateUserOffices_updatesOffices_whenUserAndProfileExistAndSameFirm() throws IOException {
            // Arrange
            UUID firmId = UUID.randomUUID();
            UUID entraUserId = UUID.randomUUID();
            UUID userProfileId = UUID.randomUUID();
            UUID officeId1 = UUID.randomUUID();
            UUID officeId2 = UUID.randomUUID();

            Firm userFirm = Firm.builder().id(firmId).build();

            Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
            Office office1 = Office.builder().id(officeId1).address(address).firm(userFirm).build();
            Office office2 = Office.builder().id(officeId2).address(address).firm(userFirm).build();

            UserProfile userProfile = UserProfile.builder()
                    .id(userProfileId)
                    .activeProfile(true)
                    .firm(userFirm)
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();
            EntraUser entraUser = EntraUser.builder()
                    .id(entraUserId)
                    .userProfiles(Set.of(userProfile))
                    .build();
            userProfile.setEntraUser(entraUser);

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));
            when(mockOfficeRepository.findAllById(any())).thenReturn(List.of(office1, office2));
            when(mockUserProfileRepository.saveAndFlush(any())).thenReturn(userProfile);

            List<String> selectedOffices = List.of(officeId1.toString(), officeId2.toString());

            // Act
            userService.updateUserOffices(userProfileId.toString(), selectedOffices);

            // Assert
            assertThat(userProfile.getOffices()).containsExactlyInAnyOrder(office1, office2);
            verify(mockUserProfileRepository).saveAndFlush(userProfile);
        }

        @Test
        void updateUserOffices_doesNotUpdateAllOffices_whenUserAndProfileExistAndDifferentFirm() throws IOException {
            // Arrange
            UUID firmId = UUID.randomUUID();
            UUID entraUserId = UUID.randomUUID();
            UUID userProfileId = UUID.randomUUID();
            UUID officeId1 = UUID.randomUUID();
            UUID officeId2 = UUID.randomUUID();

            Firm userFirm = Firm.builder().id(firmId).build();

            Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
            Office office1 = Office.builder().id(officeId1).address(address).firm(userFirm).build();
            Office office2 = Office.builder().id(officeId2).address(address).build();

            UserProfile userProfile = UserProfile.builder()
                    .id(userProfileId)
                    .activeProfile(true)
                    .firm(userFirm)
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();
            EntraUser entraUser = EntraUser.builder()
                    .id(entraUserId)
                    .userProfiles(Set.of(userProfile))
                    .build();
            userProfile.setEntraUser(entraUser);

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));
            when(mockOfficeRepository.findAllById(any())).thenReturn(List.of(office1, office2));
            when(mockUserProfileRepository.saveAndFlush(any())).thenReturn(userProfile);

            List<String> selectedOffices = List.of(officeId1.toString(), officeId2.toString());

            // Act
            userService.updateUserOffices(userProfileId.toString(), selectedOffices);

            // Assert
            assertThat(userProfile.getOffices()).containsExactlyInAnyOrder(office1);
            verify(mockUserProfileRepository).saveAndFlush(userProfile);
        }

        @Test
        void updateUserOffices_throwsIoException_whenUserNotFound() {
            // Arrange
            UUID userProfileId = UUID.randomUUID();
            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.empty());

            List<String> selectedOffices = List.of(UUID.randomUUID().toString());

            // Act & Assert
            IOException exception = Assertions.assertThrows(IOException.class,
                    () -> userService.updateUserOffices(userProfileId.toString(), selectedOffices));
            assertThat(exception.getMessage()).contains("User profile not found for user ID: " + userProfileId);
        }

        @Test
        void updateUserOffices_throwsIoException_whenActiveProfileNotFound() {
            // Arrange
            UUID userProfileId = UUID.randomUUID();

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.empty());

            List<String> selectedOffices = List.of(UUID.randomUUID().toString());

            // Act & Assert
            IOException exception = Assertions.assertThrows(IOException.class,
                    () -> userService.updateUserOffices(userProfileId.toString(), selectedOffices));
            assertThat(exception.getMessage()).contains("User profile not found for user ID: " + userProfileId);
        }

        @Test
        void updateUserOffices_handlesEmptyOfficesList() throws IOException {
            // Arrange
            UUID entraUserId = UUID.randomUUID();
            UUID userProfileId = UUID.randomUUID();

            UserProfile userProfile = UserProfile.builder()
                    .id(userProfileId)
                    .activeProfile(true)
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();
            EntraUser entraUser = EntraUser.builder()
                    .id(entraUserId)
                    .userProfiles(Set.of(userProfile))
                    .build();
            userProfile.setEntraUser(entraUser);

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));
            when(mockOfficeRepository.findAllById(any())).thenReturn(Collections.emptyList());
            when(mockUserProfileRepository.saveAndFlush(any())).thenReturn(userProfile);

            // Act
            userService.updateUserOffices(userProfileId.toString(), Collections.emptyList());

            // Assert
            assertThat(userProfile.getOffices()).isEmpty();
            verify(mockUserProfileRepository).saveAndFlush(userProfile);
        }
    }

    @Nested
    class GetAllUsersTests {

        @Test
        void getAllUsers_returnsUserList_whenGraphReturnsUsers() {
            // Arrange
            User user1 = new User();
            user1.setDisplayName("User 1");
            User user2 = new User();
            user2.setDisplayName("User 2");

            UserCollectionResponse response = mock(UserCollectionResponse.class);
            when(response.getValue()).thenReturn(List.of(user1, user2));

            UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class);
            when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
            when(usersRequestBuilder.get()).thenReturn(response);

            // Act
            List<User> result = userService.getAllUsers();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).contains(user1, user2);
        }

        @Test
        void getAllUsers_returnsEmptyList_whenGraphReturnsNull() {
            // Arrange
            UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class);
            when(mockGraphServiceClient.users()).thenReturn(usersRequestBuilder);
            when(usersRequestBuilder.get()).thenReturn(null);

            // Act
            List<User> result = userService.getAllUsers();

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetDirectoryRolesByUserIdTests {

        @Test
        void getDirectoryRolesByUserId_returnsDirectoryRoles_whenRolesExist() {
            // Arrange
            DirectoryRole role1 = new DirectoryRole();
            role1.setDisplayName("Role 1");
            DirectoryRole role2 = new DirectoryRole();
            role2.setDisplayName("Role 2");

            // Mock a different type to test filtering - use DirectoryObject
            DirectoryObject nonDirectoryRole = mock(DirectoryObject.class);

            List<DirectoryObject> directoryObjects = new ArrayList<>();
            directoryObjects.add(role1);
            directoryObjects.add(nonDirectoryRole);
            directoryObjects.add(role2);

            DirectoryObjectCollectionResponse response = mock(DirectoryObjectCollectionResponse.class);
            when(response.getValue()).thenReturn(directoryObjects);

            String userId = "test-user-id";
            UsersRequestBuilder usersRb = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
            when(mockGraphServiceClient.users()).thenReturn(usersRb);
            when(usersRb.byUserId(userId).memberOf().get()).thenReturn(response);

            // Act
            List<DirectoryRole> result = userService.getDirectoryRolesByUserId(userId);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).contains(role1, role2);
        }

        @Test
        void getDirectoryRolesByUserId_throwsNullPointerException_whenResponseIsNull() {
            // Arrange
            String userId = "test-user-id";
            UsersRequestBuilder usersRb = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
            when(mockGraphServiceClient.users()).thenReturn(usersRb);
            when(usersRb.byUserId(userId).memberOf().get()).thenReturn(null);

            // Act & Assert
            NullPointerException exception = Assertions.assertThrows(NullPointerException.class,
                    () -> userService.getDirectoryRolesByUserId(userId));
            assertThat(exception).isNotNull();
        }
    }

    @Nested
    class GetUserAssignedAppsForLandingPageTests {

        @Test
        void getUserAssignedAppsforLandingPage_returnsMatchingApps_whenUserHasMatchingApps() {
            // Arrange
            UUID entraUserId = UUID.randomUUID();
            UUID userProfileId = UUID.randomUUID();
            UUID appId1 = UUID.randomUUID();
            UUID appId2 = UUID.randomUUID();

            App app1 = App.builder().id(appId1).name("Test App 1").build();
            App app2 = App.builder().id(appId2).name("Test App 2").build();

            AppRole role1 = AppRole.builder().app(app1).build();
            AppRole role2 = AppRole.builder().app(app2).build();

            UserProfile userProfile = UserProfile.builder()
                    .id(userProfileId)
                    .appRoles(Set.of(role1, role2))
                    .activeProfile(true)
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();
            EntraUser user = EntraUser.builder()
                    .id(entraUserId)
                    .userProfiles(Set.of(userProfile))
                    .build();

            when(mockEntraUserRepository.findById(entraUserId)).thenReturn(Optional.of(user));
            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

            List<LaaApplication> configuredApps = List.of(
                    LaaApplication.builder().name("Test App 1").ordinal(1).build(),
                    LaaApplication.builder().name("Test App 2").ordinal(2).build(),
                    LaaApplication.builder().name("Test App 3").ordinal(3).build(),
                    LaaApplication.builder().name("Non-matching App").ordinal(4).build());
            when(laaApplicationsList.getApplications()).thenReturn(configuredApps);

            // Act
            Set<LaaApplication> result = userService.getUserAssignedAppsforLandingPage(entraUserId.toString());

            // Assert
            assertThat(result).hasSize(2);
            List<String> resultNames = result.stream().map(LaaApplication::getName).toList();
            assertThat(resultNames).containsExactlyInAnyOrder("Test App 1", "Test App 2");
        }

        @Test
        void getUserAssignedAppsforLandingPage_returnsEmptySet_whenUserHasNoMatchingApps() {
            // Arrange
            UUID entraUserId = UUID.randomUUID();
            UUID userProfileId = UUID.randomUUID();
            App app = App.builder().name("Non-configured App").build();
            AppRole role = AppRole.builder().app(app).build();

            UserProfile userProfile = UserProfile.builder()
                    .id(userProfileId)
                    .appRoles(Set.of(role))
                    .activeProfile(true)
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();
            EntraUser user = EntraUser.builder()
                    .id(entraUserId)
                    .userProfiles(Set.of(userProfile))
                    .build();

            when(mockEntraUserRepository.findById(entraUserId)).thenReturn(Optional.of(user));
            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

            List<LaaApplication> configuredApps = List.of(
                    LaaApplication.builder().name("Different App").ordinal(1).build());
            when(laaApplicationsList.getApplications()).thenReturn(configuredApps);

            // Act
            Set<LaaApplication> result = userService.getUserAssignedAppsforLandingPage(entraUserId.toString());

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        void getUserAssignedAppsforLandingPage_returnsSortedByOrdinal() {
            // Arrange
            UUID entraUserId = UUID.randomUUID();
            UUID userProfileId = UUID.randomUUID();
            App app1 = App.builder().name("App C").build();
            App app2 = App.builder().name("App A").build();
            App app3 = App.builder().name("App B").build();

            AppRole role1 = AppRole.builder().app(app1).build();
            AppRole role2 = AppRole.builder().app(app2).build();
            AppRole role3 = AppRole.builder().app(app3).build();

            UserProfile userProfile = UserProfile.builder()
                    .id(userProfileId)
                    .appRoles(Set.of(role1, role2, role3))
                    .activeProfile(true)
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();
            EntraUser user = EntraUser.builder()
                    .id(entraUserId)
                    .userProfiles(Set.of(userProfile))
                    .build();

            when(mockEntraUserRepository.findById(entraUserId)).thenReturn(Optional.of(user));
            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

            List<LaaApplication> configuredApps = List.of(
                    LaaApplication.builder().name("App C").ordinal(3).build(),
                    LaaApplication.builder().name("App A").ordinal(1).build(),
                    LaaApplication.builder().name("App B").ordinal(2).build());
            when(laaApplicationsList.getApplications()).thenReturn(configuredApps);

            // Act
            Set<LaaApplication> result = userService.getUserAssignedAppsforLandingPage(entraUserId.toString());

            // Assert
            assertThat(result).hasSize(3);
            List<LaaApplication> resultList = new ArrayList<>(result);
            assertThat(resultList.get(0).getName()).isEqualTo("App A");
            assertThat(resultList.get(1).getName()).isEqualTo("App B");
            assertThat(resultList.get(2).getName()).isEqualTo("App C");
        }
    }

    @Nested
    class CreateUserTests {

        @Test
        void createUser_createsAndInvitesUser_withCorrectUserProfile() {
            // Arrange
            EntraUserDto user = new EntraUserDto();
            user.setEmail("test@example.com");
            user.setFirstName("John");
            user.setLastName("Doe");

            EntraUser savedUser = EntraUser.builder().build();
            when(mockEntraUserRepository.saveAndFlush(any(EntraUser.class))).thenReturn(savedUser);

            RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
            createdUser.setId("id");
            createdUser.setMail("test@example.com");
            RegisterUserResponse registerUserResponse = RegisterUserResponse.builder().createdUser(createdUser).build();
            when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);
            FirmDto firmDto = FirmDto.builder().name("Test Firm").build();

            // Act
            EntraUser result = userService.createUser(user, firmDto, UserType.EXTERNAL_SINGLE_FIRM, "admin");

            // Assert
            assertThat(result).isNotNull();
            verify(mockEntraUserRepository).saveAndFlush(any(EntraUser.class));
            verify(techServicesClient).registerNewUser(any(EntraUserDto.class));
        }

        @Test
        void createUser_setsCorrectUserType_whenIsFirmAdminFalse() {
            // Arrange
            EntraUserDto user = new EntraUserDto();
            user.setEmail("test@example.com");

            ArgumentCaptor<EntraUser> userCaptor = ArgumentCaptor.forClass(EntraUser.class);
            when(mockEntraUserRepository.saveAndFlush(userCaptor.capture())).thenReturn(EntraUser.builder().build());

            RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
            createdUser.setId("id");
            createdUser.setMail("test@example.com");
            RegisterUserResponse registerUserResponse = RegisterUserResponse.builder().createdUser(createdUser).build();
            when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);

            FirmDto firmDto = FirmDto.builder().name("Test Firm").build();

            // Act
            userService.createUser(user, firmDto, UserType.EXTERNAL_SINGLE_FIRM, "admin");

            // Assert
            EntraUser capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getUserProfiles()).hasSize(1);
            UserProfile profile = capturedUser.getUserProfiles().iterator().next();
            assertThat(profile.getUserType()).isEqualTo(UserType.EXTERNAL_SINGLE_FIRM);
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void getAllAvailableRolesForApps_handlesEmptyAppsList() {
            // Arrange
            when(mockAppRepository.findAllById(any())).thenReturn(Collections.emptyList());

            // Act
            List<AppRoleDto> result = userService.getAllAvailableRolesForApps(Collections.emptyList());

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        void getAllAvailableRolesForApps_handlesNullAppRoles() {
            // Arrange
            App app = App.builder().id(UUID.randomUUID()).name("Test App").appRoles(new HashSet<>()).build();
            when(mockAppRepository.findAllById(any())).thenReturn(List.of(app));

            // Act - should not throw NPE
            List<AppRoleDto> result = userService.getAllAvailableRolesForApps(
                    List.of(app.getId().toString()));
            assertThat(result).isEmpty();
        }

        @Test
        void updateUserRoles_handlesEmptyRolesList() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserProfile userProfile = UserProfile.builder().activeProfile(true).userType(UserType.INTERNAL).build();
            EntraUser user = EntraUser.builder().id(userId).userProfiles(Set.of(userProfile)).build();
            userProfile.setEntraUser(user);

            when(mockAppRoleRepository.findAllById(any())).thenReturn(Collections.emptyList());
            when(mockUserProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));

            // Act
            userService.updateUserRoles(userId.toString(), Collections.emptyList());

            // Assert
            assertThat(userProfile.getAppRoles()).isEmpty();
            verify(mockUserProfileRepository).save(userProfile);
            verify(techServicesClient, times(1)).updateRoleAssignment(userId);
        }

        @Test
        void getUserAuthorities_handlesInactiveUser() {
            // Arrange
            EntraUser entraUser = EntraUser.builder()
                    .firstName("Test")
                    .userStatus(UserStatus.DEACTIVE) // Use DEACTIVE instead of INACTIVE
                    .userProfiles(Set.of(UserProfile.builder()
                            .userType(UserType.EXTERNAL_SINGLE_FIRM)
                            .build()))
                    .build();
            when(mockEntraUserRepository.findByEntraOid(anyString())).thenReturn(Optional.of(entraUser));

            // Act
            List<String> result = userService.getUserAuthorities("test-id");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        void getUserAuthorities_handlesUserNotFound() {
            // Arrange
            when(mockEntraUserRepository.findByEntraOid(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                    () -> userService.getUserAuthorities("test-id"));
            assertThat(exception.getMessage()).contains("User not found for the given entra id");
        }

        @Test
        void formatLastSignInDateTime_handlesVariousDateFormats() {
            // Test different time zones and formats
            OffsetDateTime utcTime = OffsetDateTime.parse("2024-12-31T23:59:59Z");
            OffsetDateTime offsetTime = OffsetDateTime.parse("2024-01-01T12:30:45+05:00");

            String utcResult = userService.formatLastSignInDateTime(utcTime);
            String offsetResult = userService.formatLastSignInDateTime(offsetTime);

            assertThat(utcResult).isEqualTo("31 December 2024, 23:59");
            assertThat(offsetResult).isEqualTo("1 January 2024, 12:30");
        }

        @Test
        void isInternal_handlesMultipleUserTypes() {
            // Arrange - user with both internal and external types
            Permission userPermission = Permission.VIEW_INTERNAL_USER;
            AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();

            Set<UserProfile> userProfiles = Set.of(
                    UserProfile.builder().appRoles(Set.of(appRole)).activeProfile(true).userType(UserType.INTERNAL).build(),
                    UserProfile.builder().userType(UserType.EXTERNAL_SINGLE_FIRM).build());
            UUID userId = UUID.randomUUID();
            EntraUser entraUser = EntraUser.builder().id(userId).userProfiles(userProfiles).build();
            when(mockEntraUserRepository.findById(any())).thenReturn(Optional.of(entraUser));


            // Act
            boolean result = userService.isInternal(entraUser.getId());

            // Assert
            assertThat(result).isTrue(); // Should return true if ANY profile is internal
        }

    } // End of EdgeCaseTests nested class

    @Test
    void getDefaultSort() {
        Sort nullSort = userService.getSort(null, null);
        assertThat(nullSort.stream().toList().get(0).getProperty()).isEqualTo("userProfileStatus");
        assertThat(nullSort.stream().toList().get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(nullSort.stream().toList().get(1).getProperty()).isEqualTo("entraUser.firstName");
        assertThat(nullSort.stream().toList().get(1).getDirection()).isEqualTo(Sort.Direction.ASC);

        Sort emptySort = userService.getSort("", null);
        assertThat(emptySort.stream().toList().get(0).getProperty()).isEqualTo("userProfileStatus");
        assertThat(emptySort.stream().toList().get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(emptySort.stream().toList().get(1).getProperty()).isEqualTo("entraUser.firstName");
        assertThat(emptySort.stream().toList().get(1).getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getGivenSort() {
        Map<String, String> fieldMappings = Map.of(
            "firstName", "entraUser.firstName",
            "lastName", "entraUser.lastName",
            "email", "entraUser.email",
            "eMAIl", "entraUser.email",
            "lAstName", "entraUser.lastName",
            "USERSTATUS", "userProfileStatus"
        );

        String sort = "aSc";
        for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
            String inputField = entry.getKey();
            String expectedProperty = entry.getValue();
            Sort fieldSort = userService.getSort(inputField, sort);
            assertThat(fieldSort.stream().toList().getFirst().getProperty()).isEqualTo(expectedProperty);
            assertThat(fieldSort.stream().toList().getFirst().getDirection()).isEqualTo(Sort.Direction.ASC);
        }
        Sort descendingSort = userService.getSort("lastName", "deSC");
        assertThat(descendingSort.stream().toList().getFirst().getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getErrorSort() {
        assertThrows(IllegalArgumentException.class, () -> userService.getSort("error", null));
    }

    @Test
    void setDefaultActiveProfile_switch_firm() throws IOException {
        UUID firm1Id = UUID.randomUUID();
        UUID firm2Id = UUID.randomUUID();
        UserProfile userProfile1 = UserProfile.builder().activeProfile(true).firm(Firm.builder().id(firm1Id).build()).build();
        UserProfile userProfile2 = UserProfile.builder().activeProfile(false).firm(Firm.builder().id(firm2Id).build()).build();
        EntraUser entraUser = EntraUser.builder().userProfiles(Set.of(userProfile1, userProfile2)).build();
        ArgumentCaptor<EntraUser> captor = ArgumentCaptor.forClass(EntraUser.class);
        userService.setDefaultActiveProfile(entraUser, firm2Id);
        verify(mockEntraUserRepository).saveAndFlush(captor.capture());
        EntraUser updatedEntraUser = captor.getValue();
        for (UserProfile userProfile : updatedEntraUser.getUserProfiles()) {
            if (userProfile.getFirm().getId().equals(firm1Id)) {
                assertThat(userProfile.isActiveProfile()).isFalse();
            } else {
                assertThat(userProfile.isActiveProfile()).isTrue();
                assertThat(userProfile.getFirm().getId()).isEqualTo(firm2Id);
            }
        }
    }

    @Test
    void setDefaultActiveProfile_no_firm() throws IOException {
        UUID firm1Id = UUID.randomUUID();
        UUID firm2Id = UUID.randomUUID();
        UUID firm3Id = UUID.randomUUID();
        UserProfile userProfile1 = UserProfile.builder().activeProfile(true).firm(Firm.builder().id(firm1Id).build()).build();
        UserProfile userProfile2 = UserProfile.builder().activeProfile(false).firm(Firm.builder().id(firm2Id).build()).build();
        EntraUser entraUser = EntraUser.builder().userProfiles(Set.of(userProfile1, userProfile2)).build();
        assertThrows(IOException.class,
                () -> userService.setDefaultActiveProfile(entraUser, firm3Id));
    }

    @Test
    void isAccessGranted_returnsTrue_whenUserProfileStatusIsComplete() {
        // Given
        String userId = UUID.randomUUID().toString();
        UserProfile userProfile = UserProfile.builder()
                .id(UUID.fromString(userId))
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .build();
        when(mockUserProfileRepository.findById(UUID.fromString(userId)))
                .thenReturn(Optional.of(userProfile));

        // When
        boolean result = userService.isAccessGranted(userId);

        // Then
        assertThat(result).isTrue();
        verify(mockUserProfileRepository).findById(UUID.fromString(userId));
    }

    @Test
    void isAccessGranted_returnsFalse_whenUserProfileStatusIsNotComplete() {
        // Given
        String userId = UUID.randomUUID().toString();
        UserProfile userProfile = UserProfile.builder()
                .id(UUID.fromString(userId))
                .userProfileStatus(UserProfileStatus.PENDING)
                .build();
        when(mockUserProfileRepository.findById(UUID.fromString(userId)))
                .thenReturn(Optional.of(userProfile));

        // When
        boolean result = userService.isAccessGranted(userId);

        // Then
        assertThat(result).isFalse();
        verify(mockUserProfileRepository).findById(UUID.fromString(userId));
    }

    @Test
    void isAccessGranted_returnsFalse_whenUserProfileNotFound() {
        // Given
        String userId = UUID.randomUUID().toString();
        when(mockUserProfileRepository.findById(UUID.fromString(userId)))
                .thenReturn(Optional.empty());

        // When
        boolean result = userService.isAccessGranted(userId);

        // Then
        assertThat(result).isFalse();
        verify(mockUserProfileRepository).findById(UUID.fromString(userId));
    }

    @Test
    void grantAccess_returnsTrue_whenUserProfileExists() {
        // Given
        String userId = UUID.randomUUID().toString();
        String currentUserName = "admin";
        UserProfile userProfile = UserProfile.builder()
                .id(UUID.fromString(userId))
                .userProfileStatus(UserProfileStatus.PENDING)
                .build();
        when(mockUserProfileRepository.findById(UUID.fromString(userId)))
                .thenReturn(Optional.of(userProfile));
        when(mockUserProfileRepository.saveAndFlush(any(UserProfile.class)))
                .thenReturn(userProfile);

        // When
        boolean result = userService.grantAccess(userId, currentUserName);

        // Then
        assertThat(result).isTrue();
        assertThat(userProfile.getUserProfileStatus()).isEqualTo(UserProfileStatus.COMPLETE);
        assertThat(userProfile.getLastModifiedBy()).isEqualTo(currentUserName);
        assertThat(userProfile.getLastModified()).isNotNull();
        verify(mockUserProfileRepository).findById(UUID.fromString(userId));
        verify(mockUserProfileRepository).saveAndFlush(userProfile);
    }

    @Test
    void grantAccess_returnsFalse_whenUserProfileNotFound() {
        // Given
        String userId = UUID.randomUUID().toString();
        String currentUserName = "admin";
        when(mockUserProfileRepository.findById(UUID.fromString(userId)))
                .thenReturn(Optional.empty());

        // When
        boolean result = userService.grantAccess(userId, currentUserName);

        // Then
        assertThat(result).isFalse();
        verify(mockUserProfileRepository).findById(UUID.fromString(userId));
        verify(mockUserProfileRepository, times(0)).saveAndFlush(any(UserProfile.class));
    }

    @Test
    void getAppsByRoleType_throughGetAppsByUserType_internal() {
        // Given
        AppRole appRole1 = AppRole.builder()
                .id(UUID.randomUUID())
                .roleType(RoleType.INTERNAL)
                .app(App.builder().id(UUID.randomUUID()).name("Internal App").build())
                .build();
        AppRole appRole2 = AppRole.builder()
                .id(UUID.randomUUID())
                .roleType(RoleType.INTERNAL_AND_EXTERNAL)
                .app(App.builder().id(UUID.randomUUID()).name("Common App").build())
                .build();
        when(mockAppRoleRepository.findByRoleTypeIn(List.of(RoleType.INTERNAL, RoleType.INTERNAL_AND_EXTERNAL)))
                .thenReturn(List.of(appRole1, appRole2));

        // When
        List<AppDto> result = userService.getAppsByUserType(UserType.INTERNAL);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(AppDto::getName))
                .containsExactlyInAnyOrder("Internal App", "Common App");
        verify(mockAppRoleRepository).findByRoleTypeIn(List.of(RoleType.INTERNAL, RoleType.INTERNAL_AND_EXTERNAL));
    }

    @Test
    void getAppsByRoleType_throughGetAppsByUserType_external() {
        // Given
        AppRole appRole1 = AppRole.builder()
                .id(UUID.randomUUID())
                .roleType(RoleType.EXTERNAL)
                .app(App.builder().id(UUID.randomUUID()).name("External App").build())
                .build();
        AppRole appRole2 = AppRole.builder()
                .id(UUID.randomUUID())
                .roleType(RoleType.INTERNAL_AND_EXTERNAL)
                .app(App.builder().id(UUID.randomUUID()).name("Common App").build())
                .build();
        when(mockAppRoleRepository.findByRoleTypeIn(List.of(RoleType.EXTERNAL, RoleType.INTERNAL_AND_EXTERNAL)))
                .thenReturn(List.of(appRole1, appRole2));

        // When
        List<AppDto> result = userService.getAppsByUserType(UserType.EXTERNAL_SINGLE_FIRM);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(AppDto::getName))
                .containsExactlyInAnyOrder("External App", "Common App");
        verify(mockAppRoleRepository).findByRoleTypeIn(List.of(RoleType.EXTERNAL, RoleType.INTERNAL_AND_EXTERNAL));
    }

    @Test
    void getAppsByRoleType_removingDuplicateApps() {
        // Given - Same app appears in multiple roles
        App sharedApp = App.builder().id(UUID.randomUUID()).name("Shared App").build();
        AppRole appRole1 = AppRole.builder()
                .id(UUID.randomUUID())
                .roleType(RoleType.INTERNAL)
                .app(sharedApp)
                .build();
        AppRole appRole2 = AppRole.builder()
                .id(UUID.randomUUID())
                .roleType(RoleType.INTERNAL_AND_EXTERNAL)
                .app(sharedApp)
                .build();
        when(mockAppRoleRepository.findByRoleTypeIn(List.of(RoleType.INTERNAL, RoleType.INTERNAL_AND_EXTERNAL)))
                .thenReturn(List.of(appRole1, appRole2));

        // When
        List<AppDto> result = userService.getAppsByUserType(UserType.INTERNAL);

        // Then - Should only appear once due to distinct()
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Shared App");
        verify(mockAppRoleRepository).findByRoleTypeIn(List.of(RoleType.INTERNAL, RoleType.INTERNAL_AND_EXTERNAL));
    }

    @Test
    void shouldReturnInteralUserIds() {
        List<UUID> expectedIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(mockUserProfileRepository.findByUserTypes(UserType.INTERNAL)).thenReturn(expectedIds);

        List<UUID> result = userService.getInternalUserEntraIds();

        assertThat(result).isEqualTo(expectedIds);
    }

    @Test
    void shouldSaveNewInternalUser() {
        // Arrange
        EntraUserDto dto1 = EntraUserDto.builder()
                .entraOid(UUID.randomUUID().toString())
                .email("user1@example.com")
                .firstName("User1")
                .lastName("Test1").build();
        EntraUserDto dto2 = EntraUserDto.builder()
                .entraOid(UUID.randomUUID().toString())
                .email("user2@example.com")
                .firstName("User2")
                .lastName("Test2").build();
        List<EntraUserDto> dtos = List.of(dto1, dto2);

        List<EntraUser> savedUsers = new ArrayList<>();
        when(mockEntraUserRepository.saveAndFlush(any())).then(invocation -> {
            savedUsers.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });

        // Act
        int actual = userService.createInternalPolledUser(dtos);

        // Assert
        verify(mockEntraUserRepository, times(2)).saveAndFlush(any());
        assertThat(actual).isEqualTo(2);
    }

    @Test
    void shouldSkipCreatingInternalUser_whenNoNewUsers() {
        // Act
        int actual = userService.createInternalPolledUser(List.of());
        // Assert
        assertThat(actual).isEqualTo(0);
    }

    @Test
    void removeUserAppRole_shouldSuccessfullyRemoveRole() {
        // Arrange
        UUID userProfileId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        String roleName = "TestRole";

        AppRole roleToRemove = AppRole.builder()
                .id(UUID.randomUUID())
                .name(roleName)
                .build();

        App app = App.builder()
                .id(appId)
                .name("Test App")
                .build();

        roleToRemove.setApp(app);

        AppRole otherRole = AppRole.builder()
                .id(UUID.randomUUID())
                .name("OtherRole")
                .build();

        App otherApp = App.builder()
                .id(UUID.randomUUID())
                .name("Other App")
                .build();

        otherRole.setApp(otherApp);

        UserProfile userProfile = UserProfile.builder()
                .id(userProfileId)
                .appRoles(new HashSet<>(Set.of(roleToRemove, otherRole)))
                .build();

        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

        // Act
        userService.removeUserAppRole(userProfileId.toString(), appId.toString(), roleName);

        // Assert
        assertThat(userProfile.getAppRoles()).hasSize(1);
        assertThat(userProfile.getAppRoles()).doesNotContain(roleToRemove);
        assertThat(userProfile.getAppRoles()).contains(otherRole);
        verify(mockUserProfileRepository).saveAndFlush(userProfile);
    }

    @Test
    void removeUserAppRole_shouldWarnWhenRoleNotFound() {
        // Arrange
        UUID userProfileId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        String roleName = "NonExistentRole";

        AppRole existingRole = AppRole.builder()
                .id(UUID.randomUUID())
                .name("ExistingRole")
                .build();

        App app = App.builder()
                .id(UUID.randomUUID())
                .name("Test App")
                .build();

        existingRole.setApp(app);

        UserProfile userProfile = UserProfile.builder()
                .id(userProfileId)
                .appRoles(new HashSet<>(Set.of(existingRole)))
                .build();

        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

        // Act
        userService.removeUserAppRole(userProfileId.toString(), appId.toString(), roleName);

        // Assert - no roles should be removed
        assertThat(userProfile.getAppRoles()).hasSize(1);
        assertThat(userProfile.getAppRoles()).contains(existingRole);
        // saveAndFlush should not be called when no role is removed
        verify(mockUserProfileRepository, times(0)).saveAndFlush(any());
    }

    @Test
    void removeUserAppRole_shouldWarnWhenUserNotFound() {
        // Arrange
        UUID userProfileId = UUID.randomUUID();
        String appId = "app123";
        String roleName = "TestRole";

        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.empty());

        // Act
        userService.removeUserAppRole(userProfileId.toString(), appId, roleName);

        // Assert - saveAndFlush should not be called when user not found
        verify(mockUserProfileRepository, times(0)).saveAndFlush(any());
    }

    @Test
    void removeUserAppRole_shouldRemoveOnlyMatchingAppAndRole() {
        // Arrange
        final UUID userProfileId = UUID.randomUUID();
        final UUID appId = UUID.randomUUID();
        final String roleName = "TestRole";

        // Role with matching app and name (should be removed)
        AppRole roleToRemove = AppRole.builder()
                .id(UUID.randomUUID())
                .name(roleName)
                .build();

        App targetApp = App.builder()
                .id(appId)
                .name("Target App")
                .build();

        roleToRemove.setApp(targetApp);

        // Role with same name but different app (should NOT be removed)
        AppRole sameNameDifferentApp = AppRole.builder()
                .id(UUID.randomUUID())
                .name(roleName)
                .build();

        App differentApp = App.builder()
                .id(UUID.randomUUID())
                .name("Different App")
                .build();

        sameNameDifferentApp.setApp(differentApp);

        // Role with same app but different name (should NOT be removed)
        AppRole sameAppDifferentName = AppRole.builder()
                .id(UUID.randomUUID())
                .name("DifferentRole")
                .build();

        sameAppDifferentName.setApp(targetApp);

        UserProfile userProfile = UserProfile.builder()
                .id(userProfileId)
                .appRoles(new HashSet<>(Set.of(roleToRemove, sameNameDifferentApp, sameAppDifferentName)))
                .build();

        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

        // Act
        userService.removeUserAppRole(userProfileId.toString(), appId.toString(), roleName);

        // Assert
        assertThat(userProfile.getAppRoles()).hasSize(2);
        assertThat(userProfile.getAppRoles()).doesNotContain(roleToRemove);
        assertThat(userProfile.getAppRoles()).contains(sameNameDifferentApp);
        assertThat(userProfile.getAppRoles()).contains(sameAppDifferentName);
        verify(mockUserProfileRepository).saveAndFlush(userProfile);
    }

    @Test
    void removeUserAppRole_shouldHandleEmptyRolesSet() {
        // Arrange
        UUID userProfileId = UUID.randomUUID();
        String appId = "app123";
        String roleName = "TestRole";

        UserProfile userProfile = UserProfile.builder()
                .id(userProfileId)
                .appRoles(new HashSet<>())
                .build();

        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

        // Act
        userService.removeUserAppRole(userProfileId.toString(), appId, roleName);

        // Assert - no roles should be removed from empty set
        assertThat(userProfile.getAppRoles()).isEmpty();
        // saveAndFlush should not be called when no role is removed
        verify(mockUserProfileRepository, times(0)).saveAndFlush(any());
    }

    @Test
    void getPageOfUsersByNameOrEmailAndPermissionsAndFirm_returnsValidPage() {
        // Given
        String searchTerm = "test";
        List<Permission> permissions = List.of(Permission.CREATE_EXTERNAL_USER);
        UUID firmId = UUID.randomUUID();
        int page = 1;
        int pageSize = 10;
        String sort = "firstName";
        String direction = "ASC";

        UserProfile userProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .userType(UserType.EXTERNAL_SINGLE_FIRM)
                .entraUser(EntraUser.builder()
                        .firstName("Test")
                        .lastName("User")
                        .email("test@example.com")
                        .build())
                .firm(Firm.builder().id(firmId).name("Test Firm").build())
                .build();

        Page<UserProfile> userProfilePage = new PageImpl<>(
                List.of(userProfile),
                PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "entraUser.firstName")),
                1
        );

        when(mockUserProfileRepository.findByNameOrEmailAndPermissionsAndFirm(
                eq(searchTerm), eq(permissions), eq(permissions.size()), eq(firmId), eq(UserType.EXTERNAL_TYPES), any(PageRequest.class)))
                .thenReturn(userProfilePage);

        // When
        PaginatedUsers result = userService.getPageOfUsersByNameOrEmailAndPermissionsAndFirm(
                searchTerm, permissions, firmId, UserType.EXTERNAL_TYPES, page, pageSize, sort, direction);

        // Then
        assertThat(result.getUsers()).hasSize(1);
        assertThat(result.getTotalUsers()).isEqualTo(1);
        verify(mockUserProfileRepository).findByNameOrEmailAndPermissionsAndFirm(
                eq(searchTerm), eq(permissions), eq(permissions.size()), eq(firmId), eq(UserType.EXTERNAL_TYPES), any(PageRequest.class));
    }

    @Test
    void getPageOfUsersByNameOrEmailAndPermissionsAndFirm_withEmptyPermissions() {
        // Given
        String searchTerm = "test";
        List<Permission> permissions = List.of(); // Empty list
        UUID firmId = UUID.randomUUID();
        int page = 1;
        int pageSize = 10;
        String sort = "firstName";
        String direction = "ASC";

        Page<UserProfile> userProfilePage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "entraUser.firstName")),
                0
        );

        when(mockUserProfileRepository.findByNameOrEmailAndPermissionsAndFirm(
                eq(searchTerm), eq(null), eq(0), eq(firmId), eq(UserType.EXTERNAL_TYPES), any(PageRequest.class)))
                .thenReturn(userProfilePage);

        // When
        PaginatedUsers result = userService.getPageOfUsersByNameOrEmailAndPermissionsAndFirm(
                searchTerm, permissions, firmId, UserType.EXTERNAL_TYPES, page, pageSize, sort, direction);

        // Then
        assertThat(result.getUsers()).hasSize(0);
        verify(mockUserProfileRepository).findByNameOrEmailAndPermissionsAndFirm(
                eq(searchTerm), eq(null), eq(0), eq(firmId), eq(UserType.EXTERNAL_TYPES), any(PageRequest.class));
    }

    @Test
    void getUserPermissionsByUserId_withStringId_returnsPermissions() {
        // Given
        String userId = UUID.randomUUID().toString();
        UUID userUuid = UUID.fromString(userId);

        EntraUser entraUser = EntraUser.builder()
                .id(userUuid)
                .build();

        UserProfile userProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .activeProfile(true)
                .build();

        AppRole appRole = AppRole.builder()
                .id(UUID.randomUUID())
                .authzRole(true)
                .permissions(Set.of(Permission.CREATE_EXTERNAL_USER))
                .build();

        userProfile.setAppRoles(Set.of(appRole));
        entraUser.setUserProfiles(Set.of(userProfile));

        when(mockEntraUserRepository.findById(userUuid)).thenReturn(Optional.of(entraUser));

        // When
        Set<Permission> result = userService.getUserPermissionsByUserId(userId);

        // Then
        assertThat(result).contains(Permission.CREATE_EXTERNAL_USER);
        verify(mockEntraUserRepository).findById(userUuid);
    }

    @Test
    void getPageOfUsersByNameOrEmailAndPermissionsAndFirm_searchByFullName() {
        String searchTerm = "Test Name";
        List<Permission> permissions = List.of(Permission.VIEW_EXTERNAL_USER);
        UUID firmId = UUID.randomUUID();
        int page = 1;
        int pageSize = 10;
        String sort = "firstName";
        String direction = "ASC";

        UserProfile userProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .userType(UserType.EXTERNAL_SINGLE_FIRM)
                .entraUser(EntraUser.builder()
                        .firstName("Test")
                        .lastName("Name")
                        .email("test.name@example.com")
                        .build())
                .firm(Firm.builder().id(firmId).name("Test Firm").build())
                .build();

        Page<UserProfile> userProfilePage = new PageImpl<>(
                List.of(userProfile),
                PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "entraUser.firstName")),
                1
        );

        when(mockUserProfileRepository.findByNameOrEmailAndPermissionsAndFirm(
                eq(searchTerm), eq(permissions), eq(permissions.size()), eq(firmId), eq(UserType.EXTERNAL_TYPES), any(PageRequest.class)))
                .thenReturn(userProfilePage);

        // When
        PaginatedUsers result = userService.getPageOfUsersByNameOrEmailAndPermissionsAndFirm(
                searchTerm, permissions, firmId, UserType.EXTERNAL_TYPES, page, pageSize, sort, direction);

        // Then
        assertThat(result.getUsers()).hasSize(1);
        assertThat(result.getTotalUsers()).isEqualTo(1);
        assertThat(result.getUsers().get(0).getEntraUser().getFirstName()).isEqualTo("Test");
        assertThat(result.getUsers().get(0).getEntraUser().getLastName()).isEqualTo("Name");

        // Verify the repository was called with the full name search term
        verify(mockUserProfileRepository).findByNameOrEmailAndPermissionsAndFirm(
                eq("Test Name"), eq(permissions), eq(permissions.size()), eq(firmId), eq(UserType.EXTERNAL_TYPES), any(PageRequest.class));
    }

    @Nested
    class RoleChangeTests {

        @Test
        void updateUserRoles_successfulUpdate_sendsRoleChangeNotification() {
            String userProfileId = UUID.randomUUID().toString();
            List<String> selectedRoles = List.of(UUID.randomUUID().toString());

            UUID entraUserId = UUID.randomUUID();
            EntraUser entraUser = EntraUser.builder()
                    .id(entraUserId)
                    .entraOid("test-entra-oid")
                    .build();

            UserProfile userProfile = UserProfile.builder()
                    .id(UUID.fromString(userProfileId))
                    .userType(UserType.EXTERNAL_SINGLE_FIRM)
                    .entraUser(entraUser)
                    .legacyUserId(UUID.randomUUID())
                    .build();

            AppRole oldRole = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("OLD_ROLE")
                    .description("Old Role Description")
                    .ccmsCode("CCMS_OLD")
                    .legacySync(true)
                    .roleType(RoleType.EXTERNAL)
                    .authzRole(false)
                    .build();

            final AppRole newRole = AppRole.builder()
                    .id(UUID.fromString(selectedRoles.get(0)))
                    .name("NEW_ROLE")
                    .description("New Role Description")
                    .ccmsCode("CCMS_NEW")
                    .legacySync(true)
                    .roleType(RoleType.EXTERNAL)
                    .authzRole(false)
                    .build();

            userProfile.setAppRoles(Set.of(oldRole));
            entraUser.setUserProfiles(Set.of(userProfile));

            when(mockUserProfileRepository.findById(UUID.fromString(userProfileId)))
                    .thenReturn(Optional.of(userProfile));
            when(mockAppRoleRepository.findAllById(any()))
                    .thenReturn(List.of(newRole));
            when(mockUserProfileRepository.save(any(UserProfile.class)))
                    .thenReturn(userProfile);
            when(mockRoleChangeNotificationService.sendMessage(
                    any(UserProfile.class), any(Set.class), any(Set.class)))
                    .thenReturn(true);

            userService.updateUserRoles(userProfileId, selectedRoles);

            ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
            verify(mockUserProfileRepository).save(userProfileCaptor.capture());
            verify(mockRoleChangeNotificationService).sendMessage(
                    eq(userProfile),
                    eq(Set.of(newRole)),
                    eq(Set.of(oldRole))
            );

            UserProfile savedProfile = userProfileCaptor.getValue();
            assertThat(savedProfile.isLastCcmsSyncSuccessful()).isTrue();
        }

        @Test
        void updateUserRoles_roleChangeNotificationFails_updatedRolesSavedToDb() {
            String userProfileId = UUID.randomUUID().toString();
            List<String> selectedRoles = List.of(UUID.randomUUID().toString());

            UUID entraUserId = UUID.randomUUID();
            EntraUser entraUser = EntraUser.builder()
                    .id(entraUserId)
                    .entraOid("test-entra-oid")
                    .build();

            UserProfile userProfile = UserProfile.builder()
                    .id(UUID.fromString(userProfileId))
                    .userType(UserType.EXTERNAL_SINGLE_FIRM)
                    .entraUser(entraUser)
                    .legacyUserId(UUID.randomUUID())
                    .build();

            final AppRole newRole = AppRole.builder()
                    .id(UUID.fromString(selectedRoles.get(0)))
                    .name("NEW_ROLE")
                    .description("New Role Description")
                    .ccmsCode("CCMS_NEW")
                    .legacySync(true)
                    .roleType(RoleType.EXTERNAL)
                    .authzRole(false)
                    .build();

            userProfile.setAppRoles(Set.of());
            entraUser.setUserProfiles(Set.of(userProfile));

            when(mockUserProfileRepository.findById(UUID.fromString(userProfileId)))
                    .thenReturn(Optional.of(userProfile));
            when(mockAppRoleRepository.findAllById(any()))
                    .thenReturn(List.of(newRole));
            when(mockUserProfileRepository.save(any(UserProfile.class)))
                    .thenReturn(userProfile);
            when(mockRoleChangeNotificationService.sendMessage(
                    any(UserProfile.class), any(Set.class), any(Set.class)))
                    .thenReturn(false);

            userService.updateUserRoles(userProfileId, selectedRoles);

            ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
            verify(mockUserProfileRepository).save(userProfileCaptor.capture());
            verify(mockRoleChangeNotificationService).sendMessage(any(), any(), any());

            UserProfile savedProfile = userProfileCaptor.getValue();
            assertThat(savedProfile.isLastCcmsSyncSuccessful()).isFalse();
        }

        @Test
        void updateUserRoles_userProfileNotFound_logsWarning() {
            String userProfileId = UUID.randomUUID().toString();
            List<String> selectedRoles = List.of(UUID.randomUUID().toString());

            when(mockUserProfileRepository.findById(UUID.fromString(userProfileId)))
                    .thenReturn(Optional.empty());

            userService.updateUserRoles(userProfileId, selectedRoles);

            verify(mockUserProfileRepository, never()).save(any());
            verify(mockRoleChangeNotificationService, never()).sendMessage(any(), any(), any());
        }

        @Test
        void updateUserRoles_noPuiRoleChanges_doesNotSendNotification() {
            String userProfileId = UUID.randomUUID().toString();
            List<String> selectedRoles = List.of(UUID.randomUUID().toString());

            UUID entraUserId = UUID.randomUUID();
            EntraUser entraUser = EntraUser.builder()
                    .id(entraUserId)
                    .entraOid("test-entra-oid")
                    .build();

            UserProfile userProfile = UserProfile.builder()
                    .id(UUID.fromString(userProfileId))
                    .userType(UserType.EXTERNAL_SINGLE_FIRM)
                    .entraUser(entraUser)
                    .legacyUserId(UUID.randomUUID())
                    .build();

            final AppRole nonPuiRole = AppRole.builder()
                    .id(UUID.fromString(selectedRoles.get(0)))
                    .name("NON_PUI_ROLE")
                    .legacySync(false)
                    .roleType(RoleType.EXTERNAL)
                    .authzRole(false)
                    .build();

            userProfile.setAppRoles(Set.of());
            entraUser.setUserProfiles(Set.of(userProfile));

            when(mockUserProfileRepository.findById(UUID.fromString(userProfileId)))
                    .thenReturn(Optional.of(userProfile));
            when(mockAppRoleRepository.findAllById(any()))
                    .thenReturn(List.of(nonPuiRole));
            when(mockUserProfileRepository.save(any(UserProfile.class)))
                    .thenReturn(userProfile);
            when(mockRoleChangeNotificationService.sendMessage(
                    any(UserProfile.class), any(Set.class), any(Set.class)))
                    .thenReturn(true);

            userService.updateUserRoles(userProfileId, selectedRoles);

            ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
            verify(mockUserProfileRepository).save(userProfileCaptor.capture());
            verify(mockRoleChangeNotificationService).sendMessage(
                    eq(userProfile),
                    eq(Set.of()),
                    eq(Set.of())
            );

            UserProfile savedProfile = userProfileCaptor.getValue();
            assertThat(savedProfile.isLastCcmsSyncSuccessful()).isTrue();
        }
    }
}
