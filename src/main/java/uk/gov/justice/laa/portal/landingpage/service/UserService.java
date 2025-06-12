package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.core.content.BatchRequestContent;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.SignInActivity;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.RequestInformation;
import jakarta.servlet.http.HttpSession;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRegistration;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * userService
 */
@Service
public class UserService {

    private static final int BATCH_SIZE = 20;
    private final OfficeRepository officeRepository;

    @Value("${spring.security.oauth2.client.registration.azure.redirect-uri}")
    private String redirectUri;

    private final GraphServiceClient graphClient;
    private final EntraUserRepository entraUserRepository;
    private final AppRepository appRepository;
    private final AppRoleRepository appRoleRepository;
    private final ModelMapper mapper;
    private final NotificationService notificationService;



    /** The number of pages to load in advance when doing user pagination */
    private static final int PAGES_TO_PRELOAD = 5;

    public UserService(@Qualifier("graphServiceClient") GraphServiceClient graphClient, EntraUserRepository entraUserRepository,
                       AppRepository appRepository, AppRoleRepository appRoleRepository, ModelMapper mapper, NotificationService notificationService, OfficeRepository officeRepository) {
        this.graphClient = graphClient;
        this.entraUserRepository = entraUserRepository;
        this.appRepository = appRepository;
        this.appRoleRepository = appRoleRepository;
        this.mapper = mapper;
        this.notificationService = notificationService;
        this.officeRepository = officeRepository;
    }

    Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Returns all Users from Entra
     * <p>
     * Limitations - only returns 100 users currently
     * </p>
     *
     * @return {@code List<User>}
     */
    public List<User> getAllUsers() {
        UserCollectionResponse response = graphClient.users().get();
        return response != null ? response.getValue() : Collections.emptyList();
    }

    public void updateUserRoles(String userId, List<String> selectedRoles) {
        List<AppRole> roles = appRoleRepository.findAllById(selectedRoles.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList()));
        Optional<EntraUser> optionalUser = entraUserRepository.findById(UUID.fromString(userId));
        if (optionalUser.isPresent()) {
            EntraUser user = optionalUser.get();
            updateUserProfileRoles(user, roles);
        } else {
            logger.warn("User with id {} not found. Could not update roles.", userId);
        }
    }

    private void updateUserProfileRoles(EntraUser user, List<AppRole> roles) {
        Optional<UserProfile> userProfile = user.getUserProfiles().stream()
                // Set to default profile for now, will need to receive a user profile from front end at some point.
                .filter(UserProfile::isDefaultProfile)
                .findFirst();
        if (userProfile.isPresent()) {
            userProfile.get().setAppRoles(new HashSet<>(roles));
            entraUserRepository.saveAndFlush(user);
        } else {
            logger.warn("User profile for user ID {} not found. Could not update roles.", user.getId());
        }
    }

    public List<DirectoryRole> getDirectoryRolesByUserId(String userId) {
        return Objects.requireNonNull(graphClient.users().byUserId(userId).memberOf().get())
                .getValue()
                .stream()
                .filter(obj -> obj instanceof DirectoryRole)
                .map(obj -> (DirectoryRole) obj)
                .collect(Collectors.toList());
    }

    public User getUserById(String userId) {
        try {
            return graphClient.users().byUserId(userId).get();
        } catch (Exception e) {
            logger.error("Error fetching user with ID: {}", userId, e);
            return null;
        }
    }

    public Optional<EntraUserDto> getEntraUserById(String userId) {
        return entraUserRepository.findById(UUID.fromString(userId))
                .map(user -> mapper.map(user, EntraUserDto.class));
    }

    public String formatLastSignInDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.ENGLISH);

        return dateTime.format(formatter);
    }

    public List<LaaApplication> getManagedAppRegistrations() {
        try {
            var response = graphClient.applications().get();
            return (response != null && response.getValue() != null)
                    ? LaaAppDetailsStore.getUserAssignedApps(response.getValue())
                    : Collections.emptyList();
        } catch (Exception ex) {
            logger.error("Error fetching managed app registrations: ", ex);
            return Collections.emptyList();
        }
    }

    public PaginatedUsers getPaginatedUsers(int page, int size, HttpSession session) {
        List<User> users = getPageOfUsers(page, size, session);
        PaginatedUsers paginatedUsers = new PaginatedUsers();
        Integer totalUsers = getTotalNumberOfUsers(session);
        paginatedUsers.setTotalUsers(totalUsers == null ? 0 : totalUsers);
        paginatedUsers.setUsers(mapGraphUsersToPagedUserModel(users));
        return paginatedUsers;
    }

    private List<UserModel> mapGraphUsersToPagedUserModel(List<User> users) {
        return users.stream().map(graphUser -> {
            UserModel user = new UserModel();
            user.setId(graphUser.getId());
            user.setEmail(graphUser.getUserPrincipalName());
            user.setFullName(graphUser.getDisplayName());
            if (graphUser.getSignInActivity() != null) {
                user.setLastLoggedIn(formatLastSignInDateTime(graphUser.getSignInActivity().getLastSignInDateTime()));
            } else {
                user.setLastLoggedIn("NA");
            }
            return user;
        }).collect(Collectors.toList());
    }

    public List<User> getPageOfUsers(int page, int pageSize, HttpSession session) {
        List<User> cachedUsers = (List<User>) session.getAttribute("cachedUsers");
        Integer totalUsers = getTotalNumberOfUsers(session);
        // Calculate the bounds of the page we wish to display e.g. 0-9, 10-19
        int startPageIndex = (page - 1) * pageSize;
        int endPageIndex = Math.min(totalUsers, startPageIndex + (pageSize - 1));
        if (cachedUsers.size() - 1 < endPageIndex) {
            UserCollectionResponse response = (UserCollectionResponse) session.getAttribute("lastResponse");
            // Calculate how many pages we've already loaded based on size of cached users.
            int currentPage = (cachedUsers.size() / pageSize) + 1;
            // Eager-load some pages in advance.
            while (currentPage < page + PAGES_TO_PRELOAD) {
                if (response == null) {
                    // First run
                    response = getFirstPageOfUsersResponse(pageSize);
                } else if (response.getOdataNextLink() != null) {
                    // Fetch next page
                    response = graphClient.users().withUrl(response.getOdataNextLink()).get();
                } else {
                    // No more users, break.
                    break;
                }
                if (response == null || response.getValue() == null) {
                    // If response is still null after network call, then something has gone wrong, break.
                    break;
                }

                session.setAttribute("lastResponse", response);
                cachedUsers.addAll(response.getValue());
                currentPage++;
            }
        }
        if (startPageIndex < cachedUsers.size()) {
            return cachedUsers.subList(startPageIndex, Math.min(endPageIndex + 1, cachedUsers.size()));
        } else {
            return Collections.emptyList();
        }

    }

    private Integer getTotalNumberOfUsers(HttpSession session) {
        Integer totalUsers;
        if (session.getAttribute("totalUsers") == null) {
            totalUsers = getTotalNumberOfUsers();
            session.setAttribute("totalUsers", totalUsers);
        } else {
            totalUsers = (Integer) session.getAttribute("totalUsers");
        }
        return totalUsers;
    }

    private Integer getTotalNumberOfUsers() {
        return graphClient.users().count().get(requestConfig -> requestConfig.headers.add("ConsistencyLevel", "eventual"));
    }

    public UserCollectionResponse getFirstPageOfUsersResponse(int pageSize) {
        return graphClient.users()
                .get(requestConfig -> {
                    assert requestConfig.queryParameters != null;
                    requestConfig.queryParameters.top = pageSize;
                    requestConfig.queryParameters.select = new String[]{"displayName", "userPrincipalName", "signInActivity"};
                    requestConfig.queryParameters.count = true;
                });
    }

    public List<EntraUserDto> getSavedUsers() {
        return entraUserRepository.findAll().stream()
                .map(user -> mapper.map(user, EntraUserDto.class))
                .collect(Collectors.toList());
    }

    @Async
    public void disableUsers(List<String> ids) throws IOException {
        Collection<List<String>> batchIds = partitionBasedOnSize(ids, BATCH_SIZE);
        for (List<String> batch : batchIds) {
            BatchRequestContent batchRequestContent = new BatchRequestContent(graphClient);
            for (String id : batch) {
                User user = new User();
                user.setAccountEnabled(false);
                RequestInformation patchMessage = graphClient.users().byUserId(id).toPatchRequestInformation(user);
                batchRequestContent.addBatchRequestStep(patchMessage);
            }
            graphClient.getBatchRequestBuilder().post(batchRequestContent, null);
        }
    }

    static <T> List<List<T>> partitionBasedOnSize(List<T> inputList, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < inputList.size(); i += size) {
            partitions.add(inputList.subList(i, Math.min(i + size, inputList.size())));
        }
        return partitions;
    }

    public String getLastLoggedInByUserId(String userId) {
        User user = graphClient.users().byUserId(userId).get(requestConfiguration -> {
            requestConfiguration.queryParameters.select = new String[]{"signInActivity"};
        });
        if (user != null) {
            SignInActivity signInActivity = user.getSignInActivity();
            OffsetDateTime lastSignInDateTime = signInActivity != null ? signInActivity.getLastSignInDateTime() : null;
            if (lastSignInDateTime != null) {
                return formatLastSignInDateTime(lastSignInDateTime);
            }
        }
        return "User has not logged in yet.";
    }

    public List<AppDto> getApps() {
        return appRepository.findAll().stream()
                .map(app -> mapper.map(app, AppDto.class))
                .collect(Collectors.toList());
    }

    public List<AppRoleDto> getAllAvailableRolesForApps(List<String> selectedApps) {
        // Fetch selected apps
        List<App> apps = appRepository.findAllById(selectedApps.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList()));
        // Return roles
        return apps.stream()
                .flatMap(app -> app.getAppRoles().stream())
                .map(appRole -> mapper.map(appRole, AppRoleDto.class))
                .collect(Collectors.toList());
    }

    public User createUser(User user, List<String> roles, List<String> selectedOffices) {

        User invitedUser = inviteUser(user);
        assert invitedUser != null;

        persistNewUser(user, roles, selectedOffices);

        return invitedUser;
    }

    private User inviteUser(User user) {
        Invitation invitation = new Invitation();
        invitation.setInvitedUserEmailAddress(user.getMail());
        invitation.setInviteRedirectUrl(redirectUri);
        invitation.setInvitedUserType("Guest");
        invitation.setSendInvitationMessage(false);
        invitation.setInvitedUserDisplayName(user.getGivenName() + " " + user.getSurname());
        Invitation result = graphClient.invitations().post(invitation);

        //Send invitation email
        assert result != null;
        notificationService.notifyCreateUser(invitation.getInvitedUserDisplayName(), user.getMail(),
                result.getInviteRedeemUrl());

        return result.getInvitedUser();
    }

    private void persistNewUser(User newUser, List<String> roles, List<String> selectedOffices) {
        EntraUser entraUser = mapper.map(newUser, EntraUser.class);
        List<AppRole> appRoles = appRoleRepository.findAllById(roles.stream().map(UUID::fromString)
                .collect(Collectors.toList()));
        List<UUID> officeIds = selectedOffices.stream().map(UUID::fromString).toList();
        Set<Office> offices = new HashSet<Office>(officeRepository.findOfficeByFirm_IdIn(officeIds));
        UserProfile userProfile = UserProfile.builder()
                .defaultProfile(true)
                .appRoles(new HashSet<>(appRoles))
                // TODO: Set this dynamically once we have usertype selection on the front end
                .userType(UserType.INTERNAL)
                .createdDate(LocalDateTime.now())
                .offices(offices)
                .createdBy("Admin")
                .entraUser(entraUser)
                .build();

        entraUser.setUserProfiles(Set.of(userProfile));
        entraUser.setUserStatus(UserStatus.ACTIVE);
        entraUser.setCreatedBy("Admin");
        entraUser.setCreatedDate(LocalDateTime.now());
        Set<AppRegistration> appRegistrations = appRoles.stream()
                .map(appRole -> appRole.getApp().getAppRegistration())
                .collect(Collectors.toSet());
        entraUser.setUserAppRegistrations(appRegistrations);
        entraUserRepository.saveAndFlush(entraUser);
    }

    public List<AppRoleDto> getUserAppRolesByUserId(String userId) {
        Optional<EntraUser> optionalUser = entraUserRepository.findById(UUID.fromString(userId));
        if (optionalUser.isPresent()) {
            EntraUser user = optionalUser.get();
            return user.getUserProfiles().stream()
                    .flatMap(userProfile -> userProfile.getAppRoles().stream())
                    .map(appRole -> mapper.map(appRole, AppRoleDto.class))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public List<AppRoleDto> getAllAvailableRoles() {
        return appRoleRepository.findAll().stream()
                .map(appRole -> mapper.map(appRole, AppRoleDto.class))
                .collect(Collectors.toList());
    }

    public Set<AppDto> getUserAppsByUserId(String userId) {
        Optional<EntraUser> optionalUser = entraUserRepository.findById(UUID.fromString(userId));
        if (optionalUser.isPresent()) {
            EntraUser user = optionalUser.get();
            return user.getUserProfiles().stream()
                    .flatMap(userProfile -> userProfile.getAppRoles().stream())
                    .map(AppRole::getApp)
                    .map(app -> mapper.map(app, AppDto.class))
                    .collect(Collectors.toSet());
        } else {
            logger.warn("No user found for user id {} when getting user apps", userId);
            return Collections.emptySet();
        }
    }

    public List<AppRoleDto> getAppRolesByAppIds(List<String> appIds) {
        List<UUID> appUuids = appIds.stream().map(UUID::fromString).toList();
        return appRepository.findAllById(appUuids).stream()
                .flatMap(app -> app.getAppRoles().stream())
                .map(appRole -> mapper.map(appRole, AppRoleDto.class))
                .toList();
    }
}
