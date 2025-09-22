package uk.gov.justice.laa.portal.landingpage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
public class CustomLogoutHandlerTest {

    @InjectMocks
    private CustomLogoutHandler logoutHandler;
    @Mock
    private OAuth2AuthorizedClientService clientService;
    @Mock
    private LoginService loginService;
    @Mock
    private LogoutService logoutService;
    @Mock
    private OAuth2AuthorizedClient mockClient;

    @Test
    public void getClient() {
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");
        
        when(clientService.loadAuthorizedClient(eq("azure"), eq("Alice"))).thenReturn(mockClient);

        logoutHandler.getClient(realAuthToken);
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
    }

    @Test
    public void logout() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        when(clientService.loadAuthorizedClient(eq("azure"), eq("Alice"))).thenReturn(mockClient);

        logoutHandler.logout(request, response, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(eq(realAuthToken), eq(mockClient));
        // Should not call LogoutService.buildAzureLogoutUrl() when azure_logout parameter is not present
        verify(logoutService, never()).buildAzureLogoutUrl();
    }

    @Test
    public void logoutWithAzureLogout() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("azure_logout", "true");
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        when(clientService.loadAuthorizedClient(eq("azure"), eq("Alice"))).thenReturn(mockClient);
        when(logoutService.buildAzureLogoutUrl()).thenReturn("https://login.microsoftonline.com/tenant-id/oauth2/v2.0/logout?post_logout_redirect_uri=http%3A//localhost%3A8080/%3Fmessage%3Dlogout");

        logoutHandler.logout(request, response, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(eq(realAuthToken), eq(mockClient));
        verify(logoutService).buildAzureLogoutUrl();
    }

    @Test
    public void logoutWithAzureLogout_handlesIoException() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("azure_logout", "true");
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        when(clientService.loadAuthorizedClient(eq("azure"), eq("Alice"))).thenReturn(mockClient);
        when(logoutService.buildAzureLogoutUrl()).thenReturn("https://login.microsoftonline.com/tenant-id/oauth2/v2.0/logout?post_logout_redirect_uri=http%3A//localhost%3A8080/%3Fmessage%3Dlogout");
        
        // Mock the response to throw IOException when sendRedirect is called
        MockHttpServletResponse spyResponse = new MockHttpServletResponse() {
            @Override
            public void sendRedirect(String location) throws IOException {
                throw new IOException("Redirect failed");
            }
        };

        logoutHandler.logout(request, spyResponse, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(eq(realAuthToken), eq(mockClient));
        verify(logoutService).buildAzureLogoutUrl();
        
        // Should set fallback response when IOException occurs
        assertThat(spyResponse.getStatus()).isEqualTo(302); // SC_FOUND
        assertThat(spyResponse.getHeader("Location")).isEqualTo("/?message=logout");
    }

    @Test
    public void logoutWithAzureLogoutParameterFalse_shouldNotRedirect() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("azure_logout", "false");
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        when(clientService.loadAuthorizedClient(eq("azure"), eq("Alice"))).thenReturn(mockClient);

        logoutHandler.logout(request, response, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(eq(realAuthToken), eq(mockClient));
        verify(logoutService, never()).buildAzureLogoutUrl();
    }

    @Test
    public void logoutWithNullAzureLogoutParameter_shouldNotRedirect() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // Don't set the parameter at all to test null case
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        when(clientService.loadAuthorizedClient(eq("azure"), eq("Alice"))).thenReturn(mockClient);

        logoutHandler.logout(request, response, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(eq(realAuthToken), eq(mockClient));
        verify(logoutService, never()).buildAzureLogoutUrl();
    }

    @Test
    public void logoutWithEmptyAzureLogoutParameter_shouldNotRedirect() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("azure_logout", "");
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        when(clientService.loadAuthorizedClient(eq("azure"), eq("Alice"))).thenReturn(mockClient);

        logoutHandler.logout(request, response, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(eq(realAuthToken), eq(mockClient));
        verify(logoutService, never()).buildAzureLogoutUrl();
    }

    @Test
    public void logoutWithCaseInsensitiveAzureLogoutParameter_shouldOnlyWorkWithExactTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("azure_logout", "TRUE"); // uppercase
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        when(clientService.loadAuthorizedClient(eq("azure"), eq("Alice"))).thenReturn(mockClient);

        logoutHandler.logout(request, response, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(eq(realAuthToken), eq(mockClient));
        // Should not call buildAzureLogoutUrl because "TRUE" != "true"
        verify(logoutService, never()).buildAzureLogoutUrl();
    }

    @Test
    public void getClient_withNullAuthentication_shouldReturnNull() {
        assertThat(logoutHandler.getClient(null)).isNull();
        verify(clientService, never()).loadAuthorizedClient(any(), any());
    }

    @Test
    public void getClient_withNonOauthAuthentication_shouldReturnNull() {
        UsernamePasswordAuthenticationToken nonOauthToken = new UsernamePasswordAuthenticationToken("user", "password");

        assertThat(logoutHandler.getClient(nonOauthToken)).isNull();
        verify(clientService, never()).loadAuthorizedClient(any(), any());
    }

    @Test
    public void logout_withNullAuthentication_shouldNotCauseNpe() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // This should not throw any exception
        logoutHandler.logout(request, response, null);
        
        // Should not attempt to call any logout services when authentication is null
        verify(clientService, never()).loadAuthorizedClient(any(), any());
        verify(loginService, never()).logout(any(), any());
    }

    @Test
    public void logout_withNonOauthAuthentication_shouldNotCauseNpe() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        UsernamePasswordAuthenticationToken nonOauthToken = new UsernamePasswordAuthenticationToken("user", "password");

        // This should not throw any exception
        logoutHandler.logout(request, response, nonOauthToken);
        
        // Should not attempt to call any logout services when authentication is not OAuth2
        verify(clientService, never()).loadAuthorizedClient(any(), any());
        verify(loginService, never()).logout(any(), any());
    }

    @Test
    public void logout_withNullAuthenticationAndAzureLogout_shouldStillRedirect() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("azure_logout", "true");
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        when(logoutService.buildAzureLogoutUrl()).thenReturn("https://login.microsoftonline.com/tenant-id/oauth2/v2.0/logout?post_logout_redirect_uri=http%3A//localhost%3A8080/%3Fmessage%3Dlogout");

        // This should not throw any exception and should still handle Azure logout
        logoutHandler.logout(request, response, null);
        
        // Should not attempt to call logout services for null authentication
        verify(clientService, never()).loadAuthorizedClient(any(), any());
        verify(loginService, never()).logout(any(), any());
        
        // But should still handle Azure logout redirect
        verify(logoutService).buildAzureLogoutUrl();
    }
}
