package uk.gov.justice.laa.portal.landingpage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    @Test
    public void getClient() {
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");
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

        when(logoutService.buildAzureLogoutUrl()).thenReturn("https://login.microsoftonline.com/tenant-id/oauth2/v2.0/logout?post_logout_redirect_uri=http%3A//localhost%3A8080/login");

        logoutHandler.logout(request, response, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(any(), any());
        // Should always call LogoutService.buildAzureLogoutUrl() now
        verify(logoutService).buildAzureLogoutUrl();
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

        when(logoutService.buildAzureLogoutUrl()).thenReturn("https://login.microsoftonline.com/tenant-id/oauth2/v2.0/logout?post_logout_redirect_uri=http%3A//localhost%3A8080/login");

        logoutHandler.logout(request, response, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(any(), any());
        verify(logoutService).buildAzureLogoutUrl();
    }

    @Test
    public void logoutWithAzureLogout_handlesIoException() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("azure_logout", "true");
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        when(logoutService.buildAzureLogoutUrl()).thenReturn("https://login.microsoftonline.com/tenant-id/oauth2/v2.0/logout?post_logout_redirect_uri=http%3A//localhost%3A8080/login");
        
        // Mock the response to throw IOException when sendRedirect is called
        MockHttpServletResponse spyResponse = new MockHttpServletResponse() {
            @Override
            public void sendRedirect(String location) throws IOException {
                throw new IOException("Redirect failed");
            }
        };

        logoutHandler.logout(request, spyResponse, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(any(), any());
        verify(logoutService).buildAzureLogoutUrl();
        
        // Should set fallback response when IOException occurs
        assertThat(spyResponse.getStatus()).isEqualTo(302); // SC_FOUND
        assertThat(spyResponse.getHeader("Location")).isEqualTo("/?message=logout_partial");
    }

    @Test
    public void logoutWithAzureLogoutParameterFalse_shouldStillRedirect() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("azure_logout", "false");
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        when(logoutService.buildAzureLogoutUrl()).thenReturn("https://login.microsoftonline.com/tenant-id/oauth2/v2.0/logout?post_logout_redirect_uri=http%3A//localhost%3A8080/login");

        logoutHandler.logout(request, response, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(any(), any());
        // Should now always call buildAzureLogoutUrl regardless of parameter
        verify(logoutService).buildAzureLogoutUrl();
    }

    @Test
    public void logoutWithNullAzureLogoutParameter_shouldStillRedirect() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // Don't set the parameter at all to test null case
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        when(logoutService.buildAzureLogoutUrl()).thenReturn("https://login.microsoftonline.com/tenant-id/oauth2/v2.0/logout?post_logout_redirect_uri=http%3A//localhost%3A8080/login");

        logoutHandler.logout(request, response, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(any(), any());
        // Should now always call buildAzureLogoutUrl regardless of parameter
        verify(logoutService).buildAzureLogoutUrl();
    }

    @Test
    public void logoutWithEmptyAzureLogoutParameter_shouldStillRedirect() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("azure_logout", "");
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        when(logoutService.buildAzureLogoutUrl()).thenReturn("https://login.microsoftonline.com/tenant-id/oauth2/v2.0/logout?post_logout_redirect_uri=http%3A//localhost%3A8080/login");

        logoutHandler.logout(request, response, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(any(), any());
        // Should now always call buildAzureLogoutUrl regardless of parameter
        verify(logoutService).buildAzureLogoutUrl();
    }

    @Test
    public void logoutWithCaseInsensitiveAzureLogoutParameter_shouldStillRedirect() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("azure_logout", "TRUE"); // uppercase
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        when(logoutService.buildAzureLogoutUrl()).thenReturn("https://login.microsoftonline.com/tenant-id/oauth2/v2.0/logout?post_logout_redirect_uri=http%3A//localhost%3A8080/login");

        logoutHandler.logout(request, response, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(any(), any());
        // Should now always call buildAzureLogoutUrl regardless of parameter value
        verify(logoutService).buildAzureLogoutUrl();
    }

    @Test
    public void logout_handlesGeneralException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        // Mock LogoutService to throw a RuntimeException
        when(logoutService.buildAzureLogoutUrl()).thenThrow(new RuntimeException("Unexpected error"));

        logoutHandler.logout(request, response, realAuthToken);
        
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(any(), any());
        verify(logoutService).buildAzureLogoutUrl();
        
        // Should set error response when general exception occurs
        assertThat(response.getStatus()).isEqualTo(302); // SC_FOUND
        assertThat(response.getHeader("Location")).isEqualTo("/?message=logout_error");
    }
}
