package uk.gov.justice.laa.portal.landingpage.service;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Service
public class CustomLogoutHandler implements LogoutHandler {

    private final OAuth2AuthorizedClientService clientService;
    private final LoginService loginService;
    private final LogoutService logoutService;

    public CustomLogoutHandler(OAuth2AuthorizedClientService clientService, LoginService loginService, LogoutService logoutService) {
        this.clientService = clientService;
        this.loginService = loginService;
        this.logoutService = logoutService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // Check if authentication is valid before attempting logout operations
        if (authentication != null && authentication instanceof OAuth2AuthenticationToken) {
            // First, revoke the Graph API sessions
            OAuth2AuthorizedClient client = getClient(authentication);
            if (client != null) {
                loginService.logout(authentication, client);
            }
        }
        
        // Only redirect to Azure logout if this is not a test environment or if explicitly requested
        // Check if the request has a parameter indicating Azure logout is needed
        String azureLogout = request.getParameter("azure_logout");
        if ("true".equals(azureLogout)) {
            try {
                String logoutUrl = logoutService.buildAzureLogoutUrl();
                response.sendRedirect(logoutUrl);
            } catch (IOException e) {
                // If redirect fails, let Spring Security handle the logout normally
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", "/?message=logout");
            }
        }
        // If no Azure logout is requested, let Spring Security handle the normal logout flow
    }

    protected OAuth2AuthorizedClient getClient(Authentication authentication) {
        if (authentication == null || !(authentication instanceof OAuth2AuthenticationToken)) {
            return null;
        }
        
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        return clientService.loadAuthorizedClient(oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
    }
}
