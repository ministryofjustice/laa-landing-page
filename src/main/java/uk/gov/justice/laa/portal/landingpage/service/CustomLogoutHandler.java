package uk.gov.justice.laa.portal.landingpage.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
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
            
            // Extract session information for complete session termination
            String sessionId = extractSessionId(request);
            String canaryToken = extractCanaryToken(request);
            String sessionCookies = extractSessionCookies(request);
            
            if (sessionId != null && canaryToken != null) {
                // Attempt to terminate the Azure AD session completely
                boolean terminated = logoutService.terminateAzureSession(sessionId, canaryToken, sessionCookies);
                if (terminated) {
                    // Session terminated successfully, redirect directly to home
                    response.sendRedirect("/?message=logout");
                    return;
                }
            }
            
            // Fallback to standard Azure AD logout flow
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

    /**
     * Extracts the Azure AD session ID from the request cookies
     * Looks for the ESTSAUTHLIGHT cookie which contains the session ID
     */
    private String extractSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("ESTSAUTHLIGHT".equals(cookie.getName())) {
                    String value = cookie.getValue();
                    // ESTSAUTHLIGHT format: +sessionId, extract the sessionId part
                    if (value != null && value.startsWith("+")) {
                        return value.substring(1); // Remove the '+' prefix
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extracts the canary token from the request
     * This would typically come from a form parameter or session attribute
     * For now, we'll try to extract it from the referer URL or session
     */
    private String extractCanaryToken(HttpServletRequest request) {
        // Try to get from session first (if it was stored there during login)
        Object canary = request.getSession(false) != null ? 
            request.getSession(false).getAttribute("azure.canary") : null;
        
        if (canary != null) {
            return canary.toString();
        }

        // Try to extract from referer URL parameters
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("canary=")) {
            try {
                int start = referer.indexOf("canary=") + 7;
                int end = referer.indexOf("&", start);
                if (end == -1) end = referer.length();
                return java.net.URLDecoder.decode(referer.substring(start, end), "UTF-8");
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        // Return a placeholder - in production this would need proper implementation
        // based on how your application manages Azure AD session state
        return null;
    }

    /**
     * Extracts all relevant session cookies for Azure AD logout
     * Collects cookies needed for session termination
     */
    private String extractSessionCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
            .filter(cookie -> isAzureSessionCookie(cookie.getName()))
            .map(cookie -> cookie.getName() + "=" + cookie.getValue())
            .collect(Collectors.joining("; "));
    }

    /**
     * Determines if a cookie is relevant for Azure AD session management
     */
    private boolean isAzureSessionCookie(String cookieName) {
        return cookieName.startsWith("esctx") ||
               cookieName.startsWith("ESTSAUTH") ||
               cookieName.equals("SignInStateCookie") ||
               cookieName.equals("buid") ||
               cookieName.equals("AADSSO") ||
               cookieName.equals("AADSSOTILES") ||
               cookieName.equals("x-ms-gateway-slice") ||
               cookieName.equals("stsservicecookie") ||
               cookieName.startsWith("MSFPC") ||
               cookieName.equals("wlidperf") ||
               cookieName.equals("ai_session") ||
               cookieName.equals("fpc") ||
               cookieName.equals("brcap") ||
               cookieName.contains("MicrosoftApplicationsTelemetry");
    }
}
