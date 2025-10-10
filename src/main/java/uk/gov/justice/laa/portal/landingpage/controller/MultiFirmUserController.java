package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.forms.MultiFirmUserForm;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getObjectFromHttpSession;

/**
 * Multi-firm User Controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/multi-firm")
public class MultiFirmUserController {

    private final UserService userService;

    @Value("${feature.flag.enable.multi.firm.user}")
    private boolean enableMultiFirmUser;

    @GetMapping("/user/add/profile/before-start")
    public String addUserProfileStart() {
        if (!enableMultiFirmUser) {
            throw new RuntimeException("The page you are trying to access is not available."
                    + "Please contact your system administrator for assistance.");
        }

        return "add-multi-firm-user-profile-start";
    }

    @GetMapping("/user/add/profile")
    public String addUserProfile(Model model, HttpSession session) {

        if (!enableMultiFirmUser) {
            throw new RuntimeException("The page you are trying to access is not available."
                    + "Please contact your system administrator for assistance.");
        }

        MultiFirmUserForm multiFirmUserForm =
                getObjectFromHttpSession(session, "multiFirmUserForm", MultiFirmUserForm.class).orElse(new MultiFirmUserForm());
        model.addAttribute("multiFirmUserForm", multiFirmUserForm);
        session.setAttribute("multiFirmUserForm", multiFirmUserForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Add profile");
        return "add-multi-firm-user-profile";
    }

    @PostMapping("/user/add/profile")
    public String addUserProfilePost(@Valid MultiFirmUserForm multiFirmUserForm, BindingResult result, Model model, HttpSession session) {

        if (!enableMultiFirmUser) {
            throw new RuntimeException("The page you are trying to access is not available."
                    + "Please contact your system administrator for assistance.");
        }

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while searching for user: {}", result.getAllErrors());
            session.setAttribute("multiFirmUserForm", multiFirmUserForm);
            model.addAttribute("multiFirmUserForm", multiFirmUserForm);
            return "add-multi-firm-user-profile";
        }

        EntraUserDto entraUserDto = userService.getEntraUserByEmail(multiFirmUserForm.getEmail()).orElse(null);

        if (entraUserDto == null) {
            log.debug("No user present with the email provided: {}.", multiFirmUserForm.getEmail());
            result.rejectValue("email", "error.email", "No user present with the email provided.");
            return "add-multi-firm-user-profile";
        } else if (!entraUserDto.isMultiFirmUser()) {
            log.debug("The user is not a multi firm user: {}.", multiFirmUserForm.getEmail());
            result.rejectValue("email", "error.email", "The user is not a multi firm user.");
            return "add-multi-firm-user-profile";
        }

        model.addAttribute("entraUser", entraUserDto);
        session.setAttribute("entraUser", entraUserDto);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Add profile - " + entraUserDto.getFullName());

        // TODO: Delete when next page implemented - Start
        return "redirect:/admin/users";
        // TODO: Delete when next page implemented - End
        // TODO: Uncomment the line below
        //return "redirect:/admin/multi-firm/user/add/profile/select/apps";
    }


    @GetMapping("/user/create/cancel")
    public String cancelUserCreation(HttpSession session) {
        session.removeAttribute("entraUser");
        session.removeAttribute("multiFirmUserForm");
        return "redirect:/admin/users";
    }

    /**
     * Handle authorization exceptions when user lacks permissions to access
     * specific users
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public RedirectView handleAuthorizationException(Exception ex, HttpSession session,
                                                     HttpServletRequest request) {
        Object requestedPath = session != null ? session.getAttribute("SPRING_SECURITY_SAVED_REQUEST") : null;
        String uri = request != null ? request.getRequestURI() : "unknown";
        String method = request != null ? request.getMethod() : "unknown";
        String referer = request != null ? request.getHeader("Referer") : null;
        log.warn("Authorization denied while accessing user: reason='{}', method='{}', uri='{}', referer='{}', savedRequest='{}'",
                ex.getMessage(), method, uri, referer, requestedPath);
        return new RedirectView("/not-authorised");
    }

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception.class)
    public RedirectView handleException(Exception ex) {
        log.error("Error while user management", ex);
        return new RedirectView("/error");
    }

}
