package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRequestStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;
import uk.gov.justice.laa.portal.landingpage.forms.DelegateReactivateUserReasonForm;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserReactivationActivationRequestService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class UserActivationControllerTest {

    private UserActivationController userActivationController;

    @Mock
    private LoginService loginService;
    @Mock
    private UserService userService;
    @Mock
    private UserReactivationActivationRequestService userReactivationActivationRequestService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private HttpSession session;
    @Mock
    private Authentication authentication;
    @Mock
    private RedirectAttributes redirectAttributes;
    private Model model;

    private static final String USER_ID = "entra-user-123";
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final String REFERER = "http://localhost/admin/users";

    @BeforeEach
    void setUp() {
        userActivationController = new UserActivationController(loginService, userService, userReactivationActivationRequestService);
        userActivationController.disableUserFeatureEnabled = true;
        lenient().when(accessControlService.getEnablementFlags(any()))
                .thenReturn(new AccessControlService.EnablementFlags(false, false, false));
        model = new ExtendedModelMap();
    }


    // ==========================================
    // GET /user/delegate-reactivate/{id}
    // ==========================================
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

            given(userService.getEntraUserById(USER_ID)).willReturn(Optional.of(user));
            given(userReactivationActivationRequestService.findFirstByUserProfileIdOrderByVersionDesc(PROFILE_ID))
                    .willReturn(Optional.of(pendingRequest));

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

            given(userService.getEntraUserById(USER_ID)).willReturn(Optional.of(user));
            given(userReactivationActivationRequestService.findFirstByUserProfileIdOrderByVersionDesc(PROFILE_ID))
                    .willReturn(Optional.of(rejectedRequest));

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

    // ==========================================
    // POST /user/delegate-reactivate/{id}
    // ==========================================
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

            given(userService.getEntraUserById(USER_ID)).willReturn(Optional.of(user));

            String view = userActivationController.delegateReactivateUserPost(USER_ID, model, session, REFERER, PROFILE_ID);

            assertThat(view).isEqualTo("redirect:/admin/users/delegate-reactivate-user-reason/" + USER_ID);
            assertThat(session.getAttribute("delegateReactivateUserId")).isEqualTo(USER_ID);
            assertThat(session.getAttribute("profileId")).isEqualTo(PROFILE_ID);
        }
    }

    // ==========================================
    // GET /users/delegate-reactivate-user-reason/{id}
    // ==========================================
    @Nested
    @DisplayName("GET /users/delegate-reactivate-user-reason/{id}")
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
            given(userService.getEntraUserById(USER_ID)).willReturn(Optional.of(user));

            String view = userActivationController.delegateReactivateUserReasonsGet(USER_ID, model, session);

            assertThat(view).isEqualTo("delegate-reactivate-user-reason");
            assertThat(model.asMap())
                    .containsEntry("user", user)
                    .containsEntry("profileId", PROFILE_ID)
                    .containsEntry("delegateReactivateUserReasonForm", existingForm);
        }
    }

    // ==========================================
    // POST /users/delegate-reactivate-user-reason/{id}
    // ==========================================
    @Nested
    @DisplayName("POST /users/delegate-reactivate-user-reason/{id}")
    class DelegateReactivateUserReasonsPostTests {

        @Test
        @DisplayName("Should re-render form with error message when validation fails")
        void validationErrors_returnsReasonViewWithError() {
            session = new MockHttpSession();
            session.setAttribute("profileId", PROFILE_ID);
            DelegateReactivateUserReasonForm form = DelegateReactivateUserReasonForm.builder().build();

            BindingResult bindingResult = Mockito.mock(BindingResult.class);
            given(bindingResult.hasErrors()).willReturn(true);

            EntraUserDto user = buildEntraUserDto();
            given(userService.getEntraUserById(USER_ID)).willReturn(Optional.of(user));

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

            BindingResult bindingResult = Mockito.mock(BindingResult.class);
            given(bindingResult.hasErrors()).willReturn(false);
            EntraUserDto user = buildEntraUserDto();
            given(userService.getEntraUserById(USER_ID)).willReturn(Optional.of(user));

            String view = userActivationController.delegateReactivateUserReasonsPost(USER_ID, form, bindingResult, model, session);

            assertThat(view).isEqualTo("redirect:/admin/users/delegate-reactivate-user-check-answers/" + USER_ID);
            assertThat(session.getAttribute("delegateReactivateUserReasonForm")).isEqualTo(form);
        }
    }

    // ==========================================
    // Builder Helpers
    // ==========================================

    // ==========================================
    // GET /users/delegate-reactivate-user-check-answers/{id}
    // ==========================================
    @Nested
    @DisplayName("GET /users/delegate-reactivate-user-check-answers/{id}")
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
            given(userService.getEntraUserById(USER_ID)).willReturn(Optional.of(user));

            String view = userActivationController.delegateReactivateUserReasonsCheckAnswersGet(USER_ID, model, session);

            assertThat(view).isEqualTo("delegate-reactivate-user-check-answers");
            assertThat(model.asMap())
                    .containsEntry("user", user)
                    .containsEntry("profileId", PROFILE_ID)
                    .containsEntry("delegateReactivateUserReasonForm", form);
        }
    }

    // ==========================================
    // POST /users/delegate-reactivate-user-check-answers/{id}
    // ==========================================
    @Nested
    @DisplayName("POST /users/delegate-reactivate-user-check-answers/{id}")
    class DelegateReactivateUserReasonsCheckAnswersPostTests {

        @Test
        @DisplayName("Should throw 400 if user profile ID does not match session profile ID")
        void profileIdMismatch_throws400() {
            session = new MockHttpSession();
            session.setAttribute("delegateReactivateUserId", USER_ID);
            session.setAttribute("profileId", PROFILE_ID);
            session.setAttribute("delegateReactivateUserReasonForm", DelegateReactivateUserReasonForm.builder().build());

            UserProfileDto mismatchedProfile = UserProfileDto.builder().id(UUID.randomUUID()).build();
            given(userService.getActiveProfileByUserId(USER_ID)).willReturn(Optional.of(mismatchedProfile));

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
            EntraUserDto user = buildEntraUserDto();
            CurrentUserDto actor = new CurrentUserDto();
            actor.setUserId(UUID.randomUUID());
            UserActivationRequest createdRequest = buildUserActivationRequest(ReactivationRequestStatus.IN_REVIEW);

            given(userService.getActiveProfileByUserId(USER_ID)).willReturn(Optional.of(userProfile));
            given(userService.getEntraUserById(USER_ID)).willReturn(Optional.of(user));
            given(loginService.getCurrentUser(authentication)).willReturn(actor);
            given(userReactivationActivationRequestService.createNewRequest(any(UUID.class), eq(PROFILE_ID), eq("Approved leave returned"), eq(user)))
                    .willReturn(createdRequest);

            String view = userActivationController.delegateReactivateUserReasonsCheckAnswersPost(USER_ID, authentication, model, httpSession);

            assertThat(view).isEqualTo("delegate-reactivate-user-confirmation");
            assertThat(model.asMap()).containsEntry("user", user);

            verify(userReactivationActivationRequestService)
                    .createNewRequest(any(UUID.class), eq(PROFILE_ID), eq("Approved leave returned"), eq(user));
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
