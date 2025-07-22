package uk.gov.justice.laa.portal.landingpage.controller;

import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionData;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/***
 * Controller for handling login-related requests.
 */
@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    private final LoginService loginService;
    private final UserService userService;
    private final FirmService firmService;

    public LoginController(LoginService loginService, UserService userService, FirmService firmService) {
        this.loginService = loginService;
        this.userService = userService;
        this.firmService = firmService;
    }

    @GetMapping("/")
    public String login(@RequestParam(value = "message", required = false) String message, Model model) {
        if (message != null && message.equals("logout")) {
            String successMessage = "You have been securely logged out";
            model.addAttribute("successMessage", successMessage);
        }
        return "index";
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
    public String home(Model model, Authentication authentication, HttpSession session, @RegisteredOAuth2AuthorizedClient("azure") OAuth2AuthorizedClient authClient) {
        try {
            UserSessionData userSessionData = loginService.processUserSession(authentication, authClient, session);

            if (userSessionData != null) {
                model.addAttribute("name", userSessionData.getName());
                model.addAttribute("user", userSessionData.getUser());
                model.addAttribute("lastLogin", "N/A");
                model.addAttribute("laaApplications", userSessionData.getLaaApplications());
                boolean isAdmin = userSessionData.getUserTypes().stream().anyMatch(UserType::isAdmin);
                model.addAttribute("isAdminUser", isAdmin);
            } else {
                logger.info("No access token found");
            }
        } catch (Exception e) {
            logger.error("Error getting user list: {}", e.getMessage());
        }
        return "home";
    }

    @GetMapping("/migrate")
    public String migrate() {
        return "migrate";
    }

    @PostMapping("/signout")
    public RedirectView logout(Authentication authentication, HttpSession session, @RegisteredOAuth2AuthorizedClient("azure") OAuth2AuthorizedClient authClient) {
        loginService.logout(authentication, authClient, session);
        return new RedirectView("/?message=logout");
    }

    @ExceptionHandler(Exception.class)
    public RedirectView handleException(Exception ex) {
        logger.error("Error while user login:", ex);
        return new RedirectView("/error");
    }

    @PostMapping("/switchfirm")
    public RedirectView switchFirm(@RequestParam("firmid") String firmId, Authentication authentication,
                                   HttpSession session,
                                   @RegisteredOAuth2AuthorizedClient("azure") OAuth2AuthorizedClient authClient) throws IOException {
        EntraUser user = loginService.getCurrentEntraUser(authentication);
        userService.setDefaultActiveProfile(user, UUID.fromString(firmId));
        loginService.logout(authentication, authClient, session);
        return new RedirectView("/?message=logout");
    }

    @GetMapping("/switchfirm")
    public String userFirmsPage(Model model, Authentication authentication) {
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        List<FirmDto> firmDtoList = firmService.getUserAllFirms(entraUser);
        for (FirmDto firmDto : firmDtoList) {
            UserProfile up = entraUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst().orElse(null);
            if (Objects.nonNull(up) && Objects.nonNull(up.getFirm()) && firmDto.getId().equals(up.getFirm().getId())) {
                firmDto.setName(firmDto.getName() + " - Active");
            }
        }
        model.addAttribute("firmDtoList", firmDtoList);
        return "switch-firm";
    }

}
