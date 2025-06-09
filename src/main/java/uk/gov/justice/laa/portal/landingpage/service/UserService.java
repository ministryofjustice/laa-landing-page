package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.core.content.BatchRequestContent;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.ServicePrincipalCollectionResponse;
import com.microsoft.graph.models.SignInActivity;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.RequestInformation;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.model.UserRole;
import uk.gov.justice.laa.portal.landingpage.repository.UserModelRepository;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.List;
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
    private final UserModelRepository userModelRepository;
    private final NotificationService notificationService;

    private static final int BATCH_SIZE = 20;

    @Value("${spring.security.oauth2.client.registration.azure.redirect-uri}")
    private String redirectUri;

    /** The number of pages to load in advance when doing user pagination */
    private static final int PAGES_TO_PRELOAD = 5;

    public UserService(@Qualifier("graphServiceClient") GraphServiceClient graphClient,
                       UserModelRepository userModelRepository, NotificationService notificationService) {
        this.graphClient = graphClient;
        this.userModelRepository = userModelRepository;
        this.notificationService = notificationService;
    }

    Logger logger = LoggerFactory.getLogger(this.getClass());

    public List<UserRole> getUserAppRolesByUserId(String userId) {
        List<AppRoleAssignment> userAppRoles = getUserAppRoleAssignmentByUserId(userId);
        List<UserRole> userRoles = new ArrayList<>();
        UserRole userRole = new UserRole();

        for (AppRoleAssignment appRole : userAppRoles) {
            ServicePrincipal servicePrincipal = graphClient
                    .servicePrincipals()
                    .byServicePrincipalId(Objects.requireNonNull(appRole.getResourceId()).toString())
                    .get();

            if (servicePrincipal != null) {
                userRole.setAppId(String.valueOf(appRole.getResourceId()));
                userRole.setAppRoleId(String.valueOf(appRole.getAppRoleId()));
                userRole.setAssignmentId(appRole.getId());
                userRole.setAppName(servicePrincipal.getDisplayName());

                String roleName = Objects.requireNonNull(servicePrincipal.getAppRoles()).stream()
                        .filter(role -> Objects.equals(role.getId(), appRole.getAppRoleId()))
                        .map(AppRole::getDisplayName)
                        .findFirst()
                        .orElse("UNKNOWN");

                userRole.setRoleName(roleName);
            } else {
                userRole.setAppName("UNKNOWN");
                userRole.setRoleName("UNKNOWN");
            }

            userRoles.add(userRole);
        }

        return userRoles;
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

    public void updateUserRoles(String userId, List<String> selectedRoles) {
        List<UserRole> existingRoles = getUserAppRolesByUserId(userId);

        Set<String> currentRoleIds = existingRoles.stream()
                .map(UserRole::getAppRoleId)
                .collect(Collectors.toSet());

        Set<String> selectedRoleIds = new HashSet<>(selectedRoles != null ? selectedRoles : List.of());

        List<UserRole> availableRoles = getAllAvailableRoles();

        for (String roleId : selectedRoleIds) {
            if (!currentRoleIds.contains(roleId)) {
                String appId = findAppIdForRole(roleId, availableRoles);
                assignAppRoleToUser(userId, appId, roleId);
            }
        }

        for (UserRole role : existingRoles) {
            if (!selectedRoleIds.contains(role.getAppRoleId())) {
                removeAppRoleFromUser(userId, role.getAssignmentId());
            }
        }
    }

    private String findAppIdForRole(String roleId, List<UserRole> existingRoles) throws IllegalArgumentException {
        Optional<UserRole> userRole = existingRoles.stream()
                .filter(role -> role.getAppRoleId().equals(roleId))
                .findFirst();

        if (userRole.isPresent()) {
            return userRole.get().getAppId();
        } else {
            throw new IllegalArgumentException("App ID not found for role ID: " + roleId);
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
            logger.error("Error fetching roles: {}", e.getMessage());
            return List.of();
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

    public List<UserRole> getAllAvailableRoles() {
        List<ServicePrincipal> servicePrincipals = getServicePrincipals();

        List<UserRole> roles = new ArrayList<>();
        if (!ObjectUtils.isEmpty(servicePrincipals)) {
            for (ServicePrincipal sp : servicePrincipals) {
                for (AppRole role : Objects.requireNonNull(sp.getAppRoles())) {
                    UserRole roleInfo = new UserRole(
                            sp.getId(),
                            sp.getDisplayName(),
                            Objects.requireNonNull(role.getId()).toString(),
                            role.getDisplayName(),
                            null,
                            role.getDisplayName(),
                            null,
                            false
                    );
                    roles.add(roleInfo);
                }
            }
        }
        return roles;
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

    public List<UserModel> getSavedUsers() {
        return userModelRepository.findAll();
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

    public List<ServicePrincipal> getServicePrincipals() {
        ServicePrincipalCollectionResponse servicePrincipalsResponse = graphClient.servicePrincipals().get();
        List<ServicePrincipal> servicePrincipals;
        if (servicePrincipalsResponse != null  && (servicePrincipals = servicePrincipalsResponse.getValue()) != null) {
            return servicePrincipals;
        } else {
            logger.warn("No service principals found or response was null");
            return new ArrayList<>();
        }
    }

    public List<UserRole> getAllAvailableRolesForApps(List<String> selectedApps) {
        List<ServicePrincipal> servicePrincipals = getServicePrincipals();

        List<UserRole> roles = new ArrayList<>();
        if (!ObjectUtils.isEmpty(servicePrincipals)) {
            for (ServicePrincipal sp : servicePrincipals) {
                if (selectedApps.contains(sp.getAppId())) {
                    for (AppRole role : Objects.requireNonNull(sp.getAppRoles())) {
                        UserRole roleInfo = new UserRole(
                                sp.getId(),
                                sp.getDisplayName(),
                                Objects.requireNonNull(role.getId()).toString(),
                                role.getDisplayName(),
                                null,
                                role.getDisplayName(),
                                null,
                                false
                        );
                        roles.add(roleInfo);
                    }
                }
            }
        }
        return roles;
    }

    public User createUser(User user, List<String> roles) {

        User invitedUser = inviteUser(user);

        assert invitedUser != null;
        assignAppRoleToUser(invitedUser, roles);

        persistNewUser(user);

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

    protected void assignAppRoleToUser(User user, List<String> roles) {
        ServicePrincipalCollectionResponse principalCollection = graphClient.servicePrincipals().get();
        String resourceId;
        UUID roleId;
        for (ServicePrincipal servicePrincipal : principalCollection.getValue()) {
            for (AppRole appRole : servicePrincipal.getAppRoles()) {
                if (roles.contains(appRole.getId().toString())) {
                    resourceId = servicePrincipal.getId();
                    roleId = appRole.getId();
                    assignAppRoleToUser(user.getId(), resourceId, roleId.toString());
                }
            }
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

            logger.info("App role assigned successfully to user {}.", userId);
        } catch (Exception e) {
            logger.error("Error assigning app role to user: {}, message: {}", userId, e.getMessage());
        }
    }

    private void persistNewUser(User newUser) {
        UserModel userModel = new UserModel();
        userModel.setEmail(newUser.getDisplayName());
        userModel.setFullName(newUser.getDisplayName());
        userModel.setId(newUser.getId());
        userModelRepository.save(userModel);
    }

    public UserModel getUserModel(String entraId) {
        return userModelRepository.getUserModelById(entraId);
    }
}
