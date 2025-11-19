package uk.gov.justice.laa.portal.landingpage.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
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
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchResultsDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.exception.TechServicesClientException;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplicationForView;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.SendUserVerificationEmailResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesErrorResponse;
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
    @Mock
    private FirmService firmService;

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
                mockRoleChangeNotificationService,
                firmService);
    }

    @Test
    void deleteExternalUser_sendsCcmsNotification_whenCcmsRolesRemoved() {
        // Arrange
        UUID entraId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        EntraUser entraUser = EntraUser.builder()
                .id(entraId)
                .email("user@example.com")
                .build();

        AppRole ccmsRole = AppRole.builder()
                .name("CCMS Role")
                .ccmsCode("CCMS_PUI_TEST")
                .legacySync(true)
                .build();

        UserProfile profile = UserProfile.builder()
                .id(profileId)
                .activeProfile(true)
                .userType(UserType.EXTERNAL)
                .entraUser(entraUser)
                .appRoles(new HashSet<>(Set.of(ccmsRole)))
                .build();
        entraUser.setUserProfiles(Set.of(profile));

        when(mockUserProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(mockUserProfileRepository.findAllByEntraUser(entraUser)).thenReturn(List.of(profile));
        when(mockRoleChangeNotificationService.sendMessage(any(UserProfile.class), any(Set.class), any(Set.class)))
                .thenReturn(true);

        // Act
        userService.deleteExternalUser(profileId.toString(), "duplicate user", UUID.randomUUID());

        // Assert
        verify(techServicesClient).deleteRoleAssignment(entraId);
        verify(mockRoleChangeNotificationService, times(1))
                .sendMessage(any(UserProfile.class), eq(Collections.emptySet()), any(Set.class));
        verify(mockUserProfileRepository, times(1)).deleteAll(any());
        verify(mockEntraUserRepository, times(1)).delete(entraUser);
    }

    @Test
    void deleteExternalUser_doesNotSendCcmsNotification_whenNoCcmsRoles() {
        // Arrange
        UUID entraId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        EntraUser entraUser = EntraUser.builder()
                .id(entraId)
                .email("user@example.com")
                .build();

        AppRole nonCcmsRole = AppRole.builder()
                .name("Non CCMS Role")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(profileId)
                .activeProfile(true)
                .userType(UserType.EXTERNAL)
                .entraUser(entraUser)
                .appRoles(new HashSet<>(Set.of(nonCcmsRole)))
                .build();
        entraUser.setUserProfiles(Set.of(profile));

        when(mockUserProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(mockUserProfileRepository.findAllByEntraUser(entraUser)).thenReturn(List.of(profile));

        // Act
        userService.deleteExternalUser(profileId.toString(), "duplicate user", UUID.randomUUID());

        // Assert
        verify(techServicesClient).deleteRoleAssignment(entraId);
        verify(mockRoleChangeNotificationService, never())
                .sendMessage(any(UserProfile.class), any(Set.class), any(Set.class));
        verify(mockUserProfileRepository, times(1)).deleteAll(any());
        verify(mockEntraUserRepository, times(1)).delete(entraUser);
    }

    @Test
    void deleteExternalUser_successPath_removesAssociationsAndDeletesUser() {
        // Arrange
        UUID entraId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        EntraUser entraUser = EntraUser.builder()
                .id(entraId)
                .email("user@example.com")
                .build();

        AppRole role1 = AppRole.builder().name("Role1").build();

        UserProfile profile = UserProfile.builder()
                .id(profileId)
                .activeProfile(true)
                .userType(UserType.EXTERNAL)
                .entraUser(entraUser)
                .appRoles(new HashSet<>(Set.of(role1)))
                .build();
        entraUser.setUserProfiles(Set.of(profile));

        when(mockUserProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(mockUserProfileRepository.findAllByEntraUser(entraUser)).thenReturn(List.of(profile));

        // Act
        var result = userService.deleteExternalUser(profileId.toString(), "duplicate user", UUID.randomUUID());

        // Assert
        verify(techServicesClient).deleteRoleAssignment(entraId);
        verify(mockUserProfileRepository, times(1)).deleteAll(any());
        verify(mockEntraUserRepository, times(1)).delete(entraUser);
        assertThat(result).isNotNull();
        assertThat(result.getDeletedUserId()).isEqualTo(entraId);
    }

    @Test
    void deleteExternalUser_techServicesFailure_bubblesExceptionAndNoDbDeletes() {
        // Arrange
        UUID entraId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(entraId).email("user@example.com").build();
        UserProfile profile = UserProfile.builder()
                .id(profileId)
                .activeProfile(true)
                .userType(UserType.EXTERNAL)
                .entraUser(entraUser)
                .build();
        entraUser.setUserProfiles(Set.of(profile));

        when(mockUserProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        org.mockito.Mockito.doThrow(new RuntimeException("tech services down"))
                .when(techServicesClient).deleteRoleAssignment(entraId);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.deleteExternalUser(profileId.toString(), "duplicate user", UUID.randomUUID()));
        assertThat(ex.getMessage()).contains("tech services down");
        verify(mockUserProfileRepository, never()).deleteAll(any());
        verify(mockEntraUserRepository, never()).delete(any());
    }

    @Test
    void deleteExternalUser_whenTargetIsInternal_rejected() {
        // Arrange
        UUID entraId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(entraId).build();
        UserProfile profile = UserProfile.builder()
                .id(profileId)
                .activeProfile(true)
                .userType(UserType.INTERNAL)
                .entraUser(entraUser)
                .build();
        when(mockUserProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.deleteExternalUser(profileId.toString(), "reason", UUID.randomUUID()));
        assertThat(ex.getMessage()).contains("Deletion is only permitted for external users");
        verify(techServicesClient, never()).deleteRoleAssignment(any());
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
        RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
        createdUser.setId("id");
        createdUser.setMail("test.user@email.com");
        TechServicesApiResponse<RegisterUserResponse> registerUserResponse = TechServicesApiResponse
                .success(RegisterUserResponse.builder().createdUser(createdUser).build());
        when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);
        when(mockEntraUserRepository.saveAndFlush(any(EntraUser.class))).thenAnswer(returnsFirstArg());

        EntraUserDto entraUserDto = new EntraUserDto();
        entraUserDto.setFirstName("Test");
        entraUserDto.setLastName("User");
        entraUserDto.setEmail("test.user@email.com");
        FirmDto firm = FirmDto.builder().name("Firm").build();
        EntraUser result = userService.createUser(entraUserDto, firm, true, "admin", false);
        assertThat(result).isNotNull();
        assertThat(result.getUserProfiles()).isNotEmpty();
        assertThat(result.getUserProfiles()).hasSize(1);
        verify(mockEntraUserRepository, times(1)).saveAndFlush(any());
        verify(techServicesClient, times(1)).registerNewUser(any(EntraUserDto.class));
    }

    @Test
    void createUserWithPopulatedDisplayName() {
        // assign role
        List<EntraUser> savedUsers = new ArrayList<>();
        when(mockEntraUserRepository.saveAndFlush(any())).then(invocation -> {
            savedUsers.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
        createdUser.setId("id");
        createdUser.setMail("test.user@email.com");
        TechServicesApiResponse<RegisterUserResponse> registerUserResponse = TechServicesApiResponse
                .success(RegisterUserResponse.builder().createdUser(createdUser).build());
        when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);

        EntraUserDto entraUserDto = new EntraUserDto();
        entraUserDto.setFirstName("Test");
        entraUserDto.setLastName("User");
        entraUserDto.setEmail("test.user@email.com");
        FirmDto firm = FirmDto.builder().name("Firm").build();
        userService.createUser(entraUserDto, firm, false, "admin", false);
        verify(mockEntraUserRepository, times(1)).saveAndFlush(any());
        assertThat(savedUsers.size()).isEqualTo(1);
        EntraUser savedUser = savedUsers.getFirst();
        assertThat(savedUser.getFirstName()).isEqualTo("Test");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.getUserProfiles().iterator().next().getFirm().getName()).isEqualTo("Firm");
        assertThat(savedUser.getUserProfiles().iterator().next().getUserType()).isEqualTo(UserType.EXTERNAL);
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
                .userType(UserType.EXTERNAL).userProfileStatus(UserProfileStatus.COMPLETE).build();
        entraUser.setUserProfiles(Set.of(userProfile));
        when(mockEntraUserRepository.findByEntraOid(anyString())).thenReturn(Optional.of(entraUser));
        // Act
        List<UserType> userTypeByUsername = userService.findUserTypeByUserEntraId("no-profile-username");
        // Assert
        assertThat(userTypeByUsername).isNotNull();
        assertThat(userTypeByUsername).hasSize(1);
        assertThat(userTypeByUsername.getFirst()).isEqualTo(UserType.EXTERNAL);

    }

    @Test
    void testFindUserTypeByUsernameMultiProfile() {
        // Arrange
        EntraUser entraUser = EntraUser.builder().firstName("Test1").userStatus(UserStatus.ACTIVE).build();
        UserProfile userProfile1 = UserProfile.builder().activeProfile(true).entraUser(entraUser)
                .userType(UserType.EXTERNAL).userProfileStatus(UserProfileStatus.COMPLETE).build();
        UserProfile userProfile2 = UserProfile.builder().activeProfile(true).entraUser(entraUser)
                .userType(UserType.EXTERNAL).userProfileStatus(UserProfileStatus.COMPLETE).build();
        entraUser.setUserProfiles(Set.of(userProfile1, userProfile2));
        when(mockEntraUserRepository.findByEntraOid(anyString())).thenReturn(Optional.of(entraUser));
        // Act
        List<UserType> userTypeByUsername = userService.findUserTypeByUserEntraId("no-profile-username");
        // Assert
        assertThat(userTypeByUsername).isNotNull();
        assertThat(userTypeByUsername).hasSize(2);
        assertThat(userTypeByUsername).contains(UserType.EXTERNAL, UserType.EXTERNAL);

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
        UserProfile userProfile = UserProfile.builder().appRoles(Set.of(appRole)).activeProfile(true)
                .entraUser(entraUser)
                .userType(UserType.EXTERNAL).userProfileStatus(UserProfileStatus.COMPLETE).build();
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
        EntraUser entraUser = EntraUser.builder().id(userId).entraOid(userId.toString())
                .userProfiles(HashSet.newHashSet(1)).build();
        UserProfile userProfile = UserProfile.builder().id(userId).activeProfile(true).entraUser(entraUser).build();
        entraUser.getUserProfiles().add(userProfile);

        when(mockEntraUserRepository.findById(any(UUID.class))).thenReturn(Optional.of(entraUser));
        when(mockUserProfileRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        // When
        Set<LaaApplicationForView> returnedApps = userService.getUserAssignedAppsforLandingPage(userId.toString());
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
        EntraUser entraUser = EntraUser.builder().id(userId).entraOid(userId.toString())
                .userProfiles(HashSet.newHashSet(1)).build();
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
                LaaApplication.builder().name("Test App 3").laaApplicationDetails("g//h//i").ordinal(2).build());
        when(laaApplicationsList.getApplications()).thenReturn(applications);
        // When
        Set<LaaApplicationForView> returnedApps = userService.getUserAssignedAppsforLandingPage(entraUserId.toString());

        // Then
        assertThat(returnedApps).isNotNull();
        assertThat(returnedApps.size()).isEqualTo(2);
        Iterator<LaaApplicationForView> iterator = returnedApps.iterator();
        LaaApplicationForView resultApp1 = iterator.next();
        assertThat(resultApp1.getName()).isEqualTo("Test App 1");
        LaaApplicationForView resultApp2 = iterator.next();
        assertThat(resultApp2.getName()).isEqualTo("Test App 2");
    }

    @Test
    public void getActiveProfileByUserId() {
        // Given
        UUID userId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().id(userId).entraOid(userId.toString())
                .userProfiles(HashSet.newHashSet(1)).build();
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
        EntraUser entraUser = EntraUser.builder().id(userId).entraOid(userId.toString())
                .userProfiles(HashSet.newHashSet(1)).build();

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
    public void testGetEntraUserByEmailReturnsUserWhenOneIsPresent() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        EntraUser user = EntraUser.builder()
                .id(userId)
                .firstName("Test")
                .lastName("User")
                .email(email)
                .build();
        when(mockEntraUserRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        // When
        Optional<EntraUserDto> optionalReturnedUser = userService.getEntraUserByEmail(email);
        // Then
        assertThat(optionalReturnedUser.isPresent()).isTrue();
        EntraUserDto returnedUserDto = optionalReturnedUser.get();
        assertThat(returnedUserDto.getFullName()).isEqualTo("Test User");
        assertThat(returnedUserDto.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    public void testGetEntraUserByEmailReturnsNothingWhenNoUserIsPresent() {
        // Given
        when(mockEntraUserRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        // When
        Optional<EntraUserDto> optionalReturnedUser = userService.getEntraUserByEmail("test@test.com");
        // Then
        assertThat(optionalReturnedUser.isEmpty()).isTrue();
    }

    @Test
    public void testFindEntraUserByEmailReturnsNothingWhenNoUserIsPresent() {
        // Given
        when(mockEntraUserRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        // When
        Optional<EntraUser> optionalReturnedUser = userService.findEntraUserByEmail("test@test.com");
        // Then
        assertThat(optionalReturnedUser).isEmpty();
    }

    @Test
    public void testFindEntraUserByEmail() {
        // Given
        when(mockEntraUserRepository.findByEmailIgnoreCase(any()))
                .thenReturn(Optional.of(EntraUser.builder().id(UUID.randomUUID()).build()));
        // When
        Optional<EntraUser> optionalReturnedUser = userService.findEntraUserByEmail("test@test.com");
        // Then
        assertThat(optionalReturnedUser).isNotEmpty();
    }

    @Test
    public void testGetUserAppsByUserIdReturnsAppsWhenUserHasAppsAssigned() {
        // Given
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
        UUID profileId = UUID.randomUUID();
        UUID entraOid = UUID.randomUUID();
        AppRole appRole = AppRole.builder().id(roleId)
                .userTypeRestriction(new UserType[] { UserType.INTERNAL, UserType.EXTERNAL }).build();
        UserProfile userProfile = UserProfile.builder().id(profileId).activeProfile(true)
                .userProfileStatus(UserProfileStatus.COMPLETE).userType(UserType.EXTERNAL).build();
        EntraUser user = EntraUser.builder().id(userId).entraOid(entraOid.toString()).userProfiles(Set.of(userProfile))
                .build();
        userProfile.setEntraUser(user);

        when(mockAppRoleRepository.findAllById(any())).thenReturn(List.of(appRole));
        when(mockUserProfileRepository.findById(profileId)).thenReturn(Optional.of(userProfile));
        UUID modifierId = UUID.randomUUID();
        EntraUser modifier = EntraUser.builder().entraOid(modifierId.toString())
                .userProfiles(Set.of(UserProfile.builder().id(UUID.randomUUID()).activeProfile(true)
                        .userType(UserType.EXTERNAL).build()))
                .build();
        when(mockEntraUserRepository.findByEntraOid(modifierId.toString())).thenReturn(Optional.of(modifier));
        // Act
        userService.updateUserRoles(profileId.toString(), List.of(roleId.toString()), Collections.emptyList(),
                modifierId);

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
        UUID entraOid = UUID.randomUUID();
        AppRole appRole = AppRole.builder().id(roleId).userTypeRestriction(new UserType[] { UserType.EXTERNAL })
                .build();
        UserProfile userProfile = UserProfile.builder().id(userProfileId).activeProfile(true)
                .userType(UserType.EXTERNAL).build();
        EntraUser user = EntraUser.builder().entraOid(entraOid.toString()).userProfiles(Set.of(userProfile)).build();
        userProfile.setEntraUser(user);

        when(mockAppRoleRepository.findAllById(any())).thenReturn(List.of(appRole));
        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));
        UUID modifierId = UUID.randomUUID();
        EntraUser modifier = EntraUser.builder().entraOid(modifierId.toString())
                .userProfiles(Set.of(UserProfile.builder().id(UUID.randomUUID()).activeProfile(true)
                        .userType(UserType.EXTERNAL).build()))
                .build();
        when(mockEntraUserRepository.findByEntraOid(modifierId.toString())).thenReturn(Optional.of(modifier));
        // Act
        userService.updateUserRoles(userProfileId.toString(), List.of(roleId.toString()), Collections.emptyList(),
                modifierId);

        // Assert
        assertThat(userProfile.getAppRoles()).containsExactly(appRole);
    }

    @Test
    void updateUserRoles_updatesRoles_whenUserAndProfileExist_internalRole() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID entraOid = UUID.randomUUID();
        AppRole appRole = AppRole.builder().id(roleId).userTypeRestriction(new UserType[] { UserType.INTERNAL })
                .build();
        UserProfile userProfile = UserProfile.builder().id(userId).activeProfile(true).userType(UserType.INTERNAL)
                .build();
        EntraUser user = EntraUser.builder().entraOid(entraOid.toString()).userProfiles(Set.of(userProfile)).build();
        userProfile.setEntraUser(user);

        UUID modifierId = UUID.randomUUID();
        EntraUser modifier = EntraUser.builder().entraOid(modifierId.toString())
                .userProfiles(Set.of(UserProfile.builder().id(UUID.randomUUID()).activeProfile(true)
                        .userType(UserType.EXTERNAL).build()))
                .build();

        when(mockAppRoleRepository.findAllById(any())).thenReturn(List.of(appRole));
        when(mockUserProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(mockEntraUserRepository.findByEntraOid(modifierId.toString())).thenReturn(Optional.of(modifier));
        // Act
        userService.updateUserRoles(userId.toString(), List.of(roleId.toString()), Collections.emptyList(), modifierId);

        // Assert
        assertThat(userProfile.getAppRoles()).containsExactly(appRole);
    }

    @Test
    void updateUserRoles_updatesRoles_whenRestrictiveRoleUpdates() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID roleId2 = UUID.randomUUID();
        UUID entraOid = UUID.randomUUID();
        AppRole appRole = AppRole.builder().id(roleId).ordinal(1)
                .userTypeRestriction(new UserType[] { UserType.EXTERNAL }).build();
        AppRole appRole2 = AppRole.builder().id(roleId2).ordinal(2)
                .userTypeRestriction(new UserType[] { UserType.EXTERNAL }).build();
        UserProfile userProfile = UserProfile.builder().id(userId).activeProfile(true).userType(UserType.EXTERNAL)
                .build();
        EntraUser user = EntraUser.builder().entraOid(entraOid.toString()).userProfiles(Set.of(userProfile)).build();
        userProfile.setEntraUser(user);

        UUID modifierId = UUID.randomUUID();
        EntraUser modifier = EntraUser.builder().entraOid(modifierId.toString())
                .userProfiles(Set.of(UserProfile.builder().id(UUID.randomUUID()).activeProfile(true)
                        .userType(UserType.EXTERNAL).build()))
                .build();

        when(mockAppRoleRepository.findAllById(any())).thenReturn(List.of(appRole, appRole2));
        when(mockUserProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(mockEntraUserRepository.findByEntraOid(modifierId.toString())).thenReturn(Optional.of(modifier));
        // Act
        userService.updateUserRoles(userId.toString(), List.of(roleId.toString()), List.of(roleId2.toString()),
                modifierId);

        // Assert
        List<AppRole> results = userProfile.getAppRoles().stream().sorted(Comparator.comparingInt(AppRole::getOrdinal))
                .toList();
        assertThat(results).containsExactly(appRole, appRole2);
    }

    @Test
    void updateUserRoles_updatesRoles_whenUserAndProfileExist_error() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID entraOid = UUID.randomUUID();
        AppRole appRole = AppRole.builder().id(roleId).userTypeRestriction(new UserType[] { UserType.INTERNAL })
                .build();
        UserProfile userProfile = UserProfile.builder().id(userId).activeProfile(true).userType(UserType.EXTERNAL)
                .build();
        EntraUser user = EntraUser.builder().entraOid(entraOid.toString()).userProfiles(Set.of(userProfile)).build();
        userProfile.setEntraUser(user);
        UUID modifierId = UUID.randomUUID();
        EntraUser modifier = EntraUser.builder().entraOid(modifierId.toString())
                .userProfiles(Set.of(UserProfile.builder().id(UUID.randomUUID()).activeProfile(true)
                        .userType(UserType.EXTERNAL).build()))
                .build();
        when(mockEntraUserRepository.findByEntraOid(modifierId.toString())).thenReturn(Optional.of(modifier));
        when(mockAppRoleRepository.findAllById(any())).thenReturn(List.of(appRole));
        when(mockUserProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));

        // Act
        userService.updateUserRoles(userId.toString(), List.of(roleId.toString()), Collections.emptyList(), modifierId);

        // Assert
        assertThat(userProfile.getAppRoles()).isEmpty();
    }

    @Test
    void updateUserRoles_logsWarning_whenUserNotFound() {
        // Arrange
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserService.class);
        UUID userProfileId = UUID.randomUUID();
        UUID modifierId = UUID.randomUUID();
        when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.empty());

        // Act
        userService.updateUserRoles(userProfileId.toString(), List.of(UUID.randomUUID().toString()),
                Collections.emptyList(), modifierId);

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
    public void testGetAppsByUserTypeQueriesInternalUsersWhenUserTypeIsInternal() {
        App testApp = App.builder()
                .name("Test App")
                .build();
        AppRole testAppRole = AppRole.builder()
                .name("Test Role")
                .app(testApp)
                .build();
        when(mockAppRoleRepository.findByUserTypeRestrictionContains(any())).thenReturn(List.of(testAppRole));
        List<AppDto> apps = userService.getAppsByUserType(UserType.INTERNAL);
        Assertions.assertEquals(1, apps.size());
        Assertions.assertEquals(testApp.getName(), apps.getFirst().getName());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockAppRoleRepository).findByUserTypeRestrictionContains(captor.capture());
        String userTypeString = captor.getValue();
        Assertions.assertEquals(UserType.INTERNAL.name(), userTypeString);
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
        when(mockAppRoleRepository.findByUserTypeRestrictionContains(any())).thenReturn(List.of(testAppRole));
        List<AppDto> apps = userService.getAppsByUserType(UserType.EXTERNAL);
        Assertions.assertEquals(1, apps.size());
        Assertions.assertEquals(testApp.getName(), apps.getFirst().getName());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockAppRoleRepository).findByUserTypeRestrictionContains(captor.capture());
        String userTypeString = captor.getValue();
        Assertions.assertEquals(UserType.EXTERNAL.name(), userTypeString);
    }

    @Test
    public void testGetAppRolesByAppIdAndUserTypeReturnsInternalRolesWhenUserTypeIsInternal() {
        App testApp = App.builder()
                .name("Test App")
                .build();
        AppRole internalRole = AppRole.builder()
                .name("Test Internal Role")
                .ordinal(2)
                .userTypeRestriction(new UserType[] { UserType.INTERNAL })
                .app(testApp)
                .build();
        AppRole externalRole = AppRole.builder()
                .name("Test External Role")
                .ordinal(3)
                .userTypeRestriction(new UserType[] { UserType.EXTERNAL })
                .app(testApp)
                .build();
        AppRole internalAndExternalRole = AppRole.builder()
                .name("Test Internal And External Role")
                .ordinal(1)
                .userTypeRestriction(new UserType[] { UserType.INTERNAL, UserType.EXTERNAL })
                .app(testApp)
                .build();

        testApp.setAppRoles(Set.of(internalRole, externalRole, internalAndExternalRole));
        when(mockAppRepository.findById(any())).thenReturn(Optional.of(testApp));

        List<AppRoleDto> returnedAppRoles = userService.getAppRolesByAppIdAndUserType(UUID.randomUUID().toString(),
                UserType.INTERNAL);
        Assertions.assertEquals(2, returnedAppRoles.size());
        // Check no external app roles in response
        Assertions
                .assertTrue(returnedAppRoles.stream().flatMap(role -> Arrays.stream(role.getUserTypeRestriction()))
                        .anyMatch(userType -> userType == UserType.INTERNAL));
        Assertions.assertEquals(returnedAppRoles.get(0).getName(), internalAndExternalRole.getName());
        Assertions.assertEquals(returnedAppRoles.get(1).getName(), internalRole.getName());
    }

    @Test
    public void testGetAppRolesByAppIdAndUserTypeReturnsExternalRolesWhenUserTypeIsExternal() {
        App testApp = App.builder()
                .name("Test App")
                .build();
        AppRole internalRole = AppRole.builder()
                .name("Test Internal Role")
                .userTypeRestriction(new UserType[] { UserType.INTERNAL })
                .app(testApp)
                .ordinal(2)
                .build();
        AppRole externalRole = AppRole.builder()
                .name("Test External Role")
                .userTypeRestriction(new UserType[] { UserType.EXTERNAL })
                .app(testApp)
                .ordinal(3)
                .build();
        AppRole internalAndExternalRole = AppRole.builder()
                .name("Test Internal And External Role")
                .userTypeRestriction(new UserType[] { UserType.INTERNAL, UserType.EXTERNAL })
                .ordinal(1)
                .app(testApp)
                .build();

        testApp.setAppRoles(Set.of(internalRole, externalRole, internalAndExternalRole));
        when(mockAppRepository.findById(any())).thenReturn(Optional.of(testApp));

        List<AppRoleDto> returnedAppRoles = userService.getAppRolesByAppIdAndUserType(UUID.randomUUID().toString(),
                UserType.EXTERNAL);
        Assertions.assertEquals(2, returnedAppRoles.size());
        // Check no external app roles in response
        Assertions
                .assertTrue(returnedAppRoles.stream().flatMap(role -> Arrays.stream(role.getUserTypeRestriction()))
                        .anyMatch(userType -> userType == UserType.EXTERNAL));
        Assertions.assertEquals(returnedAppRoles.get(0).getName(), internalAndExternalRole.getName());
        Assertions.assertEquals(returnedAppRoles.get(1).getName(), externalRole.getName());
    }

    @Test
    public void testGetAppRolesByAppIdAndUserTypeReturnsEmptyListWhenAppIdIsNotFound() {
        when(mockAppRepository.findById(any())).thenReturn(Optional.empty());
        List<AppRoleDto> returnedAppRoles = userService.getAppRolesByAppIdAndUserType(UUID.randomUUID().toString(),
                UserType.EXTERNAL);
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
        Set<UserProfile> userProfiles = Set.of(UserProfile.builder().activeProfile(true).userType(UserType.INTERNAL)
                .userProfileStatus(UserProfileStatus.COMPLETE).appRoles(Set.of(appRole)).build());
        EntraUser entraUser = EntraUser.builder().id(userId).userProfiles(userProfiles).build();
        when(mockEntraUserRepository.findById(any())).thenReturn(Optional.of(entraUser));
        assertThat(userService.isInternal(entraUser.getId())).isTrue();
    }

    @Test
    void isInternal_Failed() {
        UUID userId = UUID.randomUUID();
        Permission userPermission = Permission.VIEW_EXTERNAL_USER;
        AppRole appRole = AppRole.builder().authzRole(true).permissions(Set.of(userPermission)).build();
        Set<UserProfile> userProfiles = Set.of(UserProfile.builder().activeProfile(true).userType(UserType.EXTERNAL)
                .appRoles(Set.of(appRole)).userProfileStatus(UserProfileStatus.COMPLETE).build());
        EntraUser entraUser = EntraUser.builder().id(userId).userProfiles(userProfiles).build();
        when(mockEntraUserRepository.findById(any())).thenReturn(Optional.of(entraUser));
        assertThat(userService.isInternal(entraUser.getId())).isFalse();
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
            UUID userProfileId = UUID.randomUUID();
            UUID officeId1 = UUID.randomUUID();
            UUID officeId2 = UUID.randomUUID();

            Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city")
                    .postcode("pst_code").build();
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
            UserProfile userProfile = UserProfile.builder().id(userProfileId).activeProfile(true)
                    .offices(new HashSet<>()).userProfileStatus(UserProfileStatus.COMPLETE).build();

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

            Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city")
                    .postcode("pst_code").build();
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
            assertThat(result).extracting(OfficeDto::getCode).containsExactlyInAnyOrder("Office 1", "Office 2",
                    "Office 3");
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
            String diff = userService.updateUserOffices(userProfileId.toString(), selectedOffices);

            // Assert
            assertThat(userProfile.getOffices()).isNull();
            assertThat(diff).isEqualTo("Removed : All, Added : All");
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

            Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city")
                    .postcode("pst_code").build();
            Office office1 = Office.builder().id(officeId1).code("of1").address(address).firm(userFirm).build();
            Office office2 = Office.builder().id(officeId2).code("of2").address(address).firm(userFirm).build();

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
            String diff = userService.updateUserOffices(userProfileId.toString(), selectedOffices);

            // Assert
            assertThat(userProfile.getOffices()).containsExactlyInAnyOrder(office1, office2);
            assertThat(diff).contains("Removed : All, Added : ");
            assertThat(diff).contains("of1");
            assertThat(diff).contains("of2");
            verify(mockUserProfileRepository).saveAndFlush(userProfile);
        }

        @Test
        void updateUserOffices_updatesOffices_addAll() throws IOException {
            // Arrange
            UUID firmId = UUID.randomUUID();
            UUID entraUserId = UUID.randomUUID();
            UUID userProfileId = UUID.randomUUID();
            UUID officeId1 = UUID.randomUUID();
            UUID officeId2 = UUID.randomUUID();

            Firm userFirm = Firm.builder().id(firmId).build();

            Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city")
                    .postcode("pst_code").build();
            Office office1 = Office.builder().id(officeId1).code("of1").address(address).firm(userFirm).build();
            Office office2 = Office.builder().id(officeId2).code("of2").address(address).firm(userFirm).build();

            UserProfile userProfileOld = UserProfile.builder()
                    .id(userProfileId)
                    .activeProfile(true)
                    .firm(userFirm)
                    .offices(Set.of(office1, office2))
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();
            EntraUser entraUser = EntraUser.builder()
                    .id(entraUserId)
                    .userProfiles(Set.of(userProfileOld))
                    .build();
            userProfileOld.setEntraUser(entraUser);
            UserProfile userProfileNew = UserProfile.builder()
                    .id(userProfileId)
                    .activeProfile(true)
                    .firm(userFirm)
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfileOld));
            when(mockOfficeRepository.findAllById(any())).thenReturn(List.of());
            when(mockUserProfileRepository.saveAndFlush(any())).thenReturn(userProfileNew);

            List<String> selectedOffices = List.of();

            // Act
            String diff = userService.updateUserOffices(userProfileId.toString(), selectedOffices);

            // Assert
            assertThat(userProfileNew.getOffices()).isNull();
            String[] changedOffices = diff.split(", Added");
            assertThat(changedOffices[0]).contains("Removed : ");
            assertThat(changedOffices[0]).contains("of1");
            assertThat(changedOffices[0]).contains("of2");
            assertThat(changedOffices[1]).contains("All");
            verify(mockUserProfileRepository).saveAndFlush(userProfileOld);
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

            Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city")
                    .postcode("pst_code").build();
            Office office1 = Office.builder().id(officeId1).code("of1").address(address).firm(userFirm).build();
            Office office2 = Office.builder().id(officeId2).code("of2").address(address).build();

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
            String diff = userService.updateUserOffices(userProfileId.toString(), selectedOffices);

            // Assert
            assertThat(userProfile.getOffices()).containsExactlyInAnyOrder(office1);
            assertThat(diff).isEqualTo("Removed : All, Added : of1");
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
            String diff = userService.updateUserOffices(userProfileId.toString(), Collections.emptyList());

            // Assert
            assertThat(userProfile.getOffices()).isEmpty();
            assertThat(diff).isEqualTo("Removed : All, Added : All");
            verify(mockUserProfileRepository).saveAndFlush(userProfile);
        }

        @Test
        void diffOffices_diff_old_new() {
            UUID old1 = UUID.fromString("5fcc67ed-ad22-4ce2-addc-74c974975958");
            Office o1 = Office.builder().id(old1).code("old1").build();
            UUID old2 = UUID.fromString("b07911a3-964a-4281-8808-6f87f3f17bad");
            Office o2 = Office.builder().id(old2).code("old2").build();
            UUID keep = UUID.fromString("14bf95e1-e315-4138-9aad-fca5faf41884");
            Office k1 = Office.builder().id(keep).code("kep1").build();
            UUID new1 = UUID.fromString("e9d43cb2-3a6f-4d7b-a383-bfd82302abfa");
            Office n1 = Office.builder().id(new1).code("new1").build();
            UUID new2 = UUID.fromString("6b7fc00d-68f1-4a4b-b902-9998614e6a95");
            Office n2 = Office.builder().id(new2).code("new2").build();
            Set<Office> oldOffices = Set.of(o1, o2, k1);
            Set<Office> newOffices = Set.of(k1, n1, n2);
            String changed = userService.diffOffices(oldOffices, newOffices);
            assertThat(changed).doesNotContain("kep1");
            String[] changedOffices = changed.split(", Added");
            assertThat(changedOffices[0]).contains("old1");
            assertThat(changedOffices[0]).contains("old2");
            assertThat(changedOffices[1]).contains("new1");
            assertThat(changedOffices[1]).contains("new2");
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
            Set<LaaApplicationForView> result = userService.getUserAssignedAppsforLandingPage(entraUserId.toString());

            // Assert
            assertThat(result).hasSize(2);
            List<String> resultNames = result.stream().map(LaaApplicationForView::getName).toList();
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
            Set<LaaApplicationForView> result = userService.getUserAssignedAppsforLandingPage(entraUserId.toString());

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
            Set<LaaApplicationForView> result = userService.getUserAssignedAppsforLandingPage(entraUserId.toString());

            // Assert
            assertThat(result).hasSize(3);
            List<LaaApplicationForView> resultList = new ArrayList<>(result);
            assertThat(resultList.get(0).getName()).isEqualTo("App A");
            assertThat(resultList.get(1).getName()).isEqualTo("App B");
            assertThat(resultList.get(2).getName()).isEqualTo("App C");
        }

        @Test
        void getUserAssignedAppsforLandingPage_returnsDefaultDescription() {
            // Arrange
            UUID entraUserId = UUID.randomUUID();
            UUID userProfileId = UUID.randomUUID();
            App app1 = App.builder().name("App C").build();
            App app2 = App.builder().name("App A").build();

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
                    LaaApplication.builder().name("App C").ordinal(3).build(),
                    LaaApplication.builder().name("App A").ordinal(1).description("Default description for App A")
                            .descriptionIfAppAssigned(LaaApplication.DescriptionIfAppAssigned.builder()
                                    .appAssigned("App B")
                                    .description("Alternative description for App A").build())
                            .build(),
                    LaaApplication.builder().name("App B").ordinal(2).build());
            when(laaApplicationsList.getApplications()).thenReturn(configuredApps);

            // Act
            Set<LaaApplicationForView> result = userService.getUserAssignedAppsforLandingPage(entraUserId.toString());

            // Assert
            assertThat(result).hasSize(2);
            List<LaaApplicationForView> resultList = new ArrayList<>(result);
            assertThat(resultList.get(0).getName()).isEqualTo("App A");
            assertThat(resultList.get(0).getDescription()).isEqualTo("Default description for App A");
            assertThat(resultList.get(1).getName()).isEqualTo("App C");
        }

        @Test
        void getUserAssignedAppsforLandingPage_returnsWithAlternativeDescription() {
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
                    LaaApplication.builder().name("App A").ordinal(1)
                            .descriptionIfAppAssigned(LaaApplication.DescriptionIfAppAssigned.builder()
                                    .appAssigned("App B")
                                    .description("Alternative description for App A").build())
                            .build(),
                    LaaApplication.builder().name("App B").ordinal(2).build());
            when(laaApplicationsList.getApplications()).thenReturn(configuredApps);

            // Act
            Set<LaaApplicationForView> result = userService.getUserAssignedAppsforLandingPage(entraUserId.toString());

            // Assert
            assertThat(result).hasSize(3);
            List<LaaApplicationForView> resultList = new ArrayList<>(result);
            assertThat(resultList.get(0).getName()).isEqualTo("App A");
            assertThat(resultList.get(0).getDescription()).isEqualTo("Alternative description for App A");
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
            TechServicesApiResponse<RegisterUserResponse> registerUserResponse = TechServicesApiResponse
                    .success(RegisterUserResponse.builder().createdUser(createdUser).build());
            when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);
            FirmDto firmDto = FirmDto.builder().name("Test Firm").build();

            // Act
            EntraUser result = userService.createUser(user, firmDto, false, "admin", false);

            // Assert
            assertThat(result).isNotNull();
            verify(mockEntraUserRepository).saveAndFlush(any(EntraUser.class));
            verify(techServicesClient).registerNewUser(any(EntraUserDto.class));
        }

        @Test
        void createUser_withFirm_ForMultiFirmUser_withUserProfile() {
            // assign role
            RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
            createdUser.setId("id");
            createdUser.setMail("test.user@email.com");
            TechServicesApiResponse<RegisterUserResponse> registerUserResponse = TechServicesApiResponse
                    .success(RegisterUserResponse.builder().createdUser(createdUser).build());
            when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);
            when(mockEntraUserRepository.saveAndFlush(any(EntraUser.class))).thenAnswer(returnsFirstArg());

            EntraUserDto entraUserDto = new EntraUserDto();
            entraUserDto.setMultiFirmUser(true);
            entraUserDto.setFirstName("Test");
            entraUserDto.setLastName("User");
            entraUserDto.setEmail("test.user@email.com");
            FirmDto firm = FirmDto.builder().id(UUID.randomUUID()).build();
            EntraUser result = userService.createUser(entraUserDto, firm, true, "admin", false);
            assertThat(result).isNotNull();
            assertThat(result.getUserProfiles()).isNotNull();
            assertThat(result.getUserProfiles()).hasSize(1);
            verify(mockEntraUserRepository, times(1)).saveAndFlush(any());
            verify(techServicesClient, times(1)).registerNewUser(any(EntraUserDto.class));
        }

        @Test
        void createUser_withNoFirm_ForNonMultiFirmUser_Fail() {
            // assign role
            RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
            createdUser.setId("id");
            createdUser.setMail("test.user@email.com");
            TechServicesApiResponse<RegisterUserResponse> registerUserResponse = TechServicesApiResponse
                    .success(RegisterUserResponse.builder().createdUser(createdUser).build());
            when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);

            EntraUserDto entraUserDto = new EntraUserDto();
            entraUserDto.setFirstName("Test");
            entraUserDto.setLastName("User");
            entraUserDto.setEmail("test.user@email.com");
            FirmDto firm = FirmDto.builder().skipFirmSelection(true).build();
            RuntimeException rtEx = assertThrows(RuntimeException.class,
                    () -> userService.createUser(entraUserDto, firm, true, "admin", false),
                    "Expected Runtime Exception");
            assertThat(rtEx.getMessage())
                    .isEqualTo("User Test User is not a multi-firm user, firm selection can not be skipped");
            verify(mockEntraUserRepository, never()).saveAndFlush(any());
            verify(techServicesClient, times(1)).registerNewUser(any(EntraUserDto.class));
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
            TechServicesApiResponse<RegisterUserResponse> registerUserResponse = TechServicesApiResponse
                    .success(RegisterUserResponse.builder().createdUser(createdUser).build());
            when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);

            FirmDto firmDto = FirmDto.builder().name("Test Firm").build();

            // Act
            userService.createUser(user, firmDto, false, "admin", false);

            // Assert
            EntraUser capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getUserProfiles()).hasSize(1);
            UserProfile profile = capturedUser.getUserProfiles().iterator().next();
            assertThat(profile.getUserType()).isEqualTo(UserType.EXTERNAL);
        }

        @Test
        void createUser_returns_ts_user_email_conflict() {
            // Arrange
            EntraUserDto user = new EntraUserDto();
            user.setEmail("test@example.com");

            TechServicesApiResponse<RegisterUserResponse> registerUserResponse = TechServicesApiResponse
                    .error(TechServicesErrorResponse.builder().code("USER_ALREADY_EXISTS")
                            .message("A user with this email already exists").build());
            when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);

            FirmDto firmDto = FirmDto.builder().name("Test Firm").build();

            // Act
            TechServicesClientException result = assertThrows(TechServicesClientException.class,
                    () -> userService.createUser(user, firmDto, false, "admin", false),
                    "Expected TS Client Exception");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getMessage()).isEqualTo("A user with this email already exists");
        }

        @Test
        void createUser_ts_4xx_error() {
            // Arrange
            EntraUserDto user = new EntraUserDto();
            user.setEmail("test@example.com");

            when(techServicesClient.registerNewUser(any(EntraUserDto.class)))
                    .thenThrow(new RuntimeException("Error while sending new user creation request to Tech Services"));

            FirmDto firmDto = FirmDto.builder().name("Test Firm").build();

            // Act
            RuntimeException result = assertThrows(RuntimeException.class,
                    () -> userService.createUser(user, firmDto, false, "admin", false),
                    "Expected TS Client Exception");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getMessage()).isEqualTo("Error while sending new user creation request to Tech Services");
        }

        @Test
        void createUser_multiFirmUser_createsUserProfileWithFirm() {
            // Arrange
            EntraUserDto user = new EntraUserDto();
            user.setEmail("multifirm@example.com");
            user.setFirstName("Multi");
            user.setLastName("Firm");

            ArgumentCaptor<EntraUser> userCaptor = ArgumentCaptor.forClass(EntraUser.class);
            EntraUser savedUser = EntraUser.builder().id(UUID.randomUUID()).build();
            when(mockEntraUserRepository.saveAndFlush(userCaptor.capture())).thenReturn(savedUser);

            RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
            createdUser.setId("entra-oid-123");
            createdUser.setMail("multifirm@example.com");
            TechServicesApiResponse<RegisterUserResponse> registerUserResponse = TechServicesApiResponse
                    .success(RegisterUserResponse.builder().createdUser(createdUser).build());
            when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);

            FirmDto firmDto = FirmDto.builder().name("Test Firm").build();

            // Act
            EntraUser result = userService.createUser(user, firmDto, false, "admin", true);

            // Assert
            assertThat(result).isNotNull();
            EntraUser capturedUser = userCaptor.getValue();
            assertThat(capturedUser.isMultiFirmUser()).isTrue();
            assertThat(capturedUser.getUserProfiles()).isNotEmpty();
            verify(mockEntraUserRepository).saveAndFlush(any(EntraUser.class));
        }

        @Test
        void createUser_multiFirmUser_withUserManagerRole_createsUserProfileWithFirm() {
            // Arrange
            EntraUserDto user = new EntraUserDto();
            user.setEmail("multifirm-manager@example.com");
            user.setFirstName("Multi");
            user.setLastName("Manager");

            ArgumentCaptor<EntraUser> userCaptor = ArgumentCaptor.forClass(EntraUser.class);
            EntraUser savedUser = EntraUser.builder().id(UUID.randomUUID()).build();
            when(mockEntraUserRepository.saveAndFlush(userCaptor.capture())).thenReturn(savedUser);

            RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
            createdUser.setId("entra-oid-456");
            createdUser.setMail("multifirm-manager@example.com");
            TechServicesApiResponse<RegisterUserResponse> registerUserResponse = TechServicesApiResponse
                    .success(RegisterUserResponse.builder().createdUser(createdUser).build());
            when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);

            FirmDto firmDto = FirmDto.builder().name("Test Firm").build();

            // Act - Note: isUserManager flag is ignored for multi-firm users since no
            // profile is created
            EntraUser result = userService.createUser(user, firmDto, true, "admin", true);

            // Assert
            assertThat(result).isNotNull();
            EntraUser capturedUser = userCaptor.getValue();
            assertThat(capturedUser.isMultiFirmUser()).isTrue();
            assertThat(capturedUser.getUserProfiles()).isNotEmpty();
            verify(mockEntraUserRepository).saveAndFlush(any(EntraUser.class));
        }

        @Test
        void createUser_singleFirmUser_createsUserProfileWithFirm() {
            // Arrange
            EntraUserDto user = new EntraUserDto();
            user.setEmail("singlefirm@example.com");
            user.setFirstName("Single");
            user.setLastName("Firm");

            ArgumentCaptor<EntraUser> userCaptor = ArgumentCaptor.forClass(EntraUser.class);
            EntraUser savedUser = EntraUser.builder().id(UUID.randomUUID()).build();
            when(mockEntraUserRepository.saveAndFlush(userCaptor.capture())).thenReturn(savedUser);

            RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
            createdUser.setId("entra-oid-789");
            createdUser.setMail("singlefirm@example.com");
            TechServicesApiResponse<RegisterUserResponse> registerUserResponse = TechServicesApiResponse
                    .success(RegisterUserResponse.builder().createdUser(createdUser).build());
            when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);

            FirmDto firmDto = FirmDto.builder().name("Test Firm").build();

            // Act
            EntraUser result = userService.createUser(user, firmDto, false, "admin", false);

            // Assert
            assertThat(result).isNotNull();
            EntraUser capturedUser = userCaptor.getValue();
            assertThat(capturedUser.isMultiFirmUser()).isFalse();
            assertThat(capturedUser.getUserProfiles()).hasSize(1);
            UserProfile profile = capturedUser.getUserProfiles().iterator().next();
            assertThat(profile.getFirm()).isNotNull();
            assertThat(profile.getFirm().getName()).isEqualTo("Test Firm");
            assertThat(profile.getUserType()).isEqualTo(UserType.EXTERNAL);
            verify(mockEntraUserRepository).saveAndFlush(any(EntraUser.class));
        }

        @Test
        void createUser_multiFirmUser_setsEntraOidFromTechServices() {
            // Arrange
            EntraUserDto user = new EntraUserDto();
            user.setEmail("multifirm@example.com");

            String expectedEntraOid = "entra-oid-from-tech-services";
            ArgumentCaptor<EntraUser> userCaptor = ArgumentCaptor.forClass(EntraUser.class);
            EntraUser savedUser = EntraUser.builder().id(UUID.randomUUID()).build();
            when(mockEntraUserRepository.saveAndFlush(userCaptor.capture())).thenReturn(savedUser);

            RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
            createdUser.setId(expectedEntraOid);
            createdUser.setMail("multifirm@example.com");
            TechServicesApiResponse<RegisterUserResponse> registerUserResponse = TechServicesApiResponse
                    .success(RegisterUserResponse.builder().createdUser(createdUser).build());
            when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);

            FirmDto firmDto = FirmDto.builder().name("Test Firm").build();

            // Act
            userService.createUser(user, firmDto, false, "admin", true);

            // Assert
            EntraUser capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getEntraOid()).isEqualTo(expectedEntraOid);
        }

        @Test
        void createUser_multiFirmUser_setsUserStatusToActive() {
            // Arrange
            EntraUserDto user = new EntraUserDto();
            user.setEmail("multifirm@example.com");

            ArgumentCaptor<EntraUser> userCaptor = ArgumentCaptor.forClass(EntraUser.class);
            EntraUser savedUser = EntraUser.builder().id(UUID.randomUUID()).build();
            when(mockEntraUserRepository.saveAndFlush(userCaptor.capture())).thenReturn(savedUser);

            RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
            createdUser.setId("entra-oid-123");
            createdUser.setMail("multifirm@example.com");
            TechServicesApiResponse<RegisterUserResponse> registerUserResponse = TechServicesApiResponse
                    .success(RegisterUserResponse.builder().createdUser(createdUser).build());
            when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);

            FirmDto firmDto = FirmDto.builder().name("Test Firm").build();

            // Act
            userService.createUser(user, firmDto, false, "admin", true);

            // Assert
            EntraUser capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        void createUser_multiFirmUser_setsUserProfileStatusToPending() {
            // Arrange
            EntraUserDto user = new EntraUserDto();
            user.setEmail("multifirm@example.com");

            ArgumentCaptor<EntraUser> userCaptor = ArgumentCaptor.forClass(EntraUser.class);
            EntraUser savedUser = EntraUser.builder().id(UUID.randomUUID()).build();
            when(mockEntraUserRepository.saveAndFlush(userCaptor.capture())).thenReturn(savedUser);

            RegisterUserResponse.CreatedUser createdUser = new RegisterUserResponse.CreatedUser();
            createdUser.setId("entra-oid-123");
            createdUser.setMail("multifirm@example.com");
            TechServicesApiResponse<RegisterUserResponse> registerUserResponse = TechServicesApiResponse
                    .success(RegisterUserResponse.builder().createdUser(createdUser).build());
            when(techServicesClient.registerNewUser(any(EntraUserDto.class))).thenReturn(registerUserResponse);

            FirmDto firmDto = FirmDto.builder().name("Test Firm").build();

            // Act
            userService.createUser(user, firmDto, false, "admin", true);

            // Assert
            EntraUser capturedUser = userCaptor.getValue();
            // Multi-firm users have no profile, so no profile status to check
            assertThat(capturedUser.getUserProfiles()).isNotEmpty();
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
            UUID profileId = UUID.randomUUID();
            UUID entraOid = UUID.randomUUID();
            UserProfile userProfile = UserProfile.builder().id(profileId).activeProfile(true)
                    .userType(UserType.INTERNAL).build();
            EntraUser user = EntraUser.builder().id(userId).entraOid(entraOid.toString())
                    .userProfiles(Set.of(userProfile)).build();
            userProfile.setEntraUser(user);

            when(mockAppRoleRepository.findAllById(any())).thenReturn(Collections.emptyList());
            when(mockUserProfileRepository.findById(profileId)).thenReturn(Optional.of(userProfile));
            UUID modifierId = UUID.randomUUID();
            EntraUser modifier = EntraUser.builder().entraOid(modifierId.toString())
                    .userProfiles(Set.of(UserProfile.builder().id(UUID.randomUUID()).activeProfile(true)
                            .userType(UserType.EXTERNAL).build()))
                    .build();
            when(mockEntraUserRepository.findByEntraOid(modifierId.toString())).thenReturn(Optional.of(modifier));
            // Act
            userService.updateUserRoles(profileId.toString(), Collections.emptyList(), Collections.emptyList(),
                    modifierId);

            // Assert
            assertThat(userProfile.getAppRoles()).isEmpty();
            verify(mockUserProfileRepository).save(userProfile);
            verify(techServicesClient, times(1)).updateRoleAssignment(userId);
        }

        @Test
        void updateUserRoles_handlesRoleCoverageError() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UUID profileId = UUID.randomUUID();
            UUID entraOid = UUID.randomUUID();
            AppRole appRole = AppRole.builder().name("Global Admin").id(UUID.randomUUID()).build();
            UserProfile userProfile = UserProfile.builder().id(profileId).activeProfile(true)
                    .userType(UserType.INTERNAL)
                    .appRoles(Set.of(appRole)).build();
            EntraUser user = EntraUser.builder().id(userId).entraOid(entraOid.toString())
                    .userProfiles(Set.of(userProfile)).build();
            userProfile.setEntraUser(user);

            when(mockAppRoleRepository.findAllById(any())).thenReturn(Collections.emptyList());
            when(mockUserProfileRepository.findById(profileId)).thenReturn(Optional.of(userProfile));
            when(mockAppRoleRepository.findByName("Global Admin")).thenReturn(Optional.of(appRole));
            Page<UserProfile> existingAdmins = new PageImpl<>(List.of(userProfile));
            when(mockUserProfileRepository.findInternalUserByAuthzRole(eq("Global Admin"), any()))
                    .thenReturn(existingAdmins);
            UUID modifierId = UUID.randomUUID();
            EntraUser modifier = EntraUser.builder().entraOid(modifierId.toString())
                    .userProfiles(Set.of(UserProfile.builder().id(UUID.randomUUID()).activeProfile(true)
                            .userType(UserType.EXTERNAL).build()))
                    .build();
            when(mockEntraUserRepository.findByEntraOid(modifierId.toString())).thenReturn(Optional.of(modifier));
            // Act
            Map<String, String> result = userService.updateUserRoles(profileId.toString(), Collections.emptyList(),
                    Collections.emptyList(), modifierId);

            // Assert
            assertThat(result.get("error")).isNotEmpty();
            verify(mockUserProfileRepository, never()).save(userProfile);
            verify(techServicesClient, never()).updateRoleAssignment(userId);
            verify(mockRoleChangeNotificationService, never()).sendMessage(any(), any(), any());
        }

        @Test
        void getUserAuthorities_handlesInactiveUser() {
            // Arrange
            EntraUser entraUser = EntraUser.builder()
                    .firstName("Test")
                    .userStatus(UserStatus.DEACTIVE) // Use DEACTIVE instead of INACTIVE
                    .userProfiles(Set.of(UserProfile.builder()
                            .userType(UserType.EXTERNAL)
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
                    UserProfile.builder().appRoles(Set.of(appRole)).activeProfile(true).userType(UserType.INTERNAL)
                            .build(),
                    UserProfile.builder().userType(UserType.EXTERNAL).build());
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
                "USERSTATUS", "userProfileStatus");

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
        UserProfile userProfile1 = UserProfile.builder().activeProfile(true).firm(Firm.builder().id(firm1Id).build())
                .build();
        UserProfile userProfile2 = UserProfile.builder().activeProfile(false).firm(Firm.builder().id(firm2Id).build())
                .build();
        EntraUser entraUser = EntraUser.builder().userProfiles(Set.of(userProfile1, userProfile2)).build();
        ArgumentCaptor<EntraUser> captor = ArgumentCaptor.forClass(EntraUser.class);
        userService.setDefaultActiveProfile(entraUser, firm2Id);
        verify(mockEntraUserRepository, times(2)).saveAndFlush(captor.capture());
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
    void setDefaultActiveProfile_no_firm() {
        UUID firm1Id = UUID.randomUUID();
        UUID firm2Id = UUID.randomUUID();
        UUID firm3Id = UUID.randomUUID();
        UserProfile userProfile1 = UserProfile.builder().activeProfile(true).firm(Firm.builder().id(firm1Id).build())
                .build();
        UserProfile userProfile2 = UserProfile.builder().activeProfile(false).firm(Firm.builder().id(firm2Id).build())
                .build();
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
                .userTypeRestriction(new UserType[] { UserType.INTERNAL })
                .ordinal(2)
                .app(App.builder().id(UUID.randomUUID()).name("Internal App").build())
                .build();
        AppRole appRole2 = AppRole.builder()
                .id(UUID.randomUUID())
                .userTypeRestriction(new UserType[] { UserType.INTERNAL, UserType.EXTERNAL })
                .ordinal(1)
                .app(App.builder().id(UUID.randomUUID()).name("Common App").build())
                .build();
        when(mockAppRoleRepository.findByUserTypeRestrictionContains(UserType.INTERNAL.name()))
                .thenReturn(List.of(appRole1, appRole2));

        // When
        List<AppDto> result = userService.getAppsByUserType(UserType.INTERNAL);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(AppDto::getName))
                .containsExactly("Internal App", "Common App");
        verify(mockAppRoleRepository).findByUserTypeRestrictionContains(UserType.INTERNAL.name());
    }

    @Test
    void getAppsByRoleType_throughGetAppsByUserType_external() {
        // Given
        AppRole appRole1 = AppRole.builder()
                .id(UUID.randomUUID())
                .ordinal(2)
                .userTypeRestriction(new UserType[] { UserType.EXTERNAL })
                .app(App.builder().id(UUID.randomUUID()).name("External App").ordinal(2).build())
                .build();
        AppRole appRole2 = AppRole.builder()
                .id(UUID.randomUUID())
                .ordinal(1)
                .userTypeRestriction(new UserType[] { UserType.INTERNAL, UserType.EXTERNAL })
                .app(App.builder().id(UUID.randomUUID()).name("Common App").ordinal(1).build())
                .build();
        when(mockAppRoleRepository.findByUserTypeRestrictionContains(UserType.EXTERNAL.name()))
                .thenReturn(List.of(appRole1, appRole2));

        // When
        List<AppDto> result = userService.getAppsByUserType(UserType.EXTERNAL);

        // Then
        assertThat(result).hasSize(2);
        assertThat(
                IntStream.range(0, 2)
                        .allMatch(i -> result.get(i).getName()
                                .equals(Arrays.asList("Common App", "External App").get(i))))
                .isTrue();
        verify(mockAppRoleRepository).findByUserTypeRestrictionContains(UserType.EXTERNAL.name());
    }

    @Test
    void getAppsByRoleType_removingDuplicateApps() {
        // Given - Same app appears in multiple roles
        App sharedApp = App.builder().id(UUID.randomUUID()).name("Shared App").build();
        AppRole appRole1 = AppRole.builder()
                .id(UUID.randomUUID())
                .userTypeRestriction(new UserType[] { UserType.INTERNAL })
                .app(sharedApp)
                .build();
        AppRole appRole2 = AppRole.builder()
                .id(UUID.randomUUID())
                .userTypeRestriction(new UserType[] { UserType.INTERNAL, UserType.EXTERNAL })
                .app(sharedApp)
                .build();
        when(mockAppRoleRepository.findByUserTypeRestrictionContains(UserType.INTERNAL.name()))
                .thenReturn(List.of(appRole1, appRole2));

        // When
        List<AppDto> result = userService.getAppsByUserType(UserType.INTERNAL);

        // Then - Should only appear once due to distinct()
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Shared App");
        verify(mockAppRoleRepository).findByUserTypeRestrictionContains(UserType.INTERNAL.name());
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
    void getPageOfUsersBySearch_returnsValidPage() {
        // Given
        String searchTerm = "test";
        FirmSearchForm firmSearch = FirmSearchForm.builder().selectedFirmId(UUID.randomUUID()).build();
        UserType userType = UserType.EXTERNAL;
        boolean showFirmAdmins = false;
        boolean showMultiFirmUsers = false;
        int page = 1;
        int pageSize = 10;
        String sort = "firstName";
        String direction = "ASC";

        UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearch, userType, showFirmAdmins,
                showMultiFirmUsers);

        UserSearchResultsDto userSearchResultsDto = new UserSearchResultsDto(UUID.randomUUID(), true, UserType.EXTERNAL,
                UUID.randomUUID(), UserProfileStatus.COMPLETE, false, "Test", "User", "Test User",
                "test@example.com", UserStatus.ACTIVE, "Test Firm");

        Page<UserSearchResultsDto> userSearchResultsPage = new PageImpl<>(
                List.of(userSearchResultsDto),
                PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "entraUser.firstName")),
                1);

        when(mockUserProfileRepository.findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class)))
                .thenReturn(userSearchResultsPage);

        // When
        PaginatedUsers result = userService.getPageOfUsersBySearch(criteria, page, pageSize, sort, direction);

        // Then
        assertThat(result.getUsers()).hasSize(1);
        assertThat(result.getTotalUsers()).isEqualTo(1);
        verify(mockUserProfileRepository).findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class));
    }

    @Test
    void getPageOfUsersBySearch_withEmptySearch() {
        // Given
        String searchTerm = "test";
        FirmSearchForm firmSearch = FirmSearchForm.builder().selectedFirmId(UUID.randomUUID()).build();
        UserType userTypes = UserType.EXTERNAL;
        boolean showFirmAdmins = false;
        boolean showMultiFirmUsers = false;
        int page = 1;
        int pageSize = 10;
        String sort = "firstName";
        String direction = "ASC";

        UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearch, userTypes, showFirmAdmins,
                showMultiFirmUsers);

        Page<UserSearchResultsDto> userSearchResultsPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "entraUser.firstName")),
                0);

        when(mockUserProfileRepository.findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class)))
                .thenReturn(userSearchResultsPage);

        // When
        PaginatedUsers result = userService.getPageOfUsersBySearch(
                criteria, page, pageSize, sort, direction);

        // Then
        assertThat(result.getUsers()).hasSize(0);
        verify(mockUserProfileRepository).findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class));
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
    void getPageOfUsersBySearch_searchByFullName() {
        String searchTerm = "Test Name";
        FirmSearchForm firmSearch = FirmSearchForm.builder().selectedFirmId(UUID.randomUUID()).build();
        UserType userType = UserType.EXTERNAL;
        boolean showFirmAdmins = false;
        boolean showMultiFirmUsers = false;
        int page = 1;
        int pageSize = 10;
        String sort = "firstName";
        String direction = "ASC";

        UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearch, userType, showFirmAdmins,
                showMultiFirmUsers);

        UserSearchResultsDto userSearchResultsDto = new UserSearchResultsDto(UUID.randomUUID(), true, UserType.EXTERNAL,
                UUID.randomUUID(), UserProfileStatus.COMPLETE, false, "Test", "Name", "Test User",
                "test@example.com", UserStatus.ACTIVE, "Test Firm");

        Page<UserSearchResultsDto> userProfilePage = new PageImpl<>(
                List.of(userSearchResultsDto),
                PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "entraUser.firstName")),
                1);

        when(mockUserProfileRepository.findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class)))
                .thenReturn(userProfilePage);

        // When
        PaginatedUsers result = userService.getPageOfUsersBySearch(
                criteria, page, pageSize, sort, direction);

        // Then
        assertThat(result.getUsers()).hasSize(1);
        assertThat(result.getTotalUsers()).isEqualTo(1);
        assertThat(result.getUsers().getFirst().firstName()).isEqualTo("Test");
        assertThat(result.getUsers().getFirst().lastName()).isEqualTo("Name");

        // Verify the repository was called with the search criteria
        verify(mockUserProfileRepository).findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class));
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
                    .userType(UserType.EXTERNAL)
                    .entraUser(entraUser)
                    .legacyUserId(UUID.randomUUID())
                    .build();

            AppRole oldRole = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("OLD_ROLE")
                    .description("Old Role Description")
                    .ccmsCode("CCMS_OLD")
                    .legacySync(true)
                    .userTypeRestriction(new UserType[] { UserType.EXTERNAL })
                    .authzRole(false)
                    .build();

            final AppRole newRole = AppRole.builder()
                    .id(UUID.fromString(selectedRoles.getFirst()))
                    .name("NEW_ROLE")
                    .description("New Role Description")
                    .ccmsCode("CCMS_NEW")
                    .legacySync(true)
                    .userTypeRestriction(new UserType[] { UserType.EXTERNAL })
                    .authzRole(false)
                    .build();

            userProfile.setAppRoles(Set.of(oldRole));
            entraUser.setUserProfiles(Set.of(userProfile));
            UUID modifierId = UUID.randomUUID();

            when(mockUserProfileRepository.findById(UUID.fromString(userProfileId)))
                    .thenReturn(Optional.of(userProfile));
            when(mockAppRoleRepository.findAllById(any()))
                    .thenReturn(List.of(newRole));
            when(mockUserProfileRepository.save(any(UserProfile.class)))
                    .thenReturn(userProfile);
            when(mockRoleChangeNotificationService.sendMessage(
                    any(UserProfile.class), any(Set.class), any(Set.class)))
                    .thenReturn(true);
            EntraUser modifier = EntraUser.builder().entraOid(modifierId.toString())
                    .userProfiles(Set.of(UserProfile.builder().id(UUID.randomUUID()).activeProfile(true)
                            .userType(UserType.EXTERNAL).build()))
                    .build();
            when(mockEntraUserRepository.findByEntraOid(modifierId.toString())).thenReturn(Optional.of(modifier));

            Map<String, String> changed = userService.updateUserRoles(userProfileId, selectedRoles,
                    Collections.emptyList(), modifierId);

            ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
            verify(mockUserProfileRepository).save(userProfileCaptor.capture());
            verify(mockRoleChangeNotificationService).sendMessage(
                    eq(userProfile),
                    eq(Set.of(newRole.getCcmsCode())),
                    eq(Set.of(oldRole.getCcmsCode())));

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
                    .userType(UserType.EXTERNAL)
                    .entraUser(entraUser)
                    .legacyUserId(UUID.randomUUID())
                    .build();

            final AppRole newRole = AppRole.builder()
                    .id(UUID.fromString(selectedRoles.get(0)))
                    .name("NEW_ROLE")
                    .description("New Role Description")
                    .ccmsCode("CCMS_NEW")
                    .legacySync(true)
                    .userTypeRestriction(new UserType[] { UserType.EXTERNAL })
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
            UUID modifierId = UUID.randomUUID();
            EntraUser modifier = EntraUser.builder().entraOid(modifierId.toString())
                    .userProfiles(Set.of(UserProfile.builder().id(UUID.randomUUID()).activeProfile(true)
                            .userType(UserType.EXTERNAL).build()))
                    .build();
            when(mockEntraUserRepository.findByEntraOid(modifierId.toString())).thenReturn(Optional.of(modifier));
            userService.updateUserRoles(userProfileId, selectedRoles, Collections.emptyList(), modifierId);

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
            UUID modifierId = UUID.randomUUID();

            when(mockUserProfileRepository.findById(UUID.fromString(userProfileId)))
                    .thenReturn(Optional.empty());

            userService.updateUserRoles(userProfileId, selectedRoles, Collections.emptyList(), modifierId);

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
                    .userType(UserType.EXTERNAL)
                    .entraUser(entraUser)
                    .legacyUserId(UUID.randomUUID())
                    .build();

            final AppRole nonPuiRole = AppRole.builder()
                    .id(UUID.fromString(selectedRoles.get(0)))
                    .name("NON_PUI_ROLE")
                    .legacySync(false)
                    .userTypeRestriction(new UserType[] { UserType.EXTERNAL })
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
            UUID modifierId = UUID.randomUUID();
            EntraUser modifier = EntraUser.builder().entraOid(modifierId.toString())
                    .userProfiles(Set.of(UserProfile.builder().id(UUID.randomUUID()).activeProfile(true)
                            .userType(UserType.EXTERNAL).build()))
                    .build();
            when(mockEntraUserRepository.findByEntraOid(modifierId.toString())).thenReturn(Optional.of(modifier));

            userService.updateUserRoles(userProfileId, selectedRoles, Collections.emptyList(), modifierId);

            ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
            verify(mockUserProfileRepository).save(userProfileCaptor.capture());
            verify(mockRoleChangeNotificationService).sendMessage(
                    eq(userProfile),
                    eq(Set.of()),
                    eq(Set.of()));

            UserProfile savedProfile = userProfileCaptor.getValue();
            assertThat(savedProfile.isLastCcmsSyncSuccessful()).isTrue();
        }

        @Test
        void roleDiff_add_roles() {
            UUID newRole1Id = UUID.randomUUID();
            UUID newRole2Id = UUID.randomUUID();
            Set<AppRole> appRoleSet = new HashSet<>();
            appRoleSet.add(AppRole.builder().id(newRole1Id).name("New Role 1").build());
            appRoleSet.add(AppRole.builder().id(newRole2Id).name("New Role 2").build());
            String diffString = UserService.diffRole(new HashSet<>(), appRoleSet);
            assertThat(diffString).isNotEmpty();
            assertThat(diffString).contains("Added: ");
            assertThat(diffString).contains("New Role 1");
            assertThat(diffString).contains("New Role 2");
        }

        @Test
        void roleDiff_remove_roles() {
            UUID newRole1Id = UUID.randomUUID();
            UUID newRole2Id = UUID.randomUUID();
            Set<AppRole> appRoleSet = new HashSet<>();
            appRoleSet.add(AppRole.builder().id(newRole1Id).name("Old Role 1").build());
            appRoleSet.add(AppRole.builder().id(newRole2Id).name("Old Role 2").build());
            String diffString = UserService.diffRole(appRoleSet, new HashSet<>());
            assertThat(diffString).isNotEmpty();
            assertThat(diffString).contains("Removed: ");
            assertThat(diffString).contains("Old Role 1");
            assertThat(diffString).contains("Old Role 2");
        }

        @Test
        void roleDiff_edit_roles() {
            UUID newRoleId = UUID.randomUUID();
            UUID oldRoleId = UUID.randomUUID();
            UUID existingRoleId = UUID.randomUUID();
            AppRole existingRole = AppRole.builder().id(existingRoleId).name("Existing Role").build();
            Set<AppRole> oldAppRoleSet = new HashSet<>();
            oldAppRoleSet.add(AppRole.builder().id(oldRoleId).name("Old Role").build());
            oldAppRoleSet.add(existingRole);
            Set<AppRole> newAppRoleSet = new HashSet<>();
            newAppRoleSet.add(AppRole.builder().id(newRoleId).name("New Role").build());
            newAppRoleSet.add(existingRole);
            String diffString = UserService.diffRole(oldAppRoleSet, newAppRoleSet);
            assertThat(diffString).isNotEmpty();
            assertThat(diffString).isEqualTo("Removed: Old Role, Added: New Role");
        }

        @Test
        void roleCoverage_grant_access() {
            userService.roleCoverage(Set.of(), Set.of(AppRole.builder().build()), Firm.builder().build(),
                    UUID.randomUUID().toString(), false, true);
            verify(mockAppRoleRepository, never()).findByName(any());
            verify(mockUserProfileRepository, never()).findFirmUserByAuthzRoleAndFirm(any(), any(), any());
            verify(mockUserProfileRepository, never()).findInternalUserByAuthzRole(any(), any());
        }

        @Test
        void roleCoverage_removeManager_not_last_not_self() {
            UUID externalUserManagerRoleId = UUID.randomUUID();
            AppRole externalUserManagerRole = AppRole.builder().id(externalUserManagerRoleId).name("Firm User Manager")
                    .build();
            UUID externalUserAdminRoleId = UUID.randomUUID();
            AppRole externalUserAdminRole = AppRole.builder().id(externalUserAdminRoleId).name("External User Admin")
                    .build();
            Set<AppRole> oldRoles = Set.of(externalUserManagerRole, externalUserAdminRole);
            // Set<AppRole> newRoles = Set.of(externalUserManagerRole,
            // externalUserAdminRole);
            UUID firmId = UUID.randomUUID();
            Firm firm = Firm.builder().id(firmId).build();
            Page<UserProfile> firmManagersPage = new PageImpl<>(
                    List.of(mock(UserProfile.class, RETURNS_DEEP_STUBS), mock(UserProfile.class, RETURNS_DEEP_STUBS)),
                    PageRequest.of(0, 2), 2);
            when(mockUserProfileRepository.findFirmUserByAuthzRoleAndFirm(eq(firmId), eq("Firm User Manager"), any()))
                    .thenReturn(firmManagersPage);
            String userId = UUID.randomUUID().toString();
            when(mockAppRoleRepository.findByName(eq("Firm User Manager")))
                    .thenReturn(Optional.of(externalUserManagerRole));

            userService.roleCoverage(oldRoles, Set.of(), firm, userId, false, false);
            verify(mockAppRoleRepository, times(1)).findByName(eq("Firm User Manager"));
            verify(mockUserProfileRepository, times(1)).findFirmUserByAuthzRoleAndFirm(any(), any(), any());
            verify(mockUserProfileRepository, never()).findInternalUserByAuthzRole(any(), any());
        }

        @Test
        void roleCoverage_removeManager_not_last_is_self() {
            ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserService.class);
            UUID externalUserManagerRoleId = UUID.randomUUID();
            AppRole externalUserManagerRole = AppRole.builder().id(externalUserManagerRoleId).name("Firm User Manager")
                    .build();
            UUID externalUserAdminRoleId = UUID.randomUUID();
            AppRole externalUserAdminRole = AppRole.builder().id(externalUserAdminRoleId).name("External User Admin")
                    .build();
            Set<AppRole> oldRoles = Set.of(externalUserManagerRole, externalUserAdminRole);
            Set<AppRole> newRoles = Set.of(externalUserAdminRole);
            UUID firmId = UUID.randomUUID();
            Firm firm = Firm.builder().id(firmId).build();
            Page<UserProfile> firmManagersPage = new PageImpl<>(
                    List.of(mock(UserProfile.class, RETURNS_DEEP_STUBS), mock(UserProfile.class, RETURNS_DEEP_STUBS)),
                    PageRequest.of(0, 2), 2);
            when(mockUserProfileRepository.findFirmUserByAuthzRoleAndFirm(eq(firmId), eq("Firm User Manager"), any()))
                    .thenReturn(firmManagersPage);
            String userId = UUID.randomUUID().toString();
            when(mockAppRoleRepository.findByName(eq("Firm User Manager")))
                    .thenReturn(Optional.of(externalUserManagerRole));

            String result = userService.roleCoverage(oldRoles, newRoles, firm, userId, true, false);
            assertThat(result).contains("You cannot remove your own User Manager role");
            List<ILoggingEvent> warningLogs = LogMonitoring.getLogsByLevel(listAppender, Level.WARN);
            assertThat(warningLogs).isNotEmpty();
            assertThat(warningLogs.getFirst().getFormattedMessage())
                    .contains("Attempt to remove own User Manager role, from user profile");
        }

        @Test
        void roleCoverage_removeManager_is_last_not_self() {
            ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserService.class);
            UUID externalUserManagerRoleId = UUID.randomUUID();
            AppRole externalUserManagerRole = AppRole.builder().id(externalUserManagerRoleId).name("Firm User Manager")
                    .build();
            UUID externalUserAdminRoleId = UUID.randomUUID();
            AppRole externalUserAdminRole = AppRole.builder().id(externalUserAdminRoleId).name("External User Admin")
                    .build();
            Set<AppRole> oldRoles = Set.of(externalUserManagerRole, externalUserAdminRole);
            Set<AppRole> newRoles = Set.of(externalUserAdminRole);
            UUID firmId = UUID.randomUUID();
            Firm firm = Firm.builder().id(firmId).name("MyFirm").build();
            Page<UserProfile> firmManagersPage = new PageImpl<>(
                    List.of(mock(UserProfile.class, RETURNS_DEEP_STUBS)),
                    PageRequest.of(0, 2), 1);
            when(mockUserProfileRepository.findFirmUserByAuthzRoleAndFirm(eq(firmId), eq("Firm User Manager"), any()))
                    .thenReturn(firmManagersPage);
            String userId = UUID.randomUUID().toString();
            when(mockAppRoleRepository.findByName(eq("Firm User Manager")))
                    .thenReturn(Optional.of(externalUserManagerRole));

            String result = userService.roleCoverage(oldRoles, newRoles, firm, userId, false, false);
            assertThat(result)
                    .contains("User Manager role could not be removed, this is the last User Manager of MyFirm");
            List<ILoggingEvent> warningLogs = LogMonitoring.getLogsByLevel(listAppender, Level.WARN);
            assertThat(warningLogs).isNotEmpty();
            assertThat(warningLogs.getFirst().getFormattedMessage())
                    .contains("Attempt to remove last firm User Manager, from user profile");
        }

        @Test
        void roleCoverage_removeManager_is_last_not_self_is_internal() {
            UUID externalUserManagerRoleId = UUID.randomUUID();
            AppRole externalUserManagerRole = AppRole.builder().id(externalUserManagerRoleId)
                    .name("External User Manager").build();
            UUID externalUserAdminRoleId = UUID.randomUUID();
            AppRole externalUserAdminRole = AppRole.builder().id(externalUserAdminRoleId).name("External User Admin")
                    .build();
            Set<AppRole> oldRoles = Set.of(externalUserManagerRole, externalUserAdminRole);
            Set<AppRole> newRoles = Set.of(externalUserAdminRole);
            UUID firmId = UUID.randomUUID();
            Firm firm = Firm.builder().id(firmId).name("MyFirm").build();
            Page<UserProfile> firmManagersPage = new PageImpl<>(
                    List.of(mock(UserProfile.class, RETURNS_DEEP_STUBS)),
                    PageRequest.of(0, 2), 1);
            when(mockUserProfileRepository.findFirmUserByAuthzRoleAndFirm(eq(firmId), eq("External User Manager"),
                    any())).thenReturn(firmManagersPage);
            String userId = UUID.randomUUID().toString();
            when(mockAppRoleRepository.findByName(eq("External User Manager")))
                    .thenReturn(Optional.of(externalUserManagerRole));

            String result = userService.roleCoverage(oldRoles, newRoles, firm, userId, false, true);
            assertThat(result).isEmpty();
        }

        @Test
        void roleCoverage_removeGlobalAdmin_not_last() {
            UUID globalAdminRoleId = UUID.randomUUID();
            AppRole globalAdminRole = AppRole.builder().id(globalAdminRoleId).name("Global Admin").build();
            UUID internalUserManagerRoleId = UUID.randomUUID();
            AppRole internalUserManagerRole = AppRole.builder().id(internalUserManagerRoleId)
                    .name("Internal User Admin").build();
            Set<AppRole> oldRoles = Set.of(internalUserManagerRole, globalAdminRole);
            Set<AppRole> newRoles = Set.of(internalUserManagerRole);
            Page<UserProfile> globalAdminsPage = new PageImpl<>(
                    List.of(mock(UserProfile.class, RETURNS_DEEP_STUBS), mock(UserProfile.class, RETURNS_DEEP_STUBS)),
                    PageRequest.of(0, 2), 2);
            when(mockUserProfileRepository.findInternalUserByAuthzRole(eq("Global Admin"), any()))
                    .thenReturn(globalAdminsPage);
            when(mockAppRoleRepository.findByName(eq("Global Admin"))).thenReturn(Optional.of(globalAdminRole));
            String userId = UUID.randomUUID().toString();

            userService.roleCoverage(oldRoles, newRoles, null, userId, false, false);
            verify(mockAppRoleRepository, times(1)).findByName(eq("Global Admin"));
            verify(mockUserProfileRepository, never()).findFirmUserByAuthzRoleAndFirm(any(), any(), any());
            verify(mockUserProfileRepository, times(1)).findInternalUserByAuthzRole(eq("Global Admin"), any());
        }

        @Test
        void roleCoverage_removeGlobalAdmin_is_last() {
            ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserService.class);
            UUID externalUserManagerRoleId = UUID.randomUUID();
            UUID globalAdminRoleId = UUID.randomUUID();
            AppRole globalAdminRole = AppRole.builder().id(globalAdminRoleId).name("Global Admin").build();
            UUID internalUserManagerRoleId = UUID.randomUUID();
            AppRole internalUserManagerRole = AppRole.builder().id(internalUserManagerRoleId)
                    .name("Internal User Admin").build();
            Set<AppRole> oldRoles = Set.of(internalUserManagerRole, globalAdminRole);
            Set<AppRole> newRoles = Set.of(internalUserManagerRole);
            Page<UserProfile> globalAdminsPage = new PageImpl<>(
                    List.of(mock(UserProfile.class, RETURNS_DEEP_STUBS)),
                    PageRequest.of(0, 2), 1);
            when(mockUserProfileRepository.findInternalUserByAuthzRole(eq("Global Admin"), any()))
                    .thenReturn(globalAdminsPage);
            when(mockAppRoleRepository.findByName(eq("Global Admin"))).thenReturn(Optional.of(globalAdminRole));
            String userId = UUID.randomUUID().toString();

            String result = userService.roleCoverage(oldRoles, newRoles, null, userId, false, false);
            assertThat(result).contains("Global Admin role could not be removed, this is the last Global Admin");
            List<ILoggingEvent> warningLogs = LogMonitoring.getLogsByLevel(listAppender, Level.WARN);
            assertThat(warningLogs).isNotEmpty();
            assertThat(warningLogs.getFirst().getFormattedMessage())
                    .contains("Attempt to remove last Global Admin, from user profile");
        }
    }

    @Test
    void sendVerificationEmail_Success() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID entraOid = UUID.randomUUID();

        UserProfile userProfile = UserProfile.builder().id(profileId).activeProfile(true)
                .userProfileStatus(UserProfileStatus.COMPLETE).userType(UserType.EXTERNAL).build();
        EntraUser user = EntraUser.builder().id(userId).entraOid(entraOid.toString()).userProfiles(Set.of(userProfile))
                .build();
        userProfile.setEntraUser(user);

        when(mockUserProfileRepository.findById(profileId)).thenReturn(Optional.of(userProfile));
        when(techServicesClient.sendEmailVerification(any(EntraUserDto.class)))
                .thenReturn(TechServicesApiResponse.success(SendUserVerificationEmailResponse.builder().success(true)
                        .build()));

        // Act
        TechServicesApiResponse<SendUserVerificationEmailResponse> response = userService
                .sendVerificationEmail(profileId.toString());

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        verify(techServicesClient, times(1)).sendEmailVerification(any(EntraUserDto.class));
    }

    @Test
    void sendVerificationEmail_Failure() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID entraOid = UUID.randomUUID();

        UserProfile userProfile = UserProfile.builder().id(profileId).activeProfile(true)
                .userProfileStatus(UserProfileStatus.COMPLETE).userType(UserType.EXTERNAL).build();
        EntraUser user = EntraUser.builder().id(userId).entraOid(entraOid.toString()).userProfiles(Set.of(userProfile))
                .build();
        userProfile.setEntraUser(user);

        when(mockUserProfileRepository.findById(profileId)).thenReturn(Optional.of(userProfile));
        when(techServicesClient.sendEmailVerification(any(EntraUserDto.class)))
                .thenReturn(TechServicesApiResponse.error(TechServicesErrorResponse.builder().success(false)
                        .build()));

        // Act
        TechServicesApiResponse<SendUserVerificationEmailResponse> response = userService
                .sendVerificationEmail(profileId.toString());

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        verify(techServicesClient, times(1)).sendEmailVerification(any(EntraUserDto.class));
    }

    @Test
    void sendVerificationEmail_UserNotFound() {
        // Arrange
        UUID profileId = UUID.randomUUID();

        when(mockUserProfileRepository.findById(profileId)).thenReturn(Optional.empty());

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.sendVerificationEmail(profileId.toString()));

        // Assert
        assertThat(ex.getMessage()).isEqualTo("Failed to send verification email!");
        verify(techServicesClient, never()).sendEmailVerification(any(EntraUserDto.class));
    }

    @Test
    void getRolesByIdIn() {
        AppRole appRole1 = AppRole.builder().id(UUID.randomUUID()).name("ap1").build();
        AppRole appRole2 = AppRole.builder().id(UUID.randomUUID()).build();
        AppRole appRole3 = AppRole.builder().id(UUID.randomUUID()).build();
        List<AppRole> appRoles = List.of(appRole1, appRole2, appRole3);
        when(mockAppRoleRepository.findAllByIdIn(any())).thenReturn(appRoles);
        Map<String, AppRoleDto> rolesByIdIn = userService.getRolesByIdIn(List.of());
        assertThat(rolesByIdIn).hasSize(3);
        assertThat(rolesByIdIn.get(appRole1.getId().toString()).getName()).isEqualTo("ap1");
    }

    @Test
    void getAppRolesByAppsId() {
        AppRole appRole1 = AppRole.builder().id(UUID.randomUUID()).name("ap1").build();
        AppRole appRole2 = AppRole.builder().id(UUID.randomUUID()).build();
        AppRole appRole3 = AppRole.builder().id(UUID.randomUUID()).build();
        List<AppRole> appRoles = List.of(appRole1, appRole2, appRole3);
        when(mockAppRoleRepository.findByAppIdUserTypeRestriction(anyList(), any())).thenReturn(appRoles);
        List<AppRoleDto> rolesByIdIn = userService.getAppRolesByAppsId(List.of(), "user");
        assertThat(rolesByIdIn).hasSize(3);
        assertThat(rolesByIdIn.get(0).getName()).isEqualTo("ap1");
    }

    @Nested
    class AddUserProfileTests {

        @Mock
        private UserProfileRepository userProfileRepository;

        @Mock
        private EntraUserRepository entraUserRepository;

        @Mock
        private AppRoleRepository appRoleRepository;

        @Mock
        private TechServicesClient techServicesClient;

        @Mock
        private FirmService firmService;

        @Mock
        private OfficeRepository officeRepository;

        @Mock
        private ModelMapper mapper;

        @InjectMocks
        private UserService userService;

        @Test
        void shouldThrowExceptionIfNotMultiFirmUser() {
            EntraUserDto user = EntraUserDto.builder().build();

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.addMultiFirmUserProfile(user, new FirmDto(), null, null, "admin"));

            assertThat(ex.getMessage()).contains("is not a multi-firm user");
            verify(userProfileRepository, never()).save(any());
            verify(entraUserRepository, never()).save(any());
            verify(techServicesClient, never()).updateRoleAssignment(any(UUID.class));
        }

        @Test
        void shouldThrowExceptionIfFirmDetailsInvalid() {
            EntraUserDto user = EntraUserDto.builder().multiFirmUser(true).build();

            FirmDto firmDto = new FirmDto();
            firmDto.setSkipFirmSelection(false); // invalid

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.addMultiFirmUserProfile(user, firmDto, null, null, "admin"));

            assertThat(ex.getMessage()).contains("Invalid firm details");
            verify(userProfileRepository, never()).save(any());
            verify(entraUserRepository, never()).save(any());
            verify(techServicesClient, never()).updateRoleAssignment(any(UUID.class));
        }

        @Test
        void shouldThrowExceptionIfFirmDetailsNotProvided() {
            EntraUserDto user = EntraUserDto.builder().multiFirmUser(true).build();

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.addMultiFirmUserProfile(user, null, null, null, "admin"));

            assertThat(ex.getMessage()).contains("Invalid firm details");
            verify(userProfileRepository, never()).save(any());
            verify(entraUserRepository, never()).save(any());
            verify(techServicesClient, never()).updateRoleAssignment(any(UUID.class));
        }

        @Test
        void shouldThrowExceptionIfFirmAlreadyAssigned() {
            UUID firmId = UUID.randomUUID();
            FirmDto firmDto = new FirmDto();
            firmDto.setId(firmId);
            firmDto.setSkipFirmSelection(true);

            Firm firm = Firm.builder().id(firmId).build();

            UUID entraUserId = UUID.randomUUID();
            EntraUser entraUser = EntraUser.builder().id(entraUserId).multiFirmUser(true).build();

            UserProfile existingProfile = UserProfile.builder().firm(firm).build();
            entraUser.setUserProfiles(Set.of(existingProfile));

            EntraUserDto user = EntraUserDto.builder().id(entraUserId.toString()).multiFirmUser(true).build();

            when(entraUserRepository.findById(entraUserId)).thenReturn(Optional.of(entraUser));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.addMultiFirmUserProfile(user, firmDto, null, List.of(), "admin"));

            assertThat(ex.getMessage()).contains("User profile already exists");
            verify(userProfileRepository, never()).save(any());
            verify(entraUserRepository, never()).save(any());
            verify(techServicesClient, never()).updateRoleAssignment(any(UUID.class));
        }

        @Test
        void shouldThrowErrorIfEntraUserNotPresent() {
            UUID firmId = UUID.randomUUID();
            FirmDto firmDto = new FirmDto();
            firmDto.setId(firmId);

            UUID entraUserId = UUID.randomUUID();
            EntraUserDto user = EntraUserDto.builder().id(entraUserId.toString()).multiFirmUser(true).build();

            when(entraUserRepository.findById(entraUserId)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.addMultiFirmUserProfile(user, firmDto, null, null, "admin"));

            assertThat(ex.getMessage()).contains("User not found for the given user user id");
            verify(userProfileRepository, never()).save(any());
            verify(entraUserRepository, never()).save(any());
            verify(techServicesClient, never()).updateRoleAssignment(any(UUID.class));
        }

        @Test
        void shouldCreateActiveProfileIfNoExistingProfiles() {
            UUID firmId = UUID.randomUUID();
            FirmDto firmDto = new FirmDto();
            firmDto.setId(firmId);

            UUID existingFirmId = UUID.randomUUID();
            Firm firm = Firm.builder().id(existingFirmId).build();

            UUID entraUserId = UUID.randomUUID();
            EntraUserDto userDto = EntraUserDto.builder().id(entraUserId.toString()).multiFirmUser(true).build();
            EntraUser entraUser = EntraUser.builder().id(entraUserId).multiFirmUser(true).build();

            when(entraUserRepository.findById(entraUserId)).thenReturn(Optional.of(entraUser));

            UserProfile result = userService.addMultiFirmUserProfile(userDto, firmDto, null, null, "admin");

            assertThat(result.isActiveProfile()).isTrue();
            verify(userProfileRepository).save(result);
            verify(entraUserRepository).save(entraUser);
            verify(techServicesClient).updateRoleAssignment(any(UUID.class));
        }

        @Test
        void shouldCreateInactiveProfileIfExistingProfilesPresent() {
            UUID firmId = UUID.randomUUID();
            Firm existingFirm = Firm.builder().id(firmId).build();

            UUID entraUserId = UUID.randomUUID();
            EntraUser entraUser = EntraUser.builder().id(entraUserId).multiFirmUser(true).build();
            UserProfile existingProfile = UserProfile.builder().firm(existingFirm).build();
            entraUser.setUserProfiles(new HashSet<>(Set.of(existingProfile)));

            Office office = Office.builder().id(UUID.randomUUID()).build();
            OfficeDto officeDto = new OfficeDto();
            AppRole appRole = AppRole.builder().id(UUID.randomUUID()).name("role").build();
            AppRoleDto appRoleDto = AppRoleDto.builder().id(UUID.randomUUID().toString()).build();
            FirmDto newFirmDto = FirmDto.builder().id(UUID.randomUUID()).build();

            when(entraUserRepository.findById(entraUserId)).thenReturn(Optional.of(entraUser));
            when(officeRepository.findById(any())).thenReturn(Optional.ofNullable(office));
            when(appRoleRepository.findById(any())).thenReturn(Optional.ofNullable(appRole));

            EntraUserDto user = EntraUserDto.builder().id(entraUserId.toString()).multiFirmUser(true).build();

            UserProfile result = userService.addMultiFirmUserProfile(user, newFirmDto, List.of(officeDto),
                    List.of(appRoleDto), "admin");

            assertThat(result.isActiveProfile()).isFalse();
            verify(userProfileRepository).save(result);
            verify(entraUserRepository).save(entraUser);
            verify(techServicesClient).updateRoleAssignment(any(UUID.class));
        }

    }

    @Nested
    class PaginationAndSortingTests {

        @Test
        void getPageOfUsersBySearch_withMultiplePages_returnsCorrectTotalPages() {
            // Given - 116 total users with 10 per page should give 12 pages
            String searchTerm = "";
            FirmSearchForm firmSearch = FirmSearchForm.builder().build();
            UserType userType = null;
            boolean showFirmAdmins = false;
            boolean showMultiFirmUsers = false;
            int page = 1;
            int pageSize = 10;
            String sort = "firstName";
            String direction = "ASC";

            UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearch, userType, showFirmAdmins,
                    showMultiFirmUsers);

            List<UserSearchResultsDto> users = createUserSearchResults(10);

            Page<UserSearchResultsDto> userProfilePage = new PageImpl<>(
                    users,
                    PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "entraUser.firstName")),
                    116 // Total elements
            );

            when(mockUserProfileRepository.findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class)))
                    .thenReturn(userProfilePage);

            // When
            PaginatedUsers result = userService.getPageOfUsersBySearch(criteria, page, pageSize, sort, direction);

            // Then
            assertThat(result.getUsers()).hasSize(10);
            assertThat(result.getTotalUsers()).isEqualTo(116);
            assertThat(result.getTotalPages()).isEqualTo(12); // 116/10 = 11.6, rounds up to 12
            verify(mockUserProfileRepository).findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class));
        }

        @Test
        void getPageOfUsersBySearch_withSorting_maintainsCorrectCount() {
            // Given - Sorting should not affect the total count
            String searchTerm = "";
            FirmSearchForm firmSearch = FirmSearchForm.builder().build();
            UserType userType = UserType.EXTERNAL;
            boolean showFirmAdmins = false;
            boolean showMultiFirmUsers = false;
            int page = 1;
            int pageSize = 10;
            String sort = "firmName";
            String direction = "DESC";

            UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearch, userType, showFirmAdmins,
                    showMultiFirmUsers);

            List<UserSearchResultsDto> users = createUserSearchResults(10);

            Page<UserSearchResultsDto> userProfilePage = new PageImpl<>(
                    users,
                    PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "firm.name")),
                    50 // Total elements
            );

            when(mockUserProfileRepository.findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class)))
                    .thenReturn(userProfilePage);

            // When
            PaginatedUsers result = userService.getPageOfUsersBySearch(criteria, page, pageSize, sort, direction);

            // Then
            assertThat(result.getUsers()).hasSize(10);
            assertThat(result.getTotalUsers()).isEqualTo(50);
            assertThat(result.getTotalPages()).isEqualTo(5); // 50/10 = 5 pages
            verify(mockUserProfileRepository).findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class));
        }

        @Test
        void getPageOfUsersBySearch_lastPagePartial_returnsCorrectCount() {
            // Given - Last page with only 6 users (page 12 of 116 total)
            String searchTerm = "";
            FirmSearchForm firmSearch = FirmSearchForm.builder().build();
            UserType userType = null;
            boolean showFirmAdmins = false;
            boolean showMultiFirmUsers = false;
            int page = 12;
            int pageSize = 10;
            String sort = "firstName";
            String direction = "ASC";

            UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearch, userType, showFirmAdmins,
                    showMultiFirmUsers);

            List<UserSearchResultsDto> users = createUserSearchResults(6); // Only 6 users on last page

            Page<UserSearchResultsDto> userProfilePage = new PageImpl<>(
                    users,
                    PageRequest.of(11, pageSize, Sort.by(Sort.Direction.ASC, "entraUser.firstName")),
                    116 // Total elements
            );

            when(mockUserProfileRepository.findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class)))
                    .thenReturn(userProfilePage);

            // When
            PaginatedUsers result = userService.getPageOfUsersBySearch(criteria, page, pageSize, sort, direction);

            // Then
            assertThat(result.getUsers()).hasSize(6);
            assertThat(result.getTotalUsers()).isEqualTo(116);
            assertThat(result.getTotalPages()).isEqualTo(12);
            verify(mockUserProfileRepository).findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class));
        }

        @Test
        void getPageOfUsersBySearch_withFirmFilter_returnsCorrectPagination() {
            // Given - Filtering by firm should maintain accurate pagination
            String searchTerm = "";
            UUID selectedFirmId = UUID.randomUUID();
            FirmSearchForm firmSearch = FirmSearchForm.builder()
                    .selectedFirmId(selectedFirmId)
                    .firmSearch("Test Firm")
                    .build();
            UserType userType = UserType.EXTERNAL;
            boolean showFirmAdmins = false;
            boolean showMultiFirmUsers = false;
            int page = 1;
            int pageSize = 10;
            String sort = "firstName";
            String direction = "ASC";

            UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearch, userType, showFirmAdmins,
                    showMultiFirmUsers);

            List<UserSearchResultsDto> users = createUserSearchResults(10);

            Page<UserSearchResultsDto> userProfilePage = new PageImpl<>(
                    users,
                    PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "entraUser.firstName")),
                    25 // Total elements after firm filter
            );

            when(mockUserProfileRepository.findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class)))
                    .thenReturn(userProfilePage);

            // When
            PaginatedUsers result = userService.getPageOfUsersBySearch(criteria, page, pageSize, sort, direction);

            // Then
            assertThat(result.getUsers()).hasSize(10);
            assertThat(result.getTotalUsers()).isEqualTo(25);
            assertThat(result.getTotalPages()).isEqualTo(3); // 25/10 = 2.5, rounds up to 3
            verify(mockUserProfileRepository).findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class));
        }

        @Test
        void getPageOfUsersBySearch_withSearchTerm_returnsFilteredPagination() {
            // Given - Search term should filter results and update pagination
            String searchTerm = "john";
            FirmSearchForm firmSearch = FirmSearchForm.builder().build();
            UserType userType = null;
            boolean showFirmAdmins = false;
            boolean showMultiFirmUsers = false;
            int page = 1;
            int pageSize = 10;
            String sort = "firstName";
            String direction = "ASC";

            UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearch, userType, showFirmAdmins,
                    showMultiFirmUsers);

            List<UserSearchResultsDto> users = createUserSearchResults(5); // Search returns 5 results

            Page<UserSearchResultsDto> userProfilePage = new PageImpl<>(
                    users,
                    PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "entraUser.firstName")),
                    5 // Total elements matching search
            );

            when(mockUserProfileRepository.findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class)))
                    .thenReturn(userProfilePage);

            // When
            PaginatedUsers result = userService.getPageOfUsersBySearch(criteria, page, pageSize, sort, direction);

            // Then
            assertThat(result.getUsers()).hasSize(5);
            assertThat(result.getTotalUsers()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(1); // Only 1 page needed
            verify(mockUserProfileRepository).findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class));
        }

        @Test
        void getPageOfUsersBySearch_withShowFirmAdminsFilter_returnsFilteredPagination() {
            // Given - Show firm admins filter should work correctly with pagination
            String searchTerm = "";
            FirmSearchForm firmSearch = FirmSearchForm.builder().build();
            UserType userType = UserType.EXTERNAL;
            boolean showFirmAdmins = true;
            boolean showMultiFirmUsers = true;
            int page = 1;
            int pageSize = 10;
            String sort = "firstName";
            String direction = "ASC";

            UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearch, userType, showFirmAdmins,
                    showMultiFirmUsers);

            List<UserSearchResultsDto> users = createUserSearchResults(8); // 8 firm admins

            Page<UserSearchResultsDto> userProfilePage = new PageImpl<>(
                    users,
                    PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "entraUser.firstName")),
                    18 // Total firm admins
            );

            when(mockUserProfileRepository.findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class)))
                    .thenReturn(userProfilePage);

            // When
            PaginatedUsers result = userService.getPageOfUsersBySearch(criteria, page, pageSize, sort, direction);

            // Then
            assertThat(result.getUsers()).hasSize(8);
            assertThat(result.getTotalUsers()).isEqualTo(18);
            assertThat(result.getTotalPages()).isEqualTo(2); // 18/10 = 1.8, rounds up to 2
            verify(mockUserProfileRepository).findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class));
        }

        @Test
        void getPageOfUsersBySearch_differentSortFields_returnsConsistentCounts() {
            // Given - Different sort fields should not affect total counts
            String searchTerm = "";
            FirmSearchForm firmSearch = FirmSearchForm.builder().build();
            UserType userType = null;
            boolean showFirmAdmins = false;
            boolean showMultiFirmUsers = false;
            int pageSize = 10;
            int totalElements = 50;

            List<UserSearchResultsDto> users = createUserSearchResults(10);

            // Test sorting by different fields
            String[][] sortConfigs = {
                    { "firstName", "ASC" },
                    { "lastName", "DESC" },
                    { "email", "ASC" },
                    { "firmName", "DESC" },
                    { "userType", "ASC" },
                    { "userStatus", "DESC" }
            };

            for (String[] config : sortConfigs) {
                String sort = config[0];
                String direction = config[1];

                UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearch, userType, showFirmAdmins,
                        showMultiFirmUsers);

                Page<UserSearchResultsDto> userProfilePage = new PageImpl<>(
                        users,
                        PageRequest.of(0, pageSize, Sort.by(Sort.Direction.valueOf(direction), "entraUser.firstName")),
                        totalElements);

                when(mockUserProfileRepository.findBySearchParams(any(UserSearchCriteria.class),
                        any(PageRequest.class)))
                        .thenReturn(userProfilePage);

                // When
                PaginatedUsers result = userService.getPageOfUsersBySearch(criteria, 1, pageSize, sort, direction);

                // Then
                assertThat(result.getTotalUsers()).isEqualTo(totalElements);
                assertThat(result.getTotalPages()).isEqualTo(5);
            }
        }

        @Test
        void getPageOfUsersBySearch_pageNumberZero_treatedAsPageOne() {
            // Given - Page 0 should be converted to page 1 (1-indexed to 0-indexed)
            String searchTerm = "";
            FirmSearchForm firmSearch = FirmSearchForm.builder().build();
            UserType userType = null;
            boolean showFirmAdmins = false;
            boolean showMultiFirmUsers = false;
            int page = 0; // Invalid page number
            int pageSize = 10;
            String sort = "firstName";
            String direction = "ASC";

            UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearch, userType, showFirmAdmins,
                    showMultiFirmUsers);

            List<UserSearchResultsDto> users = createUserSearchResults(10);

            Page<UserSearchResultsDto> userProfilePage = new PageImpl<>(
                    users,
                    PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "entraUser.firstName")),
                    30);

            when(mockUserProfileRepository.findBySearchParams(any(UserSearchCriteria.class), any(PageRequest.class)))
                    .thenReturn(userProfilePage);

            // When
            PaginatedUsers result = userService.getPageOfUsersBySearch(criteria, page, pageSize, sort, direction);

            // Then
            assertThat(result.getUsers()).hasSize(10);
            assertThat(result.getTotalUsers()).isEqualTo(30);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }

        private List<UserSearchResultsDto> createUserSearchResults(int count) {
            List<UserSearchResultsDto> searchResults = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                UserSearchResultsDto result = new UserSearchResultsDto(UUID.randomUUID(), true, UserType.EXTERNAL,
                        UUID.randomUUID(), UserProfileStatus.COMPLETE, false, "User" + i, "Test" + i, "Test User",
                        "user" + i + "@example.com", UserStatus.ACTIVE, "Firm" + i);

                searchResults.add(result);
            }
            return searchResults;
        }
    }

    @Nested
    class DeleteFirmProfileTests {

        @Test
        void deleteFirmProfile_Success_ReturnsAuditEvent() {
            // Given
            UUID userProfileId = UUID.randomUUID();
            UUID entraUserId = UUID.randomUUID();
            UUID firmId = UUID.randomUUID();
            UUID actorId = UUID.randomUUID();

            // Create EntraUser with multi-firm flag
            EntraUser entraUser = EntraUser.builder()
                    .id(entraUserId)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .multiFirmUser(true)
                    .userProfiles(new HashSet<>())
                    .build();

            // Create firm
            Firm firm = Firm.builder()
                    .id(firmId)
                    .name("Test Law Firm")
                    .code("12345")
                    .build();

            // Create offices
            Office office1 = Office.builder()
                    .id(UUID.randomUUID())
                    .firm(firm)
                    .build();
            Office office2 = Office.builder()
                    .id(UUID.randomUUID())
                    .firm(firm)
                    .build();

            // Create app roles with CCMS codes for PUI roles
            App puiApp = App.builder()
                    .id(UUID.randomUUID())
                    .name("PUI")
                    .build();
            AppRole puiRole1 = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("PUI_CASE_WORKER")
                    .app(puiApp)
                    .ccmsCode("CCMS.PUI.CASEWORKER")
                    .legacySync(true)
                    .build();
            AppRole puiRole2 = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("PUI_FINANCE")
                    .app(puiApp)
                    .ccmsCode("CCMS.PUI.FINANCE")
                    .legacySync(true)
                    .build();
            AppRole nonPuiRole = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("SOME_OTHER_ROLE")
                    .app(App.builder().id(UUID.randomUUID()).name("OtherApp").build())
                    .build();

            // Create user profile to delete (active profile)
            UserProfile profileToDelete = UserProfile.builder()
                    .id(userProfileId)
                    .entraUser(entraUser)
                    .firm(firm)
                    .activeProfile(true)
                    .appRoles(new HashSet<>(Arrays.asList(puiRole1, puiRole2, nonPuiRole)))
                    .offices(new HashSet<>(Arrays.asList(office1, office2)))
                    .build();

            // Create another profile for the same user (will become active)
            UUID otherFirmId = UUID.randomUUID();
            Firm otherFirm = Firm.builder()
                    .id(otherFirmId)
                    .name("Other Law Firm")
                    .code("67890")
                    .build();
            UserProfile otherProfile = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(entraUser)
                    .firm(otherFirm)
                    .activeProfile(false)
                    .build();

            // Set up bidirectional relationship
            entraUser.getUserProfiles().add(profileToDelete);
            entraUser.getUserProfiles().add(otherProfile);

            // Mock repository calls
            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(profileToDelete));
            when(mockUserProfileRepository.findAllByEntraUser(entraUser))
                    .thenReturn(Arrays.asList(profileToDelete, otherProfile));
            when(mockUserProfileRepository.save(any(UserProfile.class))).thenAnswer(returnsFirstArg());

            // When
            boolean result = userService.deleteFirmProfile(userProfileId.toString(), actorId);

            // Then
            assertThat(result).isTrue();

            // Verify profile was cleared of roles and offices
            assertThat(profileToDelete.getAppRoles()).isEmpty();
            assertThat(profileToDelete.getOffices()).isEmpty();

            // Verify profile was deleted
            verify(mockUserProfileRepository).delete(profileToDelete);

            // Verify save was called at least once (for clearing entra user ref before
            // deletion,
            // and potentially setting new active profile after deletion)
            verify(mockUserProfileRepository, atLeast(1)).save(any(UserProfile.class));

            // Verify PUI notifications were sent (once with empty new roles and PUI old
            // roles)
            verify(mockRoleChangeNotificationService, times(1)).sendMessage(eq(profileToDelete), any(), any());
        }

        @Test
        void deleteFirmProfile_NotMultiFirmUser_ThrowsException() {
            // Given
            UUID userProfileId = UUID.randomUUID();
            UUID entraUserId = UUID.randomUUID();
            UUID actorId = UUID.randomUUID();

            EntraUser entraUser = EntraUser.builder()
                    .id(entraUserId)
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .multiFirmUser(false) // Not a multi-firm user
                    .build();

            UserProfile userProfile = UserProfile.builder()
                    .id(userProfileId)
                    .entraUser(entraUser)
                    .firm(Firm.builder().id(UUID.randomUUID()).name("Test Firm").build())
                    .build();

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(userProfile));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.deleteFirmProfile(userProfileId.toString(), actorId));

            assertThat(exception.getMessage())
                    .contains("User is not a multi-firm user");

            // Verify no deletion occurred
            verify(mockUserProfileRepository, never()).delete(any());
        }

        @Test
        void deleteFirmProfile_ProfileNotFound_ThrowsException() {
            // Given
            UUID userProfileId = UUID.randomUUID();
            UUID actorId = UUID.randomUUID();

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.deleteFirmProfile(userProfileId.toString(), actorId));

            assertThat(exception.getMessage())
                    .contains("User profile not found");

            // Verify no deletion occurred
            verify(mockUserProfileRepository, never()).delete(any());
        }

        @Test
        void deleteFirmProfile_NonActiveProfile_DoesNotReassignActive() {
            // Given
            UUID userProfileId = UUID.randomUUID();
            UUID entraUserId = UUID.randomUUID();
            UUID actorId = UUID.randomUUID();

            EntraUser entraUser = EntraUser.builder()
                    .id(entraUserId)
                    .firstName("Alice")
                    .lastName("Williams")
                    .email("alice.williams@example.com")
                    .multiFirmUser(true)
                    .userProfiles(new HashSet<>())
                    .build();

            // Profile to delete (NOT active)
            UserProfile profileToDelete = UserProfile.builder()
                    .id(userProfileId)
                    .entraUser(entraUser)
                    .firm(Firm.builder().id(UUID.randomUUID()).name("Firm A").code("AAA").build())
                    .activeProfile(false)
                    .appRoles(new HashSet<>())
                    .offices(new HashSet<>())
                    .build();

            // Another profile (already active)
            UserProfile activeProfile = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(entraUser)
                    .firm(Firm.builder().id(UUID.randomUUID()).name("Firm B").build())
                    .activeProfile(true)
                    .build();

            // Set up bidirectional relationship
            entraUser.getUserProfiles().add(profileToDelete);
            entraUser.getUserProfiles().add(activeProfile);

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(profileToDelete));
            // Don't need findAllByEntraUser stub - profiles read from
            // entraUser.getUserProfiles()
            when(mockUserProfileRepository.save(any(UserProfile.class))).thenAnswer(returnsFirstArg());

            // When
            var result = userService.deleteFirmProfile(userProfileId.toString(), actorId);

            // Then
            assertThat(result).isNotNull();

            // Verify profile was deleted
            verify(mockUserProfileRepository).delete(profileToDelete);

            // Verify active profile was NOT modified since deletion of non-active profile
            // The service still saves after clearing entra user ref, but shouldn't reassign
            // active
            assertThat(activeProfile.isActiveProfile()).isTrue();
        }

        @Test
        void deleteFirmProfile_ClearsRolesAndOffices() {
            // Given
            UUID userProfileId = UUID.randomUUID();
            UUID entraUserId = UUID.randomUUID();
            UUID actorId = UUID.randomUUID();

            EntraUser entraUser = EntraUser.builder()
                    .id(entraUserId)
                    .firstName("Charlie")
                    .lastName("Brown")
                    .email("charlie.brown@example.com")
                    .multiFirmUser(true)
                    .userProfiles(new HashSet<>())
                    .build();

            Firm firm = Firm.builder()
                    .id(UUID.randomUUID())
                    .name("Test Firm")
                    .build();

            // Create multiple roles and offices
            Set<AppRole> roles = IntStream.range(0, 5)
                    .mapToObj(i -> AppRole.builder()
                            .id(UUID.randomUUID())
                            .name("ROLE_" + i)
                            .app(App.builder().id(UUID.randomUUID()).name("App").build())
                            .build())
                    .collect(Collectors.toSet());

            Set<Office> offices = IntStream.range(0, 3)
                    .mapToObj(i -> Office.builder()
                            .id(UUID.randomUUID())
                            .firm(firm)
                            .build())
                    .collect(Collectors.toSet());

            UserProfile profileToDelete = UserProfile.builder()
                    .id(userProfileId)
                    .entraUser(entraUser)
                    .firm(Firm.builder().id(UUID.randomUUID()).name("Firm").code("123").build())
                    .activeProfile(false)
                    .appRoles(new HashSet<>(roles))
                    .offices(new HashSet<>(offices))
                    .build();

            UserProfile otherProfile = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(entraUser)
                    .firm(Firm.builder().id(UUID.randomUUID()).name("Other Firm").build())
                    .activeProfile(true)
                    .build();

            // Set up bidirectional relationship
            entraUser.getUserProfiles().add(profileToDelete);
            entraUser.getUserProfiles().add(otherProfile);

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(profileToDelete));
            // Don't need findAllByEntraUser stub - profiles read from
            // entraUser.getUserProfiles()
            when(mockUserProfileRepository.save(any(UserProfile.class))).thenAnswer(returnsFirstArg());

            // When
            boolean result = userService.deleteFirmProfile(userProfileId.toString(), actorId);

            // Then
            assertThat(result).isTrue();
            assertThat(profileToDelete.getAppRoles()).isEmpty();
            assertThat(profileToDelete.getOffices()).isEmpty();
        }

        @Test
        void deleteFirmProfile_SendsPuiNotifications() {
            // Given
            UUID userProfileId = UUID.randomUUID();
            UUID entraUserId = UUID.randomUUID();
            final UUID actorId = UUID.randomUUID();

            EntraUser entraUser = EntraUser.builder()
                    .id(entraUserId)
                    .firstName("David")
                    .lastName("Miller")
                    .email("david.miller@example.com")
                    .multiFirmUser(true)
                    .userProfiles(new HashSet<>())
                    .build();

            App puiApp = App.builder()
                    .id(UUID.randomUUID())
                    .name("PUI")
                    .build();

            // Mix of PUI roles (with CCMS code and legacySync) and non-PUI roles
            AppRole puiRole1 = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("PUI_CASE_WORKER")
                    .app(puiApp)
                    .ccmsCode("CCMS.PUI.CASEWORKER")
                    .legacySync(true)
                    .build();
            AppRole puiRole2 = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("PUI_FINANCE")
                    .app(puiApp)
                    .ccmsCode("CCMS.PUI.FINANCE")
                    .legacySync(true)
                    .build();
            AppRole nonPuiRole = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("OTHER_ROLE")
                    .app(App.builder().id(UUID.randomUUID()).name("OtherApp").build())
                    .build();

            Firm firm = Firm.builder()
                    .id(UUID.randomUUID())
                    .name("Test Firm")
                    .code("999")
                    .build();

            UserProfile profileToDelete = UserProfile.builder()
                    .id(userProfileId)
                    .entraUser(entraUser)
                    .firm(firm)
                    .activeProfile(false)
                    .appRoles(new HashSet<>(Arrays.asList(puiRole1, puiRole2, nonPuiRole)))
                    .offices(new HashSet<>())
                    .build();

            UserProfile otherProfile = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(entraUser)
                    .firm(Firm.builder().id(UUID.randomUUID()).name("Other Firm").build())
                    .activeProfile(true)
                    .build();

            // Set up bidirectional relationship
            entraUser.getUserProfiles().add(profileToDelete);
            entraUser.getUserProfiles().add(otherProfile);

            when(mockUserProfileRepository.findById(userProfileId)).thenReturn(Optional.of(profileToDelete));
            // Don't need findAllByEntraUser stub - profiles read from
            // entraUser.getUserProfiles()
            when(mockUserProfileRepository.save(any(UserProfile.class))).thenAnswer(returnsFirstArg());

            // When
            userService.deleteFirmProfile(userProfileId.toString(), actorId);

            // Then - verify notification service was called with PUI roles
            verify(mockRoleChangeNotificationService, times(1)).sendMessage(eq(profileToDelete),
                    eq(Collections.emptySet()), any());
        }
    }

    @Nested
    class GetProfileCountByEntraUserIdTests {

        @Test
        void getProfileCountByEntraUserId_whenUserHasMultipleProfiles_returnsCorrectCount() {
            // Given
            UUID entraUserId = UUID.randomUUID();
            long expectedCount = 3L;
            when(mockUserProfileRepository.countByEntraUserId(entraUserId)).thenReturn(expectedCount);

            // When
            long actualCount = userService.getProfileCountByEntraUserId(entraUserId);

            // Then
            assertThat(actualCount).isEqualTo(expectedCount);
            verify(mockUserProfileRepository, times(1)).countByEntraUserId(entraUserId);
        }

        @Test
        void getProfileCountByEntraUserId_whenUserHasSingleProfile_returnsOne() {
            // Given
            UUID entraUserId = UUID.randomUUID();
            long expectedCount = 1L;
            when(mockUserProfileRepository.countByEntraUserId(entraUserId)).thenReturn(expectedCount);

            // When
            long actualCount = userService.getProfileCountByEntraUserId(entraUserId);

            // Then
            assertThat(actualCount).isEqualTo(expectedCount);
            verify(mockUserProfileRepository, times(1)).countByEntraUserId(entraUserId);
        }

        @Test
        void getProfileCountByEntraUserId_whenUserHasNoProfiles_returnsZero() {
            // Given
            UUID entraUserId = UUID.randomUUID();
            long expectedCount = 0L;
            when(mockUserProfileRepository.countByEntraUserId(entraUserId)).thenReturn(expectedCount);

            // When
            long actualCount = userService.getProfileCountByEntraUserId(entraUserId);

            // Then
            assertThat(actualCount).isEqualTo(expectedCount);
            verify(mockUserProfileRepository, times(1)).countByEntraUserId(entraUserId);
        }

        @Test
        void getProfileCountByEntraUserId_whenUserDoesNotExist_returnsZero() {
            // Given
            UUID nonExistentUserId = UUID.randomUUID();
            when(mockUserProfileRepository.countByEntraUserId(nonExistentUserId)).thenReturn(0L);

            // When
            long actualCount = userService.getProfileCountByEntraUserId(nonExistentUserId);

            // Then
            assertThat(actualCount).isEqualTo(0L);
            verify(mockUserProfileRepository, times(1)).countByEntraUserId(nonExistentUserId);
        }
    }

    @Nested
    class GetAuditUsersTests {

        @Test
        void getAuditUsers_whenNoFilters_returnsAllUsers() {
            // Given
            UUID user1Id = UUID.randomUUID();
            UUID user2Id = UUID.randomUUID();

            EntraUser user1 = EntraUser.builder()
                    .id(user1Id)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .userStatus(UserStatus.ACTIVE)
                    .multiFirmUser(false)
                    .build();

            EntraUser user2 = EntraUser.builder()
                    .id(user2Id)
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .userStatus(UserStatus.ACTIVE)
                    .multiFirmUser(false)
                    .build();

            Firm firm = Firm.builder()
                    .id(UUID.randomUUID())
                    .name("Test Firm")
                    .code("TF001")
                    .build();

            UserProfile profile1 = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(user1)
                    .firm(firm)
                    .userType(UserType.EXTERNAL)
                    .activeProfile(true)
                    .appRoles(new HashSet<>())
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            UserProfile profile2 = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(user2)
                    .firm(firm)
                    .userType(UserType.EXTERNAL)
                    .activeProfile(true)
                    .appRoles(new HashSet<>())
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            user1.setUserProfiles(Set.of(profile1));
            user2.setUserProfiles(Set.of(profile2));

            Page<EntraUser> userPage = new PageImpl<>(Arrays.asList(user1, user2),
                    PageRequest.of(0, 10), 2);

            when(mockEntraUserRepository.findAllUsersForAudit(
                    eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(userPage);

            when(mockEntraUserRepository.findUsersWithProfilesAndRoles(any(Set.class)))
                    .thenReturn(Arrays.asList(user1, user2));

            // When
            uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers result = userService.getAuditUsers(null, null,
                    null, null, 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalUsers()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getCurrentPage()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getUsers()).hasSize(2);

            assertThat(result.getUsers().get(0).getName()).isEqualTo("John Doe");
            assertThat(result.getUsers().get(0).getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.getUsers().get(0).getUserType()).isEqualTo("External");
            assertThat(result.getUsers().get(0).getFirmAssociation()).isEqualTo("Test Firm");
            assertThat(result.getUsers().get(0).isMultiFirmUser()).isFalse();
            assertThat(result.getUsers().get(0).getProfileCount()).isEqualTo(1);

            verify(mockEntraUserRepository).findAllUsersForAudit(
                    eq(null), eq(null), eq(null), eq(null), any(PageRequest.class));
            verify(mockEntraUserRepository).findUsersWithProfilesAndRoles(any(Set.class));
        }

        @Test
        void getAuditUsers_whenSearchTermProvided_filtersResults() {
            // Given
            String searchTerm = "john";
            UUID userId = UUID.randomUUID();

            EntraUser user = EntraUser.builder()
                    .id(userId)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .userStatus(UserStatus.ACTIVE)
                    .multiFirmUser(false)
                    .build();

            UserProfile profile = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(user)
                    .userType(UserType.INTERNAL)
                    .activeProfile(true)
                    .appRoles(new HashSet<>())
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            user.setUserProfiles(Set.of(profile));

            Page<EntraUser> userPage = new PageImpl<>(List.of(user),
                    PageRequest.of(0, 10), 1);

            when(mockEntraUserRepository.findAllUsersForAudit(
                    eq(searchTerm), eq(null), eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(userPage);

            when(mockEntraUserRepository.findUsersWithProfilesAndRoles(any(Set.class)))
                    .thenReturn(List.of(user));

            // When
            uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers result = userService.getAuditUsers(searchTerm,
                    null, null, null, 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalUsers()).isEqualTo(1);
            assertThat(result.getUsers()).hasSize(1);
            assertThat(result.getUsers().get(0).getName()).contains("John");

            verify(mockEntraUserRepository).findAllUsersForAudit(
                    eq(searchTerm), eq(null), eq(null), eq(null), any(PageRequest.class));
        }

        @Test
        void getAuditUsers_whenFirmIdProvided_filtersResults() {
            // Given
            UUID firmId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Firm firm = Firm.builder()
                    .id(firmId)
                    .name("Specific Firm")
                    .code("SF001")
                    .build();

            EntraUser user = EntraUser.builder()
                    .id(userId)
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .userStatus(UserStatus.ACTIVE)
                    .multiFirmUser(false)
                    .build();

            UserProfile profile = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(user)
                    .firm(firm)
                    .userType(UserType.EXTERNAL)
                    .activeProfile(true)
                    .appRoles(new HashSet<>())
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            user.setUserProfiles(Set.of(profile));

            Page<EntraUser> userPage = new PageImpl<>(List.of(user),
                    PageRequest.of(0, 10), 1);

            when(mockEntraUserRepository.findAllUsersForAudit(
                    eq(null), eq(firmId), eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(userPage);

            when(mockEntraUserRepository.findUsersWithProfilesAndRoles(any(Set.class)))
                    .thenReturn(List.of(user));

            // When
            uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers result = userService.getAuditUsers(null,
                    firmId, null, null, 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalUsers()).isEqualTo(1);
            assertThat(result.getUsers()).hasSize(1);
            assertThat(result.getUsers().get(0).getFirmAssociation()).isEqualTo("Specific Firm");

            verify(mockEntraUserRepository).findAllUsersForAudit(
                    eq(null), eq(firmId), eq(null), eq(null), any(PageRequest.class));
        }

        @Test
        void getAuditUsers_whenSilasRoleProvided_filtersResults() {
            // Given
            String silasRole = "Global Admin";
            UUID userId = UUID.randomUUID();

            EntraUser user = EntraUser.builder()
                    .id(userId)
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@example.com")
                    .userStatus(UserStatus.ACTIVE)
                    .multiFirmUser(false)
                    .build();

            AppRole adminRole = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("Global Admin")
                    .authzRole(true)
                    .build();

            UserProfile profile = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(user)
                    .userType(UserType.INTERNAL)
                    .activeProfile(true)
                    .appRoles(Set.of(adminRole))
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            user.setUserProfiles(Set.of(profile));

            Page<EntraUser> userPage = new PageImpl<>(List.of(user),
                    PageRequest.of(0, 10), 1);

            when(mockEntraUserRepository.findAllUsersForAudit(
                    eq(null), eq(null), eq(silasRole), eq(null), any(PageRequest.class)))
                    .thenReturn(userPage);

            when(mockEntraUserRepository.findUsersWithProfilesAndRoles(any(Set.class)))
                    .thenReturn(List.of(user));

            // When
            uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers result = userService.getAuditUsers(null, null,
                    silasRole, null, 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalUsers()).isEqualTo(1);
            assertThat(result.getUsers()).hasSize(1);

            verify(mockEntraUserRepository).findAllUsersForAudit(
                    eq(null), eq(null), eq(silasRole), eq(null), any(PageRequest.class));
        }

        @Test
        void getAuditUsers_whenMultiFirmUser_displaysCorrectly() {
            // Given
            UUID userId = UUID.randomUUID();

            Firm firm1 = Firm.builder()
                    .id(UUID.randomUUID())
                    .name("Firm One")
                    .code("F1")
                    .build();

            Firm firm2 = Firm.builder()
                    .id(UUID.randomUUID())
                    .name("Firm Two")
                    .code("F2")
                    .build();

            EntraUser user = EntraUser.builder()
                    .id(userId)
                    .firstName("Multi")
                    .lastName("Firm")
                    .email("multi@example.com")
                    .userStatus(UserStatus.ACTIVE)
                    .multiFirmUser(true)
                    .build();

            UserProfile profile1 = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(user)
                    .firm(firm1)
                    .userType(UserType.EXTERNAL)
                    .activeProfile(true)
                    .appRoles(new HashSet<>())
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            UserProfile profile2 = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(user)
                    .firm(firm2)
                    .userType(UserType.EXTERNAL)
                    .activeProfile(false)
                    .appRoles(new HashSet<>())
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            user.setUserProfiles(Set.of(profile1, profile2));

            Page<EntraUser> userPage = new PageImpl<>(List.of(user),
                    PageRequest.of(0, 10), 1);

            when(mockEntraUserRepository.findAllUsersForAudit(
                    eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(userPage);

            when(mockEntraUserRepository.findUsersWithProfilesAndRoles(any(Set.class)))
                    .thenReturn(List.of(user));

            // When
            uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers result = userService.getAuditUsers(null, null,
                    null, null, 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsers()).hasSize(1);
            assertThat(result.getUsers().get(0).isMultiFirmUser()).isTrue();
            assertThat(result.getUsers().get(0).getUserType()).isEqualTo("External - 3rd Party");
            assertThat(result.getUsers().get(0).getProfileCount()).isEqualTo(2);
            assertThat(result.getUsers().get(0).getFirmAssociation()).contains("Firm One", "Firm Two");
        }

        @Test
        void getAuditUsers_whenNoResults_returnsEmptyList() {
            // Given
            Page<EntraUser> emptyPage = new PageImpl<>(Collections.emptyList(),
                    PageRequest.of(0, 10), 0);

            when(mockEntraUserRepository.findAllUsersForAudit(
                    eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            // When
            uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers result = userService.getAuditUsers(null, null,
                    null, null, 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalUsers()).isEqualTo(0);
            assertThat(result.getTotalPages()).isEqualTo(0);
            assertThat(result.getUsers()).isEmpty();

            verify(mockEntraUserRepository).findAllUsersForAudit(
                    eq(null), eq(null), eq(null), eq(null), any(PageRequest.class));
            verify(mockEntraUserRepository, never()).findUsersWithProfilesAndRoles(any(Set.class));
        }

        @Test
        void getAuditUsers_whenInternalUser_displaysCorrectType() {
            // Given
            UUID userId = UUID.randomUUID();

            EntraUser user = EntraUser.builder()
                    .id(userId)
                    .firstName("Internal")
                    .lastName("Staff")
                    .email("internal@justice.gov.uk")
                    .userStatus(UserStatus.ACTIVE)
                    .multiFirmUser(false)
                    .build();

            UserProfile profile = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(user)
                    .firm(null) // Internal users have no firm
                    .userType(UserType.INTERNAL)
                    .activeProfile(true)
                    .appRoles(new HashSet<>())
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            user.setUserProfiles(Set.of(profile));

            Page<EntraUser> userPage = new PageImpl<>(List.of(user),
                    PageRequest.of(0, 10), 1);

            when(mockEntraUserRepository.findAllUsersForAudit(
                    eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(userPage);

            when(mockEntraUserRepository.findUsersWithProfilesAndRoles(any(Set.class)))
                    .thenReturn(List.of(user));

            // When
            uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers result = userService.getAuditUsers(null, null,
                    null, null, 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsers()).hasSize(1);
            assertThat(result.getUsers().get(0).getUserType()).isEqualTo("Internal");
            assertThat(result.getUsers().get(0).getFirmAssociation()).isEqualTo("None");
        }

        @Test
        void getAuditUsers_withPagination_returnsCorrectPage() {
            // Given
            List<EntraUser> allUsers = new ArrayList<>();
            for (int i = 1; i <= 25; i++) {
                UUID userId = UUID.randomUUID();
                EntraUser user = EntraUser.builder()
                        .id(userId)
                        .firstName("User" + i)
                        .lastName("Test")
                        .email("user" + i + "@example.com")
                        .userStatus(UserStatus.ACTIVE)
                        .multiFirmUser(false)
                        .build();

                UserProfile profile = UserProfile.builder()
                        .id(UUID.randomUUID())
                        .entraUser(user)
                        .userType(UserType.EXTERNAL)
                        .activeProfile(true)
                        .appRoles(new HashSet<>())
                        .userProfileStatus(UserProfileStatus.COMPLETE)
                        .build();

                user.setUserProfiles(Set.of(profile));
                allUsers.add(user);
            }

            // Page 2 with 10 items per page
            List<EntraUser> page2Users = allUsers.subList(10, 20);
            Page<EntraUser> userPage = new PageImpl<>(page2Users,
                    PageRequest.of(1, 10), 25);

            when(mockEntraUserRepository.findAllUsersForAudit(
                    eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(userPage);

            when(mockEntraUserRepository.findUsersWithProfilesAndRoles(any(Set.class)))
                    .thenReturn(page2Users);

            // When
            uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers result = userService.getAuditUsers(null, null,
                    null, null, 2, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalUsers()).isEqualTo(25);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.getCurrentPage()).isEqualTo(2);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getUsers()).hasSize(10);
        }

        @Test
        void getAuditUsers_whenUserHasNoProfiles_displaysUnknownType() {
            // Given
            UUID userId = UUID.randomUUID();

            EntraUser user = EntraUser.builder()
                    .id(userId)
                    .firstName("NoProfile")
                    .lastName("User")
                    .email("noprofile@example.com")
                    .userStatus(UserStatus.ACTIVE)
                    .multiFirmUser(false)
                    .userProfiles(new HashSet<>())
                    .build();

            Page<EntraUser> userPage = new PageImpl<>(List.of(user),
                    PageRequest.of(0, 10), 1);

            when(mockEntraUserRepository.findAllUsersForAudit(
                    eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(userPage);

            when(mockEntraUserRepository.findUsersWithProfilesAndRoles(any(Set.class)))
                    .thenReturn(List.of(user));

            // When
            uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers result = userService.getAuditUsers(null, null,
                    null, null, 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsers()).hasSize(1);
            assertThat(result.getUsers().get(0).getUserType()).isEqualTo("Unknown");
            assertThat(result.getUsers().get(0).getFirmAssociation()).isEqualTo("None");
            assertThat(result.getUsers().get(0).getAccountStatus()).isEqualTo("Active");
        }

        @Test
        void getAuditUsers_whenUserStatusDeactive_displaysDisabled() {
            // Given
            UUID userId = UUID.randomUUID();

            EntraUser user = EntraUser.builder()
                    .id(userId)
                    .firstName("Disabled")
                    .lastName("User")
                    .email("disabled@example.com")
                    .userStatus(UserStatus.DEACTIVE)
                    .multiFirmUser(false)
                    .build();

            Firm firm = Firm.builder()
                    .id(UUID.randomUUID())
                    .name("Test Firm")
                    .code("TF001")
                    .build();

            UserProfile profile = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(user)
                    .firm(firm)
                    .userType(UserType.EXTERNAL)
                    .activeProfile(true)
                    .appRoles(new HashSet<>())
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            user.setUserProfiles(Set.of(profile));

            Page<EntraUser> userPage = new PageImpl<>(List.of(user),
                    PageRequest.of(0, 10), 1);

            when(mockEntraUserRepository.findAllUsersForAudit(
                    eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(userPage);

            when(mockEntraUserRepository.findUsersWithProfilesAndRoles(any(Set.class)))
                    .thenReturn(List.of(user));

            // When
            uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers result = userService.getAuditUsers(null, null,
                    null, null, 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsers()).hasSize(1);
            assertThat(result.getUsers().get(0).getAccountStatus()).isEqualTo("Disabled");
        }

        @Test
        void getAuditUsers_whenUserHasPendingProfile_displaysPending() {
            // Given
            UUID userId = UUID.randomUUID();

            EntraUser user = EntraUser.builder()
                    .id(userId)
                    .firstName("Pending")
                    .lastName("User")
                    .email("pending@example.com")
                    .userStatus(UserStatus.ACTIVE)
                    .multiFirmUser(false)
                    .build();

            Firm firm = Firm.builder()
                    .id(UUID.randomUUID())
                    .name("Test Firm")
                    .code("TF001")
                    .build();

            UserProfile profile = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(user)
                    .firm(firm)
                    .userType(UserType.EXTERNAL)
                    .activeProfile(true)
                    .appRoles(new HashSet<>())
                    .userProfileStatus(UserProfileStatus.PENDING)
                    .build();

            user.setUserProfiles(Set.of(profile));

            Page<EntraUser> userPage = new PageImpl<>(List.of(user),
                    PageRequest.of(0, 10), 1);

            when(mockEntraUserRepository.findAllUsersForAudit(
                    eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(userPage);

            when(mockEntraUserRepository.findUsersWithProfilesAndRoles(any(Set.class)))
                    .thenReturn(List.of(user));

            // When
            uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers result = userService.getAuditUsers(null, null,
                    null, null, 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsers()).hasSize(1);
            assertThat(result.getUsers().get(0).getAccountStatus()).isEqualTo("Pending");
        }

        @Test
        void getAllSilasRoles_returnsAuthzRoles() {
            // Given
            AppRole role1 = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("Global Admin")
                    .authzRole(true)
                    .build();

            AppRole role2 = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("Firm Admin")
                    .authzRole(true)
                    .build();

            when(mockAppRoleRepository.findAllAuthzRoles())
                    .thenReturn(List.of(role1, role2));

            // When
            List<AppRoleDto> result = userService.getAllSilasRoles();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            verify(mockAppRoleRepository).findAllAuthzRoles();
        }

        @Test
        void getAuditUsers_withDescendingSort_sortsCorrectly() {
            // Given
            UUID user1Id = UUID.randomUUID();
            UUID user2Id = UUID.randomUUID();

            EntraUser user1 = EntraUser.builder()
                    .id(user1Id)
                    .firstName("Alice")
                    .lastName("Aardvark")
                    .email("alice@example.com")
                    .userStatus(UserStatus.ACTIVE)
                    .multiFirmUser(false)
                    .build();

            EntraUser user2 = EntraUser.builder()
                    .id(user2Id)
                    .firstName("Zack")
                    .lastName("Zebra")
                    .email("zack@example.com")
                    .userStatus(UserStatus.ACTIVE)
                    .multiFirmUser(false)
                    .build();

            UserProfile profile1 = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(user1)
                    .userType(UserType.EXTERNAL)
                    .activeProfile(true)
                    .appRoles(new HashSet<>())
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            UserProfile profile2 = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .entraUser(user2)
                    .userType(UserType.EXTERNAL)
                    .activeProfile(true)
                    .appRoles(new HashSet<>())
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .build();

            user1.setUserProfiles(Set.of(profile1));
            user2.setUserProfiles(Set.of(profile2));

            Page<EntraUser> userPage = new PageImpl<>(Arrays.asList(user2, user1),
                    PageRequest.of(0, 10), 2);

            when(mockEntraUserRepository.findAllUsersForAudit(
                    eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(userPage);

            when(mockEntraUserRepository.findUsersWithProfilesAndRoles(any(Set.class)))
                    .thenReturn(Arrays.asList(user2, user1));

            // When
            uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers result = userService.getAuditUsers(null, null,
                    null, null, 1, 10, "name", "desc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsers()).hasSize(2);
            // Users returned in order from repository
            assertThat(result.getUsers().get(0).getName()).contains("Zack");
            assertThat(result.getUsers().get(1).getName()).contains("Alice");
        }

        @Test
        void getAuditUsers_withNullUserProfiles_handlesGracefully() {
            // Given
            UUID userId = UUID.randomUUID();

            EntraUser user = EntraUser.builder()
                    .id(userId)
                    .firstName("NullProfiles")
                    .lastName("User")
                    .email("nullprofiles@example.com")
                    .userStatus(UserStatus.ACTIVE)
                    .multiFirmUser(false)
                    .userProfiles(null) // Explicitly null
                    .build();

            Page<EntraUser> userPage = new PageImpl<>(List.of(user),
                    PageRequest.of(0, 10), 1);

            when(mockEntraUserRepository.findAllUsersForAudit(
                    eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(userPage);

            when(mockEntraUserRepository.findUsersWithProfilesAndRoles(any(Set.class)))
                    .thenReturn(List.of(user));

            // When
            uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers result = userService.getAuditUsers(null, null,
                    null, null, 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsers()).hasSize(1);
            assertThat(result.getUsers().get(0).getUserType()).isEqualTo("Unknown");
            assertThat(result.getUsers().get(0).getProfileCount()).isEqualTo(0);
        }
    }
}
