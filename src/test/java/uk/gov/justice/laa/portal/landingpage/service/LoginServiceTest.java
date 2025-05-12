package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.User;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionData;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @InjectMocks
    private LoginService loginService;

    @Mock
    private GraphApiService graphApiService;
    @Mock
    private UserService userService;
    @Mock
    private HttpSession session;
    @Mock
    private OAuth2AuthenticationToken oauthToken;
    @Mock
    private OAuth2User principal;
    @Mock
    private OAuth2AuthorizedClient authorizedClient;
    @Mock
    private OAuth2AccessToken accessToken;

    private static final String TEST_REDIRECT_URI = "http://localhost:8080/login/oauth2/code/azure";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_TOKEN_VALUE = "test-token-123";
    private static final String TEST_NAME = "Test User";
    private static String AZURE_TENANT_ID;
    private static String AZURE_CLIENT_ID;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginService, "redirectUri", TEST_REDIRECT_URI);
        AZURE_TENANT_ID = System.getenv("AZURE_TENANT_ID") != null ? System.getenv("AZURE_TENANT_ID") : "mockTenantId";
        AZURE_CLIENT_ID = System.getenv("AZURE_CLIENT_ID") != null ? System.getenv("AZURE_CLIENT_ID") : "mockClientId";
    }

    @Test
    void createsSessionData_whenGraphReturnsValues() {

        // Arrange
        var principal =
                new DefaultOAuth2User(
                        List.of(new SimpleGrantedAuthority("ROLE_USER")),
                        Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                        "preferred_username");
        var authToken = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "azure");

        // (Stubbed OAuth2 client)
        var token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token-123",
                Instant.now(),
                Instant.now().plusSeconds(3600));
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        when(client.getAccessToken()).thenReturn(token);

        // (Stub Graph API responses)
        when(graphApiService.getAppRoleAssignments(anyString())).thenReturn(List.of(new AppRoleAssignment()));
        when(graphApiService.getUserAssignedApps(anyString())).thenReturn(List.of(new AppRole()));
        when(graphApiService.getUserProfile(anyString())).thenReturn(new User());
        when(graphApiService.getLastSignInTime(anyString())).thenReturn(LocalDateTime.of(2025, 4, 28, 12, 0));
        when(userService.getManagedAppRegistrations()).thenReturn(List.of());

        // Act
        var userSessionData = loginService.processUserSession(authToken, client, session);

        // Assert
        assertThat(userSessionData).isNotNull();
        assertThat(userSessionData.getName()).isEqualTo("Alice");

    }

    // TODO: Test currently fails; url is expected to contain "mockTenantId" from AZURE_TENANT_ID but returns "null" instead
    // Not sure if this is the test or the code
    @Test
    void buildAzureLoginUrl_withValidEmail_returnsCorrectlyFormattedUrl() {

        // Arrange
        String expectedEncodedRedirectUri = URLEncoder.encode(TEST_REDIRECT_URI, StandardCharsets.UTF_8);
        String expectedEncodedEmail = URLEncoder.encode(TEST_EMAIL, StandardCharsets.UTF_8);
        String expectedUrl = String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?client_id=%s&response_type=code&scope=openid%%20profile%%20email&redirect_uri=%s&login_hint=%s&response_mode=query",
                AZURE_TENANT_ID, AZURE_CLIENT_ID, expectedEncodedRedirectUri, expectedEncodedEmail
        );

        // Act
        String actualUrl = loginService.buildAzureLoginUrl(TEST_EMAIL);

        // Assert
        assertThat(actualUrl).isEqualTo(expectedUrl);
    }

    @Test
    void processUserSession_withValidAuthentication_returnsUserSessionData() {

        // Arrange
        when(oauthToken.getPrincipal()).thenReturn(principal);
        when(principal.getAttribute("name")).thenReturn(TEST_NAME);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn(TEST_TOKEN_VALUE);

        List<AppRoleAssignment> mockAssignments = List.of(new AppRoleAssignment());
        List<AppRole> mockUserRoles = List.of(new AppRole());
        User mockUser = new User();
        LocalDateTime mockLoginTime = LocalDateTime.of(2024, 5, 8, 10, 0, 0);
        List<LaaApplication> mockManagedApps = List.of(new LaaApplication());
        List<LaaApplication> mockUserAppsAndRoles = List.of(new LaaApplication());

        when(graphApiService.getAppRoleAssignments(TEST_TOKEN_VALUE)).thenReturn(mockAssignments);
        when(graphApiService.getUserAssignedApps(TEST_TOKEN_VALUE)).thenReturn(mockUserRoles);
        when(graphApiService.getUserProfile(TEST_TOKEN_VALUE)).thenReturn(mockUser);
        when(graphApiService.getLastSignInTime(TEST_TOKEN_VALUE)).thenReturn(mockLoginTime);
        when(userService.getManagedAppRegistrations()).thenReturn(mockManagedApps);
        when(graphApiService.getUserAppsAndRoles(TEST_TOKEN_VALUE)).thenReturn(mockUserAppsAndRoles);

        // Act
        UserSessionData result = loginService.processUserSession(oauthToken, authorizedClient, session);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(TEST_NAME);
        assertThat(result.getAccessToken()).isEqualTo(TEST_TOKEN_VALUE);
        assertThat(result.getAppRoleAssignments()).isEqualTo(mockAssignments);
        assertThat(result.getUserAppRoles()).isEqualTo(mockUserRoles);
        assertThat(result.getUser()).isEqualTo(mockUser);
        assertThat(result.getLastLogin()).isEqualTo("08-05-2024 10:00:00");
        assertThat(result.getLaaApplications()).isEqualTo(mockManagedApps);
        assertThat(result.getUserAppsAndRoles()).isEqualTo(mockUserAppsAndRoles);
        verify(session).setAttribute("accessToken", TEST_TOKEN_VALUE);
    }

    @Test
    void processUserSession_whenAuthenticationIsNull_returnsNull() {

        // Arrange & Act
        UserSessionData result = loginService.processUserSession(null, authorizedClient, session);

        // Assert
        assertThat(result).isNull();
        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    void processUserSession_whenAccessTokenIsNull_returnsNull() {

        // Arrange
        when(oauthToken.getPrincipal()).thenReturn(principal);
        when(authorizedClient.getAccessToken()).thenReturn(null);

        // Act
        UserSessionData result = loginService.processUserSession(oauthToken, authorizedClient, session);

        // Assert
        assertThat(result).isNull();
        verify(session, never()).setAttribute(anyString(), any());
        verify(graphApiService, never()).getAppRoleAssignments(anyString());
    }

    @Test
    void processUserSession_whenLastLoginIsNull_formatsLastLoginAsNA() {

        // Arrange
        when(oauthToken.getPrincipal()).thenReturn(principal);
        when(principal.getAttribute("name")).thenReturn(TEST_NAME);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn(TEST_TOKEN_VALUE);
        when(graphApiService.getLastSignInTime(TEST_TOKEN_VALUE)).thenReturn(null);

        // Mock other graphApiService and userService calls to return empty/default values
        when(graphApiService.getAppRoleAssignments(anyString())).thenReturn(Collections.emptyList());
        when(graphApiService.getUserAssignedApps(anyString())).thenReturn(Collections.emptyList());
        when(graphApiService.getUserProfile(anyString())).thenReturn(new User());
        when(userService.getManagedAppRegistrations()).thenReturn(Collections.emptyList());
        when(graphApiService.getUserAppsAndRoles(anyString())).thenReturn(Collections.emptyList());

        // Act
        UserSessionData result = loginService.processUserSession(oauthToken, authorizedClient, session);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLastLogin()).isEqualTo("N/A");
        verify(session).setAttribute("accessToken", TEST_TOKEN_VALUE);
    }

    @Test
    void processUserSession_withRealOAuth2User_populatesNameFromPrincipal() {

        // Arrange
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");
        OAuth2AccessToken realAccessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                TEST_TOKEN_VALUE,
                Instant.now(),
                Instant.now().plusSeconds(3600));
        OAuth2AuthorizedClient realAuthClient = mock(OAuth2AuthorizedClient.class);
        when(realAuthClient.getAccessToken()).thenReturn(realAccessToken);

        // Mock graph/user service calls
        when(graphApiService.getAppRoleAssignments(TEST_TOKEN_VALUE)).thenReturn(Collections.emptyList());
        when(graphApiService.getUserAssignedApps(TEST_TOKEN_VALUE)).thenReturn(Collections.emptyList());
        when(graphApiService.getUserProfile(TEST_TOKEN_VALUE)).thenReturn(new User());
        when(graphApiService.getLastSignInTime(TEST_TOKEN_VALUE)).thenReturn(null);
        when(userService.getManagedAppRegistrations()).thenReturn(Collections.emptyList());
        when(graphApiService.getUserAppsAndRoles(TEST_TOKEN_VALUE)).thenReturn(Collections.emptyList());

        // Act
        UserSessionData userSessionData = loginService.processUserSession(realAuthToken, realAuthClient, session);

        // Assert
        assertThat(userSessionData).isNotNull();
        assertThat(userSessionData.getName()).isEqualTo("Alice");
        assertThat(userSessionData.getAccessToken()).isEqualTo(TEST_TOKEN_VALUE);
        assertThat(userSessionData.getLastLogin()).isEqualTo("N/A");
        verify(session).setAttribute("accessToken", TEST_TOKEN_VALUE);
    }
}