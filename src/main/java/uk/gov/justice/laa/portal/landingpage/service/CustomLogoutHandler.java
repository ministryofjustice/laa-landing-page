package uk.gov.justice.laa.portal.landingpage.service;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Service
public class CustomLogoutHandler implements LogoutHandler {

    private final LogoutService logoutService;

    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    public CustomLogoutHandler(LogoutService logoutService) {
        this.logoutService = logoutService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        
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
}
