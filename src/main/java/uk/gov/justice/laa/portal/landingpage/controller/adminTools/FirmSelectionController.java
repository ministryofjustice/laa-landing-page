package uk.gov.justice.laa.portal.landingpage.controller.adminTools;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
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
import uk.gov.justice.laa.portal.landingpage.dto.AddUserProfileAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.environment.AccessGuard;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.forms.MultiFirmUserForm;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.justice.laa.portal.landingpage.service.FirmComparatorByRelevance.relevance;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getObjectFromHttpSession;

/**
 * Multi-firm User Controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).ADMIN_PERMISSIONS)" +
        "or @accessGuard.canDelegate(authentication)")
@RequestMapping("/adminFirmSelection")
public class FirmSelectionController {

    private final UserService userService;

    private final LoginService loginService;

    private final EventService eventService;

    private final ModelMapper mapper;

    private final FirmService firmService;

    @GetMapping("/selectUser")
    public String selectUserGet(Model model, HttpSession session, Authentication authentication) {
        MultiFirmUserForm multiFirmUserForm = getObjectFromHttpSession(session, "multiFirmUserForm",
                MultiFirmUserForm.class).orElse(new MultiFirmUserForm());
        model.addAttribute("multiFirmUserForm", multiFirmUserForm);
        model.addAttribute("email", multiFirmUserForm.getEmail());
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Add profile");

        model.addAttribute("backUrl", "/admin/users");
        return "admin-tools/select-user";
    }

    @PostMapping("/selectUser")
    public String selectUserPost(@Valid MultiFirmUserForm multiFirmUserForm, BindingResult result,
            Model model, HttpSession session, Authentication authentication) {

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while searching for user: {}", result.getAllErrors());
            session.setAttribute("multiFirmUserForm", multiFirmUserForm);
            model.addAttribute("multiFirmUserForm", multiFirmUserForm);
            model.addAttribute("backUrl", "/admin/users");

            return "admin-tools/select-user";
        }

        session.setAttribute("multiFirmUserForm", multiFirmUserForm);

        Optional<EntraUser> entraUserOptional = userService.findEntraUserByEmail(multiFirmUserForm.getEmail());

        if (entraUserOptional.isPresent()) {

            EntraUser entraUser = entraUserOptional.get();

            if (!entraUser.isMultiFirmUser()) {
                log.debug("The user is not a multi firm user: {}.", multiFirmUserForm.getEmail());
                result.rejectValue("email", "error.email",
                        "This user cannot be added at this time. Contact your Contract Manager to check their access permissions.");
                model.addAttribute("multiFirmUserForm", multiFirmUserForm);
                model.addAttribute("backUrl", "/admin/users");

                return "admin-tools/select-user";
            }

            if (entraUser.getUserProfiles() != null && !entraUser.getUserProfiles().isEmpty()) {
                UserProfile authenticatedUserProfile = loginService.getCurrentProfile(authentication);

                String targetFirmIdForSameFirmCheck = (String) session.getAttribute("delegateTargetFirmId");

                Firm compareFirm = targetFirmIdForSameFirmCheck != null
                        ? firmService.getById(UUID.fromString(targetFirmIdForSameFirmCheck))
                        : authenticatedUserProfile.getFirm();

                Optional<UserProfile> sameFirmProfile = entraUser.getUserProfiles().stream()
                        .filter(up -> up.getFirm().equals(compareFirm)).findFirst();

                if (sameFirmProfile.isPresent()) {
                    log.debug(
                            "This user already has access for your firm. Manage them from the Manage Your Users screen.");
                    result.rejectValue("email", "error.email",
                            "This user already has a profile for this firm. You can amend their access from the Manage your users table.");
                    model.addAttribute("backUrl", "/admin/users");
                    return "admin-tools/select-user";
                }

                Firm targetFirm;
                String targetFirmId = (String) session.getAttribute("delegateTargetFirmId");
                if (targetFirmId != null) {
                    targetFirm = firmService.getById(UUID.fromString(targetFirmId));
                } else {
                    targetFirm = authenticatedUserProfile.getFirm();
                }
                for (UserProfile up : entraUser.getUserProfiles()) {
                    Firm existingFirm = up.getFirm();
/* will never be null

Firm existingFirm = up.getFirm();
                    if (existingFirm == null) {
                        continue;
                    }*/
                    if (isAncestor(existingFirm, targetFirm)) {
                        result.rejectValue("email", "error.email", "This user already belongs to a parent firm in this hierarchy and cannot be assigned to a child firm.");
                        String backUrl2 = "/admin/users";
                        model.addAttribute("backUrl", backUrl2);
                        return "admin-tools/select-user";
                    }
                    if (isAncestor(targetFirm, existingFirm)) {
                        result.rejectValue("email", "error.email", "This user already belongs to a child firm in this hierarchy and cannot be assigned to a parent firm.");
                        String backUrl3 = "/admin/users";
                        model.addAttribute("backUrl", backUrl3);
                        return "admin-tools/select-user";
                    }
                }
            }

            EntraUserDto entraUserDto = mapper.map(entraUser, EntraUserDto.class);

            model.addAttribute("entraUser", entraUserDto);
            session.setAttribute("entraUser", entraUserDto);

            model.addAttribute(ModelAttributes.PAGE_TITLE, "Add profile - " + entraUserDto.getFullName());
            return "redirect:/adminFirmSelection/selectFirm";
        } else {
            log.debug("User not found for the given user email: {}", multiFirmUserForm.getEmail());
            result.rejectValue("email", "error.email",
                    "We could not find this user. Try again or ask the Legal Aid Agency to create a new account for them.");
            model.addAttribute("backUrl", "/admin/users");
            return "admin-tools/select-user";
        }
    }

    @GetMapping("/selectFirm")
    public String selectFirmGet(FirmSearchForm firmSearchForm, HttpSession session, Model model,
                                 @RequestParam(value = "firmSearchResultCount", defaultValue = "10") Integer count) {

        Boolean isMultiFirmUser = (Boolean) session.getAttribute("isMultiFirmUser");

        // If firmSearchForm is already populated from session (e.g., validation
        // errors), keep it
        FirmSearchForm existingForm = (FirmSearchForm) session.getAttribute("firmSearchForm");
        if (existingForm != null) {
            firmSearchForm = existingForm;
        } else if (session.getAttribute("firm") != null) {
            // Grab firm search details from session firm if coming here from the
            // confirmation screen.
            FirmDto firm = (FirmDto) session.getAttribute("firm");
            firmSearchForm = FirmSearchForm.builder()
                    .selectedFirmId(firm.getId())
                    .firmSearch(firm.getName())
                    .build();
        }
        int validatedCount = Math.max(10, Math.min(count, 100));
        boolean showSkipFirmSelection = Boolean.TRUE.equals(isMultiFirmUser);
        model.addAttribute("firmSearchForm", firmSearchForm);
        model.addAttribute("firmSearchResultCount", validatedCount);
        model.addAttribute("showSkipFirmSelection", showSkipFirmSelection);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Select firm");
        return "admin-tools/add-user-firm";
    }

    @PostMapping("/selectFirm")
    public String selectFirmPost(@Valid FirmSearchForm firmSearchForm, BindingResult result,
                               HttpSession session, Model model) {
        Boolean isMultiFirmUser = (Boolean) session.getAttribute("isMultiFirmUser");
        boolean showSkipFirmSelection = Boolean.TRUE.equals(isMultiFirmUser);
        model.addAttribute("showSkipFirmSelection", showSkipFirmSelection);

        MultiFirmUserForm multiFirmUserForm = (MultiFirmUserForm) session.getAttribute("multiFirmUserForm");
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while searching for firm: {}", result.getAllErrors());
            // Store the form in session to preserve input on redirect
            session.setAttribute("firmSearchForm", firmSearchForm);
            return "admin-tools/add-user-firm";
        }

        session.removeAttribute("firm");

        if (firmSearchForm.getFirmSearch() != null && !firmSearchForm.getFirmSearch().isBlank()) {
            // Fallback: search by name if no specific firm was selected
            List<FirmDto> firms = firmService.getAllFirmsFromCache();
            FirmDto selectedFirm = firms.stream()
                    .filter(firm -> firm.getName().toLowerCase().contains(firmSearchForm.getFirmSearch().toLowerCase()))
                    .sorted((s1, s2) -> Integer.compare(relevance(s2, firmSearchForm.getFirmSearch()),
                            relevance(s1, firmSearchForm.getFirmSearch())))
                    .findFirst()
                    .orElse(null);

            if (selectedFirm == null) {
                result.rejectValue("firmSearch", "error.firm",
                        "No firm found with that name. Please select from the dropdown.");
                return "admin-tools/add-user-firm";
            }
            firmSearchForm.setFirmSearch(selectedFirm.getName());
            firmSearchForm.setSelectedFirmId(selectedFirm.getId());
            session.setAttribute("firm", selectedFirm);
        }

        //check if the user has the Firm Already Assigned
        if (userService.hasUserFirmAlreadyAssigned(multiFirmUserForm.getEmail(), firmSearchForm.getSelectedFirmId())) {
            result.rejectValue("firmSearch", "error.firm",
                    "User profile already exists for this firm.");
            return "admin-tools/add-user-firm";
        }
        session.setAttribute("firmSearchForm", firmSearchForm);
        session.setAttribute("delegateTargetFirmId", firmSearchForm.getSelectedFirmId().toString());
        return "redirect:/adminFirmSelection/check-answers";
    }
    @GetMapping("/check-answers")
    public String checkAnswerGet(Model model, Authentication authentication, HttpSession session) {
        String targetFirmId = (String) session.getAttribute("delegateTargetFirmId");
        FirmDto firmDto = firmService.getFirm(UUID.fromString(targetFirmId));
        model.addAttribute("firm", firmDto);
        EntraUserDto user = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("externalUser", true);
        model.addAttribute("isMultiFirmUser", true);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Add profile - Check your answers - " + user.getFullName());

        return "admin-tools/add-profile-check-answers";
    }

    @PostMapping("/check-answers")
    public String checkAnswerPost(Authentication authentication, HttpSession session, Model model) {
        EntraUserDto user = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();
        EntraUser entraUser = userService.findEntraUserByEmail(user.getEmail()).get();
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        String targetFirmIdForCreation = (String) session.getAttribute("delegateTargetFirmId");
        FirmDto firmDto = firmService.getFirm(UUID.fromString(targetFirmIdForCreation));

        try {
            //get offices
            List<OfficeDto> offices = entraUser.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
                    .flatMap(profile -> profile.getOffices().stream())
                    .map(office -> mapper.map(office, OfficeDto.class))
                    .toList();
            //get Roles
            List<AppRoleDto> roles = entraUser.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
                    .flatMap(profile -> profile.getAppRoles().stream())
                    .map(role -> mapper.map(role, AppRoleDto.class))
                    .toList();

            UserProfile newUserProfile = userService.addMultiFirmUserProfile(user, firmDto, offices,
                    roles, currentUserDto.getName());

            AddUserProfileAuditEvent addUserProfileAuditEvent = new AddUserProfileAuditEvent(
                    currentUserDto,
                    newUserProfile.getId(),
                    user,
                    firmDto.getId(),
                    "Firm",
                    firmDto.getName());
            eventService.logEvent(addUserProfileAuditEvent);
        } catch (Exception ex) {
            log.error("Error creating new profile for user: {}", user.getFullName(), ex);
            throw ex;
        }

        return "redirect:/adminFirmSelection/confirmation";
    }

    @GetMapping("/confirmation")
    public String confirmation(Model model, HttpSession session) {
        EntraUserDto user = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class)
                .orElse(EntraUserDto.builder().firstName("Unknown").lastName("Unknown").fullName("Unknown Unknown")
                        .build());
        model.addAttribute("user", user);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "User profile created - " + user.getFullName());
        clearSessionAttributes(session);
        return "multi-firm-user/add-profile-confirmation";
    }

    @GetMapping("/cancel")
    public String cancel(HttpSession session) {
        clearSessionAttributes(session);
        return "redirect:/admin/users";
    }

    private void clearSessionAttributes(HttpSession session) {
        session.removeAttribute("entraUser");
        session.removeAttribute("multiFirmUserForm");
        session.removeAttribute("firmSearchForm");
        session.removeAttribute("firm");
        session.removeAttribute("delegateTargetFirmId");
    }

    /**
     * Handle authorization exceptions when user lacks permissions to access
     * specific users
     */
    @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
    public RedirectView handleAuthorizationException(Exception ex, HttpSession session,
            HttpServletRequest request) {
        Object requestedPath = session != null ? session.getAttribute("SPRING_SECURITY_SAVED_REQUEST") : null;
        String uri = request != null ? request.getRequestURI() : "unknown";
        String method = request != null ? request.getMethod() : "unknown";
        String referer = request != null ? request.getHeader("Referer") : null;
        log.warn(
                "Authorization denied while accessing user: reason='{}', method='{}', uri='{}', referer='{}', savedRequest='{}'",
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

    private boolean isAncestor(Firm potentialAncestor, Firm potentialDescendant) {
        if (potentialAncestor == null || potentialDescendant == null) {
            return false;
        }
        UUID ancestorId = potentialAncestor.getId();
        Firm cursor = potentialDescendant.getParentFirm();
        while (cursor != null) {
            if (cursor.getId() != null && cursor.getId().equals(ancestorId)) {
                return true;
            }
            cursor = cursor.getParentFirm();
        }
        return false;
    }

}
