package uk.gov.justice.laa.portal.landingpage.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.microsoft.graph.core.content.BatchRequestContent;
import com.microsoft.graph.core.content.BatchResponseContent;
import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.RequestInformation;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.repository.UserModelRepository;

/**
 * userService
 */
@Service
public class UserService {

    private final GraphServiceClient graphClient;
    private final UserModelRepository userModelRepository;
    private final CreateUserNotificationService createUserNotificationService;
    private static final int BATCH_SIZE = 20;

    public UserService(@Qualifier("graphServiceClient") GraphServiceClient graphClient, UserModelRepository userModelRepository, CreateUserNotificationService createUserNotificationService) {
        this.graphClient = graphClient;
        this.userModelRepository = userModelRepository;
        this.createUserNotificationService = createUserNotificationService;
    }

    Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * create User at Entra
     *
     * @return {@code User}
     */
    public User createUser(String username, String password) {
        User newUser = buildNewUser(username, password);
        User savedUser = graphClient.users().post(newUser);
        // Add new user to database.
        persistNewUser(newUser, savedUser);
        createUserNotificationService.notifyCreateUser(savedUser.getDisplayName(), savedUser.getMail(), password, savedUser.getId());
        return savedUser;
    }

    public List<Map<String, Object>> getUserAppRolesByUserId(String userId) {
        List<AppRoleAssignment> userAppRoles = getUserAppRoleAssignmentByUserId(userId);
        List<Map<String, Object>> roleDetails = new ArrayList<>();

        for (AppRoleAssignment appRole : userAppRoles) {
            ServicePrincipal servicePrincipal = graphClient
                    .servicePrincipals()
                    .byServicePrincipalId(String.valueOf(appRole.getResourceId()))
                    .get();

            Map<String, Object> roleInfo = new HashMap<>();
            roleInfo.put("appId", appRole.getResourceId());

            if (servicePrincipal == null) {
                roleInfo.put("appName", "UNKNOWN");
                roleInfo.put("roleName", "UNKNOWN");
            } else {
                roleInfo.put("appName", servicePrincipal.getDisplayName());

                // Find the actual role name based on appRoleId
                String roleName = servicePrincipal.getAppRoles().stream()
                        .filter(role -> role.getId().equals(appRole.getAppRoleId()))
                        .map(AppRole::getDisplayName)
                        .findFirst()
                        .orElse("UNKNOWN");

                roleInfo.put("roleName", roleName);
            }
            roleDetails.add(roleInfo);
        }
        return roleDetails;
    }

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

    public List<DirectoryRole> getDirectoryRolesByUserId(String userId) {
        return Objects.requireNonNull(graphClient.users().byUserId(userId).memberOf().get())
                .getValue()
                .stream()
                .filter(obj -> obj instanceof DirectoryRole)
                .map(obj -> (DirectoryRole) obj)
                .collect(Collectors.toList());
    }

    /**
     * Get App Role Assignments to User by User ID
     *
     * @param userId {@code String}
     * @return {@code List<AppRoleAssignment}
     */
    public List<AppRoleAssignment> getUserAppRoleAssignmentByUserId(String userId) {
        try {
            return graphClient
                    .users()
                    .byUserId(userId)
                    .appRoleAssignments()
                    .get()
                    .getValue();
        } catch (ApiException e) {
            System.err.println("Error fetching roles: " + e.getMessage());
            return List.of();
        }
    }

    public void assignAppRoleToUser(String userId, String appId, String appRoleId) {
        AppRoleAssignment appRoleAssignment = new AppRoleAssignment();
        appRoleAssignment.setPrincipalId(UUID.fromString(userId));
        appRoleAssignment.setResourceId(UUID.fromString(appId));
        appRoleAssignment.setAppRoleId(UUID.fromString(appRoleId));

        try {
            graphClient
                    .users()
                    .byUserId(userId)
                    .appRoleAssignments()
                    .post(appRoleAssignment);

            System.out.println("App role successfully assigned to user.");
        } catch (Exception e) {
            System.err.println("Failed to assign app role: " + e.getMessage());
        }
    }

    public void removeAppRoleFromUser(String userId, String appRoleAssignmentId) {
        try {
            graphClient
                    .users()
                    .byUserId(userId)
                    .appRoleAssignments()
                    .byAppRoleAssignmentId(appRoleAssignmentId)
                    .delete();

            System.out.println("App role successfully removed from user.");
        } catch (Exception e) {
            System.err.println("Failed to remove app role: " + e.getMessage());
        }
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

    public List<UserModel> getSavedUsers() {
        return userModelRepository.findAll();
    }

    private static User buildNewUser(String username, String password) {
        User user = new User();
        user.setAccountEnabled(true);
        user.setDisplayName(username);
        user.setMailNickname("someone");
        user.setUserPrincipalName(username + "@mojodevlexternal.onmicrosoft.com");
        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.setForceChangePasswordNextSignIn(true);
        passwordProfile.setPassword(password);
        user.setPasswordProfile(passwordProfile);
        return user;
    }

    private void persistNewUser(User newUser, User savedUser) {
        UserModel userModel = new UserModel();
        userModel.setEmail(newUser.getDisplayName());
        userModel.setPassword("NotSave");
        userModel.setFullName(newUser.getDisplayName());
        userModel.setId(savedUser.getId());
        userModelRepository.save(userModel);
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

    /**
     * Get the last logged in date for a user by their ID.
     *
     * @param userId The ID of the user.
     * @return The last logged in date as a formatted string, or "NA" if not
     * available.
     */
    public String getLastLoggedInByUserId(String userId) {
        User user = getUserById(userId);
        if (user != null && user.getSignInActivity() != null && user.getSignInActivity().getLastSignInDateTime() != null) {
            return formatLastSignInDateTime(user.getSignInActivity().getLastSignInDateTime());
        }
        return "NA";
    }
}
