package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionData;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    private static final String TEST_TOKEN_VALUE = "test-token-123";
    private static final String TEST_NAME = "Test User";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginService, "redirectUri", TEST_REDIRECT_URI);
    }

    @Test
    void processUserSession_withValidAuthentication_returnsUserSessionData() {

        // Arrange
        when(oauthToken.getPrincipal()).thenReturn(principal);
        when(principal.getAttribute("name")).thenReturn(TEST_NAME);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn(TEST_TOKEN_VALUE);

        Set<LaaApplication> mockManagedApps = Set.of(new LaaApplication());
        EntraUserDto mockUser = mock(EntraUserDto.class, Mockito.RETURNS_DEEP_STUBS);
        when(userService.findUserByUserEntraId(null)).thenReturn(mockUser);
        when(userService.getUserAssignedAppsforLandingPage(null)).thenReturn(mockManagedApps);

        // Act
        UserSessionData result = loginService.processUserSession(oauthToken, authorizedClient, session);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(TEST_NAME);
        assertThat(result.getAccessToken()).isEqualTo(TEST_TOKEN_VALUE);
        assertThat(result.getUser()).isEqualTo(mockUser);
        assertThat(result.getLaaApplications()).isEqualTo(mockManagedApps);
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
    }

    @Test
    void processUserSession_whenLastLoginIsNull_formatsLastLoginAsNa() {

        // Arrange
        when(oauthToken.getPrincipal()).thenReturn(principal);
        when(principal.getAttribute("name")).thenReturn(TEST_NAME);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn(TEST_TOKEN_VALUE);
        EntraUserDto mockUser = mock(EntraUserDto.class, Mockito.RETURNS_DEEP_STUBS);
        when(userService.findUserByUserEntraId(null)).thenReturn(mockUser);

        // Act
        UserSessionData result = loginService.processUserSession(oauthToken, authorizedClient, session);

        // Assert
        assertThat(result).isNotNull();
        verify(session).setAttribute("accessToken", TEST_TOKEN_VALUE);
    }

    @Test
    void processUserSession_withRealOauth2User_populatesNameFromPrincipal() {

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
        EntraUserDto mockUser = mock(EntraUserDto.class, Mockito.RETURNS_DEEP_STUBS);
        when(userService.findUserByUserEntraId(null)).thenReturn(mockUser);

        // Act
        UserSessionData userSessionData = loginService.processUserSession(realAuthToken, realAuthClient, session);

        // Assert
        assertThat(userSessionData).isNotNull();
        assertThat(userSessionData.getName()).isEqualTo("Alice");
    }

    @Test
    void getCurrentUser_withRealPrincipal_populatesNameAndIdFromPrincipal() {

        // Arrange
        UUID userId = UUID.randomUUID();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "oid", userId.toString()),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        // Act
        CurrentUserDto userDto = loginService.getCurrentUser(realAuthToken);

        // Assert
        assertThat(userDto).isNotNull();
        assertThat(userDto.getName()).isEqualTo("Alice");
        assertThat(userDto.getUserId()).isEqualTo(userId);
    }

    @Test
    void getCurrentUser_withNullAuth() {
        // Act
        CurrentUserDto userDto = loginService.getCurrentUser(null);

        // Assert
        assertThat(userDto).isNull();
    }
}