package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.audit.ReactivateUserRequestApprovedAuditEvent;
import uk.gov.justice.laa.portal.landingpage.audit.ReactivateUserRequestRejectedAuditEvent;
import uk.gov.justice.laa.portal.landingpage.audit.ReactivateUserRequestSubmittedAuditEvent;
import uk.gov.justice.laa.portal.landingpage.audit.ReactivateUserRequestUpdatedAuditEvent;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import static uk.gov.justice.laa.portal.landingpage.controller.UserController.buildErrorString;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.ReactivationRequestsPageData;
import uk.gov.justice.laa.portal.landingpage.dto.UserActivationRequestSummaryDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;
import uk.gov.justice.laa.portal.landingpage.forms.DelegateReactivateUserCommentForm;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestStatus;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;
import uk.gov.justice.laa.portal.landingpage.service.UserReactivationRequestService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getObjectFromHttpSession;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class UserActivationController {
    private final LoginService loginService;
    private final UserService userService;
    private final UserReactivationRequestService userReactivationRequestService;
    private final AccessControlService accessControlService;
    private final UserAccountStatusService userAccountStatusService;
    private final EventService eventService;

    @Value("${feature.flag.delegate.user.activation}")
    public boolean delegateUserActivationFeatureEnabled;

    @GetMapping("/user/delegate-reactivate/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserGet(@PathVariable String id,
                                            HttpSession session,
                                            Model model,
                                            @RequestParam(required = false) String profileId,
                                            @RequestParam String referer,
                                            Authentication authentication,
                                            RedirectAttributes redirectAttributes) {
        log.info("Initiating delegate reactivate GET flow for id: {} and profileId: {}", id, profileId);

        if (!delegateUserActivationFeatureEnabled) {
            log.info("Delegate reactivate feature disabled. Throwing 404 for userId: {}", id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }
        clearSessionAttributes(session);

        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();

        if (!userService.isValidUserProfileId(id, profileId)) {
            log.info("Invalid access to reactivate page for profileId: {} for userId: {}", profileId, id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        Optional<UserActivationRequest> request = userReactivationRequestService.findFirstByUserEntraIdOrderByCreatedAtDescVersionDesc(id);
        if (request.isPresent()
                && !(ReactivationRequestStatus.REJECTED.equals(request.get().getStatus())
                || ReactivationRequestStatus.APPROVED.equals(request.get().getStatus()))) {
            log.info("Delegate reactivation request already in progress for id: {}, profileId: {}, requestId: {}", id, profileId, request.get().getId());
            redirectAttributes.addFlashAttribute("errorMessage", "A delegate request is already in progress");
            redirectAttributes.addFlashAttribute("requestId", request.get().getId().toString());
            return "redirect:" + getCancelPathFromReferer(referer, id, profileId);
        }

        EntraUser currentEntraUser = loginService.getCurrentEntraUser(authentication);
        boolean isInternalActor = userService.isInternal(currentEntraUser.getId());

        model.addAttribute("user", user);
        model.addAttribute("profileId", profileId);
        model.addAttribute("referer", referer);
        model.addAttribute("isInternalActor", isInternalActor);
        model.addAttribute("cancelPath", getCancelPathFromReferer(referer, id, profileId));
        session.setAttribute("delegateReactivateUserId", id);
        session.setAttribute("profileId", profileId);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
        return "delegate-reactivate-user";
    }

    @PostMapping("/user/delegate-reactivate/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserPost(@PathVariable String id,
                                             Model model,
                                             HttpSession session,
                                             @RequestParam(required = false) String profileId,
                                             @RequestParam String referer,
                                             RedirectAttributes redirectAttributes) {
        log.info("Processing delegate reactivate POST for id: {} and profileId: {}", id, profileId);

        String idFromSession = getObjectFromHttpSession(session, "delegateReactivateUserId", String.class).orElseThrow();
        if (id == null || !id.equals(idFromSession)) {
            log.info("Session validation failed in delegateReactivateUserPost. Path ID: {}, Session ID: {}", id, idFromSession);
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        if (!userService.isValidUserProfileId(id, profileId)) {
            log.info("Invalid submit to reactivate page for id: {} profileId: {} for userId: {}", id, profileId, id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        session.setAttribute("delegateReactivateUserId", id);
        session.setAttribute("profileId", profileId);

        redirectAttributes.addAttribute("profileId", profileId);
        redirectAttributes.addAttribute("referer", referer);

        return "redirect:/admin/user/delegate-reactivate-user-comment/" + id;
    }

    @GetMapping("/user/delegate-reactivate-user-comment/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserCommentsGet(@PathVariable String id,
                                                    Model model,
                                                    HttpSession session,
                                                    @RequestParam(required = false) String profileId,
                                                    @RequestParam String referer) {
        log.info("Rendering delegate reactivate comment view for userId: {}", id);

        if (!delegateUserActivationFeatureEnabled) {
            log.info("Delegate reactivate feature disabled. Throwing 404 for userId: {}", id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }

        String idFromSession = getObjectFromHttpSession(session, "delegateReactivateUserId", String.class).orElseThrow();
        if (id == null || !id.equals(idFromSession)) {
            log.info("Session mismatch on delegateReactivateUserCommentsGet. Path ID: {}, Session ID: {}", id, idFromSession);
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        DelegateReactivateUserCommentForm delegateReactivateUserCommentForm =
                getObjectFromHttpSession(session, "delegateReactivateUserCommentForm", DelegateReactivateUserCommentForm.class)
                        .orElse(new DelegateReactivateUserCommentForm());

        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("delegateReactivateUserCommentForm", delegateReactivateUserCommentForm);
        model.addAttribute("profileId", profileId);
        model.addAttribute("referer", referer);
        model.addAttribute("cancelPath", getCancelPathFromReferer(referer, id, profileId));
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");

        return "delegate-reactivate-user-comment";
    }

    @PostMapping("/user/delegate-reactivate-user-comment/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserCommentsPost(@PathVariable String id,
                                                     @Valid DelegateReactivateUserCommentForm delegateReactivateUserCommentForm,
                                                     BindingResult result,
                                                     Model model,
                                                     HttpSession session,
                                                     @RequestParam(required = false) String profileId,
                                                     @RequestParam String referer,
                                                     RedirectAttributes redirectAttributes) {
        log.info("Processing delegate reactivate comments POST for userId: {}", id);

        if (result.hasErrors()) {
            log.info("Validation errors encountered on comment form for userId: {}", id);
            String errorMessage = buildErrorString(result);
            EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
            model.addAttribute("user", user);
            model.addAttribute("profileId", profileId);
            model.addAttribute("referer", referer);
            model.addAttribute("cancelPath", getCancelPathFromReferer(referer, id, profileId));
            model.addAttribute("delegateReactivateUserCommentForm", delegateReactivateUserCommentForm);
            model.addAttribute("errorMessage", errorMessage);
            model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
            return "delegate-reactivate-user-comment";
        }

        if (!userService.isValidUserProfileId(id, profileId)) {
            log.info("Invalid access to reactivate comments page for id: {}, profileId: {} for userId: {}", id, profileId, id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        redirectAttributes.addAttribute("profileId", profileId);
        redirectAttributes.addAttribute("referer", referer);
        redirectAttributes.addFlashAttribute("delegateReactivateUserCommentForm", delegateReactivateUserCommentForm);
        session.setAttribute("delegateReactivateUserCommentForm", delegateReactivateUserCommentForm);
        redirectAttributes.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");

        return "redirect:/admin/user/delegate-reactivate-user-check-answers/" + id;
    }

    @GetMapping("/user/delegate-reactivate-user-check-answers/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserCommentsCheckAnswersGet(@PathVariable String id,
                                                                Model model,
                                                                HttpSession session,
                                                                @RequestParam(required = false) String profileId,
                                                                @RequestParam String referer) {

        if (getObjectFromHttpSession(session, "delegateReactivateUserId", String.class).isEmpty()) {
            return "journey-completed";
        }

        log.info("Rendering check-answers step for userId: {}", id);

        if (!delegateUserActivationFeatureEnabled) {
            log.info("Delegate reactivate feature disabled. Throwing 404 for userId: {}", id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }

        String idFromSession = getObjectFromHttpSession(session, "delegateReactivateUserId", String.class).orElseThrow();
        if (id == null || !id.equals(idFromSession)) {
            log.info("Session mismatch in check-answers view for userId: {}", id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        DelegateReactivateUserCommentForm delegateReactivateUserCommentForm =
                getObjectFromHttpSession(session, "delegateReactivateUserCommentForm", DelegateReactivateUserCommentForm.class)
                        .orElseThrow();

        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("delegateReactivateUserCommentForm", delegateReactivateUserCommentForm);
        model.addAttribute("profileId", profileId);
        model.addAttribute("referer", referer);
        model.addAttribute("cancelPath", getCancelPathFromReferer(referer, id, profileId));
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
        return "delegate-reactivate-user-check-answers";
    }

    @PostMapping("/user/delegate-reactivate-user-check-answers/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserCommentsCheckAnswersPost(@PathVariable String id,
                                                                 Authentication authentication,
                                                                 Model model,
                                                                 HttpSession session,
                                                                 @RequestParam(required = false) String profileId,
                                                                 @RequestParam String referer) {
        log.info("Submitting delegate reactivate request for userId: {}", id);

        if (!delegateUserActivationFeatureEnabled) {
            log.info("Delegate reactivate feature disabled. Throwing 404 for userId: {}", id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }

        String idFromSession = getObjectFromHttpSession(session, "delegateReactivateUserId", String.class).orElseThrow();
        if (id == null || !id.equals(idFromSession)) {
            log.info("Bad Request: Path ID {} does not match session ID {}", id, idFromSession);
            throw new ResponseStatusException(HttpStatusCode.valueOf(400));
        }

        DelegateReactivateUserCommentForm delegateReactivateUserCommentForm =
                getObjectFromHttpSession(session, "delegateReactivateUserCommentForm", DelegateReactivateUserCommentForm.class)
                        .orElseThrow();

        if (!userService.isValidUserProfileId(id, profileId)) {
            log.info("Invalid access to track reactivate page for profileId: {} for userId: {}", profileId, id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        EntraUser user = loginService.getCurrentEntraUser(authentication);
        CurrentUserDto actor = loginService.getCurrentUser(authentication);
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);

        UserActivationRequest userActivationRequest = userReactivationRequestService
                .createReactivationRequest(id, profileId, delegateReactivateUserCommentForm.getComment(), user.getEntraOid());
        log.info("A delegate enable user request {} has been raised by {} for {}", userActivationRequest.getRequestId(), actor.getUserId(), id);

        ReactivateUserRequestSubmittedAuditEvent event = new ReactivateUserRequestSubmittedAuditEvent(currentUserDto, id, profileId);
        eventService.logEvent(event);

        model.addAttribute("user", user);
        model.addAttribute("profileId", profileId);
        model.addAttribute("referer", referer);
        model.addAttribute("cancelPath", getCancelPathFromReferer(referer, id, profileId));
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
        clearSessionAttributes(session);
        return "delegate-reactivate-user-confirmation";
    }

    @GetMapping("/user/delegate-reactivate/track/{id}")
    @PreAuthorize("@accessControlService.canTrackDelegateEnableUser(#id, #requestId)")
    public String trackDelegateReactivateUserRequestsGet(@PathVariable String id,
                                                         HttpSession session,
                                                         Model model,
                                                         @RequestParam(required = false) String profileId,
                                                         @RequestParam String referer,
                                                         @RequestParam String requestId,
                                                         RedirectAttributes redirectAttributes) {
        log.debug("Tracking delegate reactivate requests for userId: {}, profileId: {}", id, profileId);

        if (!delegateUserActivationFeatureEnabled) {
            log.info("Delegate reactivate feature disabled. Throwing 404 for userId: {}", id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }

        final EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        Optional<UserActivationRequest> request = userReactivationRequestService.findFirstByUserEntraIdAndRequestIdOrderByVersionDesc(id, requestId);

        if (!userService.isValidUserProfileId(id, profileId)) {
            log.info("Invalid access to track reactivate page for profileId: {} for userId: {}", profileId, id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        if (request.isEmpty()) {
            log.info("No delegate activation request found to track for user id: {}, request id: {}", id, requestId);
            redirectAttributes.addFlashAttribute("errorMessage", "There is no open delegate activation request");
            return "redirect:" + getCancelPathFromReferer(referer, id, profileId);
        }

        if (!model.containsAttribute("delegateReactivateUserCommentForm")) {
            DelegateReactivateUserCommentForm form = getObjectFromHttpSession(session, "delegateReactivateUserCommentForm", DelegateReactivateUserCommentForm.class)
                    .orElse(new DelegateReactivateUserCommentForm());
            model.addAttribute("delegateReactivateUserCommentForm", form);
        }

        boolean isRequestClosed = ReactivationRequestStatus.APPROVED.equals(request.get().getStatus())
                || ReactivationRequestStatus.REJECTED.equals(request.get().getStatus());
        boolean canActionDelegateEnableUser = !isRequestClosed && accessControlService.canManageDelegateEnableUser(id);

        model.addAttribute("isRequestClosed", isRequestClosed);
        model.addAttribute("canActionDelegateEnableUser", canActionDelegateEnableUser);
        model.addAttribute("user", user);
        model.addAttribute("profileId", profileId);
        model.addAttribute("referer", referer);
        model.addAttribute("cancelPath", getCancelPathFromReferer(referer, id, profileId));
        model.addAttribute("requestId", request.get().getRequestId().toString());
        model.addAttribute("requestCurrentStatus", request.get().getStatus());

        List<UserActivationRequestSummaryDto> latestRequestHistoryForUser
                = userReactivationRequestService.getRequestHistoryForUserIdAndRequestId(id, requestId);
        model.addAttribute("reactivationRequests", latestRequestHistoryForUser);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
        return "delegate-reactivate-user-tracking";
    }

    @PostMapping("/user/delegate-reactivate/track/{id}")
    @PreAuthorize("@accessControlService.canTrackDelegateEnableUser(#id, #requestId)")
    public String trackDelegateReactivateUserRequestsPost(
            @PathVariable String id,
            @RequestParam(required = false) String profileId,
            @RequestParam String requestId,
            @RequestParam String referer,
            @Valid @ModelAttribute("delegateReactivateUserCommentForm") DelegateReactivateUserCommentForm delegateReactivateUserCommentForm,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        log.debug("Updating tracking info for request ID: {}, ID: {}", requestId, id);

        if (bindingResult.hasErrors()) {
            log.info("Validation errors encountered while updating tracking for requestId: {}", requestId);
            String errorMessage = buildErrorString(bindingResult);

            redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "delegateReactivateUserCommentForm", bindingResult);
            redirectAttributes.addFlashAttribute("delegateReactivateUserCommentForm", delegateReactivateUserCommentForm);
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);

            redirectAttributes.addAttribute("id", id);
            redirectAttributes.addAttribute("profileId", profileId);
            redirectAttributes.addAttribute("requestId", requestId);
            redirectAttributes.addAttribute("referer", referer);
            redirectAttributes.addAttribute("cancelPath", getCancelPathFromReferer(referer, id, profileId));

            return "redirect:/admin/user/delegate-reactivate/track/{id}";
        }

        EntraUser user = loginService.getCurrentEntraUser(authentication);
        CurrentUserDto actor = loginService.getCurrentUser(authentication);

        UserActivationRequest request = userReactivationRequestService.updateReactivateRequestState(requestId, id, profileId,
                delegateReactivateUserCommentForm.getComment(), user.getEntraOid());
        log.info("A delegate enable user request {} has been updated by {} for {}", requestId, actor.getUserId(), id);

        String activity = ReactivationRequestStatus.IN_REVIEW.equals(request.getStatus()) ? "information provided" : "information requested";
        ReactivateUserRequestUpdatedAuditEvent event = new ReactivateUserRequestUpdatedAuditEvent(actor, id, profileId, activity, request.getComments());
        eventService.logEvent(event);

        redirectAttributes.addAttribute("id", id);
        redirectAttributes.addAttribute("profileId", profileId);
        redirectAttributes.addAttribute("requestId", requestId);
        redirectAttributes.addAttribute("referer", referer);
        redirectAttributes.addAttribute("cancelPath", getCancelPathFromReferer(referer, id, profileId));

        return "redirect:/admin/user/delegate-reactivate/track/{id}";
    }

    @GetMapping("/user/delegate-reactivate/reject/{id}")
    @PreAuthorize("@accessControlService.canManageDelegateEnableUser(#id)")
    public String rejectDelegateReactivateUserRequestsGet(@PathVariable String id,
                                                          HttpSession session,
                                                          Model model,
                                                          @RequestParam(required = false) String profileId,
                                                          @RequestParam String referer,
                                                          @RequestParam String requestId,
                                                          RedirectAttributes redirectAttributes) {
        log.info("Rendering rejection form for userId: {}, profileId: {}", id, profileId);

        if (!delegateUserActivationFeatureEnabled) {
            log.info("Delegate reactivate feature disabled. Throwing 404 for userId: {}", id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }

        if (!userService.isValidUserProfileId(id, profileId)) {
            log.info("Invalid access to reject reactivate page for profileId: {} for userId: {}", profileId, id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        Optional<UserActivationRequest> request = userReactivationRequestService.findFirstByUserEntraIdAndRequestIdOrderByVersionDesc(id, requestId);

        if (request.isEmpty()) {
            log.info("No delegate activation request found to reject for userId: {}", id);
            redirectAttributes.addFlashAttribute("errorMessage", "There is no open delegate activation request");
            return "redirect:" + getCancelPathFromReferer(referer, id, profileId);
        } else if (!request.get().getRequestId().equals(UUID.fromString(requestId))) {
            log.info("Rendering rejection form for requestId: {}, userId: {}", requestId, id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(400));
        } else if (ReactivationRequestStatus.APPROVED.equals(request.get().getStatus()) || ReactivationRequestStatus.REJECTED.equals(request.get().getStatus())) {
            log.info("Delegate activation request for userId: {} is already in status: {}", id, request.get().getStatus());
            redirectAttributes.addFlashAttribute("errorMessage", "There is no open delegate activation request");
            request.ifPresent(userActivationRequest -> redirectAttributes.addFlashAttribute("requestId", userActivationRequest.getId().toString()));
            return "redirect:" + getCancelPathFromReferer(referer, id, profileId);
        }

        if (!model.containsAttribute("delegateReactivateUserCommentForm")) {
            DelegateReactivateUserCommentForm form = getObjectFromHttpSession(session, "delegateReactivateUserCommentForm", DelegateReactivateUserCommentForm.class)
                    .orElse(new DelegateReactivateUserCommentForm());
            model.addAttribute("delegateReactivateUserCommentForm", form);
        }

        model.addAttribute("user", user);
        model.addAttribute("id", id);
        model.addAttribute("profileId", profileId);
        model.addAttribute("requestId", request.get().getRequestId().toString());
        model.addAttribute("referer", referer);
        model.addAttribute("cancelPath", getCancelPathFromReferer(referer, id, profileId));

        List<UserActivationRequestSummaryDto> latestRequestHistoryForUser
                = userReactivationRequestService.getRequestHistoryForUserIdAndRequestId(id, requestId);
        model.addAttribute("reactivationRequests", latestRequestHistoryForUser);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
        return "delegate-reactivate-user-rejection";
    }

    @PostMapping("/user/delegate-reactivate/reject/{id}")
    @PreAuthorize("@accessControlService.canManageDelegateEnableUser(#id)")
    public String rejectDelegateReactivateUserRequestsPost(
            @PathVariable String id,
            HttpSession session,
            Model model,
            @RequestParam(required = false) String profileId,
            @RequestParam String requestId,
            @RequestParam String referer,
            @Valid @ModelAttribute("delegateReactivateUserCommentForm") DelegateReactivateUserCommentForm delegateReactivateUserCommentForm,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        log.info("Processing rejection POST for requestId: {}, userId: {}", requestId, id);

        if (bindingResult.hasErrors()) {
            log.info("Validation errors during rejection of requestId: {}", requestId);
            String errorMessage = buildErrorString(bindingResult);

            redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "delegateReactivateUserCommentForm", bindingResult);
            redirectAttributes.addFlashAttribute("delegateReactivateUserCommentForm", delegateReactivateUserCommentForm);
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);

            redirectAttributes.addAttribute("id", id);
            redirectAttributes.addAttribute("profileId", profileId);
            redirectAttributes.addAttribute("requestId", requestId);
            redirectAttributes.addAttribute("referer", referer);
            redirectAttributes.addAttribute("cancelPath", getCancelPathFromReferer(referer, id, profileId));
            return "redirect:/admin/user/delegate-reactivate/reject/{id}";
        }

        EntraUser user = loginService.getCurrentEntraUser(authentication);
        CurrentUserDto actor = loginService.getCurrentUser(authentication);

        userReactivationRequestService.rejectReactivationRequest(requestId, id, profileId, delegateReactivateUserCommentForm.getComment(), user.getEntraOid());
        log.info("A delegate enable user request {} has been rejected by {} for {}", requestId, actor.getUserId(), id);

        ReactivateUserRequestRejectedAuditEvent event = new ReactivateUserRequestRejectedAuditEvent(actor, id, profileId, delegateReactivateUserCommentForm.getComment());
        eventService.logEvent(event);

        clearSessionAttributes(session);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");

        EntraUserDto targetUser = userService.getEntraUserById(id).orElseThrow();
        model.addAttribute("userName", targetUser.getFullName());

        return "delegate-reactivate-user-reject-confirmation";
    }

    @PostMapping("/user/delegate-reactivate/approve/{id}")
    @PreAuthorize("@accessControlService.canManageDelegateEnableUser(#id)")
    public String approveDelegateReactivateUserRequestsPost(
            @PathVariable String id,
            HttpSession session,
            Model model,
            @RequestParam(required = false) String profileId,
            @RequestParam String requestId,
            Authentication authentication) {

        log.info("Processing approval POST for requestId: {}, userId: {}", requestId, id);

        EntraUser user = loginService.getCurrentEntraUser(authentication);
        CurrentUserDto actor = loginService.getCurrentUser(authentication);

        userReactivationRequestService.approveReactivationRequest(requestId, id, profileId, user.getEntraOid());
        log.info("A delegate enable user request {} has been approved by {} for {}", requestId, actor.getUserId(), id);

        ReactivateUserRequestApprovedAuditEvent event = new ReactivateUserRequestApprovedAuditEvent(actor, id, profileId);
        eventService.logEvent(event);

        userAccountStatusService.enableUser(UUID.fromString(id), user.getId());
        log.info("User {} has been enabled successfully by {}", id, user.getId());

        clearSessionAttributes(session);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");

        EntraUserDto targetUser = userService.getEntraUserById(id).orElseThrow();
        model.addAttribute("userName", targetUser.getFullName());

        return "delegate-reactivate-user-approve-confirmation";
    }

    @GetMapping("/users/reactivation-requests")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_EXTERNAL_USER)")
    public String displayReactivationRequests(
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "sort", defaultValue = "dateSubmitted") String sort,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "selectedRequestStatuses", required = false) List<ReactivationRequestStatus> selectedRequestStatuses,
            @RequestParam(name = "showFirmAdmins", required = false) boolean showFirmAdmins,
            @RequestParam(name = "showMultiFirmUsers", required = false) boolean showMultiFirmUsers,
            @RequestParam(name = "showProviderUsers", required = false) boolean showProviderUsers,
            @RequestParam(name = "defaultStatusApplied", defaultValue = "false") boolean defaultStatusApplied,
            Model model,
            Authentication authentication) {

        log.debug("Fetching reactivation requests list. Page: {}, Size: {}, Search: '{}'", page, size, search);

        var pageMode = userReactivationRequestService.getPageMode(authentication);

        // Stamp the default status into the URL once so the default is explicit and user-clearable.
        if (!defaultStatusApplied && (selectedRequestStatuses == null || selectedRequestStatuses.isEmpty())) {
            log.debug("Applying default status filter IN_REVIEW for redirect.");
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/users/reactivation-requests")
                    .queryParam("size", size)
                    .queryParam("page", page)
                    .queryParam("sort", sort)
                    .queryParam("direction", direction)
                    .queryParam("defaultStatusApplied", true)
                    .queryParam("selectedRequestStatuses", ReactivationRequestStatus.IN_REVIEW.name());

            if (showFirmAdmins) {
                builder.queryParam("showFirmAdmins", true);
            }
            if (showMultiFirmUsers) {
                builder.queryParam("showMultiFirmUsers", true);
            }
            if (showProviderUsers) {
                builder.queryParam("showProviderUsers", true);
            }

            if (search != null && !search.trim().isEmpty()) {
                builder.queryParam("search", search.trim());
            }

            return "redirect:" + builder.build().encode().toUriString();
        }

        List<ReactivationRequestStatus> statusFilters = selectedRequestStatuses == null
                ? new ArrayList<>()
                : selectedRequestStatuses;

        ReactivationRequestsPageData pageData = userReactivationRequestService.getPage(
                authentication,
                search,
                statusFilters,
                showFirmAdmins,
                showMultiFirmUsers,
                showProviderUsers,
                page,
                size,
                sort,
                direction);

        model.addAttribute("pageHeading", pageData.pageMode().getHeading());
        model.addAttribute("manageMode", pageData.pageMode().isManageMode());
        model.addAttribute("requests", pageData.paginatedRequests().getRequests());
        model.addAttribute("requestedPageSize", size);
        model.addAttribute("actualPageSize", pageData.paginatedRequests().getRequests().size());
        model.addAttribute("page", pageData.paginatedRequests().getCurrentPage());
        model.addAttribute("totalRequests", pageData.paginatedRequests().getTotalRequests());
        model.addAttribute("totalPages", pageData.paginatedRequests().getTotalPages());
        model.addAttribute("search", search == null ? "" : search.trim());
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("selectedRequestStatuses", pageData.appliedStatuses());
        model.addAttribute("showFirmAdmins", pageData.showFirmAdmins());
        model.addAttribute("showMultiFirmUsers", pageData.showMultiFirmUsers());
        model.addAttribute("showProviderUsers", pageData.showProviderUsers());
        model.addAttribute("defaultStatusApplied", true);
        model.addAttribute(ModelAttributes.PAGE_TITLE, pageData.pageMode().getHeading());

        return "reactivation-requests";
    }

    public void clearSessionAttributes(HttpSession session) {
        log.debug("Clearing delegate reactivation session attributes");
        session.removeAttribute("user");
        session.removeAttribute("delegateReactivateUserId");
        session.removeAttribute("delegateReactivateUserCommentForm");
        session.removeAttribute("profileId");
    }

    private String getCancelPathFromReferer(String referer, String entraUserId, String userProfileId) {
        if ("manage".equals(referer) && userProfileId != null) {
            return String.format("/admin/users/manage/%s", userProfileId);
        } else if ("audit".equals(referer)) {
            return String.format("/admin/users/audit/%s", entraUserId);
        } else if ("list".equals(referer)) {
            return "/admin/users/reactivation-requests";
        } else {
            return "/home";
        }
    }
}
