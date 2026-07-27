package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRequestStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;
import uk.gov.justice.laa.portal.landingpage.repository.UserActivationRequestRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserReactivationActivationRequestServiceTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    @Mock
    private UserActivationRequestRepository requestRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @InjectMocks
    private UserReactivationActivationRequestService service;

    private EntraUserDto buildEntraUserDto(UUID entraOid) {
        return EntraUserDto.builder().id("entra-id").entraOid(entraOid.toString()).fullName("Test User").email("test.user@justice.gov.uk").build();
    }

    private UserActivationRequest buildUserActivationRequest(ReactivationRequestStatus status, int version) {
        return UserActivationRequest.builder().id(UUID.randomUUID()).requestId(REQUEST_ID).userProfileId(PROFILE_ID).status(status).version(version).actorEntraOid(ACTOR_ID).build();
    }

    // ==========================================
    // findFirstByUserProfileIdOrderByVersionDesc
    // ==========================================
    @Nested
    @DisplayName("findFirstByUserProfileIdOrderByVersionDesc")
    class FindFirstByUserProfileIdOrderByVersionDescTests {

        @Test
        @DisplayName("Should return empty optional when no request exists for profile ID")
        void noRequestFound_returnsEmptyOptional() {
            given(requestRepository.findFirstByUserProfileIdOrderByVersionDesc(PROFILE_ID)).willReturn(Optional.empty());

            Optional<UserActivationRequest> result = service.findFirstByUserProfileIdOrderByVersionDesc(PROFILE_ID);

            assertThat(result).isEmpty();
            verify(requestRepository).findFirstByUserProfileIdOrderByVersionDesc(PROFILE_ID);
        }

        @Test
        @DisplayName("Should return request when found for profile ID")
        void requestFound_returnsRequest() {
            UserActivationRequest existing = buildUserActivationRequest(ReactivationRequestStatus.IN_REVIEW, 1);
            given(requestRepository.findFirstByUserProfileIdOrderByVersionDesc(PROFILE_ID)).willReturn(Optional.of(existing));

            Optional<UserActivationRequest> result = service.findFirstByUserProfileIdOrderByVersionDesc(PROFILE_ID);

            assertThat(result).contains(existing);
        }
    }

    // ==========================================
    // Builder Helpers
    // ==========================================

    // ==========================================
    // createNewRequest
    // ==========================================
    @Nested
    @DisplayName("createNewRequest")
    class CreateNewRequestTests {

        @Test
        @DisplayName("Should create new request when no existing request is present")
        void noExistingRequest_createsNewRequest() {
            EntraUserDto user = buildEntraUserDto(ACTOR_ID);
            given(requestRepository.findFirstByUserProfileIdOrderByVersionDesc(PROFILE_ID)).willReturn(Optional.empty());

            UserActivationRequest result = service.createNewRequest(REQUEST_ID, PROFILE_ID, "Reactivation reason", user);

            ArgumentCaptor<UserActivationRequest> captor = ArgumentCaptor.forClass(UserActivationRequest.class);
            verify(requestRepository).save(captor.capture());

            UserActivationRequest saved = captor.getValue();
            assertThat(saved.getRequestId()).isEqualTo(REQUEST_ID);
            assertThat(saved.getUserProfileId()).isEqualTo(PROFILE_ID);
            assertThat(saved.getStatus()).isEqualTo(ReactivationRequestStatus.IN_REVIEW);
            assertThat(saved.getComments()).isEqualTo("Reactivation reason");
            assertThat(saved.getActorEntraOid()).isEqualTo(ACTOR_ID);
            assertThat(saved.getCreatedAt()).isNotNull();

            assertThat(result).isEqualTo(saved);
        }

        @Test
        @DisplayName("Should create new request when existing request is REJECTED")
        void existingRejectedRequest_createsNewRequest() {
            EntraUserDto user = buildEntraUserDto(ACTOR_ID);
            UserActivationRequest rejected = buildUserActivationRequest(ReactivationRequestStatus.REJECTED, 1);
            given(requestRepository.findFirstByUserProfileIdOrderByVersionDesc(PROFILE_ID)).willReturn(Optional.of(rejected));

            UserActivationRequest result = service.createNewRequest(REQUEST_ID, PROFILE_ID, "Retry reason", user);

            assertThat(result).isNotNull();
            verify(requestRepository).save(any(UserActivationRequest.class));
        }

        @Test
        @DisplayName("Should create new request when existing request is APPROVED")
        void existingApprovedRequest_createsNewRequest() {
            EntraUserDto user = buildEntraUserDto(ACTOR_ID);
            UserActivationRequest approved = buildUserActivationRequest(ReactivationRequestStatus.APPROVED, 1);
            given(requestRepository.findFirstByUserProfileIdOrderByVersionDesc(PROFILE_ID)).willReturn(Optional.of(approved));

            UserActivationRequest result = service.createNewRequest(REQUEST_ID, PROFILE_ID, "New request", user);

            assertThat(result).isNotNull();
            verify(requestRepository).save(any(UserActivationRequest.class));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when request is currently IN_REVIEW")
        void activeRequestInReview_throwsIllegalStateException() {
            EntraUserDto user = buildEntraUserDto(ACTOR_ID);
            UserActivationRequest inReview = buildUserActivationRequest(ReactivationRequestStatus.IN_REVIEW, 1);
            given(requestRepository.findFirstByUserProfileIdOrderByVersionDesc(PROFILE_ID)).willReturn(Optional.of(inReview));

            assertThatThrownBy(() -> service.createNewRequest(REQUEST_ID, PROFILE_ID, "Reason", user))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Request already being processed for user " + PROFILE_ID);

            verify(requestRepository, never()).save(any());
        }
    }

    // ==========================================
    // saveRequestState
    // ==========================================
    @Nested
    @DisplayName("saveRequestState")
    class SaveRequestStateTests {

        @BeforeEach
        void setupUserProfiles() {
            given(userProfileRepository.existsById(PROFILE_ID)).willReturn(true);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when target user profile does not exist")
        void targetUserNotFound_throwsException() {
            given(userProfileRepository.existsById(PROFILE_ID)).willReturn(false);

            assertThatThrownBy(() -> service.saveRequestState(REQUEST_ID, PROFILE_ID, ReactivationRequestStatus.APPROVED, "Comments", ACTOR_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Target user not found with ID: " + PROFILE_ID);

            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when acting user profile does not exist")
        void actingUserNotFound_throwsException() {
            given(userProfileRepository.existsById(ACTOR_ID)).willReturn(false);

            assertThatThrownBy(() -> service.saveRequestState(REQUEST_ID, PROFILE_ID, ReactivationRequestStatus.APPROVED, "Comments", ACTOR_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Acting user not found with ID: " + ACTOR_ID);

            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should save state with version 1 when requestId is null")
        void nullRequestId_generatesUuidAndVersionOne() {
            given(requestRepository.save(any(UserActivationRequest.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(userProfileRepository.existsById(ACTOR_ID)).willReturn(true);

            UserActivationRequest result = service.saveRequestState(null, PROFILE_ID, ReactivationRequestStatus.APPROVED, "Approved by admin", ACTOR_ID);

            assertThat(result.getRequestId()).isNotNull();
            assertThat(result.getVersion()).isEqualTo(1);
            assertThat(result.getStatus()).isEqualTo(ReactivationRequestStatus.APPROVED);
            assertThat(result.getComments()).isEqualTo("Approved by admin");
            assertThat(result.getUserProfileId()).isEqualTo(PROFILE_ID);
            assertThat(result.getActorEntraOid()).isEqualTo(ACTOR_ID);
        }

        @Test
        @DisplayName("Should save state with version 1 when non-null requestId has no prior history")
        void nonNullRequestIdNoHistory_savesVersionOne() {
            given(requestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.empty());
            given(requestRepository.save(any(UserActivationRequest.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(userProfileRepository.existsById(ACTOR_ID)).willReturn(true);

            UserActivationRequest result = service.saveRequestState(REQUEST_ID, PROFILE_ID, ReactivationRequestStatus.IN_REVIEW, "Reviewing", ACTOR_ID);

            assertThat(result.getRequestId()).isEqualTo(REQUEST_ID);
            assertThat(result.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should increment version when existing record is IN_REVIEW")
        void activeRequestInReview_incrementsVersion() {
            UserActivationRequest existing = buildUserActivationRequest(ReactivationRequestStatus.IN_REVIEW, 2);
            given(requestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.of(existing));
            given(requestRepository.save(any(UserActivationRequest.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(userProfileRepository.existsById(ACTOR_ID)).willReturn(true);

            UserActivationRequest result = service.saveRequestState(REQUEST_ID, PROFILE_ID, ReactivationRequestStatus.APPROVED, "Approved", ACTOR_ID);

            assertThat(result.getRequestId()).isEqualTo(REQUEST_ID);
            assertThat(result.getVersion()).isEqualTo(3);
            assertThat(result.getStatus()).isEqualTo(ReactivationRequestStatus.APPROVED);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when request has already been APPROVED")
        void alreadyApproved_throwsIllegalStateException() {
            UserActivationRequest existing = buildUserActivationRequest(ReactivationRequestStatus.APPROVED, 1);
            given(requestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.of(existing));
            given(userProfileRepository.existsById(ACTOR_ID)).willReturn(true);

            assertThatThrownBy(() -> service.saveRequestState(REQUEST_ID, PROFILE_ID, ReactivationRequestStatus.REJECTED, "Cannot reject", ACTOR_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(String.format("Request already processed for user %s. Request ID: %s", PROFILE_ID, REQUEST_ID));

            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when request has already been REJECTED")
        void alreadyRejected_throwsIllegalStateException() {
            UserActivationRequest existing = buildUserActivationRequest(ReactivationRequestStatus.REJECTED, 1);
            given(requestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.of(existing));
            given(userProfileRepository.existsById(ACTOR_ID)).willReturn(true);

            assertThatThrownBy(() -> service.saveRequestState(REQUEST_ID, PROFILE_ID, ReactivationRequestStatus.APPROVED, "Cannot approve", ACTOR_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(String.format("Request already processed for user %s. Request ID: %s", PROFILE_ID, REQUEST_ID));

            verify(requestRepository, never()).save(any());
        }
    }
}
