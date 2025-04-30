package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.User;
import jakarta.servlet.http.HttpSession;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @InjectMocks
    private LoginService loginService;

    @Mock
    private GraphApiService graph;
    @Mock
    private UserService userService;
    @Mock
    private HttpSession session;

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
        when(graph.getAppRoleAssignments(anyString())).thenReturn(List.of(new AppRoleAssignment()));
        when(graph.getUserAssignedApps(anyString())).thenReturn(List.of(new AppRole()));
        when(graph.getUserProfile(anyString())).thenReturn(new User());
        when(graph.getLastSignInTime(anyString())).thenReturn(LocalDateTime.of(2025, 4, 28, 12, 0));
        when(userService.getManagedAppRegistrations()).thenReturn(List.of());

        // Act
        var userSessionData = loginService.processUserSession(authToken, client, session);

        // Assert
        // (Unfinished - may need to verify more attributes in this section)
        assertThat(userSessionData).isNotNull();
        assertThat(userSessionData.getName()).isEqualTo("Alice");
    }
}