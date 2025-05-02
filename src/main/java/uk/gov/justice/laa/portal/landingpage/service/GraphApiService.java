package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.serviceprincipals.getbyids.GetByIdsPostRequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.callGraphApi;

/**
 * service class for graph api.
 */
@Service
public class GraphApiService {

    public static final UUID DEFAULT_ENTRA_APP_ROLE = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final String GRAPH_URL = "https://graph.microsoft.com/v1.0";

    private final ApplicationContext context;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public GraphApiService(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Get App Role Assignments of User
     *
     * @param accessToken The OAuth2 access token required to authenticate the request.
     * @return {@code List<AppRoleAssignment}
     */
    public List<AppRoleAssignment> getAppRoleAssignments(String accessToken) {
        String url = GRAPH_URL + "/me/appRoleAssignments";

        String jsonResponse = callGraphApi(accessToken, url);

        List<AppRoleAssignment> appRoleAssignments = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode values = root.path("value");

            for (JsonNode node : values) {
                AppRoleAssignment appRoleAssignment = new AppRoleAssignment();
                appRoleAssignment.setResourceId(UUID.fromString(node.path("resourceId").asText()));
                appRoleAssignment.setResourceDisplayName(node.path("resourceDisplayName").asText());
                appRoleAssignment.setAppRoleId(UUID.fromString(node.path("appRoleId").asText()));
                appRoleAssignments.add(appRoleAssignment);
            }
        } catch (Exception e) {
            logger.error("Unexpected error processing app role assignments", e);
        }

        return appRoleAssignments;
    }

    /**
     * Get the user apps and the user roles for the apps
     * <p>
     * This method loads the user role assignments against apps and map them with service principles, then
     * constructs the laa apps list
     *
     * @param accessToken The OAuth2 access token required to authenticate the request.
     * @return {@code List<LaaApplication}
     */
    public List<LaaApplication> getUserAppsAndRoles(String accessToken) {

        // Load app role assignments
        List<AppRoleAssignment> appRoleAssignments = getAppRoleAssignments(accessToken);

        if (appRoleAssignments == null || appRoleAssignments.isEmpty()) {
            // no app role assignments so returning empty list
            return Collections.emptyList();
        }

        // construct list of service principle Ids from app roles
        List<String> servicePrincipleIds = appRoleAssignments.stream().map(AppRoleAssignment::getResourceId).filter(Objects::nonNull).map(UUID::toString).toList();

        GetByIdsPostRequestBody getByIdsPostRequestBody = new GetByIdsPostRequestBody();
        getByIdsPostRequestBody.setIds(servicePrincipleIds);

        // Load the service principles by their ids in app role assignments
        List<DirectoryObject> servicePrincipals = Objects.requireNonNull(graphicServiceClientByAccessToken(accessToken).servicePrincipals().getByIds().post(getByIdsPostRequestBody)).getValue();

        if (servicePrincipals == null || servicePrincipals.isEmpty()) {
            // no service principles so returning empty list
            return Collections.emptyList();
        }

        // map the app role assignments against service principles
        Map<String, LaaApplication> laaApplications = new HashMap<>();

        for (AppRoleAssignment appRoleAssignment : appRoleAssignments) {
            ServicePrincipal servicePrincipal = servicePrincipals.stream().map(ServicePrincipal.class::cast)
                    .filter(spr -> Objects.equals(spr.getId(), String.valueOf(appRoleAssignment.getResourceId())))
                    .findFirst().orElse(null);
            if (servicePrincipal != null) {
                LaaApplication laaApplication = laaApplications.computeIfAbsent(servicePrincipal.getAppId(),
                        l -> LaaApplication.builder().id(servicePrincipal.getAppId()).role(new HashSet<>()).build());
                AppRole role = Optional.ofNullable(servicePrincipal.getAppRoles()).orElse(Collections.emptyList()).stream()
                        .filter(r -> Objects.equals(r.getId(), appRoleAssignment.getAppRoleId())).findFirst().orElse(null);

                if (role == null) {
                    role = new AppRole();
                    if (appRoleAssignment.getAppRoleId() != null && appRoleAssignment.getAppRoleId().equals(DEFAULT_ENTRA_APP_ROLE)) {
                        role.setId(DEFAULT_ENTRA_APP_ROLE);
                        role.setDisplayName("Default");
                    }
                }

                laaApplication.getRole().add(role);
                laaApplications.put(servicePrincipal.getAppId(), laaApplication);
            }
        }

        // Finally construct the laa apps list which contains all the app information along with the user roles
        laaApplications.values().forEach(LaaAppDetailsStore::populateAppDetails);


        return new ArrayList<>(laaApplications.values());

    }


    public User getUserProfile(String accessToken) {
        String url = GRAPH_URL + "/me";
        try {
            String jsonResponse = callGraphApi(accessToken, url);
            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


            return objectMapper.readValue(jsonResponse, User.class);
        } catch (Exception e) {
            logger.error("Unexpected error processing user profile", e);
        }
        return null;
    }

    /**
     * Retrieves the last sign-in time of the authenticated user.
     *
     * @param accessToken The OAuth2 access token required to authenticate the request.
     * @return Last sign-in timestamp as a {@link String}, or {@code null} if not available.
     */
    public LocalDateTime getLastSignInTime(String accessToken) {
        String url = GRAPH_URL + "/me?$select=signInActivity";

        String jsonResponse = callGraphApi(accessToken, url);
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode signInActivity = root.path("signInActivity");

            if (!signInActivity.isMissingNode()) {
                String lastSignInString = signInActivity.path("lastSignInDateTime").asText(null);
                if (lastSignInString != null) {
                    return LocalDateTime.parse(lastSignInString, DateTimeFormatter.ISO_DATE_TIME);
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error retrieving last sign-in time", e);
        }

        return null;
    }


    /**
     * Get groups and roles assigned to a User
     *
     * @param accessToken The OAuth2 access token required to authenticate the request.
     * @return A list of {@link AppRole} objects
     */
    public List<AppRole> getUserAssignedApps(String accessToken) {
        String url = "https://graph.microsoft.com/v1.0/me/memberOf";

        String jsonResponse = callGraphApi(accessToken, url);

        List<AppRole> appRoles = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode values = root.path("value");

            for (JsonNode node : values) {
                AppRole appRole = new AppRole();
                appRole.setDisplayName(node.path("displayName").asText());
                appRole.setDescription(node.path("description").asText());

                appRoles.add(appRole);
            }
        } catch (Exception e) {
            logger.error("Unexpected error processing app role assignments", e);
        }

        return appRoles;
    }

    private GraphServiceClient graphicServiceClientByAccessToken(String accessToken) {
        return (GraphServiceClient) context.getBean("graphicServiceClientByAccessToken", accessToken);
    }

}
