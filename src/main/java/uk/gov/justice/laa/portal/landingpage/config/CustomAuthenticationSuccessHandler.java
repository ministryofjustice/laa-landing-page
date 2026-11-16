package uk.gov.justice.laa.portal.landingpage.config;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

/**
 * Custom OAuth2 authentication success handler that records the last successful SiLAS login
 * timestamp immediately after successful OAuth2 authentication.
 *
 * The handler delegates to LoginService.recordSuccessfulLogin() which applies eligibility
 * checks before recording the timestamp.
 */
@Component
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginService loginService;

    public CustomAuthenticationSuccessHandler(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            loginService.recordSuccessfulLogin(authentication);
        } catch (Exception ex) {
            log.error("Failed to record login timestamp", ex);
        }
        response.sendRedirect("/home");
    }
}
