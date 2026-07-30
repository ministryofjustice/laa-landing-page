package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserActivationControllerTest {

    private UserActivationController userActivationController;

    @Mock
    private LoginService loginService;
    @Mock
    private UserService userService;
    @Mock
    private UserReactivationActivationRequestService userReactivationActivationRequestService;
    @Mock
    private HttpSession session;
    @Mock
    private Authentication authentication;
    @Mock
    private RedirectAttributes redirectAttributes;
    private Model model;

    private static final String USER_ID = UUID.randomUUID().toString();
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final String REFERER = "http://localhost/admin/users";

    @BeforeEach
    void setUp() {
        userActivationController = new UserActivationController(loginService, userService, userReactivationActivationRequestService);
        userActivationController.disableUserFeatureEnabled = true;
        model = new ExtendedModelMap();
    }

    @Nested
    @DisplayName("GET /user/delegate-reactivate/{id}")
    class DelegateReactivateUserGetTests {

        @Test
        @DisplayName("Should throw 404 when feature flag is disabled")
        void featureDisabled_throws404() {
            userActivationController.disableUserFeatureEnabled = false;

            assertThatThrownBy(() -> userActivationController.delegateReactivateUserGet(USER_ID, session, model, REFERER, PROFILE_ID, redirectAttributes))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("404");
        }

        @Test
        @DisplayName("Should redirect with error when request is already in progress (PENDING status)")
        void requestInProgress_redirectsToManagePage() {
            EntraUserDto user = buildEntraUserDto();
            UserActivationRequest pendingRequest = buildUserActivationRequest(ReactivationRequestStatus.IN_REVIEW);

            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));
            when(userReactivationActivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID))
                    .thenReturn(Optional.of(pendingRequest));

            redirectAttributes = new RedirectAttributesModelMap();
            String view = userActivationController.delegateReactivateUserGet(USER_ID, session, model, REFERER, PROFILE_ID, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin/users/manage/" + PROFILE_ID);
            assertThat(redirectAttributes.getFlashAttributes())
                    .extractingByKey("errorMessage")
                    .isEqualTo("A delegate request is already in progress");
            assertThat(redirectAttributes.getFlashAttributes())
                    .extractingByKey("requestId")
                    .isEqualTo(pendingRequest.getId());
        }

        @Test
        @DisplayName("Should populate model and session when no active request exists (or is REJECTED/APPROVED)")
        void noActiveRequest_rendersForm() {
            session = new MockHttpSession();
            EntraUserDto user = buildEntraUserDto();
            UserActivationRequest rejectedRequest = buildUserActivationRequest(ReactivationRequestStatus.REJECTED);

            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));
            when(userReactivationActivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID))
                    .thenReturn(Optional.of(rejectedRequest));

            String view = userActivationController.delegateReactivateUserGet(USER_ID, session, model, REFERER, PROFILE_ID, redirectAttributes);

            assertThat(view).isEqualTo("delegate-reactivate-user");
            assertThat(model.asMap())
                    .containsEntry("user", user)
                    .containsEntry("profileId", PROFILE_ID)
                    .containsEntry("referer", REFERER);

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

            assertThatThrownBy(() -> userActivationController.delegateReactivateUserPost(USER_ID, model, session, REFERER, PROFILE_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("403");
        }

        @Test
        @DisplayName("Should set attributes and redirect to reasons step on success")
        void validSession_redirectsToReasons() {
            session = new MockHttpSession();
            session.setAttribute("delegateReactivateUserId", USER_ID);
            EntraUserDto user = buildEntraUserDto();

            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));

            String view = userActivationController.delegateReactivateUserPost(USER_ID, model, session, REFERER, PROFILE_ID);

            assertThat(view).isEqualTo("redirect:/admin/user/delegate-reactivate-user-reason/" + USER_ID);
            assertThat(session.getAttribute("delegateReactivateUserId")).isEqualTo(USER_ID);
            assertThat(session.getAttribute("profileId")).isEqualTo(PROFILE_ID);
        }
    }

    @Nested
    @DisplayName("GET /user/delegate-reactivate-user-reason/{id}")
    class DelegateReactivateUserReasonsGetTests {

        @Test
        @DisplayName("Should throw 404 when feature flag is disabled")
        void featureDisabled_throws404() {
            userActivationController.disableUserFeatureEnabled = false;

            assertThatThrownBy(() -> userActivationController.delegateReactivateUserReasonsGet(USER_ID, model, session))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("404");
        }

        @Test
        @DisplayName("Should throw 403 when session user ID does not match path variable")
        void invalidSessionUser_throws403() {
            session = new MockHttpSession();
            session.setAttribute("delegateReactivateUserId", "invalid-id");

            assertThatThrownBy(() -> userActivationController.delegateReactivateUserReasonsGet(USER_ID, model, session))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("403");
        }

        @Test
        @DisplayName("Should load form from session if present or create new instance")
        void validSession_rendersReasonsView() {
            session = new MockHttpSession();
            session.setAttribute("delegateReactivateUserId", USER_ID);
            session.setAttribute("profileId", PROFILE_ID);

            DelegateReactivateUserReasonForm existingForm = DelegateReactivateUserReasonForm.builder()
                    .reason("Existing reason")
                    .build();
            session.setAttribute("delegateReactivateUserReasonForm", existingForm);

            EntraUserDto user = buildEntraUserDto();
            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));

            String view = userActivationController.delegateReactivateUserReasonsGet(USER_ID, model, session);

            assertThat(view).isEqualTo("delegate-reactivate-user-reason");
            assertThat(model.asMap())
                    .containsEntry("user", user)
                    .containsEntry("profileId", PROFILE_ID)
                    .containsEntry("delegateReactivateUserReasonForm", existingForm);
        }
    }

    @Nested
    @DisplayName("POST /user/delegate-reactivate-user-reason/{id}")
    class DelegateReactivateUserReasonsPostTests {

        @Test
        @DisplayName("Should re-render form with error message when validation fails")
        void validationErrors_returnsReasonViewWithError() {
            session = new MockHttpSession();
            session.setAttribute("profileId", PROFILE_ID);
            DelegateReactivateUserReasonForm form = DelegateReactivateUserReasonForm.builder().build();

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);

            EntraUserDto user = buildEntraUserDto();
            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));

            String view = userActivationController.delegateReactivateUserReasonsPost(USER_ID, form, bindingResult, model, session);

            assertThat(view).isEqualTo("delegate-reactivate-user-reason");
            assertThat(model.asMap()).containsKey("errorMessage");
            assertThat(session.getAttribute("delegateReactivateUserReasonForm")).isNull();
        }

        @Test
        @DisplayName("Should save form to session and redirect to check answers when validation passes")
        void validationSuccess_savesSessionAndRedirects() {
            session = new MockHttpSession();
            session.setAttribute("profileId", PROFILE_ID);
            DelegateReactivateUserReasonForm form = DelegateReactivateUserReasonForm.builder()
                    .reason("Valid user reactivation request reason")
                    .build();

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(false);
            EntraUserDto user = buildEntraUserDto();
            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));

            String view = userActivationController.delegateReactivateUserReasonsPost(USER_ID, form, bindingResult, model, session);

            assertThat(view).isEqualTo("redirect:/admin/user/delegate-reactivate-user-check-answers/" + USER_ID);
            assertThat(session.getAttribute("delegateReactivateUserReasonForm")).isEqualTo(form);
        }
    }

    @Nested
    @DisplayName("GET /user/delegate-reactivate-user-check-answers/{id}")
    class DelegateReactivateUserReasonsCheckAnswersGetTests {

        @Test
        @DisplayName("Should throw 403 if session ID does not match path variable")
        void invalidSessionUser_throws403() {
            session = new MockHttpSession();
            session.setAttribute("profileId", PROFILE_ID);
            session.setAttribute("delegateReactivateUserId", "mismatched-id");

            assertThatThrownBy(() -> userActivationController.delegateReactivateUserReasonsCheckAnswersGet(USER_ID, model, session))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("403");
        }

        @Test
        @DisplayName("Should throw NoSuchElementException if form is missing from session")
        void missingFormInSession_throwsException() {
            session.setAttribute("profileId", PROFILE_ID);
            session.setAttribute("delegateReactivateUserId", USER_ID);

            assertThatThrownBy(() -> userActivationController.delegateReactivateUserReasonsCheckAnswersGet(USER_ID, model, session))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("Should render check answers view with populated model")
        void validSession_rendersCheckAnswersView() {
            session = new MockHttpSession();
            session.setAttribute("profileId", PROFILE_ID);
            session.setAttribute("delegateReactivateUserId", USER_ID);

            DelegateReactivateUserReasonForm form = DelegateReactivateUserReasonForm.builder().reason("Reactivation justification").build();
            session.setAttribute("delegateReactivateUserReasonForm", form);

            EntraUserDto user = buildEntraUserDto();
            when(userService.getEntraUserById(USER_ID)).thenReturn(Optional.of(user));

            String view = userActivationController.delegateReactivateUserReasonsCheckAnswersGet(USER_ID, model, session);

            assertThat(view).isEqualTo("delegate-reactivate-user-check-answers");
            assertThat(model.asMap())
                    .containsEntry("user", user)
                    .containsEntry("profileId", PROFILE_ID)
                    .containsEntry("delegateReactivateUserReasonForm", form);
        }
    }

    @Nested
    @DisplayName("POST /user/delegate-reactivate-user-check-answers/{id}")
    class DelegateReactivateUserReasonsCheckAnswersPostTests {

        @Test
        @DisplayName("Should throw 400 if user profile ID does not match session profile ID")
        void profileIdMismatch_throws400() {
            session = new MockHttpSession();
            session.setAttribute("delegateReactivateUserId", USER_ID);
            session.setAttribute("profileId", PROFILE_ID);
            session.setAttribute("delegateReactivateUserReasonForm", DelegateReactivateUserReasonForm.builder().build());

            UserProfileDto mismatchedProfile = UserProfileDto.builder().id(UUID.randomUUID()).build();
            when(userService.getActiveProfileByUserId(USER_ID)).thenReturn(Optional.of(mismatchedProfile));

            assertThatThrownBy(() -> userActivationController.delegateReactivateUserReasonsCheckAnswersPost(USER_ID, authentication, model, session))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("400");

            verify(userReactivationActivationRequestService, never()).createNewRequest(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should create activation request and render confirmation view on success")
        void validSubmission_createsRequestAndRendersConfirmation() {
            MockHttpSession httpSession = new MockHttpSession();
            httpSession.setAttribute("delegateReactivateUserId", USER_ID);
            httpSession.setAttribute("profileId", PROFILE_ID);

            DelegateReactivateUserReasonForm form = DelegateReactivateUserReasonForm.builder()
                    .reason("Approved leave returned")
                    .build();
            httpSession.setAttribute("delegateReactivateUserReasonForm", form);

            UserProfileDto userProfile = UserProfileDto.builder().id(PROFILE_ID).build();
            EntraUser entraUser = EntraUser.builder()
                    .id(UUID.fromString(USER_ID))
                    .entraOid(USER_ID)
                    .firstName("John Doe")
                    .email("john.doe@justice.gov.uk")
                    .build();
            CurrentUserDto actor = new CurrentUserDto();
            actor.setUserId(UUID.randomUUID());
            UserActivationRequest createdRequest = buildUserActivationRequest(ReactivationRequestStatus.IN_REVIEW);

            when(userService.getActiveProfileByUserId(USER_ID)).thenReturn(Optional.of(userProfile));
            when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
            when(loginService.getCurrentUser(any())).thenReturn(actor);
            when(userReactivationActivationRequestService.createNewRequest(any(UUID.class), any(), any(), any()))
                    .thenReturn(createdRequest);

            String view = userActivationController.delegateReactivateUserReasonsCheckAnswersPost(USER_ID, authentication, model, httpSession);

            assertThat(view).isEqualTo("delegate-reactivate-user-confirmation");
            assertThat(model.asMap()).containsEntry("user", entraUser);

            verify(userReactivationActivationRequestService)
                    .createNewRequest(any(UUID.class), eq(PROFILE_ID), eq("Approved leave returned"), eq(USER_ID));
        }
    }

    @Nested
    class TrackDelegateReactivateUserRequestsGetTests {

        @Test
        void get_WhenFeatureDisabled_ThrowsResponseStatusException404() {
            ReflectionTestUtils.setField(userActivationController, "disableUserFeatureEnabled", false);
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            assertThatThrownBy(() -> userActivationController.trackDelegateReactivateUserRequestsGet(
                    "user-123", session, model, "http://referer.com", UUID.randomUUID(), redirectAttributes))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("404");
        }

        @Test
        void get_WhenUserNotFound_ThrowsNoSuchElementException() {
            String userId = "non-existent-user";
            UUID profileId = UUID.randomUUID();
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            when(userService.getEntraUserById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userActivationController.trackDelegateReactivateUserRequestsGet(
                    userId, session, model, "http://referer.com", profileId, redirectAttributes))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void get_WhenRequestStatusIsApproved_RedirectsToManageUsersWithFlashAttributes() {
            String userId = "user-123";
            UUID profileId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            EntraUserDto mockUser = mock(EntraUserDto.class);
            UserActivationRequest mockRequest = mock(UserActivationRequest.class);

            when(userService.getEntraUserById(userId)).thenReturn(Optional.of(mockUser));
            when(userReactivationActivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(profileId))
                    .thenReturn(Optional.of(mockRequest));
            when(mockRequest.getStatus()).thenReturn(ReactivationRequestStatus.APPROVED);
            when(mockRequest.getId()).thenReturn(requestId);

            String view = userActivationController.trackDelegateReactivateUserRequestsGet(
                    userId, session, model, "http://referer.com", profileId, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin/users/manage/" + profileId);
            assertThat(redirectAttributes.getFlashAttributes())
                    .extractingByKey("errorMessage")
                    .isEqualTo("A delegate request is already in progress");
        }

        @Test
        void get_WhenRequestStatusIsRejected_RedirectsToManageUsersWithFlashAttributes() {
            String userId = "user-123";
            UUID profileId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            EntraUserDto mockUser = mock(EntraUserDto.class);
            UserActivationRequest mockRequest = mock(UserActivationRequest.class);

            when(userService.getEntraUserById(userId)).thenReturn(Optional.of(mockUser));
            when(userReactivationActivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(profileId))
                    .thenReturn(Optional.of(mockRequest));
            when(mockRequest.getStatus()).thenReturn(ReactivationRequestStatus.REJECTED);
            when(mockRequest.getId()).thenReturn(requestId);

            String view = userActivationController.trackDelegateReactivateUserRequestsGet(
                    userId, session, model, "http://referer.com", profileId, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin/users/manage/" + profileId);
            assertThat(redirectAttributes.getFlashAttributes())
                    .extractingByKey("errorMessage")
                    .isEqualTo("A delegate request is already in progress");
        }

        @Test
        void get_WhenRequestIsEmpty_RedirectsToManageUsersWithFlashErrorMessage() {
            String userId = "user-123";
            UUID profileId = UUID.randomUUID();
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            EntraUserDto mockUser = mock(EntraUserDto.class);

            when(userService.getEntraUserById(userId)).thenReturn(Optional.of(mockUser));
            when(userReactivationActivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(profileId))
                    .thenReturn(Optional.empty());

            String view = userActivationController.trackDelegateReactivateUserRequestsGet(
                    userId, session, model, "http://referer.com", profileId, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin/users/manage/" + profileId);
            assertThat(redirectAttributes.getFlashAttributes())
                    .extractingByKey("errorMessage")
                    .isEqualTo("No delegate requests present");
        }

        @Test
        void get_WhenRequestInProgress_PopulatesModelAndReturnsTrackingView() {
            String userId = "user-123";
            UUID profileId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            String referer = "http://localhost/previous-page";
            MockHttpSession session = new MockHttpSession();
            Model model = new ConcurrentModel();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            EntraUserDto mockUser = mock(EntraUserDto.class);
            when(mockUser.getFullName()).thenReturn("Alex Smith");

            UserActivationRequest mockRequest = mock(UserActivationRequest.class);
            when(mockRequest.getStatus()).thenReturn(ReactivationRequestStatus.IN_REVIEW);
            when(mockRequest.getRequestId()).thenReturn(requestId);

            List<UserActivationRequestSummaryDto> historyList = List.of(mock(UserActivationRequestSummaryDto.class));

            when(userService.getEntraUserById(userId)).thenReturn(Optional.of(mockUser));
            when(userReactivationActivationRequestService.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(profileId))
                    .thenReturn(Optional.of(mockRequest));
            when(userReactivationActivationRequestService.getLatestRequestHistoryForUserProfile(profileId))
                    .thenReturn(historyList);

            String view = userActivationController.trackDelegateReactivateUserRequestsGet(
                    userId, session, model, referer, profileId, redirectAttributes);

            assertThat(view).isEqualTo("delegate-reactivate-user-tracking");
            assertThat(model.getAttribute("user")).isEqualTo(mockUser);
            assertThat(model.getAttribute("profileId")).isEqualTo(profileId);
            assertThat(model.getAttribute("referer")).isEqualTo(referer);
            assertThat(model.getAttribute("requestId")).isEqualTo(requestId);
            assertThat(model.getAttribute("reactivationRequests")).isEqualTo(historyList);
            assertThat(model.getAttribute("delegateReactivateUserReasonForm")).isNotNull();
            assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Delegate Reactivate User - Alex Smith");
        }
    }

    @Nested
    class TrackDelegateReactivateUserRequestsPostTests {

        @Test
        void post_WhenBindingResultHasErrors_SetsFlashAndQueryAttributesAndRedirects() {
            UUID id = UUID.randomUUID();
            UUID profileId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();

            DelegateReactivateUserReasonForm form = new DelegateReactivateUserReasonForm();
            BindingResult bindingResult = mock(BindingResult.class);
            Authentication auth = mock(Authentication.class);
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            when(bindingResult.hasErrors()).thenReturn(true);
            // Assuming buildErrorString uses getAllErrors() or similar from BindingResult
            when(bindingResult.getAllErrors()).thenReturn(List.of(new ObjectError("reason", "Reason cannot be empty")));

            String view = userActivationController.trackDelegateReactivateUserRequestsPost(
                    id, profileId, requestId, form, bindingResult, auth, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin/user/delegate-reactivate/track/{id}");
            assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
            assertThat(redirectAttributes.asMap())
                    .containsEntry("id", id.toString())
                    .containsEntry("profileId", profileId.toString())
                    .containsEntry("requestId", requestId.toString());

            verifyNoInteractions(loginService);
            verify(userReactivationActivationRequestService, never())
                    .saveRequestState(any(), any(), any(), any(), any());
        }

        @Test
        void post_WhenFormIsValid_SavesRequestAndRedirectsWithQueryAttributes() {
            final UUID id = UUID.randomUUID();
            final UUID profileId = UUID.randomUUID();
            final UUID requestId = UUID.randomUUID();
            String entraOid = UUID.randomUUID().toString();
            String actorUserId = UUID.randomUUID().toString();

            DelegateReactivateUserReasonForm form = new DelegateReactivateUserReasonForm();
            form.setReason("Reactivation approved for medical leave return.");

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
            when(userReactivationActivationRequestService.calculateNextReactivationRequestStatus(profileId))
                    .thenReturn(ReactivationRequestStatus.APPROVED);

            String view = userActivationController.trackDelegateReactivateUserRequestsPost(
                    id, profileId, requestId, form, bindingResult, auth, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin/user/delegate-reactivate/track/{id}");
            assertThat(redirectAttributes.asMap())
                    .containsEntry("id", id.toString())
                    .containsEntry("profileId", profileId.toString())
                    .containsEntry("requestId", requestId.toString());

            verify(userReactivationActivationRequestService).saveRequestState(
                    eq(requestId),
                    eq(profileId),
                    eq(ReactivationRequestStatus.APPROVED),
                    eq("Reactivation approved for medical leave return."),
                    eq(entraOid)
            );
        }
    }

    private EntraUserDto buildEntraUserDto() {
        return EntraUserDto.builder()
                .id(USER_ID)
                .fullName("John Doe")
                .email("john.doe@justice.gov.uk")
                .build();
    }

    private UserActivationRequest buildUserActivationRequest(ReactivationRequestStatus status) {
        return UserActivationRequest.builder()
                .id(UUID.randomUUID())
                .requestId(UUID.randomUUID())
                .userProfileId(PROFILE_ID)
                .status(status)
                .version(1)
                .build();
    }
}
