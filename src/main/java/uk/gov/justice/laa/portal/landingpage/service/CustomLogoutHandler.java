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
        // First, revoke the Graph API sessions
        loginService.logout(authentication, getClient(authentication));
        
        try {
            String logoutUrl = logoutService.buildAzureLogoutUrl();
            response.sendRedirect(logoutUrl);
        } catch (IOException e) {
            // If redirect fails, let Spring Security handle the logout normally
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", "/?message=logout");
        }
    }

    protected OAuth2AuthorizedClient getClient(Authentication authentication) {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        return clientService.loadAuthorizedClient(oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
    }
}
