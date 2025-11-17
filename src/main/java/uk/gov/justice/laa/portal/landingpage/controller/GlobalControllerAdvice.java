package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

@ControllerAdvice
public class GlobalControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(GlobalControllerAdvice.class);
    private final LoginService loginService;
    @Value("${feature.flag.enable.multi.firm.user}")
    private boolean enableMultiFirmUser;

    @Value("${feature.flag.enable.user.audit.table}")
    private boolean enableUserAuditTable;

    public GlobalControllerAdvice(LoginService loginService) {
        this.loginService = loginService;
    }

    @ModelAttribute("activeFirm")
    public FirmDto getActiveFirm(Authentication authentication, HttpServletRequest request) {
        // Skip for claim enrichment and other rest endpoints
        if (request != null) {
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith("/api/")) {
                return null;
            }
        }
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        FirmDto firm = new FirmDto();
        if (Objects.nonNull(entraUser)) {
            // profile not set
            if (Objects.isNull(entraUser.getUserProfiles()) || entraUser.getUserProfiles().isEmpty()) {
                firm.setName("You currently don’t have access to any profiles. Please contact the admin to be added.");
            } else if (entraUser.isMultiFirmUser()) {
                UserProfile up = entraUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst()
                        .orElse(null);
                // have active profile
                if (Objects.nonNull(up)) {
                    String firmName = up.getFirm().getName();
                    String firmCode = up.getFirm().getCode();
                    firm.setName(firmName);
                    firm.setCode(firmCode);
                    // have more than 1 firms
                    if (entraUser.getUserProfiles().size() > 1) {
                        firm.setCanChange(true);
                    }
                } else {
                    // have no active profile
                    firm.setName(
                            "You currently don't have access to any Provider Firms. Please contact the provider firm's admin to be added.");
                    firm.setCanChange(false);
                }
            } else {
                // single firm
                UserProfile up = entraUser.getUserProfiles().stream().findFirst().get();
                if (up.getUserType().equals(UserType.EXTERNAL)) {
                    String firmName = up.getFirm().getName();
                    String firmCode = up.getFirm().getCode();
                    firm.setName(firmName);
                    firm.setCode(firmCode);
                    firm.setCanChange(false);
                } else {
                    // internal
                    return null;
                }
            }
            return firm;
        }
        return null;
    }

    @ModelAttribute("enableMultiFirmUser")
    public boolean getMultiFirmEnabledFlag() {
        return enableMultiFirmUser;
    }

    @ModelAttribute("enableUserAuditTable")
    public boolean getUserAuditTableEnabledFlag() {
        return enableUserAuditTable;
    }

    @ModelAttribute("currentUser")
    public CurrentUserDto getCurrentUserProfile(Authentication authentication, HttpServletRequest request) {
        // Skip for claim enrichment and other rest endpoints
        if (request != null) {
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith("/api/")) {
                return null;
            }
        }

        return loginService.getCurrentUser(authentication);
    }

    @ModelAttribute("isInternal")
    public boolean isInternal(Authentication authentication, HttpServletRequest request) {
        // Skip for claim enrichment and other rest endpoints
        if (request != null) {
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith("/api/")) {
                return false;
            }
        }

        UserProfile up = loginService.getCurrentProfile(authentication);
        return up != null && up.getUserType() == UserType.INTERNAL;
    }

    @ModelAttribute("isExternal")
    public boolean isExternal(Authentication authentication, HttpServletRequest request) {
        // Skip for claim enrichment and other rest endpoints
        if (request != null) {
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith("/api/")) {
                return false;
            }
        }

        UserProfile up = loginService.getCurrentProfile(authentication);
        return up != null && up.getUserType() == UserType.EXTERNAL;
    }

    /**
     * Handles OAuth2 client authorization exceptions by redirecting to
     * re-authenticate.
     * This typically occurs when the access token has expired or is invalid.
     *
     * @param ex the ClientAuthorizationRequiredException
     * @return a redirect to the OAuth2 authorization endpoint
     */
    @ExceptionHandler(ClientAuthorizationRequiredException.class)
    public RedirectView handleClientAuthorizationRequired(ClientAuthorizationRequiredException ex) {
        logger.warn("OAuth2 authorization required for client: {}. Redirecting to re-authenticate.",
                ex.getClientRegistrationId());
        // Redirect to the OAuth2 authorization endpoint to get a new token
        return new RedirectView("/oauth2/authorization/" + ex.getClientRegistrationId());
    }
}
