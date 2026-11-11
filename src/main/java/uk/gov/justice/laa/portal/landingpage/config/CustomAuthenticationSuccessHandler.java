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
 * This handler fires once per actual OAuth2 authentication event (not on every page visit),
 * ensuring the timestamp represents a login event rather than page-view activity.
 * 
 * Runs after UserDisabledFilter and FirmDisabledFilter in the security filter chain,
 * so only users who pass those checks get their timestamp recorded (satisfying AC3/AC4).
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
        loginService.recordSuccessfulLogin(authentication);
        response.sendRedirect("/home");
    }
}
