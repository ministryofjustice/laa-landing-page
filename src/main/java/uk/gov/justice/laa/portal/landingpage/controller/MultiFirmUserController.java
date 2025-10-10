package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.CreateUserAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.exception.CreateUserDetailsIncompleteException;
import uk.gov.justice.laa.portal.landingpage.exception.TechServicesClientException;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.forms.MultiFirmUserForm;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.List;
import java.util.Optional;

import static uk.gov.justice.laa.portal.landingpage.service.FirmComparatorByRelevance.relevance;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getObjectFromHttpSession;

/**
 * Multi-firm User Controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/multi-firm")
public class MultiFirmUserController {

    private final LoginService loginService;
    private final UserService userService;
    private final EventService eventService;
    private final FirmService firmService;
    private final ModelMapper mapper;

    @Value("${feature.flag.enable.multi.firm.user}")
    private boolean enableMultiFirmUser;

    @GetMapping("/user/add/profile/before-start")
    public String addUserProfileStart(Model model) {
        return "add-multi-firm-user-profile-start";
    }

    @GetMapping("/user/add/profile")
    public String addUserProfile(Model model, HttpSession session) {

        if (!enableMultiFirmUser) {
            throw new RuntimeException("The page you are trying to access is not available." +
                    "Please contact your system administrator for assistance.");
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
            throw new RuntimeException("The page you are trying to access is not available." +
                    "Please contact your system administrator for assistance.");
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
        return "redirect:/admin/multi-firm/user/add/profile/select/firm";
    }

    @GetMapping("/user/add/profile/select/firm")
    public String selectFirmForMultiFirmUser(FirmSearchForm firmSearchForm, HttpSession session, Model model,
                                             @RequestParam(value = "firmSearchResultCount", defaultValue = "10") Integer count) {
        if (!enableMultiFirmUser) {
            throw new RuntimeException("The page you are trying to access is not available." +
                    "Please contact your system administrator for assistance.");
        }

        EntraUserDto entraUserDto = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();

        if (!entraUserDto.isMultiFirmUser()) {
            log.error("User {} is not a multi firm user, a new profile can not be added.", entraUserDto.getId());
            throw new RuntimeException(String.format("User %s is not a multi firm user, a new profile can not be added.", entraUserDto.getId()));
        }

        // If firmSearchForm is already populated from session (e.g., validation
        // errors), keep it
        FirmSearchForm existingForm = getObjectFromHttpSession(session, "firmSearchForm", FirmSearchForm.class).orElse(null);
        if (existingForm != null) {
            firmSearchForm = existingForm;
        } else if (session.getAttribute("firm") != null) {
            // Grab firm search details from session firm if coming here from the confirmation screen.
            FirmDto firm = (FirmDto) session.getAttribute("firm");
            firmSearchForm = FirmSearchForm.builder()
                    .selectedFirmId(firm.getId())
                    .firmSearch(firm.getName())
                    .build();
        }

        model.addAttribute("enableMultiFirmUser", enableMultiFirmUser);
        model.addAttribute("firmSearchForm", firmSearchForm);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Select firm");
        return "add-profile-firm";
    }

    @PostMapping("/user/add/profile/select/firm")
    public String selectFirmForMultiFirmUserPost(@Valid FirmSearchForm firmSearchForm, BindingResult result, HttpSession session, Model model,
                                                 @RequestParam(value = "firmSearchResultCount", defaultValue = "10") Integer count) {
        if (!enableMultiFirmUser) {
            throw new RuntimeException("The page you are trying to access is not available." +
                    "Please contact your system administrator for assistance.");
        }

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while searching for firm: {}", result.getAllErrors());
            session.setAttribute("firmSearchForm", firmSearchForm);
            model.addAttribute("firmSearchForm", firmSearchForm);
            return "add-profile-firm";
        }

        session.removeAttribute("firm");

        if (firmSearchForm.getSelectedFirmId() != null) {
            try {
                FirmDto selectedFirm = firmService.getFirm(firmSearchForm.getSelectedFirmId());
                selectedFirm.setSkipFirmSelection(firmSearchForm.isSkipFirmSelection());
                session.setAttribute("firm", selectedFirm);
            } catch (Exception e) {
                log.error("Error retrieving selected firm: {}", e.getMessage());
                result.rejectValue("firmSearch", "error.firm", "Invalid firm selection. Please try again.");
                return "add-profile-firm";
            }
        } else if (firmSearchForm.getFirmSearch() != null && !firmSearchForm.getFirmSearch().isBlank()) {
            // Fallback: search by name if no specific firm was selected
            List<FirmDto> firms = firmService.getAllFirmsFromCache();
            FirmDto selectedFirm = firms.stream()
                    .filter(firm -> firm.getName().toLowerCase().contains(firmSearchForm.getFirmSearch().toLowerCase()))
                    .sorted((s1, s2) ->
                            Integer.compare(relevance(s2, firmSearchForm.getFirmSearch()), relevance(s1, firmSearchForm.getFirmSearch())))
                    .findFirst()
                    .orElse(null);

            if (selectedFirm == null) {
                result.rejectValue("firmSearch", "error.firm",
                        "No firm found with that name. Please select from the dropdown.");
                return "add-profile-firm";
            }
            firmSearchForm.setFirmSearch(selectedFirm.getName());
            firmSearchForm.setSelectedFirmId(selectedFirm.getId());
            session.setAttribute("firm", selectedFirm);
        }

        session.setAttribute("firmSearchForm", firmSearchForm);
        return "redirect:/admin/multi-firm/user/add/profile/check-answers";
    }

    @GetMapping("/user/add/profile/check-answers")
    public String addProfileCheckAnswers(Model model, HttpSession session) {
        {
            if (!enableMultiFirmUser) {
                throw new RuntimeException("The page you are trying to access is not available." +
                        "Please contact your system administrator for assistance.");
            }
            EntraUserDto user = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class)
                    .orElseThrow(CreateUserDetailsIncompleteException::new);
            session.setAttribute("user", user);
            model.addAttribute("user", user);

            FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
            model.addAttribute("firm", selectedFirm);

            model.addAttribute("isUserManager", true);

            Boolean isMultiFirmUser = (Boolean) session.getAttribute("isMultiFirmUser");
            model.addAttribute("isMultiFirmUser", true);

            // Add feature flag to control multi-firm UI display
            model.addAttribute("enableMultiFirmUser", enableMultiFirmUser);

            model.addAttribute(ModelAttributes.PAGE_TITLE, "Check your answers");
            return "add-profile-check-answers";
        }

    }

    @PostMapping("/user/add/profile/check-answers")
    public String addUserProfileCheckAnswers(HttpSession session, Authentication authentication, Model model) {
        if (!enableMultiFirmUser) {
            throw new RuntimeException("The page you are trying to access is not available." +
                    "Please contact your system administrator for assistance.");
        }
        Optional<EntraUserDto> userOptional = getObjectFromHttpSession(session, "user", EntraUserDto.class);
        Optional<FirmDto> firmOptional = getObjectFromHttpSession(session, "firm", FirmDto.class);

        if (userOptional.isPresent()) {
            EntraUserDto user = userOptional.get();
            FirmDto selectedFirm = firmOptional.orElseThrow();
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            try {
                UserProfile userProfile = userService.addMultiFirmUserProfile(user, selectedFirm, currentUserDto.getName());

                session.setAttribute("userProfile", mapper.map(userProfile, UserProfileDto.class));

                CreateUserAuditEvent createUserAuditEvent = new CreateUserAuditEvent(currentUserDto, userProfile.getEntraUser(),
                        "(Multi-firm user)", true);
                eventService.logEvent(createUserAuditEvent);
            } catch (TechServicesClientException techServicesClientException) {
                log.debug("Error creating user: {}", techServicesClientException.getMessage());
                model.addAttribute("errorMessage", techServicesClientException.getMessage());
                model.addAttribute("errors", techServicesClientException.getErrors());

                model.addAttribute("user", user);
                model.addAttribute("firm", selectedFirm);
                boolean isUserManager = (boolean) session.getAttribute("isUserManager");
                model.addAttribute("isUserManager", isUserManager);
                model.addAttribute(ModelAttributes.PAGE_TITLE, "Check your answers");
                return "add-profile-check-answers";
            }

        } else {
            log.error("No user attribute was present in request. User not created.");
        }

        session.removeAttribute("firm");
        session.removeAttribute("selectedUserType");

        return "redirect:/admin/multi-firm/user/add/profile/confirmation";
    }

    @GetMapping("/user/add/profile/confirmation")
    public String addUserProfileCreated(Model model, HttpSession session) {
        if (!enableMultiFirmUser) {
            throw new RuntimeException("The page you are trying to access is not available." +
                    "Please contact your system administrator for assistance.");
        }
        Optional<EntraUserDto> userOptional = getObjectFromHttpSession(session, "user", EntraUserDto.class);
        Optional<UserProfileDto> userProfileOptional = getObjectFromHttpSession(session, "userProfile",
                UserProfileDto.class);

        // Get multi-firm user flag from session
        Boolean isMultiFirmUser = (Boolean) session.getAttribute("isMultiFirmUser");

        if (userOptional.isPresent()) {
            EntraUserDto user = userOptional.get();
            model.addAttribute("user", user);

            // Multi-firm users don't have a user profile - only entra_user entry
            // Regular users have a user profile with firm assignment
            if (Boolean.TRUE.equals(isMultiFirmUser)) {
                // No user profile for multi-firm users
                model.addAttribute("userProfile", null);
            } else {
                // Regular user should have a profile
                if (userProfileOptional.isPresent()) {
                    model.addAttribute("userProfile", userProfileOptional.get());
                } else {
                    log.error("No userProfile attribute was present in request for non-multi-firm user.");
                }
            }
        } else {
            log.error("No user attribute was present in request. User not added to model.");
        }

        model.addAttribute("isMultiFirmUser", isMultiFirmUser != null ? isMultiFirmUser : false);

        // Add feature flag to control multi-firm UI display
        model.addAttribute("enableMultiFirmUser", enableMultiFirmUser);

        session.removeAttribute("user");
        session.removeAttribute("userProfile");
        session.removeAttribute("isMultiFirmUser");
        model.addAttribute(ModelAttributes.PAGE_TITLE, "User created");
        return "add-profile-created";
    }

    @GetMapping("/user/create/cancel")
    public String cancelUserCreation(HttpSession session) {
        session.removeAttribute("user");
        session.removeAttribute("firm");
        session.removeAttribute("selectedUserType");
        session.removeAttribute("isFirmAdmin");
        session.removeAttribute("isMultiFirmUser");
        session.removeAttribute("multiFirmForm");
        session.removeAttribute("apps");
        session.removeAttribute("roles");
        session.removeAttribute("officeData");
        session.removeAttribute("firmSearchForm");
        session.removeAttribute("firmSearchTerm");
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
