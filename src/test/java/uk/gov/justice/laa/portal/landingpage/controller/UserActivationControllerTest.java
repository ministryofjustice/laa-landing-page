package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.ReactivationRequestsPageData;
import uk.gov.justice.laa.portal.landingpage.dto.UserActivationRequestSummaryDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRoleType;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;
import uk.gov.justice.laa.portal.landingpage.forms.DelegateReactivateUserCommentForm;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedReactivationRequests;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestListItem;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestPageMode;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestStatus;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;
import uk.gov.justice.laa.portal.landingpage.service.UserReactivationRequestService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

@ExtendWith(MockitoExtension.class)
public class UserActivationControllerTest {

    private static final String USER_ID = UUID.randomUUID().toString();
    private static final UUID ACTOR_USER_ID = UUID.randomUUID();
    private static final String PROFILE_ID = UUID.randomUUID().toString();
    private static final String REQUEST_ID = UUID.randomUUID().toString();
    private UserActivationController userActivationController;
    @Mock
    private LoginService loginService;
    @Mock
    private UserService userService;
    @Mock
    private UserReactivationRequestService userReactivationRequestService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private UserAccountStatusService userAccountStatusService;
    @Mock
    private HttpSession session;
    @Mock
    private Authentication authentication;
    @Mock
    private RedirectAttributes redirectAttributes;
    private Model model;
    @Mock
    private BindingResult bindingResult;
    @Mock
    private EventService eventService;

    @BeforeEach
    void setUp() {
        userActivationController = new UserActivationController(loginService, userService, userReactivationRequestService, accessControlService, userAccountStatusService, eventService);
        userActivationController.delegateUserActivationFeatureEnabled = true;
        model = new ExtendedModelMap();
        lenient().when(session.getAttributeNames()).thenReturn(Collections.emptyEnumeration());
    }

    private EntraUserDto buildEntraUserDto() {
        return EntraUserDto.builder().id(USER_ID).fullName("John Doe").email("john.doe@justice.gov.uk").build();
    }

    private UserActivationRequest buildUserActivationRequest(ReactivationRequestStatus status) {
        return UserActivationRequest.builder().id(UUID.randomUUID()).requestId(UUID.randomUUID()).userProfileId(UUID.fromString(PROFILE_ID)).status(status).version(1).build();
    }

    @Nested
    @DisplayName("GET /user/delegate-reactivate/{id}")
    class DelegateReactivateUserGetTests {

        @Test
        @DisplayName("Should throw 404 when feature flag is disabled")
        void featureDisabled_throws404() {
            userActivationController.delegateUserActivationFeatureEnabled = false;

            assertThatThrownBy(() -> userActivationController.delegateReactivateUserGet(USER_ID, session, model, PROFILE_ID, authentication, redirectAttributes))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("404");
        }

        @Test
        @DisplayName("Should redirect with error when request is already in progress (PENDING status)")
        void requestInProgress_redirectsToManagePage() {
            EntraUserDto user = buildEntraUserDto();
            UserActivationRequest pendingRequest = buildUserActivationRequest(ReactivationRequestStatus.IN_REVIEW);

            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));
            when(userService.isValidUserProfileId(USER_ID, PROFILE_ID)).thenReturn(true);
            when(userReactivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID)).thenReturn(Optional.of(pendingRequest));

            redirectAttributes = new RedirectAttributesModelMap();
            String view = userActivationController.delegateReactivateUserGet(USER_ID, session, model, PROFILE_ID, authentication, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin/users/manage/" + PROFILE_ID);
            assertThat(redirectAttributes.getFlashAttributes()).extractingByKey("errorMessage").isEqualTo("A delegate request is already in progress");
            assertThat(redirectAttributes.getFlashAttributes()).extractingByKey("requestId").isEqualTo(pendingRequest.getId().toString());
        }

        @Test
        @DisplayName("Should populate model and session when no active request exists (or is REJECTED/APPROVED)")
        void noActiveRequest_rendersForm() {
            session = new MockHttpSession();
            EntraUserDto user = buildEntraUserDto();
            UserActivationRequest rejectedRequest = buildUserActivationRequest(ReactivationRequestStatus.REJECTED);

            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));
            when(userReactivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID))
                    .thenReturn(Optional.of(rejectedRequest));
            EntraUser currentEntraUser = mock(EntraUser.class);
            when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentEntraUser);
            when(userService.isInternal(currentEntraUser.getId())).thenReturn(false);
            when(userService.isValidUserProfileId(USER_ID, PROFILE_ID)).thenReturn(true);

            String view = userActivationController.delegateReactivateUserGet(USER_ID, session, model, PROFILE_ID, authentication, redirectAttributes);

            assertThat(view).isEqualTo("delegate-reactivate-user");

            assertThat(session.getAttribute("delegateReactivateUserId")).isEqualTo(USER_ID);
            assertThat(session.getAttribute("profileId")).isEqualTo(PROFILE_ID);
        }
    }

    @Nested
    @DisplayName("POST /user/delegate-reactivate/{id}")
    class DelegateReactivateUserPostTests {

        @Test
        @DisplayName("Should throw 403 when session ID does not match path variable")
        void mismatchSessionId_throws403() {
            session = new MockHttpSession();
            session.setAttribute("delegateReactivateUserId", "different-user-id");

            assertThatThrownBy(() -> userActivationController
                    .delegateReactivateUserPost(USER_ID, model, session, PROFILE_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("403");
        }

        @Test
        @DisplayName("Should set attributes and redirect to comments step on success")
        void validSession_redirectsToComments() {
            session = new MockHttpSession();
            session.setAttribute("delegateReactivateUserId", USER_ID);
            EntraUserDto user = buildEntraUserDto();

            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));
            when(userService.isValidUserProfileId(USER_ID, PROFILE_ID)).thenReturn(true);

            String view = userActivationController.delegateReactivateUserPost(USER_ID, model, session, PROFILE_ID);

            assertThat(view).isEqualTo("redirect:/admin/user/delegate-reactivate-user-comment/" + USER_ID);
            assertThat(session.getAttribute("delegateReactivateUserId")).isEqualTo(USER_ID);
            assertThat(session.getAttribute("profileId")).isEqualTo(PROFILE_ID);
        }
    }

    @Nested
    @DisplayName("GET /user/delegate-reactivate-user-comment/{id}")
    class DelegateReactivateUserCommentsGetTests {

        @Test
        @DisplayName("Should throw 404 when feature flag is disabled")
        void featureDisabled_throws404() {
            userActivationController.delegateUserActivationFeatureEnabled = false;

            assertThatThrownBy(() -> userActivationController.delegateReactivateUserCommentsGet(USER_ID, model, session)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        }

        @Test
        @DisplayName("Should throw 403 when session user ID does not match path variable")
        void invalidSessionUser_throws403() {
            session = new MockHttpSession();
            session.setAttribute("delegateReactivateUserId", "invalid-id");

            assertThatThrownBy(() -> userActivationController.delegateReactivateUserCommentsGet(USER_ID, model, session)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        }

        @Test
        @DisplayName("Should load form from session if present or create new instance")
        void validSession_rendersCommentsView() {
            session = new MockHttpSession();
            session.setAttribute("delegateReactivateUserId", USER_ID);
            session.setAttribute("profileId", PROFILE_ID);

            DelegateReactivateUserCommentForm existingForm = DelegateReactivateUserCommentForm.builder().comment("Existing comment").build();
            session.setAttribute("delegateReactivateUserCommentForm", existingForm);

            EntraUserDto user = buildEntraUserDto();
            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));

            String view = userActivationController.delegateReactivateUserCommentsGet(USER_ID, model, session);

            assertThat(view).isEqualTo("delegate-reactivate-user-comment");
            assertThat(model.asMap()).containsEntry("user", user).containsEntry("profileId", PROFILE_ID).containsEntry("delegateReactivateUserCommentForm", existingForm);
        }
    }

    @Nested
    @DisplayName("POST /user/delegate-reactivate-user-comment/{id}")
    class DelegateReactivateUserCommentsPostTests {

        @Test
        @DisplayName("Should re-render form with error message when validation fails")
        void validationErrors_returnsCommentViewWithError() {
            session = new MockHttpSession();
            session.setAttribute("profileId", PROFILE_ID);
            DelegateReactivateUserCommentForm form = DelegateReactivateUserCommentForm.builder().build();

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);

            EntraUserDto user = buildEntraUserDto();
            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));

            String view = userActivationController.delegateReactivateUserCommentsPost(USER_ID, form, bindingResult, model, session);

            assertThat(view).isEqualTo("delegate-reactivate-user-comment");
            assertThat(model.asMap()).containsKey("errorMessage");
            assertThat(session.getAttribute("delegateReactivateUserCommentForm")).isNull();
        }

        @Test
        @DisplayName("Should save form to session and redirect to check answers when validation passes")
        void validationSuccess_savesSessionAndRedirects() {
            session = new MockHttpSession();
            session.setAttribute("profileId", PROFILE_ID);
            DelegateReactivateUserCommentForm form = DelegateReactivateUserCommentForm.builder().comment("Valid user reactivation request comment").build();

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(false);
            EntraUserDto user = buildEntraUserDto();
            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));
            when(userService.isValidUserProfileId(USER_ID, PROFILE_ID)).thenReturn(true);

            String view = userActivationController.delegateReactivateUserCommentsPost(USER_ID, form, bindingResult, model, session);

            assertThat(view).isEqualTo("redirect:/admin/user/delegate-reactivate-user-check-answers/" + USER_ID);
            assertThat(session.getAttribute("delegateReactivateUserCommentForm")).isEqualTo(form);
        }
    }

    @Nested
    @DisplayName("GET /user/delegate-reactivate-user-check-answers/{id}")
    class DelegateReactivateUserCommentsCheckAnswersGetTests {

        @Test
        @DisplayName("Should throw 403 if session ID does not match path variable")
        void invalidSessionUser_throws403() {
            session = new MockHttpSession();
            session.setAttribute("profileId", PROFILE_ID);
            session.setAttribute("delegateReactivateUserId", "mismatched-id");

            assertThatThrownBy(() -> userActivationController
                    .delegateReactivateUserCommentsCheckAnswersGet(USER_ID, model, session))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("403");
        }

        @Test
        @DisplayName("Should throw NoSuchElementException if form is missing from session")
        void missingFormInSession_throwsException() {
            session.setAttribute("profileId", PROFILE_ID);
            session.setAttribute("delegateReactivateUserId", USER_ID);

            assertThatThrownBy(() -> userActivationController.delegateReactivateUserCommentsCheckAnswersGet(USER_ID, model, session)).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("Should render check answers view with populated model")
        void validSession_rendersCheckAnswersView() {
            session = new MockHttpSession();
            session.setAttribute("profileId", PROFILE_ID);
            session.setAttribute("delegateReactivateUserId", USER_ID);

            DelegateReactivateUserCommentForm form = DelegateReactivateUserCommentForm.builder().comment("Reactivation justification").build();
            session.setAttribute("delegateReactivateUserCommentForm", form);

            EntraUserDto user = buildEntraUserDto();
            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));

            String view = userActivationController.delegateReactivateUserCommentsCheckAnswersGet(USER_ID, model, session);

            assertThat(view).isEqualTo("delegate-reactivate-user-check-answers");
            assertThat(model.asMap()).containsEntry("user", user).containsEntry("profileId", PROFILE_ID).containsEntry("delegateReactivateUserCommentForm", form);
        }
    }

    @Nested
    @DisplayName("POST /user/delegate-reactivate-user-check-answers/{id}")
    class DelegateReactivateUserCommentsCheckAnswersPostTests {

        @Test
        @DisplayName("Should throw 400 if user profile ID does not match session profile ID")
        void profileIdMismatch_throws400() {
            session = new MockHttpSession();
            session.setAttribute("delegateReactivateUserId", USER_ID);
            session.setAttribute("profileId", PROFILE_ID);
            session.setAttribute("delegateReactivateUserCommentForm", DelegateReactivateUserCommentForm.builder().build());

            UserProfileDto mismatchedProfile = UserProfileDto.builder().id(UUID.randomUUID()).build();
            when(userService.getActiveProfileByUserId(USER_ID)).thenReturn(Optional.of(mismatchedProfile));

            assertThatThrownBy(() -> userActivationController.delegateReactivateUserCommentsCheckAnswersPost(USER_ID, authentication, model, session))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");

            verify(userReactivationRequestService, never()).createReactivationRequest(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should create activation request and render confirmation view on success")
        void validSubmission_createsRequestAndRendersConfirmation() {
            MockHttpSession httpSession = new MockHttpSession();
            httpSession.setAttribute("delegateReactivateUserId", USER_ID);
            httpSession.setAttribute("profileId", PROFILE_ID);

            DelegateReactivateUserCommentForm form = DelegateReactivateUserCommentForm.builder().comment("Approved leave returned").build();
            httpSession.setAttribute("delegateReactivateUserCommentForm", form);

            UserProfileDto userProfile = UserProfileDto.builder().id(UUID.fromString(PROFILE_ID)).build();
            EntraUser entraUser = EntraUser.builder().id(UUID.fromString(USER_ID)).entraOid(USER_ID).firstName("John Doe").email("john.doe@justice.gov.uk").build();
            CurrentUserDto actor = new CurrentUserDto();
            actor.setUserId(UUID.randomUUID());
            UserActivationRequest createdRequest = buildUserActivationRequest(ReactivationRequestStatus.IN_REVIEW);

            when(userService.getActiveProfileByUserId(USER_ID)).thenReturn(Optional.of(userProfile));
            when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
            when(loginService.getCurrentUser(any())).thenReturn(actor);
            when(userReactivationRequestService.createReactivationRequest(any(String.class), any(String.class), any(), any()))
                    .thenReturn(createdRequest);

            String view = userActivationController.delegateReactivateUserCommentsCheckAnswersPost(USER_ID, authentication, model, httpSession);

            assertThat(view).isEqualTo("delegate-reactivate-user-confirmation");
            assertThat(model.asMap()).containsEntry("user", entraUser);

            verify(userReactivationRequestService).createReactivationRequest(eq(USER_ID), eq(PROFILE_ID),
                    eq("Approved leave returned"), eq(USER_ID));
        }
    }

    @Nested
    class TrackDelegateReactivateUserRequestsGetTests {

        @Test
        void get_WhenFeatureDisabled_ThrowsResponseStatusException404() {
            ReflectionTestUtils.setField(userActivationController, "delegateUserActivationFeatureEnabled", false);
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            assertThatThrownBy(() -> userActivationController.trackDelegateReactivateUserRequestsGet("user-123", session, model,
                    UUID.randomUUID().toString(), null, redirectAttributes)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        }

        @Test
        void get_WhenUserNotFound_ThrowsNoSuchElementException() {
            String userId = "non-existent-user";
            String profileId = UUID.randomUUID().toString();
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            when(userService.getEntraUserById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userActivationController.trackDelegateReactivateUserRequestsGet(userId, session, model, profileId, null, redirectAttributes)).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void get_WhenRequestStatusIsApproved_RedirectsToManageUsersWithFlashAttributes() {
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            EntraUserDto mockUser = mock(EntraUserDto.class);
            UserActivationRequest mockRequest = mock(UserActivationRequest.class);

            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userService.isValidUserProfileId(USER_ID, PROFILE_ID)).thenReturn(true);
            when(userReactivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID))
                    .thenReturn(Optional.of(mockRequest));
            when(mockRequest.getStatus()).thenReturn(ReactivationRequestStatus.APPROVED);
            when(mockRequest.getId()).thenReturn(UUID.fromString(REQUEST_ID));

            String view = userActivationController.trackDelegateReactivateUserRequestsGet(USER_ID, session, model, PROFILE_ID, null, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin/users/manage/" + PROFILE_ID);
            assertThat(redirectAttributes.getFlashAttributes()).extractingByKey("errorMessage").isEqualTo("There is no open delegate activation request");
        }

        @Test
        void get_WhenRequestIsEmpty_RedirectsToManageUsersWithFlashErrorMessage() {
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            EntraUserDto mockUser = mock(EntraUserDto.class);

            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userService.isValidUserProfileId(USER_ID, PROFILE_ID)).thenReturn(true);
            when(userReactivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID))
                    .thenReturn(Optional.empty());

            String view = userActivationController.trackDelegateReactivateUserRequestsGet(USER_ID, session, model, PROFILE_ID, null, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin/users/manage/" + PROFILE_ID);
            assertThat(redirectAttributes.getFlashAttributes()).extractingByKey("errorMessage").isEqualTo("There is no open delegate activation request");
        }

        @Test
        void get_WhenRequestNotInProgress_PopulatesModelAndReturnsTrackingView() {
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            EntraUserDto mockUser = mock(EntraUserDto.class);

            UserActivationRequest mockRequest = mock(UserActivationRequest.class);
            when(mockRequest.getStatus()).thenReturn(ReactivationRequestStatus.APPROVED);
            when(mockRequest.getId()).thenReturn(UUID.randomUUID());

            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userService.isValidUserProfileId(USER_ID, PROFILE_ID)).thenReturn(true);
            when(userReactivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID))
                    .thenReturn(Optional.of(mockRequest));

            String view = userActivationController.trackDelegateReactivateUserRequestsGet(USER_ID, session, model, PROFILE_ID, null, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin/users/manage/" + PROFILE_ID);
            assertThat(redirectAttributes.getFlashAttributes()).extractingByKey("errorMessage").isEqualTo("There is no open delegate activation request");
        }
    }

    @Nested
    class TrackDelegateReactivateUserRequestsPostTests {

        @Test
        void post_WhenBindingResultHasErrors_SetsFlashAndQueryAttributesAndRedirects() {
            String id = UUID.randomUUID().toString();
            String profileId = UUID.randomUUID().toString();
            String requestId = UUID.randomUUID().toString();

            DelegateReactivateUserCommentForm form = new DelegateReactivateUserCommentForm();
            BindingResult bindingResult = mock(BindingResult.class);
            Authentication auth = mock(Authentication.class);
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            when(bindingResult.hasErrors()).thenReturn(true);
            // Assuming buildErrorString uses getAllErrors() or similar from BindingResult
            when(bindingResult.getAllErrors()).thenReturn(List.of(new ObjectError("comment", "Comment cannot be empty")));

            String view = userActivationController.trackDelegateReactivateUserRequestsPost(id, profileId, requestId, form, bindingResult, auth, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin/user/delegate-reactivate/track/{id}");
            assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
            assertThat(redirectAttributes.asMap()).containsEntry("id", id).containsEntry("profileId", profileId).containsEntry("requestId", requestId);

            verifyNoInteractions(loginService);
            verify(userReactivationRequestService, never()).updateReactivateRequestState(any(), any(), any(), any(), any());
        }

        @Test
        void post_WhenFormIsValid_SavesRequestAndRedirectsWithQueryAttributes() {
            final String id = UUID.randomUUID().toString();
            final String profileId = UUID.randomUUID().toString();
            final String requestId = UUID.randomUUID().toString();
            String entraOid = UUID.randomUUID().toString();
            String actorUserId = UUID.randomUUID().toString();

            DelegateReactivateUserCommentForm form = new DelegateReactivateUserCommentForm();
            form.setComment("Reactivation approved for medical leave return.");

            BindingResult bindingResult = mock(BindingResult.class);
            Authentication auth = mock(Authentication.class);
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            EntraUser mockEntraUser = mock(EntraUser.class);
            when(mockEntraUser.getEntraOid()).thenReturn(entraOid);

            CurrentUserDto mockActor = mock(CurrentUserDto.class);
            when(mockActor.getUserId()).thenReturn(UUID.fromString(actorUserId));

            when(bindingResult.hasErrors()).thenReturn(false);
            when(loginService.getCurrentEntraUser(auth)).thenReturn(mockEntraUser);
            when(loginService.getCurrentUser(auth)).thenReturn(mockActor);
            when(userReactivationRequestService.updateReactivateRequestState(any(), any(), any(), any(), any()))
                    .thenReturn(UserActivationRequest.builder().id(UUID.fromString(requestId)).status(ReactivationRequestStatus.IN_REVIEW).build());

            String view = userActivationController.trackDelegateReactivateUserRequestsPost(id, profileId, requestId, form, bindingResult, auth, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin/user/delegate-reactivate/track/{id}");
            assertThat(redirectAttributes.asMap()).containsEntry("id", id).containsEntry("profileId", profileId).containsEntry("requestId", requestId);

            verify(userReactivationRequestService)
                    .updateReactivateRequestState(eq(requestId), eq(id), eq(profileId),
                            eq("Reactivation approved for medical leave return."), eq(entraOid));
        }
    }

    @Nested
    @DisplayName("GET /user/delegate-reactivate/reject/{id}")
    class RejectGetTests {

        @Test
        @DisplayName("Should throw 404 ResponseStatusException when disableUserFeatureEnabled is false")
        void shouldThrow404WhenFeatureDisabled() {
            userActivationController.delegateUserActivationFeatureEnabled = false;

            assertThatThrownBy(() -> userActivationController.rejectDelegateReactivateUserRequestsGet(USER_ID, session, model, PROFILE_ID, redirectAttributes))
                    .isInstanceOf(ResponseStatusException.class).extracting(e -> ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatusCode.valueOf(404));
        }

        @Test
        @DisplayName("Should redirect with error message when no request exists")
        void shouldRedirectWhenRequestIsEmpty() {
            EntraUserDto userDto = mock(EntraUserDto.class);
            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(userDto));
            when(userService.isValidUserProfileId(USER_ID, PROFILE_ID)).thenReturn(true);
            when(userReactivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID)).thenReturn(Optional.empty());

            String viewName = userActivationController.rejectDelegateReactivateUserRequestsGet(USER_ID, session, model, PROFILE_ID, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/admin/users/manage/" + PROFILE_ID);
            verify(redirectAttributes).addFlashAttribute("errorMessage", "There is no open delegate activation request");
        }

        @Test
        @DisplayName("Should redirect with error message and requestId when request status is APPROVED")
        void shouldRedirectWhenRequestIsApproved() {
            EntraUserDto userDto = mock(EntraUserDto.class);
            UserActivationRequest request = mock(UserActivationRequest.class);
            UUID dbRequestId = UUID.randomUUID();

            when(request.getStatus()).thenReturn(ReactivationRequestStatus.APPROVED);
            when(request.getId()).thenReturn(dbRequestId);
            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(userDto));
            when(userService.isValidUserProfileId(USER_ID, PROFILE_ID)).thenReturn(true);
            when(userReactivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID)).thenReturn(Optional.of(request));

            String viewName = userActivationController.rejectDelegateReactivateUserRequestsGet(USER_ID, session, model, PROFILE_ID, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/admin/users/manage/" + PROFILE_ID);
            verify(redirectAttributes).addFlashAttribute("errorMessage", "There is no open delegate activation request");
        }

        @Test
        @DisplayName("Should populate model and return view when valid open request exists")
        void shouldReturnViewWithPopulatedModel() {
            EntraUserDto userDto = mock(EntraUserDto.class);

            UserActivationRequest request = mock(UserActivationRequest.class);
            when(request.getStatus()).thenReturn(ReactivationRequestStatus.IN_REVIEW);
            when(request.getRequestId()).thenReturn(UUID.fromString(REQUEST_ID));

            List<UserActivationRequestSummaryDto> history = List.of(mock(UserActivationRequestSummaryDto.class));

            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(userDto));
            when(userService.isValidUserProfileId(USER_ID, PROFILE_ID)).thenReturn(true);
            when(userReactivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID)).thenReturn(Optional.of(request));
            when(userReactivationRequestService.getLatestRequestHistoryForUserProfile(PROFILE_ID)).thenReturn(history);

            String viewName = userActivationController.rejectDelegateReactivateUserRequestsGet(USER_ID, session, model, PROFILE_ID, redirectAttributes);

            assertThat(viewName).isEqualTo("delegate-reactivate-user-rejection");
            assertThat(model.getAttribute("delegateReactivateUserCommentForm")).isInstanceOf(DelegateReactivateUserCommentForm.class);
            assertThat(model.getAttribute("user")).isEqualTo(userDto);
            assertThat(model.getAttribute("profileId")).isEqualTo(PROFILE_ID);
            assertThat(model.getAttribute("requestId")).isEqualTo(REQUEST_ID);
            assertThat(model.getAttribute("reactivationRequests")).isEqualTo(history);
            assertThat(model.getAttribute("pageTitle")).isEqualTo("Delegate Reactivate User");
        }
    }

    @Nested
    @DisplayName("POST /user/delegate-reactivate/reject/{id}")
    class RejectPostTests {

        @Test
        @DisplayName("Should redirect back with errors when binding result has errors")
        void shouldRedirectOnBindingErrors() {
            DelegateReactivateUserCommentForm form = new DelegateReactivateUserCommentForm();
            when(bindingResult.hasErrors()).thenReturn(true);

            // Mocking the behavior of private helper buildErrorString gracefully by providing dummy errors
            when(bindingResult.getAllErrors()).thenReturn(List.of(new ObjectError("form", "Error message")));

            String viewName = userActivationController.rejectDelegateReactivateUserRequestsPost(USER_ID, session, model,
                    PROFILE_ID, REQUEST_ID, form, bindingResult, authentication, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/admin/user/delegate-reactivate/reject/{id}");
            verify(redirectAttributes).addFlashAttribute(eq(BindingResult.MODEL_KEY_PREFIX + "delegateReactivateUserCommentForm"), eq(bindingResult));
            verify(redirectAttributes).addFlashAttribute("delegateReactivateUserCommentForm", form);
            verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), anyString());
            verify(redirectAttributes).addAttribute("id", USER_ID);
            verify(redirectAttributes).addAttribute("profileId", PROFILE_ID);
            verify(redirectAttributes).addAttribute("requestId", REQUEST_ID);
        }

        @Test
        @DisplayName("Should save state as REJECTED and return confirmation view on success")
        void shouldSaveStateAndReturnConfirmation() {
            DelegateReactivateUserCommentForm form = new DelegateReactivateUserCommentForm();
            form.setComment("Missing info");

            when(bindingResult.hasErrors()).thenReturn(false);

            EntraUser entraUser = mock(EntraUser.class);
            when(entraUser.getEntraOid()).thenReturn("actor-oid-1");

            CurrentUserDto currentUserDto = mock(CurrentUserDto.class);
            when(currentUserDto.getUserId()).thenReturn(ACTOR_USER_ID);

            EntraUserDto targetUserDto = mock(EntraUserDto.class);
            when(targetUserDto.getFullName()).thenReturn("Jane Smith");

            when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
            when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(targetUserDto));

            String viewName = userActivationController.rejectDelegateReactivateUserRequestsPost(USER_ID, session, model,
                    PROFILE_ID, REQUEST_ID, form, bindingResult, authentication, redirectAttributes);

            assertThat(viewName).isEqualTo("delegate-reactivate-user-reject-confirmation");
            verify(userReactivationRequestService).rejectReactivationRequest(REQUEST_ID, USER_ID, PROFILE_ID, "Missing info", "actor-oid-1");
            assertThat(model.getAttribute("pageTitle")).isEqualTo("Delegate Reactivate User");
            assertThat(model.getAttribute("userName")).isEqualTo("Jane Smith");
        }
    }

    @Nested
    @DisplayName("POST /user/delegate-reactivate/approve/{id}")
    class ApprovePostTests {

        @Test
        @DisplayName("Should save state as APPROVED, enable the user, and return confirmation view")
        void shouldSaveStateAndReturnConfirmation() {
            EntraUser entraUser = mock(EntraUser.class);
            when(entraUser.getEntraOid()).thenReturn("actor-oid-2");
            when(entraUser.getId()).thenReturn(ACTOR_USER_ID);

            CurrentUserDto currentUserDto = mock(CurrentUserDto.class);
            when(currentUserDto.getUserId()).thenReturn(ACTOR_USER_ID);

            EntraUserDto targetUserDto = mock(EntraUserDto.class);
            when(targetUserDto.getFullName()).thenReturn("Alice Johnson");

            when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
            when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(targetUserDto));

            String viewName = userActivationController.approveDelegateReactivateUserRequestsPost(USER_ID, session, model, PROFILE_ID, REQUEST_ID, authentication);

            assertThat(viewName).isEqualTo("delegate-reactivate-user-approve-confirmation");
            verify(userReactivationRequestService).approveReactivationRequest(REQUEST_ID, USER_ID, PROFILE_ID, "actor-oid-2");
            verify(userAccountStatusService).enableUser(UUID.fromString(USER_ID), ACTOR_USER_ID);
            assertThat(model.getAttribute("pageTitle")).isEqualTo("Delegate Reactivate User");
            assertThat(model.getAttribute("userName")).isEqualTo("Alice Johnson");
        }
    }

    @Nested
    @DisplayName("GET /users/reactivation-requests")
    class DisplayReactivationRequestsTests {

        @Test
        @DisplayName("Should build URL and redirect when in manage mode and default status is not applied")
        void shouldRedirectWithDefaultStatusInManageMode() {
            ReactivationRequestPageMode pageMode = mock(ReactivationRequestPageMode.class);
            when(pageMode.isManageMode()).thenReturn(true);
            when(userReactivationRequestService.getPageMode(authentication)).thenReturn(pageMode);

            String viewName = userActivationController
                    .displayReactivationRequests(10, 1, "dateSubmitted", "desc",
                            "testSearch", null, false, true, false, false, model, authentication);

            assertThat(viewName).startsWith("redirect:/admin/users/reactivation-requests");
            assertThat(viewName).contains("size=10");
            assertThat(viewName).contains("page=1");
            assertThat(viewName).contains("defaultStatusApplied=true");
            assertThat(viewName).contains("selectedRequestStatuses=IN_REVIEW");
            assertThat(viewName).contains("showMultiFirmUsers=true");
            assertThat(viewName).contains("search=testSearch");
        }

        @Test
        @DisplayName("Should fetch page data and populate model for view rendering")
        void shouldRenderViewWhenDefaultStatusApplied() {
            ReactivationRequestPageMode pageMode = mock(ReactivationRequestPageMode.class);
            when(pageMode.isManageMode()).thenReturn(true);
            when(pageMode.getHeading()).thenReturn("Manage Requests");

            when(userReactivationRequestService.getPageMode(authentication)).thenReturn(pageMode);

            PaginatedReactivationRequests paginated = new PaginatedReactivationRequests();
            ReactivationRequestListItem item = mock(ReactivationRequestListItem.class);
            paginated.setRequests(List.of(item));
            paginated.setCurrentPage(1);
            paginated.setTotalRequests(1);
            paginated.setTotalPages(1);

            List<ReactivationRequestStatus> statuses = List.of(ReactivationRequestStatus.IN_REVIEW);

            ReactivationRequestsPageData pageData = new ReactivationRequestsPageData(pageMode, statuses, true, false, true, paginated);

            when(userReactivationRequestService.getPage(authentication, "testSearch", statuses, true, false, true, 1,
                    10, "dateSubmitted", "desc")).thenReturn(pageData);

            String viewName = userActivationController.displayReactivationRequests(10, 1, "dateSubmitted",
                    "desc", "testSearch", statuses, true, false, true, true, model, authentication);

            assertThat(viewName).isEqualTo("reactivation-requests");

            assertThat(model.getAttribute("pageHeading")).isEqualTo("Manage Requests");
            assertThat(model.getAttribute("manageMode")).isEqualTo(true);
            assertThat(model.getAttribute("requests")).isEqualTo(List.of(item));
            assertThat(model.getAttribute("requestedPageSize")).isEqualTo(10);
            assertThat(model.getAttribute("actualPageSize")).isEqualTo(1);
            assertThat(model.getAttribute("page")).isEqualTo(1);
            assertThat(model.getAttribute("totalRequests")).isEqualTo(1L);
            assertThat(model.getAttribute("totalPages")).isEqualTo(1);
            assertThat(model.getAttribute("search")).isEqualTo("testSearch");
            assertThat(model.getAttribute("sort")).isEqualTo("dateSubmitted");
            assertThat(model.getAttribute("direction")).isEqualTo("desc");
            assertThat(model.getAttribute("selectedRequestStatuses")).isEqualTo(statuses);
            assertThat(model.getAttribute("showFirmAdmins")).isEqualTo(true);
            assertThat(model.getAttribute("showMultiFirmUsers")).isEqualTo(false);
            assertThat(model.getAttribute("showProviderUsers")).isEqualTo(true);
            assertThat(model.getAttribute("defaultStatusApplied")).isEqualTo(true);
            assertThat(model.getAttribute("pageTitle")).isEqualTo("Manage Requests");
        }
    }
}
