package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionData;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

/***
 * Controller for handling login-related requests.
 */
@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    private final LoginService loginService;
    private final UserService userService;

    public LoginController(LoginService loginService, UserService userService) {
        this.loginService = loginService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    /**
     * @param email input by user
     * @return home view if successful, else login view
     */
    @PostMapping("/login")
    public RedirectView handleLogin(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An incorrect Username or Password was specified");
            return new RedirectView("/");
        }
        try {
            String azureLoginUrl = loginService.buildAzureLoginUrl(email);
            return new RedirectView(azureLoginUrl);
        } catch (Exception e) {
            logger.error("Error logging in: {}", e.getMessage());
            return new RedirectView("/");
        }
    }

    /**
     * Handles GET requests to the "/home" endpoint.
     *
     * @param model          the model to be populated with user session data
     * @param authentication the authentication object containing user credentials
     * @param session        the current HTTP session
     * @param authClient     the OAuth2 authorized client for Azure
     * @return the view for home
     */
    @GetMapping("/home")
    public String home(Model model, Authentication authentication, HttpSession session,
            @RegisteredOAuth2AuthorizedClient("azure") OAuth2AuthorizedClient authClient) {
        try {
            UserSessionData userSessionData = loginService.processUserSession(authentication, authClient, session);

            if (userSessionData != null) {
                model.addAttribute("name", userSessionData.getName());
                model.addAttribute("user", userSessionData.getUser());
                model.addAttribute("lastLogin", "N/A");
                model.addAttribute("laaApplications", userSessionData.getLaaApplications());
                boolean isAdmin = false;
                boolean canViewAuditTable = false;
                boolean hasSilasAdminRole = false;
                if (userSessionData.getUser() != null) {
                    Set<Permission> permissions = userService
                            .getUserPermissionsByUserId(userSessionData.getUser().getId());
                    isAdmin = permissions.contains(Permission.VIEW_EXTERNAL_USER)
                            || permissions.contains(Permission.VIEW_INTERNAL_USER);
                    canViewAuditTable = permissions.contains(Permission.VIEW_AUDIT_TABLE);

                    // Check if user has SiLAS Administration role
                    EntraUser currentUser = loginService.getCurrentEntraUser(authentication);
                    hasSilasAdminRole = AccessControlService.userHasAuthzRole(currentUser, AuthzRole.GLOBAL_ADMIN.getRoleName());
                }
                model.addAttribute("isAdminUser", isAdmin);
                model.addAttribute("canViewAuditTable", canViewAuditTable);
                model.addAttribute("hasSilasAdminRole", hasSilasAdminRole);

                // Check if user has no roles assigned and determine user type for custom
                // message
                if (userSessionData.getUser() != null
                        && (userSessionData.getLaaApplications() == null
                                || userSessionData.getLaaApplications().isEmpty())) {
                    boolean isInternal = userService.isInternal(userSessionData.getUser().getId());
                    model.addAttribute("userHasNoRoles", true);
                    model.addAttribute("isInternalUser", isInternal);
                } else {
                    model.addAttribute("userHasNoRoles", false);
                }
            } else {
                logger.info("No access token found");
            }
        } catch (Exception e) {
            logger.error("Error getting user list: {}", e.getMessage());
        }
        return "home";
    }

    @ExceptionHandler(Exception.class)
    public RedirectView handleException(Exception ex) {
        logger.error("Error while user login:", ex);
        return new RedirectView("/error");
    }

    @GetMapping("/logout-success")
    public String logoutSuccess() {
        return "logout";
    }

}
