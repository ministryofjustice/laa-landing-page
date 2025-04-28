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

    /*
     I think the private string redirectUri is null and causing this test to fail, but can't figure out how
     to set this as a value in the mock without editing the tested code and don't want to do this yet
     Will leave for now
    */
    @Test
    void buildsAzureUrl_withEncodedParams() {
        String url = loginService.buildAzureLoginUrl("a@b.com");
        assertThat(url).contains("login_hint=a%40b.com");
    }

    @Test
    void createsSessionData_whenGraphReturnsValues() {
        // ----- Arrange principal & token -----
        var principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"), "preferred_username");
        var authToken = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "azure");
        var client = MockitoTestUtils.authorizedClient("token-123"); // tiny helper

        // ---- stub Graph responses ----
        when(graph.getAppRoleAssignments(anyString())).thenReturn(List.of(new AppRoleAssignment()));
        when(graph.getUserAssignedApps(anyString())).thenReturn(List.of(new AppRole()));
        when(graph.getUserProfile(anyString())).thenReturn(new User());
        when(graph.getLastSignInTime(anyString())).thenReturn(LocalDateTime.of(2025, 4, 28, 12, 0));
        when(userService.getManagedAppRegistrations()).thenReturn(List.of());

        // ----- Act -----
        var dto = loginService.processUserSession(authToken, client, session);

        // ----- Assert -----
        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo("Alice");
    }

    // This was suggested, not sure if needed but leaving just for now
    /*=== small helper ===*/
    static class MockitoTestUtils {
        static OAuth2AuthorizedClient authorizedClient(String value) {
            var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, value, Instant.now(), Instant.now().plusSeconds(3600));
            OAuth2AuthorizedClient c = org.mockito.Mockito.mock(OAuth2AuthorizedClient.class);
            when(c.getAccessToken()).thenReturn(token);
            return c;
        }
    }
}