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
import uk.gov.justice.laa.portal.landingpage.audit.ReactivateUserRequestUpdatedAuditEvent;
import uk.gov.justice.laa.portal.landingpage.audit.ReactivateUserRequestSubmittedAuditEvent;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import static uk.gov.justice.laa.portal.landingpage.controller.UserController.buildErrorString;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.ReactivationRequestsPageData;
import uk.gov.justice.laa.portal.landingpage.dto.UserActivationRequestSummaryDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRoleType;
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
                                            @RequestParam String profileId,
                                            Authentication authentication,
                                            RedirectAttributes redirectAttributes) {
        log.info("Initiating delegate reactivate GET flow for userId: {} and profileId: {}", id, profileId);

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

        Optional<UserActivationRequest> request = userReactivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(profileId);
        if (request.isPresent()
                && !(ReactivationRequestStatus.REJECTED.equals(request.get().getStatus())
                || ReactivationRequestStatus.APPROVED.equals(request.get().getStatus()))) {
            log.info("Delegate reactivation request already in progress for profileId: {}, requestId: {}", profileId, request.get().getId());
            redirectAttributes.addFlashAttribute("errorMessage", "A delegate request is already in progress");
            redirectAttributes.addFlashAttribute("requestId", request.get().getId().toString());
            return "redirect:/admin/users/manage/" + profileId;
        }

        EntraUser currentEntraUser = loginService.getCurrentEntraUser(authentication);
        boolean isInternalActor = userService.isInternal(currentEntraUser.getId());

        model.addAttribute("user", user);
        model.addAttribute("profileId", profileId);
        model.addAttribute("isInternalActor", isInternalActor);
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
                                             @RequestParam String profileId) {
        log.info("Processing delegate reactivate POST for userId: {} and profileId: {}", id, profileId);

        String idFromSession = getObjectFromHttpSession(session, "delegateReactivateUserId", String.class).orElseThrow();
        if (id == null || !id.equals(idFromSession)) {
            log.info("Session validation failed in delegateReactivateUserPost. Path ID: {}, Session ID: {}", id, idFromSession);
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        if (!userService.isValidUserProfileId(id, profileId)) {
            log.info("Invalid submit to reactivate page for profileId: {} for userId: {}", profileId, id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        session.setAttribute("delegateReactivateUserId", id);
        session.setAttribute("profileId", profileId);

        model.addAttribute("user", user);
        model.addAttribute("profileId", profileId);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
        return "redirect:/admin/user/delegate-reactivate-user-comment/" + user.getId();
    }

    @GetMapping("/user/delegate-reactivate-user-comment/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserCommentsGet(@PathVariable String id,
                                                    Model model,
                                                    HttpSession session) {
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

        String profileId = getObjectFromHttpSession(session, "profileId", String.class).orElseThrow();

        DelegateReactivateUserCommentForm delegateReactivateUserCommentForm =
                getObjectFromHttpSession(session, "delegateReactivateUserCommentForm", DelegateReactivateUserCommentForm.class)
                        .orElse(new DelegateReactivateUserCommentForm());

        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("delegateReactivateUserCommentForm", delegateReactivateUserCommentForm);
        model.addAttribute("profileId", profileId);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");

        return "delegate-reactivate-user-comment";
    }

    @PostMapping("/user/delegate-reactivate-user-comment/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserCommentsPost(@PathVariable String id,
                                                     @Valid DelegateReactivateUserCommentForm delegateReactivateUserCommentForm,
                                                     BindingResult result,
                                                     Model model,
                                                     HttpSession session) {
        log.info("Processing delegate reactivate comments POST for userId: {}", id);

        String profileId = getObjectFromHttpSession(session, "profileId", String.class).orElseThrow();

        if (result.hasErrors()) {
            log.info("Validation errors encountered on comment form for userId: {}", id);
            String errorMessage = buildErrorString(result);
            EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
            model.addAttribute("user", user);
            model.addAttribute("profileId", profileId);
            model.addAttribute("delegateReactivateUserCommentForm", delegateReactivateUserCommentForm);
            model.addAttribute("errorMessage", errorMessage);
            model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
            return "delegate-reactivate-user-comment";
        }

        if (!userService.isValidUserProfileId(id, profileId)) {
            log.info("Invalid access to reactivate comments page for profileId: {} for userId: {}", profileId, id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();

        model.addAttribute("user", user);
        model.addAttribute("profileId", profileId);
        model.addAttribute("delegateReactivateUserCommentForm", delegateReactivateUserCommentForm);
        session.setAttribute("delegateReactivateUserCommentForm", delegateReactivateUserCommentForm);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
        return "redirect:/admin/user/delegate-reactivate-user-check-answers/" + user.getId();
    }

    @GetMapping("/user/delegate-reactivate-user-check-answers/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserCommentsCheckAnswersGet(@PathVariable String id,
                                                                Model model,
                                                                HttpSession session) {
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

        String profileId = getObjectFromHttpSession(session, "profileId", String.class).orElseThrow();
        DelegateReactivateUserCommentForm delegateReactivateUserCommentForm =
                getObjectFromHttpSession(session, "delegateReactivateUserCommentForm", DelegateReactivateUserCommentForm.class)
                        .orElseThrow();

        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("delegateReactivateUserCommentForm", delegateReactivateUserCommentForm);
        model.addAttribute("profileId", profileId);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
        return "delegate-reactivate-user-check-answers";
    }

    @PostMapping("/user/delegate-reactivate-user-check-answers/{id}")
    @PreAuthorize("@accessControlService.canDelegateEnableUser(#id)")
    public String delegateReactivateUserCommentsCheckAnswersPost(@PathVariable String id,
                                                                 Authentication authentication,
                                                                 Model model,
                                                                 HttpSession session) {
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

        String profileId = getObjectFromHttpSession(session, "profileId", String.class).orElseThrow();
        UserProfileDto userProfile = userService.getActiveProfileByUserId(id).orElseThrow();
        if (!userProfile.getId().equals(UUID.fromString(profileId))) {
            log.info("Profile mismatch: profileId from session ({}) does not match active profileId ({}) for userId: {}",
                    profileId, userProfile.getId(), id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(400));
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
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
        clearSessionAttributes(session);
        return "delegate-reactivate-user-confirmation";
    }

    @GetMapping("/user/delegate-reactivate/track/{id}")
    @PreAuthorize("@accessControlService.canTrackDelegateEnableUser(#id)")
    public String trackDelegateReactivateUserRequestsGet(@PathVariable String id,
                                                         HttpSession session,
                                                         Model model,
                                                         @RequestParam String profileId,
                                                         RedirectAttributes redirectAttributes) {
        log.debug("Tracking delegate reactivate requests for userId: {}, profileId: {}", id, profileId);

        if (!delegateUserActivationFeatureEnabled) {
            log.info("Delegate reactivate feature disabled. Throwing 404 for userId: {}", id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }

        final EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        Optional<UserActivationRequest> request = userReactivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(profileId);

        if (!userService.isValidUserProfileId(id, profileId)) {
            log.info("Invalid access to track reactivate page for profileId: {} for userId: {}", profileId, id);
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        if (request.isEmpty()) {
            log.info("No delegate activation request found to track for profileId: {}", profileId);
            redirectAttributes.addFlashAttribute("errorMessage", "There is no open delegate activation request");
            return "redirect:/admin/users/manage/" + profileId;
        } else if (ReactivationRequestStatus.APPROVED.equals(request.get().getStatus())) {
            log.info("Delegate activation request already APPROVED for profileId: {}", profileId);
            redirectAttributes.addFlashAttribute("errorMessage", "There is no open delegate activation request");
            request.ifPresent(userActivationRequest -> redirectAttributes.addFlashAttribute("requestId", userActivationRequest.getId().toString()));
            return "redirect:/admin/users/manage/" + profileId;
        }

        if (!model.containsAttribute("delegateReactivateUserCommentForm")) {
            DelegateReactivateUserCommentForm form = getObjectFromHttpSession(session, "delegateReactivateUserCommentForm", DelegateReactivateUserCommentForm.class)
                    .orElse(new DelegateReactivateUserCommentForm());
            model.addAttribute("delegateReactivateUserCommentForm", form);
        }

        boolean canActionDelegateEnableUser = accessControlService.canManageDelegateEnableUser(id);

        model.addAttribute("canActionDelegateEnableUser", canActionDelegateEnableUser);
        model.addAttribute("user", user);
        model.addAttribute("profileId", profileId);
        model.addAttribute("requestId", request.get().getRequestId().toString());
        model.addAttribute("requestCurrentStatus", request.get().getStatus());

        List<UserActivationRequestSummaryDto> latestRequestHistoryForUserProfile
                = userReactivationRequestService.getLatestRequestHistoryForUserProfile(profileId);
        model.addAttribute("reactivationRequests", latestRequestHistoryForUserProfile);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
        return "delegate-reactivate-user-tracking";
    }

    @PostMapping("/user/delegate-reactivate/track/{id}")
    @PreAuthorize("@accessControlService.canTrackDelegateEnableUser(#id)")
    public String trackDelegateReactivateUserRequestsPost(
            @PathVariable String id,
            @RequestParam String profileId,
            @RequestParam String requestId,
            @Valid @ModelAttribute("delegateReactivateUserCommentForm") DelegateReactivateUserCommentForm delegateReactivateUserCommentForm,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        log.debug("Updating tracking info for request ID: {}, profile ID: {}", requestId, profileId);

        if (bindingResult.hasErrors()) {
            log.info("Validation errors encountered while updating tracking for requestId: {}", requestId);
            String errorMessage = buildErrorString(bindingResult);

            redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "delegateReactivateUserCommentForm", bindingResult);
            redirectAttributes.addFlashAttribute("delegateReactivateUserCommentForm", delegateReactivateUserCommentForm);
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);

            redirectAttributes.addAttribute("id", id);
            redirectAttributes.addAttribute("profileId", profileId);
            redirectAttributes.addAttribute("requestId", requestId);

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

        return "redirect:/admin/user/delegate-reactivate/track/{id}";
    }

    @GetMapping("/user/delegate-reactivate/reject/{id}")
    @PreAuthorize("@accessControlService.canManageDelegateEnableUser(#id)")
    public String rejectDelegateReactivateUserRequestsGet(@PathVariable String id,
                                                          HttpSession session,
                                                          Model model,
                                                          @RequestParam String profileId,
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
        Optional<UserActivationRequest> request = userReactivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(profileId);

        if (request.isEmpty()) {
            log.info("No delegate activation request found to reject for profileId: {}", profileId);
            redirectAttributes.addFlashAttribute("errorMessage", "There is no open delegate activation request");
            return "redirect:/admin/users/manage/" + profileId;
        } else if (ReactivationRequestStatus.APPROVED.equals(request.get().getStatus()) || ReactivationRequestStatus.REJECTED.equals(request.get().getStatus())) {
            log.info("Delegate activation request for profileId: {} is already in status: {}", profileId, request.get().getStatus());
            redirectAttributes.addFlashAttribute("errorMessage", "There is no open delegate activation request");
            request.ifPresent(userActivationRequest -> redirectAttributes.addFlashAttribute("requestId", userActivationRequest.getId().toString()));
            return "redirect:/admin/users/manage/" + profileId;
        }

        if (!model.containsAttribute("delegateReactivateUserCommentForm")) {
            DelegateReactivateUserCommentForm form = getObjectFromHttpSession(session, "delegateReactivateUserCommentForm", DelegateReactivateUserCommentForm.class)
                    .orElse(new DelegateReactivateUserCommentForm());
            model.addAttribute("delegateReactivateUserCommentForm", form);
        }

        model.addAttribute("user", user);
        model.addAttribute("profileId", profileId);
        model.addAttribute("requestId", request.get().getRequestId().toString());

        List<UserActivationRequestSummaryDto> latestRequestHistoryForUserProfile
                = userReactivationRequestService.getLatestRequestHistoryForUserProfile(profileId);
        model.addAttribute("reactivationRequests", latestRequestHistoryForUserProfile);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Delegate Reactivate User");
        return "delegate-reactivate-user-rejection";
    }

    @PostMapping("/user/delegate-reactivate/reject/{id}")
    @PreAuthorize("@accessControlService.canManageDelegateEnableUser(#id)")
    public String rejectDelegateReactivateUserRequestsPost(
            @PathVariable String id,
            HttpSession session,
            Model model,
            @RequestParam String profileId,
            @RequestParam String requestId,
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
            @RequestParam String profileId,
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
            @RequestParam(name = "selectedUserTypes", required = false) List<ReactivationRoleType> selectedUserTypes,
            @RequestParam(name = "defaultStatusApplied", defaultValue = "false") boolean defaultStatusApplied,
            Model model,
            Authentication authentication) {

        log.debug("Fetching reactivation requests list. Page: {}, Size: {}, Search: '{}'", page, size, search);

        var pageMode = userReactivationRequestService.getPageMode(authentication);

        // For manage roles, stamp the default status into the URL once so the default is explicit and user-clearable.
        if (pageMode.isManageMode() && !defaultStatusApplied && (selectedRequestStatuses == null || selectedRequestStatuses.isEmpty())) {
            log.debug("Applying default status filter IN_REVIEW for manage mode redirect.");
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/users/reactivation-requests")
                    .queryParam("size", size)
                    .queryParam("page", page)
                    .queryParam("sort", sort)
                    .queryParam("direction", direction)
                    .queryParam("defaultStatusApplied", true)
                    .queryParam("selectedRequestStatuses", ReactivationRequestStatus.IN_REVIEW.name());

            if (selectedUserTypes != null && !selectedUserTypes.isEmpty()) {
                builder.queryParam("selectedUserTypes",
                        selectedUserTypes.stream().map(Enum::name).toArray());
            }

            if (search != null && !search.trim().isEmpty()) {
                builder.queryParam("search", search.trim());
            }

            return "redirect:" + builder.build().encode().toUriString();
        }

        List<ReactivationRequestStatus> statusFilters = selectedRequestStatuses == null
                ? new ArrayList<>()
                : selectedRequestStatuses;
        List<ReactivationRoleType> userTypeFilters = selectedUserTypes == null
                ? new ArrayList<>()
                : selectedUserTypes;

        ReactivationRequestsPageData pageData = userReactivationRequestService.getPage(
                authentication,
                search,
                statusFilters,
                userTypeFilters,
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
        model.addAttribute("selectedUserTypes", pageData.appliedActorRoleTypes());
        model.addAttribute("defaultStatusApplied", pageMode.isManageMode() || defaultStatusApplied);
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
}
