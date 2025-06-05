package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.core.content.BatchRequestContent;
import com.microsoft.graph.core.content.BatchResponseContent;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.ObjectIdentity;
import com.microsoft.graph.models.PasswordProfile;
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
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.List;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * userService
 */
@Service
public class UserService {

    private final GraphServiceClient graphClient;
    private final EntraUserRepository entraUserRepository;
    private static final int BATCH_SIZE = 20;
    protected static final String APPLICATION_ID = "0ca5b38b-6c4f-404e-b1d0-d0e8d4e0bfd5";
    private final AppRepository appRepository;
    private final AppRoleRepository appRoleRepository;
    private final ModelMapper mapper;

    @Value("${entra.defaultDomain}")
    private String defaultDomain;

    public UserService(@Qualifier("graphServiceClient") GraphServiceClient graphClient, EntraUserRepository entraUserRepository,
                       AppRepository appRepository, AppRoleRepository appRoleRepository, ModelMapper mapper) {
        this.graphClient = graphClient;
        this.entraUserRepository = entraUserRepository;
        this.appRepository = appRepository;
        this.appRoleRepository = appRoleRepository;
        this.mapper = mapper;
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

    @SuppressWarnings("unchecked")
    public Stack<String> getPageHistory(HttpSession session) {
        Stack<String> pageHistory = (Stack<String>) session.getAttribute("pageHistory");
        if (pageHistory == null) {
            pageHistory = new Stack<>();
            session.setAttribute("pageHistory", pageHistory);
        }
        return pageHistory;
    }

    // Retrieves paginated users and manages the page history for next and previous
    // navigation
    // Not working as expected for previous page stream
    public PaginatedUsers getPaginatedUsersWithHistory(Stack<String> pageHistory, int size, String nextPageLink) {
        String previousPageLink = null;

        if (nextPageLink != null) {
            if (!pageHistory.isEmpty()) {
                previousPageLink = pageHistory.pop();
            }
            pageHistory.push(nextPageLink);
        }

        PaginatedUsers paginatedUsers = getAllUsersPaginated(size, nextPageLink, previousPageLink);
        paginatedUsers.setPreviousPageLink(previousPageLink);
        int totalPages = (int) Math.ceil((double) paginatedUsers.getTotalUsers() / size);
        if (totalPages > 0) {
            paginatedUsers.setTotalPages(totalPages);
        } else {
            paginatedUsers.setTotalPages(1);
        }
        return paginatedUsers;
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

    private PaginatedUsers getAllUsersPaginated(int pageSize, String nextPageLink, String previousPageLink) {
        UserCollectionResponse response;

        if (nextPageLink == null || nextPageLink.isEmpty()) {
            response = graphClient.users()
                    .get(requestConfig -> {
                        assert requestConfig.queryParameters != null;
                        requestConfig.queryParameters.top = pageSize;
                        requestConfig.queryParameters.select = new String[]{"displayName", "userPrincipalName",
                            "signInActivity"};
                        requestConfig.queryParameters.count = true;
                    });
        } else {
            response = graphClient.users()
                    .withUrl(previousPageLink)
                    .get();
        }

        List<User> graphUsers = response != null ? response.getValue() : Collections.emptyList();
        List<UserModel> users = List.of();

        if (graphUsers != null && !graphUsers.isEmpty()) {
            users = graphUsers.stream().map(graphUser -> {
                UserModel user = new UserModel();
                user.setId(graphUser.getId());
                user.setEmail(graphUser.getUserPrincipalName());
                user.setFullName(graphUser.getDisplayName());

                if (graphUser.getSignInActivity() != null) {
                    user.setLastLoggedIn(
                            formatLastSignInDateTime(graphUser.getSignInActivity().getLastSignInDateTime()));
                } else {
                    user.setLastLoggedIn("NA");
                }

                return user;
            }).collect(Collectors.toList());
        }

        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setUsers(users);
        paginatedUsers.setNextPageLink(
                response != null && response.getOdataNextLink() != null ? response.getOdataNextLink() : null);

        int totalUsers = Optional.ofNullable(graphClient.users()
                .count()
                .get(requestConfig -> requestConfig.headers.add("ConsistencyLevel", "eventual")))
                .orElse(0);

        paginatedUsers.setTotalUsers(totalUsers);

        return paginatedUsers;
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
            BatchResponseContent responseContent = graphClient.getBatchRequestBuilder().post(batchRequestContent, null);
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
        SignInActivity signInActivity = user.getSignInActivity();
        OffsetDateTime lastSignInDateTime = signInActivity != null ? signInActivity.getLastSignInDateTime() : null;
        if (lastSignInDateTime != null) {
            return formatLastSignInDateTime(lastSignInDateTime);
        }
        return user.getDisplayName() + " has not logged in yet.";
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

    public User createUser(User user, String password, List<String> roles) {

        user.setAccountEnabled(true);
        ObjectIdentity objectIdentity = new ObjectIdentity();
        objectIdentity.setSignInType("emailAddress");
        objectIdentity.setIssuer(defaultDomain);
        objectIdentity.setIssuerAssignedId(user.getMail());
        LinkedList<ObjectIdentity> identities = new LinkedList<ObjectIdentity>();
        identities.add(objectIdentity);
        user.setIdentities(identities);
        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.setForceChangePasswordNextSignInWithMfa(true);
        passwordProfile.setPassword(password);
        user.setPasswordProfile(passwordProfile);
        user = graphClient.users().post(user);

        persistNewUser(user, roles);
        return user;
    }

    private void persistNewUser(User newUser, List<String> roles) {
        EntraUser entraUser = mapper.map(newUser, EntraUser.class);
        List<AppRole> appRoles = appRoleRepository.findAllById(roles.stream().map(UUID::fromString)
                .collect(Collectors.toList()));

        UserProfile userProfile = UserProfile.builder()
                .defaultProfile(true)
                .appRoles(new HashSet<>(appRoles))
                // TODO: Set this dynamically once we have usertype selection on the front end
                .userType(UserType.EXTERNAL_SINGLE_FIRM)
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
}
