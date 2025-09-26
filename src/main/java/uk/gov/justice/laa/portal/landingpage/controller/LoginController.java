package uk.gov.justice.laa.portal.landingpage.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.CreateUserAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.SwitchProfileAuditEvent;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionData;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
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
    private final FirmService firmService;
    private final EventService eventService;

    public LoginController(LoginService loginService, UserService userService, FirmService firmService, EventService eventService) {
        this.loginService = loginService;
        this.userService = userService;
        this.firmService = firmService;
        this.eventService = eventService;
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
                if (userSessionData.getUser() != null) {
                    Set<Permission> permissions = userService
                            .getUserPermissionsByUserId(userSessionData.getUser().getId());
                    isAdmin = permissions.contains(Permission.VIEW_EXTERNAL_USER)
                            || permissions.contains(Permission.VIEW_INTERNAL_USER);
                }
                model.addAttribute("isAdminUser", isAdmin);
                
                // Check if user has no roles assigned and determine user type for custom message
                if (userSessionData.getUser() != null 
                    && (userSessionData.getLaaApplications() == null || userSessionData.getLaaApplications().isEmpty())) {
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

    @PostMapping("/switchfirm")
    public RedirectView switchFirm(@RequestParam("firmid") String firmId, Authentication authentication) throws IOException {
        EntraUser user = loginService.getCurrentEntraUser(authentication);
        String message = "";
        if (Objects.nonNull(user) && user.isMultiFirmUser()) {
            UserProfile up = user.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst()
                    .orElse(null);
            String oldFirm = "";
            if (Objects.nonNull(up)) {
                oldFirm = up.getFirm().getId().toString();
                if (oldFirm.equals(firmId)) {
                    message = "Can not switch to the same Firm";
                    return new RedirectView("/switchfirm?message=" + message);
                }
                userService.setDefaultActiveProfile(user, UUID.fromString(firmId));
                SwitchProfileAuditEvent auditEvent = new SwitchProfileAuditEvent(user.getId().toString(), oldFirm, firmId);
                eventService.logEvent(auditEvent);
                message = "Switch firm successful";
            }
        } else {
            message = "Apply to multi firm user only";
        }
        return new RedirectView("/switchfirm?message=" + message);
    }

    @GetMapping("/logout-success")
    public String logoutSuccess() {
        return "logout";
    }

    @GetMapping("/switchfirm")
    public String userFirmsPage(@RequestParam("message") String message, Model model, Authentication authentication) {
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        if (Objects.nonNull(entraUser) && entraUser.isMultiFirmUser()) {
            List<FirmDto> firmDtoList = firmService.getUserAllFirms(entraUser);
            for (FirmDto firmDto : firmDtoList) {
                UserProfile up = entraUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst()
                        .orElse(null);
                if (Objects.nonNull(up) && Objects.nonNull(up.getFirm()) && firmDto.getId().equals(up.getFirm().getId())) {
                    firmDto.setName(firmDto.getName() + " - Active");
                }
            }
            model.addAttribute("firmDtoList", firmDtoList);
            model.addAttribute(ModelAttributes.PAGE_TITLE, "Switch firm");
            if (Objects.nonNull(message)) {
                model.addAttribute("message", message);
            }
            return "switch-firm";
        }
        return "redirect:/home";
    }

}
