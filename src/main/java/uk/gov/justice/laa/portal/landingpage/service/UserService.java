package uk.gov.justice.laa.portal.landingpage.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

/**
 * userService
 */
@Service
public class UserService {

    private static final String AZURE_CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
    private static final String AZURE_TENANT_ID = System.getenv("AZURE_TENANT_ID");
    private static final String AZURE_CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
    private static GraphServiceClient graphClient;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * create User at Entra
     *
     * @return {@code User}
     */
    public static Invitation inviteUser(String email) {

        Invitation invitation = new Invitation();
        invitation.setInvitedUserEmailAddress(email);
        invitation.setInviteRedirectUrl("http://localhost:8080");
        invitation.setSendInvitationMessage(true);
        GraphServiceClient graphClient = getGraphClient();
        return graphClient.invitations().post(invitation);
    }

    /**
     * create User at Entra
     *
     * @return {@code User}
     */
    public static User createUser(String username, String password) {

        User user = new User();
        user.setAccountEnabled(true);
        user.setDisplayName(username);
        user.setMailNickname("someone");
        user.setUserPrincipalName(username + "@mojodevlexternal.onmicrosoft.com");
        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.setForceChangePasswordNextSignIn(true);
        passwordProfile.setPassword(password);
        user.setPasswordProfile(passwordProfile);
        GraphServiceClient graphClient = getGraphClient();
        return graphClient.users().post(user);
    }

    /**
     * Get Authenticated Graph Client for API usage
     *
     * @return Usable and authenticated Graph Client
     */
    private static GraphServiceClient getGraphClient() {
        if (graphClient == null) {

            // Create secret
            final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                    .clientId(AZURE_CLIENT_ID).tenantId(AZURE_TENANT_ID).clientSecret(AZURE_CLIENT_SECRET).build();

            final String[] scopes = new String[]{"https://graph.microsoft.com/.default"};

            graphClient = new GraphServiceClient(credential, scopes);
        }

        return graphClient;
    }

    public List<Map<String, Object>> getUserAppRolesByUserId(String userId) {
        List<AppRoleAssignment> userAppRoles = getUserAppRoleAssignmentByUserId(userId);
        List<Map<String, Object>> roleDetails = new ArrayList<>();

        for (AppRoleAssignment appRole : userAppRoles) {
            ServicePrincipal servicePrincipal = getGraphClient()
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
        UserCollectionResponse response = getGraphClient().users().get();
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

    // Retrieves paginated users and manages the page history for next and previous navigation
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

        return paginatedUsers;
    }


    public List<DirectoryRole> getDirectoryRolesByUserId(String userId) {
        return Objects.requireNonNull(getGraphClient().users().byUserId(userId).memberOf().get())
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
            return getGraphClient()
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
            getGraphClient()
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
            getGraphClient()
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
        GraphServiceClient graphClient = getGraphClient();

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

    private PaginatedUsers getAllUsersPaginated(int pageSize, String nextPageLink, String previousPageLink) {
        GraphServiceClient graphClient = getGraphClient();
        UserCollectionResponse response;

        if (nextPageLink == null || nextPageLink.isEmpty()) {
            response = graphClient.users()
                    .get(requestConfig -> {
                        assert requestConfig.queryParameters != null;
                        requestConfig.queryParameters.top = pageSize;
                        requestConfig.queryParameters.select = new String[]{"displayName", "userPrincipalName", "signInActivity"};
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
                    user.setLastLoggedIn(formatLastSignInDateTime(graphUser.getSignInActivity().getLastSignInDateTime()));
                } else {
                    user.setLastLoggedIn("NA");
                }

                return user;
            }).collect(Collectors.toList());
        }

        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setUsers(users);
        paginatedUsers.setNextPageLink(response != null && response.getOdataNextLink() != null ? response.getOdataNextLink() : null);

        int totalUsers = Optional.ofNullable(graphClient.users()
                        .count()
                        .get(requestConfig -> requestConfig.headers.add("ConsistencyLevel", "eventual")))
                .orElse(0);

        paginatedUsers.setTotalUsers(totalUsers);

        return paginatedUsers;
    }
}
