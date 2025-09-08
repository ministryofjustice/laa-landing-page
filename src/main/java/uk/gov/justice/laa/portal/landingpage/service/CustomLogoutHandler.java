package uk.gov.justice.laa.portal.landingpage.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(CustomLogoutHandler.class);

    private final OAuth2AuthorizedClientService clientService;
    private final LoginService loginService;
    private final LogoutService logoutService;

    public CustomLogoutHandler(OAuth2AuthorizedClientService clientService, LoginService loginService,
            LogoutService logoutService) {
        this.clientService = clientService;
        this.loginService = loginService;
        this.logoutService = logoutService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // First, revoke the Graph API sessions
        loginService.logout(authentication, getClient(authentication));

        // Redirect to Azure logout
        try {
            String logoutUrl = logoutService.buildAzureLogoutUrl();
            response.sendRedirect(logoutUrl);
        } catch (IOException e) {
            // Log the failure for debugging and monitoring
            logger.warn("Failed to redirect to Azure logout URL, falling back to local logout. Error: {}",
                    e.getMessage());

            // Set appropriate response status and headers
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", "/?message=logout_partial");
        } catch (Exception e) {
            // Handle any other unexpected exceptions during logout URL generation
            logger.error("Unexpected error during Azure logout process: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", "/?message=logout_error");
        }
    }

    protected OAuth2AuthorizedClient getClient(Authentication authentication) {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        return clientService.loadAuthorizedClient(oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
    }
}
