package uk.gov.justice.laa.portal.landingpage.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;


@Service
public class CustomLogoutHandler implements LogoutHandler {

    private final OAuth2AuthorizedClientService clientService;
    private final LoginService loginService;

    public CustomLogoutHandler(OAuth2AuthorizedClientService clientService, LoginService loginService) {
        this.clientService = clientService;
        this.loginService = loginService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        loginService.logout(authentication, getClient(authentication));
    }

    protected OAuth2AuthorizedClient getClient(Authentication authentication) {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        return clientService.loadAuthorizedClient(oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
    }
}
