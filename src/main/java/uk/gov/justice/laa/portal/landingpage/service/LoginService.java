package uk.gov.justice.laa.portal.landingpage.service;

import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionData;
import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.User;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service class for handling login-related logic.
 */
@Service
public class LoginService {

    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

    // CHECKSTYLE.OFF: AbbreviationAsWordInName|MemberName
    @Value("${spring.security.oauth2.client.registration.azure.client-id}")
    private String AZURE_CLIENT_ID;

    @Value("${spring.security.oauth2.client.registration.azure.tenant-id}")
    private String AZURE_TENANT_ID;
    // CHECKSTYLE.ON: AbbreviationAsWordInName|MemberName

    private final GraphApiService graphApiService;
    private final UserService userService;

    @Value("${spring.security.oauth2.client.registration.azure.redirect-uri}")
    private String redirectUri;


    public LoginService(GraphApiService graphApiService, UserService userService) {
        this.graphApiService = graphApiService;
        this.userService = userService;

    }

    /**
     * Constructs the Microsoft Entra ID login URL with the given email.
     *
     * @param email The user's email.
     * @return The login URL.
     */
    public String buildAzureLoginUrl(String email) {
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);

        return String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?client_id=%s&response_type=code&scope=openid%%20profile%%20email&redirect_uri=%s&login_hint=%s&response_mode=query",
                AZURE_TENANT_ID, AZURE_CLIENT_ID, encodedRedirectUri, encodedEmail
        );
    }

    /**
     * Will fetch user session data
     *
     * @param authentication   The authentication object containing user details.
     * @param authorizedClient The authorized OAuth2 client providing the access token.
     * @param session          The HTTP session used to store the access token.
     * @return A {@link UserSessionData} object containing the user data
     */
    public UserSessionData processUserSession(Authentication authentication,
                                              OAuth2AuthorizedClient authorizedClient,
                                              HttpSession session) {
        if (authentication == null) {
            return null;
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauthToken.getPrincipal();

        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        if (accessToken == null) {
            logger.info("Access token is null");
            return null;
        }

        String name = principal.getAttribute("name");

        String tokenValue = accessToken.getTokenValue();
        session.setAttribute("accessToken", tokenValue);

        List<AppRoleAssignment> appRoleAssignments =
                graphApiService.getAppRoleAssignments(tokenValue);
        List<AppRole> userAppRoleAssignments = graphApiService.getUserAssignedApps(tokenValue);

        User user = graphApiService.getUserProfile(tokenValue);

        LocalDateTime lastLogin = graphApiService.getLastSignInTime(tokenValue);
        String formattedLastLogin = "N/A";

        if (lastLogin != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            formattedLastLogin = lastLogin.format(formatter);
        }

        List<LaaApplication> managedAppRegistrations = userService.getManagedAppRegistrations();
        List<LaaApplication> userAppsAndRoles = graphApiService.getUserAppsAndRoles(tokenValue);

        return new UserSessionData(name, tokenValue, appRoleAssignments,
                userAppRoleAssignments, user, formattedLastLogin, managedAppRegistrations, userAppsAndRoles);
    }

    /**
     * Will fetch current logged-in user
     *
     * @param authorizedClient The authorized OAuth2 client providing the access token.
     * @return A {@link UserModel} object containing the user data
     */
    public UserModel getCurrentUser(OAuth2AuthorizedClient authorizedClient) {
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        if (accessToken == null) {
            logger.info("Access token is null");
            return null;
        }

        String tokenValue = accessToken.getTokenValue();

        User user = graphApiService.getUserProfile(tokenValue);
        String entraId = user.getId();
        return userService.getUserModel(entraId);
    }
}
