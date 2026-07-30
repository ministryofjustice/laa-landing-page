package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserActivationRequestSummaryDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRequestStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;
import uk.gov.justice.laa.portal.landingpage.forms.DelegateReactivateUserReasonForm;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserReactivationActivationRequestService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.justice.laa.portal.landingpage.controller.UserController.buildErrorString;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getObjectFromHttpSession;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class UserActivationController {
    private final LoginService loginService;
    private final UserService userService;
    private final UserReactivationActivationRequestService userReactivationActivationRequestService;

    @Value("${feature.flag.disable.user}")
    public boolean disableUserFeatureEnabled;

    @Value("${feature.flag.edit.user.details}")
    public boolean editUserDetailFeatureEnabled;

    @GetMapping("/user/delegate-reactivate/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserGet(@PathVariable String id,
                                            HttpSession session,
                                            Model model,
                                            String referer,
                                            UUID profileId,
                                            RedirectAttributes redirectAttributes) {
        if (!disableUserFeatureEnabled) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }
        clearSessionAttributes(session);

        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        Optional<UserActivationRequest> request = userReactivationActivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(profileId);
        if (request.isPresent()
                && !(ReactivationRequestStatus.REJECTED.equals(request.get().getStatus())
                || ReactivationRequestStatus.APPROVED.equals(request.get().getStatus()))) {
            redirectAttributes.addFlashAttribute("errorMessage", "A delegate request is already in progress");
            redirectAttributes.addFlashAttribute("requestId", request.get().getId());
            return "redirect:/admin/users/manage/" + profileId;

        }

        model.addAttribute("user", user);
        model.addAttribute("profileId", profileId);
        model.addAttribute("referer", referer);
        session.setAttribute("delegateReactivateUserId", id);
        session.setAttribute("profileId", profileId);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User - " + user.getFullName());
        return "delegate-reactivate-user";
    }

    @PostMapping("/user/delegate-reactivate/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserPost(@PathVariable String id,
                                             Model model,
                                             HttpSession session,
                                             String referer,
                                             UUID profileId) {
        String idFromSession = getObjectFromHttpSession(session, "delegateReactivateUserId", String.class).orElseThrow();
        if (id == null || !id.equals(idFromSession)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }
        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        session.setAttribute("delegateReactivateUserId", id);
        session.setAttribute("profileId", profileId);

        model.addAttribute("user", user);
        model.addAttribute("referer", referer);
        model.addAttribute("profileId", profileId);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User - " + user.getFullName());
        return "redirect:/admin/user/delegate-reactivate-user-reason/" + user.getId();
    }

    @GetMapping("/user/delegate-reactivate-user-reason/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserReasonsGet(@PathVariable String id,
                                                   Model model,
                                                   HttpSession session) {
        if (!disableUserFeatureEnabled) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }

        String idFromSession = getObjectFromHttpSession(session, "delegateReactivateUserId", String.class).orElseThrow();
        if (id == null || !id.equals(idFromSession)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        UUID profileId = getObjectFromHttpSession(session, "profileId", UUID.class).orElseThrow();

        DelegateReactivateUserReasonForm delegateReactivateUserReasonForm =
                getObjectFromHttpSession(session, "delegateReactivateUserReasonForm", DelegateReactivateUserReasonForm.class)
                        .orElse(new DelegateReactivateUserReasonForm());


        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("delegateReactivateUserReasonForm", delegateReactivateUserReasonForm);
        model.addAttribute("profileId", profileId);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User - " + user.getFullName());

        return "delegate-reactivate-user-reason";
    }

    @PostMapping("/user/delegate-reactivate-user-reason/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserReasonsPost(@PathVariable String id,
                                                    @Valid DelegateReactivateUserReasonForm delegateReactivateUserReasonForm,
                                                    BindingResult result,
                                                    Model model,
                                                    HttpSession session) {

        UUID profileId = getObjectFromHttpSession(session, "profileId", UUID.class).orElseThrow();

        if (result.hasErrors()) {
            String errorMessage = buildErrorString(result);
            EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
            model.addAttribute("user", user);
            model.addAttribute("profileId", profileId);
            model.addAttribute("delegateReactivateUserReasonForm", delegateReactivateUserReasonForm);
            model.addAttribute("errorMessage", errorMessage);
            model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User - " + user.getFullName());
            return "delegate-reactivate-user-reason";
        }
        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();

        model.addAttribute("user", user);
        model.addAttribute("profileId", profileId);
        model.addAttribute("delegateReactivateUserReasonForm", delegateReactivateUserReasonForm);
        session.setAttribute("delegateReactivateUserReasonForm", delegateReactivateUserReasonForm);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User - " + user.getFullName());
        return "redirect:/admin/user/delegate-reactivate-user-check-answers/" + user.getId();
    }

    @GetMapping("/user/delegate-reactivate-user-check-answers/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserReasonsCheckAnswersGet(@PathVariable String id,
                                                               Model model,
                                                               HttpSession session) {
        if (!disableUserFeatureEnabled) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }

        String idFromSession = getObjectFromHttpSession(session, "delegateReactivateUserId", String.class).orElseThrow();
        if (id == null || !id.equals(idFromSession)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        UUID profileId = getObjectFromHttpSession(session, "profileId", UUID.class).orElseThrow();
        DelegateReactivateUserReasonForm delegateReactivateUserReasonForm =
                getObjectFromHttpSession(session, "delegateReactivateUserReasonForm", DelegateReactivateUserReasonForm.class)
                        .orElseThrow();


        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("delegateReactivateUserReasonForm", delegateReactivateUserReasonForm);
        model.addAttribute("profileId", profileId);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User - " + user.getFullName());
        return "delegate-reactivate-user-check-answers";
    }

    @PostMapping("/user/delegate-reactivate-user-check-answers/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserReasonsCheckAnswersPost(@PathVariable String id,
                                                                Authentication authentication,
                                                                Model model,
                                                                HttpSession session) {
        if (!disableUserFeatureEnabled) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }

        String idFromSession = getObjectFromHttpSession(session, "delegateReactivateUserId", String.class).orElseThrow();
        if (id == null || !id.equals(idFromSession)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400));
        }

        DelegateReactivateUserReasonForm delegateReactivateUserReasonForm =
                getObjectFromHttpSession(session, "delegateReactivateUserReasonForm", DelegateReactivateUserReasonForm.class)
                        .orElseThrow();

        UUID profileId = getObjectFromHttpSession(session, "profileId", UUID.class).orElseThrow();
        UserProfileDto userProfile = userService.getActiveProfileByUserId(id).orElseThrow();
        if (!userProfile.getId().equals(profileId)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400));
        }

        EntraUser user = loginService.getCurrentEntraUser(authentication);
        CurrentUserDto actor = loginService.getCurrentUser(authentication);

        UserActivationRequest userActivationRequest = userReactivationActivationRequestService
                .createNewRequest(UUID.randomUUID(), profileId, delegateReactivateUserReasonForm.getReason(), user.getEntraOid());
        log.info("A delegate enable user request {} has been raised by {} for {}", userActivationRequest.getRequestId(), actor.getUserId(), id);

        model.addAttribute("user", user);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
        clearSessionAttributes(session);
        return "delegate-reactivate-user-confirmation";
    }

    @GetMapping("/user/delegate-reactivate/track/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String trackDelegateReactivateUserRequestsGet(@PathVariable String id,
                                            HttpSession session,
                                            Model model,
                                            String referer,
                                            UUID profileId,
                                            RedirectAttributes redirectAttributes) {
        if (!disableUserFeatureEnabled) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }

        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        Optional<UserActivationRequest> request = userReactivationActivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(profileId);
        if (request.isPresent()
                &&  (ReactivationRequestStatus.REJECTED.equals(request.get().getStatus())
                || ReactivationRequestStatus.APPROVED.equals(request.get().getStatus()))) {
            redirectAttributes.addFlashAttribute("errorMessage", "A delegate request is already in progress");
            request.ifPresent(userActivationRequest -> redirectAttributes.addFlashAttribute("requestId", userActivationRequest.getId()));
            return "redirect:/admin/users/manage/" + profileId;
        }

        if (request.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No delegate requests present");
            return "redirect:/admin/users/manage/" + profileId;
        }

        List<UserActivationRequestSummaryDto> latestRequestHistoryForUserProfile
                = userReactivationActivationRequestService.getLatestRequestHistoryForUserProfile(profileId);
        DelegateReactivateUserReasonForm delegateReactivateUserReasonForm =
                getObjectFromHttpSession(session, "delegateReactivateUserReasonForm", DelegateReactivateUserReasonForm.class)
                        .orElse(new DelegateReactivateUserReasonForm());

        model.addAttribute("user", user);
        model.addAttribute("profileId", profileId);
        model.addAttribute("referer", referer);
        model.addAttribute("requestId", request.get().getRequestId());
        model.addAttribute("reactivationRequests", latestRequestHistoryForUserProfile);
        model.addAttribute("delegateReactivateUserReasonForm", delegateReactivateUserReasonForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User - " + user.getFullName());
        return "delegate-reactivate-user-tracking";
    }

    @PostMapping("/user/delegate-reactivate/track/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String trackDelegateReactivateUserRequestsPost(
            @PathVariable UUID id,
            @RequestParam UUID profileId,
            @RequestParam UUID requestId,
            @Valid @ModelAttribute("delegateReactivateUserReasonForm") DelegateReactivateUserReasonForm delegateReactivateUserReasonForm,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            String errorMessage = buildErrorString(result);
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            redirectAttributes.addAttribute("id", id);
            redirectAttributes.addAttribute("profileId", profileId);
            redirectAttributes.addAttribute("requestId", requestId);

            return "redirect:/admin/user/delegate-reactivate/track/{id}";
        }

        EntraUser user = loginService.getCurrentEntraUser(authentication);
        CurrentUserDto actor = loginService.getCurrentUser(authentication);

        ReactivationRequestStatus reactivationRequestStatus = userReactivationActivationRequestService.calculateNextReactivationRequestStatus(profileId);
        userReactivationActivationRequestService.saveRequestState(requestId, profileId, reactivationRequestStatus, delegateReactivateUserReasonForm.getReason(), user.getEntraOid());
        log.info("A delegate enable user request {} has been updated by {} for {}", requestId, actor.getUserId(), id);

        redirectAttributes.addAttribute("id", id);
        redirectAttributes.addAttribute("profileId", profileId);
        redirectAttributes.addAttribute("requestId", requestId);

        return "redirect:/admin/user/delegate-reactivate/track/{id}";

    }

    public void clearSessionAttributes(HttpSession session) {
        session.removeAttribute("user");
        session.removeAttribute("delegateReactivateUserId");
        session.removeAttribute("delegateReactivateUserReasonForm");
        session.removeAttribute("profileId");
    }
}
