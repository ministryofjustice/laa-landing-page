package uk.gov.justice.laa.portal.landingpage.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import jakarta.persistence.EntityNotFoundException;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.ReactivationRequestsPageData;
import uk.gov.justice.laa.portal.landingpage.dto.UserActivationRequestSummaryDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRoleType;
import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedReactivationRequests;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestListItem;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestPageMode;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestStatus;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserActivationRequestRepository;

@ExtendWith(MockitoExtension.class)
class UserReactivationRequestServiceTest {

    private static final String UNKNOWN_USER_NAME = "Unknown user";

    private static final String REQUEST_ID_STR = "11111111-1111-1111-1111-111111111111";
    @Mock
    private EntraUserRepository entraUserRepository;
    @Mock
    private ReactivationTypeResolver roleTypeResolver;
    @InjectMocks
    private UserReactivationRequestService service;
    @Mock
    private LoginService loginService;
    @Mock
    private FirmService firmService;
    @Mock
    private Authentication authentication;
    private static final UUID REQUEST_ID = UUID.fromString(REQUEST_ID_STR);
    private static final String USER_ENTRA_ID_STR = "22222222-2222-2222-2222-222222222222";
    private static final UUID USER_ENTRA_ID = UUID.fromString(USER_ENTRA_ID_STR);
    private static final String USER_PROFILE_ID_STR = "33333333-3333-3333-3333-333333333333";
    private static final UUID USER_PROFILE_ID = UUID.fromString(USER_PROFILE_ID_STR);
    private static final String ACTOR_ENTRA_OID = "actor-oid-123";
    private static final String INITIAL_ACTOR_ENTRA_OID = "initial-actor-oid-456";
    @Mock
    private UserActivationRequestRepository userActivationRequestRepository;
    @Mock
    private NotificationService notificationService;
    private EntraUser actorUser;
    private EntraUser initialAdminUser;
    private EntraUser targetUser;

    @BeforeEach
    void setUp() {
        actorUser = EntraUser.builder().id(UUID.randomUUID()).entraOid(ACTOR_ENTRA_OID).firstName("ActorFirst").email("actor@example.com").build();

        initialAdminUser = EntraUser.builder().id(UUID.randomUUID()).entraOid(INITIAL_ACTOR_ENTRA_OID).firstName("AdminFirst").email("admin@example.com").build();

        UserProfile targetUserProfile = UserProfile.builder().id(USER_PROFILE_ID).activeProfile(true).build();
        targetUser = EntraUser.builder().id(USER_ENTRA_ID).firstName("TargetFirst").email("target@example.com").userProfiles(Set.of(targetUserProfile)).build();
        targetUserProfile.setEntraUser(targetUser);
    }

    private UserActivationRequest buildUserActivationRequest(ReactivationRequestStatus status, int version) {
        return UserActivationRequest.builder().id(UUID.randomUUID()).requestId(REQUEST_ID).userProfileId(USER_PROFILE_ID).status(status).version(version).actorEntraOid(ACTOR_ENTRA_OID).build();
    }

    @Nested
    @DisplayName("findFirstByUserProfileIdOrderByVersionDesc")
    class FindFirstByUserProfileIdOrderByVersionDescTests {

        @Test
        @DisplayName("Should return empty optional when no request exists for profile ID")
        void noRequestFound_returnsEmptyOptional() {
            given(userActivationRequestRepository.findFirstByUserEntraIdOrderByCreatedAtDescVersionDesc(USER_PROFILE_ID)).willReturn(Optional.empty());

            Optional<UserActivationRequest> result = service.findFirstByUserEntraIdOrderByCreatedAtDescVersionDesc(USER_PROFILE_ID_STR);

            assertThat(result).isEmpty();
            verify(userActivationRequestRepository).findFirstByUserEntraIdOrderByCreatedAtDescVersionDesc(USER_PROFILE_ID);
        }

        @Test
        @DisplayName("Should return request when found for profile ID")
        void requestFound_returnsRequest() {
            UserActivationRequest existing = buildUserActivationRequest(ReactivationRequestStatus.IN_REVIEW, 1);
            given(userActivationRequestRepository.findFirstByUserEntraIdOrderByCreatedAtDescVersionDesc(USER_PROFILE_ID)).willReturn(Optional.of(existing));

            Optional<UserActivationRequest> result = service.findFirstByUserEntraIdOrderByCreatedAtDescVersionDesc(USER_PROFILE_ID_STR);

            assertThat(result).contains(existing);
        }
    }

    @Nested
    @DisplayName("createNewRequest")
    class CreateNewRequestTests {

        @Test
        @DisplayName("Should create new request when no existing request is present")
        void noExistingRequest_createsNewRequest() {
            AppRole role = AppRole.builder().id(UUID.randomUUID()).name("Provider Admin").build();
            UserProfile profile = UserProfile.builder().id(USER_PROFILE_ID).activeProfile(true).appRoles(Set.of(role)).build();
            EntraUser entraUser = EntraUser.builder().id(USER_ENTRA_ID).userProfiles(Set.of(profile)).build();
            given(entraUserRepository.findByEntraOid(any())).willReturn(Optional.of(entraUser));
            given(entraUserRepository.findById(any())).willReturn(Optional.of(entraUser));
            given(userActivationRequestRepository.findFirstByUserEntraIdOrderByCreatedAtDescVersionDesc(USER_ENTRA_ID)).willReturn(Optional.empty());
            given(entraUserRepository.findById(USER_ENTRA_ID)).willReturn(Optional.of(entraUser));
            given(userActivationRequestRepository.save(any(UserActivationRequest.class))).will(returnsFirstArg());

            UserActivationRequest result = service.createReactivationRequest(USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Reactivation comment", ACTOR_ENTRA_OID);

            ArgumentCaptor<UserActivationRequest> captor = ArgumentCaptor.forClass(UserActivationRequest.class);
            verify(userActivationRequestRepository).save(captor.capture());

            UserActivationRequest saved = captor.getValue();
            assertThat(saved.getUserProfileId()).isEqualTo(USER_PROFILE_ID);
            assertThat(saved.getStatus()).isEqualTo(ReactivationRequestStatus.IN_REVIEW);
            assertThat(saved.getComments()).isEqualTo("Reactivation comment");
            assertThat(saved.getActorEntraOid()).isEqualTo(ACTOR_ENTRA_OID);
            assertThat(saved.getCreatedAt()).isNotNull();

            assertThat(result).isEqualTo(saved);
        }

        @Test
        @DisplayName("Should create new request when existing request is REJECTED")
        void existingRejectedRequest_createsNewRequest() {
            AppRole role = AppRole.builder().id(UUID.randomUUID()).name("Provider Admin").build();
            UserProfile profile = UserProfile.builder().id(USER_PROFILE_ID).activeProfile(true).appRoles(Set.of(role)).build();
            EntraUser entraUser = EntraUser.builder().id(USER_ENTRA_ID).userProfiles(Set.of(profile)).build();
            given(entraUserRepository.findByEntraOid(any())).willReturn(Optional.of(entraUser));
            given(entraUserRepository.findById(any())).willReturn(Optional.of(entraUser));
            UserActivationRequest rejected = buildUserActivationRequest(ReactivationRequestStatus.REJECTED, 1);
            given(userActivationRequestRepository.findFirstByUserEntraIdOrderByCreatedAtDescVersionDesc(USER_ENTRA_ID)).willReturn(Optional.of(rejected));
            given(entraUserRepository.findById(any(UUID.class))).willReturn(Optional.of(entraUser));
            given(userActivationRequestRepository.save(any(UserActivationRequest.class))).will(returnsFirstArg());

            UserActivationRequest result = service.createReactivationRequest(USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Retry comment", ACTOR_ENTRA_OID);

            assertThat(result).isNotNull();
            verify(userActivationRequestRepository).save(any(UserActivationRequest.class));
        }

        @Test
        @DisplayName("Should create new request when existing request is APPROVED")
        void existingApprovedRequest_createsNewRequest() {
            AppRole role = AppRole.builder().id(UUID.randomUUID()).name("Provider Admin").build();
            UserProfile profile = UserProfile.builder().id(USER_PROFILE_ID).activeProfile(true).appRoles(Set.of(role)).build();
            EntraUser entraUser = EntraUser.builder().id(USER_ENTRA_ID).userProfiles(Set.of(profile)).build();
            given(entraUserRepository.findByEntraOid(any())).willReturn(Optional.of(entraUser));
            UserActivationRequest approved = buildUserActivationRequest(ReactivationRequestStatus.APPROVED, 1);
            given(userActivationRequestRepository.findFirstByUserEntraIdOrderByCreatedAtDescVersionDesc(USER_ENTRA_ID)).willReturn(Optional.of(approved));
            given(entraUserRepository.findById(USER_ENTRA_ID)).willReturn(Optional.of(entraUser));
            given(userActivationRequestRepository.save(any(UserActivationRequest.class))).will(returnsFirstArg());

            UserActivationRequest result = service.createReactivationRequest(USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "New request", ACTOR_ENTRA_OID);

            assertThat(result).isNotNull();
            verify(userActivationRequestRepository).save(any(UserActivationRequest.class));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when request is currently IN_REVIEW")
        void activeRequestInReview_throwsIllegalStateException() {
            UserActivationRequest inReview = buildUserActivationRequest(ReactivationRequestStatus.IN_REVIEW, 1);
            given(userActivationRequestRepository.findFirstByUserEntraIdOrderByCreatedAtDescVersionDesc(USER_ENTRA_ID)).willReturn(Optional.of(inReview));

            assertThatThrownBy(() -> service.createReactivationRequest(USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Comment", ACTOR_ENTRA_OID))
                    .isInstanceOf(IllegalStateException.class).hasMessage("Request already being processed for user " + USER_ENTRA_ID);

            verify(userActivationRequestRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("rejectReactivationRequest & processReactivationState Tests")
    class RejectReactivationRequestTests {

        @Test
        @DisplayName("Should process rejection when initial actor was PROVIDER_ADMIN and notify both admin and target user")
        void rejectReactivationRequest_WhenActorIsProviderAdmin_NotifiesAdminAndTargetUser() {
            // Given
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(actorUser));
            given(roleTypeResolver.resolve(actorUser)).willReturn(ReactivationRoleType.LAA_USER_REGISTRATION);

            UserActivationRequest initialRequest = new UserActivationRequest();
            initialRequest.setRequestId(REQUEST_ID);
            initialRequest.setActorRoleType(ReactivationRoleType.PROVIDER_ADMIN);
            initialRequest.setActorEntraOid(INITIAL_ACTOR_ENTRA_OID);

            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(REQUEST_ID)).willReturn(Optional.of(initialRequest));

            UserActivationRequest latestRequest = new UserActivationRequest();
            latestRequest.setRequestId(REQUEST_ID);
            latestRequest.setVersion(2);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.of(latestRequest));

            given(entraUserRepository.findById(USER_ENTRA_ID)).willReturn(Optional.of(targetUser));
            given(entraUserRepository.findByEntraOid(INITIAL_ACTOR_ENTRA_OID)).willReturn(Optional.of(initialAdminUser));

            given(userActivationRequestRepository.save(any(UserActivationRequest.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            UserActivationRequest result = service.rejectReactivationRequest(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Incomplete documents", ACTOR_ENTRA_OID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ReactivationRequestStatus.REJECTED);
            assertThat(result.getVersion()).isEqualTo(3);
            assertThat(result.getComments()).isEqualTo("Incomplete documents");
            assertThat(result.getActorRoleType()).isEqualTo(ReactivationRoleType.LAA_USER_REGISTRATION);

            // Verify notifications
            verify(notificationService).notifyReactivationRequestRejected(String.valueOf(actorUser.getId()), initialAdminUser.getFirstName(),
                    initialAdminUser.getEmail(), initialAdminUser.getId().toString(), USER_ENTRA_ID_STR, targetUser.getEmail());

            verify(notificationService).notifyReactivationRequestRejected(String.valueOf(actorUser.getId()), targetUser.getFirstName(),
                    targetUser.getEmail(), targetUser.getId().toString(), targetUser.getId().toString(), targetUser.getEmail());
        }

        @Test
        @DisplayName("Should process rejection when initial actor was NOT PROVIDER_ADMIN and notify only target user")
        void rejectReactivationRequest_WhenInitialActorNotProviderAdmin_NotifiesOnlyTargetUser() {
            // Given
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(actorUser));
            given(roleTypeResolver.resolve(actorUser)).willReturn(ReactivationRoleType.LAA_OST);

            UserActivationRequest initialRequest = new UserActivationRequest();
            initialRequest.setRequestId(REQUEST_ID);
            initialRequest.setActorRoleType(ReactivationRoleType.LAA_USER_REGISTRATION);

            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(REQUEST_ID)).willReturn(Optional.of(initialRequest));
            UserActivationRequest latestRequest = new UserActivationRequest();
            latestRequest.setRequestId(REQUEST_ID);
            latestRequest.setVersion(1);
            latestRequest.setStatus(ReactivationRequestStatus.IN_REVIEW);
            latestRequest.setActorRoleType(ReactivationRoleType.LAA_OST);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.of(latestRequest));
            given(entraUserRepository.findById(USER_ENTRA_ID)).willReturn(Optional.of(targetUser));

            given(userActivationRequestRepository.save(any(UserActivationRequest.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            UserActivationRequest result = service.rejectReactivationRequest(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Rejected by OST", ACTOR_ENTRA_OID);

            // Then
            assertThat(result.getVersion()).isEqualTo(2);
            assertThat(result.getStatus()).isEqualTo(ReactivationRequestStatus.REJECTED);

            // Verify admin is NOT notified
            verify(notificationService, times(1)).notifyReactivationRequestRejected(eq(String.valueOf(actorUser.getId())), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should process rejection when initial actor was PROVIDER_ADMIN and notify both admin and target user when rejected by Sync")
        void rejectReactivationRequest_WhenActorIsProviderAdmin_NotifiesAdminAndTargetUserWhenRejectedBySync() {
            UserActivationRequest initialRequest = new UserActivationRequest();
            initialRequest.setRequestId(REQUEST_ID);
            initialRequest.setActorRoleType(ReactivationRoleType.PROVIDER_ADMIN);
            initialRequest.setActorEntraOid(INITIAL_ACTOR_ENTRA_OID);

            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(REQUEST_ID)).willReturn(Optional.of(initialRequest));

            UserActivationRequest latestRequest = new UserActivationRequest();
            latestRequest.setRequestId(REQUEST_ID);
            latestRequest.setVersion(2);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.of(latestRequest));

            given(entraUserRepository.findById(USER_ENTRA_ID)).willReturn(Optional.of(targetUser));
            given(entraUserRepository.findByEntraOid(INITIAL_ACTOR_ENTRA_OID)).willReturn(Optional.of(initialAdminUser));

            given(userActivationRequestRepository.save(any(UserActivationRequest.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            UserActivationRequest result = service.rejectReactivationRequest(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Incomplete documents", "SYNC");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ReactivationRequestStatus.REJECTED);
            assertThat(result.getVersion()).isEqualTo(3);
            assertThat(result.getComments()).isEqualTo("Incomplete documents");
            assertThat(result.getActorRoleType()).isEqualTo(ReactivationRoleType.SYNC);

            // Verify notifications
            verify(notificationService).notifyReactivationRequestRejected("SYSTEM", initialAdminUser.getFirstName(),
                    initialAdminUser.getEmail(), initialAdminUser.getId().toString(), USER_ENTRA_ID_STR, targetUser.getEmail());

            verify(notificationService).notifyReactivationRequestRejected("SYSTEM", targetUser.getFirstName(),
                    targetUser.getEmail(), targetUser.getId().toString(), targetUser.getId().toString(), targetUser.getEmail());
        }

        @Test
        @DisplayName("Should throw NoSuchElementException when actor Entra user is not found")
        void processReactivationState_WhenActorNotFound_ThrowsException() {
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.rejectReactivationRequest(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Comments", ACTOR_ENTRA_OID)).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("Should throw NoSuchElementException when initial request record is not found")
        void processReactivationState_WhenInitialRequestNotFound_ThrowsException() {
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(actorUser));
            given(roleTypeResolver.resolve(actorUser)).willReturn(ReactivationRoleType.LAA_USER_REGISTRATION);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(REQUEST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.rejectReactivationRequest(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Comments", ACTOR_ENTRA_OID))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when user profile does not exist")
        void createReactivationRequestEntry_WhenProfileNotFound_ThrowsEntityNotFoundException() {
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(actorUser));
            given(roleTypeResolver.resolve(actorUser)).willReturn(ReactivationRoleType.LAA_USER_REGISTRATION);

            UserActivationRequest initialRequest = new UserActivationRequest();
            initialRequest.setRequestId(REQUEST_ID);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(REQUEST_ID)).willReturn(Optional.of(initialRequest));

            assertThatThrownBy(() -> service.rejectReactivationRequest(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Comments", ACTOR_ENTRA_OID))
                    .isInstanceOf(EntityNotFoundException.class).hasMessageContaining("Target user not found with ID: " + USER_ENTRA_ID_STR);
        }

        @Test
        @DisplayName("Should throw NoSuchElementException when target provider user is not found")
        void processReactivationState_WhenTargetUserNotFound_ThrowsException() {
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(actorUser));
            given(entraUserRepository.findById(USER_ENTRA_ID)).willReturn(Optional.empty());
            given(roleTypeResolver.resolve(actorUser)).willReturn(ReactivationRoleType.LAA_USER_REGISTRATION);

            UserActivationRequest initialRequest = new UserActivationRequest();
            initialRequest.setRequestId(REQUEST_ID);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(REQUEST_ID)).willReturn(Optional.of(initialRequest));

            assertThatThrownBy(() -> service.rejectReactivationRequest(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Comments", ACTOR_ENTRA_OID))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateReactivateRequestState Tests")
    class UpdateReactivateRequestStateTests {

        @Test
        @DisplayName("Should update state to INFORMATION_REQUIRED and send notification when role resolves to external user")
        void updateReactivateRequestState_WhenInfoRequiredAndActorIsProviderAdmin_NotifiesAdmin() {
            // Given
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(actorUser));
            // Non-internal roles result in INFORMATION_REQUIRED
            given(roleTypeResolver.resolve(actorUser)).willReturn(ReactivationRoleType.NONE);

            // Active request validation passes
            UserActivationRequest latestRequest = new UserActivationRequest();
            latestRequest.setRequestId(REQUEST_ID);
            latestRequest.setStatus(ReactivationRequestStatus.IN_REVIEW);
            latestRequest.setVersion(1);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.of(latestRequest));

            // Initial request was raised by PROVIDER_ADMIN
            UserActivationRequest initialRequest = new UserActivationRequest();
            initialRequest.setRequestId(REQUEST_ID);
            initialRequest.setActorRoleType(ReactivationRoleType.PROVIDER_ADMIN);
            initialRequest.setActorEntraOid(INITIAL_ACTOR_ENTRA_OID);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(REQUEST_ID)).willReturn(Optional.of(initialRequest));

            given(entraUserRepository.findByEntraOid(INITIAL_ACTOR_ENTRA_OID)).willReturn(Optional.of(initialAdminUser));
            given(entraUserRepository.findById(UUID.fromString(USER_ENTRA_ID_STR))).willReturn(Optional.of(targetUser));

            given(userActivationRequestRepository.save(any(UserActivationRequest.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            UserActivationRequest result = service.updateReactivateRequestState(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Need ID proof", ACTOR_ENTRA_OID);

            // Then
            assertThat(result.getStatus()).isEqualTo(ReactivationRequestStatus.INFORMATION_REQUIRED);
            assertThat(result.getVersion()).isEqualTo(2);

            verify(notificationService).notifyReactivationRequestInfoRequested(actorUser.getId().toString(), initialAdminUser.getFirstName(),
                    initialAdminUser.getEmail(), initialAdminUser.getId().toString(), USER_ENTRA_ID_STR, targetUser.getEmail());
        }

        @Test
        @DisplayName("Should update state to INFORMATION_REQUIRED and skip info notification when actor is LAA_USER_REGISTRATION")
        void updateReactivateRequestState_WhenInfoRequired_DoesNotSendInfoRequestedNotification() {
            // Given
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(actorUser));
            given(entraUserRepository.findById(USER_ENTRA_ID)).willReturn(Optional.of(targetUser));
            given(roleTypeResolver.resolve(actorUser)).willReturn(ReactivationRoleType.LAA_USER_REGISTRATION);

            UserActivationRequest latestRequest = new UserActivationRequest();
            latestRequest.setRequestId(REQUEST_ID);
            latestRequest.setStatus(ReactivationRequestStatus.IN_REVIEW);
            latestRequest.setVersion(1);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.of(latestRequest));
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(REQUEST_ID)).willReturn(Optional.of(latestRequest));
            given(userActivationRequestRepository.save(any(UserActivationRequest.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            UserActivationRequest result = service.updateReactivateRequestState(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Updating notes", ACTOR_ENTRA_OID);

            // Then
            assertThat(result.getStatus()).isEqualTo(ReactivationRequestStatus.INFORMATION_REQUIRED);
            assertThat(result.getVersion()).isEqualTo(2);

            verify(notificationService, never()).notifyReactivationRequestInfoRequested(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should update state to INFORMATION_REQUIRED but skip notification when initial request was NOT from PROVIDER_ADMIN")
        void updateReactivateRequestState_WhenInfoRequiredAndInitialActorNotProviderAdmin_SkipsNotification() {
            // Given
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(actorUser));
            given(entraUserRepository.findById(USER_ENTRA_ID)).willReturn(Optional.of(targetUser));
            given(roleTypeResolver.resolve(actorUser)).willReturn(ReactivationRoleType.NONE);

            UserActivationRequest latestRequest = new UserActivationRequest();
            latestRequest.setRequestId(REQUEST_ID);
            latestRequest.setStatus(ReactivationRequestStatus.IN_REVIEW);
            latestRequest.setVersion(1);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.of(latestRequest));

            UserActivationRequest initialRequest = new UserActivationRequest();
            initialRequest.setRequestId(REQUEST_ID);
            initialRequest.setActorRoleType(ReactivationRoleType.LAA_OST); // Not PROVIDER_ADMIN
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(REQUEST_ID)).willReturn(Optional.of(initialRequest));

            given(userActivationRequestRepository.save(any(UserActivationRequest.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            UserActivationRequest result = service.updateReactivateRequestState(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Info needed", ACTOR_ENTRA_OID);

            // Then
            assertThat(result.getStatus()).isEqualTo(ReactivationRequestStatus.INFORMATION_REQUIRED);
            verify(notificationService, never()).notifyReactivationRequestInfoRequested(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when request does not exist during validation")
        void updateReactivateRequestState_WhenRequestNotFound_ThrowsException() {
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(actorUser));
            given(roleTypeResolver.resolve(actorUser)).willReturn(ReactivationRoleType.LAA_USER_REGISTRATION);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateReactivateRequestState(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Comments", ACTOR_ENTRA_OID))
                    .isInstanceOf(EntityNotFoundException.class).hasMessageContaining("Reactivation request not found for ID: " + REQUEST_ID_STR);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when request is already APPROVED")
        void updateReactivateRequestState_WhenRequestAlreadyApproved_ThrowsException() {
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(actorUser));
            given(roleTypeResolver.resolve(actorUser)).willReturn(ReactivationRoleType.LAA_USER_REGISTRATION);

            UserActivationRequest approvedRequest = new UserActivationRequest();
            approvedRequest.setRequestId(REQUEST_ID);
            approvedRequest.setStatus(ReactivationRequestStatus.APPROVED);

            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.of(approvedRequest));

            assertThatThrownBy(() -> service.updateReactivateRequestState(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Comments", ACTOR_ENTRA_OID))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("Reactivation request already processed for ID: " + REQUEST_ID_STR);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when request is already REJECTED")
        void updateReactivateRequestState_WhenRequestAlreadyRejected_ThrowsException() {
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(actorUser));
            given(roleTypeResolver.resolve(actorUser)).willReturn(ReactivationRoleType.LAA_USER_REGISTRATION);

            UserActivationRequest rejectedRequest = new UserActivationRequest();
            rejectedRequest.setRequestId(REQUEST_ID);
            rejectedRequest.setStatus(ReactivationRequestStatus.REJECTED);

            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.of(rejectedRequest));

            assertThatThrownBy(() -> service.updateReactivateRequestState(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Comments", ACTOR_ENTRA_OID))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("Reactivation request already processed for ID: " + REQUEST_ID_STR);
        }
    }


    @Nested
    @DisplayName("saveRequestState")
    class SaveRequestStateTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException when target user profile does not exist")
        void targetUserNotFound_throwsException() {
            assertThatThrownBy(() -> service.updateReactivateRequestState(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Comments", ACTOR_ENTRA_OID))
                    .isInstanceOf(NoSuchElementException.class).hasMessage("No value present");

            verify(userActivationRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return error when non-null requestId has no prior history")
        void nonNullRequestIdNoHistory_returnsError() {
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(UUID.fromString(REQUEST_ID_STR))).willReturn(Optional.empty());
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(EntraUser.builder().id(UUID.randomUUID()).build()));
            given(roleTypeResolver.resolve(any())).willReturn(ReactivationRoleType.PROVIDER_ADMIN);

            assertThatThrownBy(() -> service.updateReactivateRequestState(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Comments", ACTOR_ENTRA_OID))
                    .isInstanceOf(EntityNotFoundException.class).hasMessage("Reactivation request not found for ID: " + REQUEST_ID);

            assertThatThrownBy(() -> service.updateReactivateRequestState(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Reviewing", ACTOR_ENTRA_OID))
                    .isInstanceOf(EntityNotFoundException.class).hasMessage(String.format("Reactivation request not found for ID: %s", REQUEST_ID));

            verify(userActivationRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should increment version when existing record is IN_REVIEW")
        void activeRequestInReview_incrementsVersion() {
            UserActivationRequest existing = buildUserActivationRequest(ReactivationRequestStatus.IN_REVIEW, 2);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(UUID.fromString(REQUEST_ID_STR))).willReturn(Optional.of(existing));
            given(userActivationRequestRepository.save(any(UserActivationRequest.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(EntraUser.builder().id(UUID.randomUUID()).build()));
            given(roleTypeResolver.resolve(any())).willReturn(ReactivationRoleType.PROVIDER_ADMIN);
            given(entraUserRepository.findById(any(UUID.class)))
                    .willReturn(Optional.of(EntraUser.builder().id(UUID.randomUUID()).email("test@email.com")
                            .userProfiles(Set.of(UserProfile.builder().id(USER_PROFILE_ID).activeProfile(true).build()))
                            .build()));
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(UUID.fromString(REQUEST_ID_STR))).willReturn(Optional.of(existing));

            UserActivationRequest result = service.approveReactivationRequest(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, ACTOR_ENTRA_OID);

            assertThat(result.getRequestId()).isEqualTo(UUID.fromString(REQUEST_ID_STR));
            assertThat(result.getVersion()).isEqualTo(3);
            assertThat(result.getStatus()).isEqualTo(ReactivationRequestStatus.APPROVED);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when request has already been APPROVED")
        void alreadyApproved_throwsIllegalStateException() {
            UserActivationRequest existing = buildUserActivationRequest(ReactivationRequestStatus.APPROVED, 1);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(UUID.fromString(REQUEST_ID_STR))).willReturn(Optional.of(existing));
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(EntraUser.builder().id(UUID.randomUUID()).build()));

            assertThatThrownBy(() -> service.updateReactivateRequestState(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Cannot approve", ACTOR_ENTRA_OID))
                    .isInstanceOf(IllegalStateException.class).hasMessage(String.format("Reactivation request already processed for ID: %s", REQUEST_ID));

            verify(userActivationRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when request has already been REJECTED")
        void alreadyRejected_throwsIllegalStateException() {
            UserActivationRequest existing = buildUserActivationRequest(ReactivationRequestStatus.REJECTED, 1);
            given(userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(UUID.fromString(REQUEST_ID_STR))).willReturn(Optional.of(existing));
            given(entraUserRepository.findByEntraOid(ACTOR_ENTRA_OID)).willReturn(Optional.of(EntraUser.builder().id(UUID.randomUUID()).build()));

            assertThatThrownBy(() -> service.updateReactivateRequestState(REQUEST_ID_STR, USER_ENTRA_ID_STR, USER_PROFILE_ID_STR, "Cannot reject", ACTOR_ENTRA_OID))
                    .isInstanceOf(IllegalStateException.class).hasMessage(String.format("Reactivation request already processed for ID: %s", REQUEST_ID));

            verify(userActivationRequestRepository, never()).save(any());
        }

        @Nested
        @DisplayName("getLatestRequestHistoryForUserProfile")
        class GetLatestRequestHistoryForUserProfile {

            @Test
            @DisplayName("Should return empty list when history query yields no records")
            void shouldReturnEmptyListWhenHistoryIsEmpty() {
                when(userActivationRequestRepository.findRequestHistoryByRequestId(UUID.fromString(REQUEST_ID_STR)))
                        .thenReturn(Collections.emptyList());

                List<UserActivationRequestSummaryDto> result = service.getRequestHistoryForUserIdAndRequestId(USER_PROFILE_ID_STR, REQUEST_ID_STR);

                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("Should return history when records are found")
            void shouldReturnHistoryListWhenFound() {
                UserActivationRequestSummaryDto dto = mock(UserActivationRequestSummaryDto.class);

                when(userActivationRequestRepository.findRequestHistoryByRequestId(UUID.fromString(REQUEST_ID_STR))).thenReturn(List.of(dto));

                List<UserActivationRequestSummaryDto> result = service.getRequestHistoryForUserIdAndRequestId(USER_PROFILE_ID_STR, REQUEST_ID_STR);

                assertThat(result).hasSize(1).containsExactly(dto);
            }
        }

        @Nested
        @DisplayName("calculateNextReactivationRequestStatus")
        class CalculateNextReactivationRequestStatus {

            @Test
            @DisplayName("Should return IN_REVIEW when role is PROVIDER_ADMIN")
            void shouldReturnInReviewForProviderAdmin() {
                EntraUser actor = mock(EntraUser.class);
                when(roleTypeResolver.resolve(actor)).thenReturn(ReactivationRoleType.PROVIDER_ADMIN);

                ReactivationRequestStatus status = service.calculateNextReactivationRequestStatus(actor);

                assertThat(status).isEqualTo(ReactivationRequestStatus.IN_REVIEW);
            }

            @Test
            @DisplayName("Should return INFORMATION_REQUIRED when role is not PROVIDER_ADMIN")
            void shouldReturnInformationRequiredForNonProviderAdmin() {
                EntraUser actor = mock(EntraUser.class);
                when(roleTypeResolver.resolve(actor)).thenReturn(ReactivationRoleType.LAA);

                ReactivationRequestStatus status = service.calculateNextReactivationRequestStatus(actor);

                assertThat(status).isEqualTo(ReactivationRequestStatus.INFORMATION_REQUIRED);
            }
        }

        @Nested
        class HasOpenReactivationRequestTests {

            @ParameterizedTest
            @EnumSource(
                    value = ReactivationRequestStatus.class,
                    names = {"IN_REVIEW", "INFORMATION_REQUIRED"}
            )
            @DisplayName("Should return true when at least one request has an open status")
            void hasOpenReactivationRequest_ShouldReturnTrue_WhenStatusIsOpen(ReactivationRequestStatus openStatus) {
                UUID userId = UUID.randomUUID();
                UUID profileId1 = UUID.randomUUID();
                UUID profileId2 = UUID.randomUUID();

                UserProfile profile1 = mock(UserProfile.class);
                when(profile1.getId()).thenReturn(profileId1);

                UserProfile profile2 = mock(UserProfile.class);
                when(profile2.getId()).thenReturn(profileId2);

                EntraUser entraUser = mock(EntraUser.class);
                when(entraUser.getUserProfiles()).thenReturn(Set.of(profile1, profile2));
                when(entraUserRepository.findById(userId)).thenReturn(Optional.of(entraUser));

                UserActivationRequest request1 = UserActivationRequest.builder().status(openStatus).build();

                UserActivationRequest request2 = UserActivationRequest.builder().status(ReactivationRequestStatus.REJECTED).build();

                when(userActivationRequestRepository.findTopForEachUserProfileId(anyList()))
                        .thenReturn(List.of(request1, request2));

                boolean result = service.hasOpenReactivationRequest(userId);

                assertThat(result).isTrue();
            }

            @Test
            @DisplayName("Should return false when requests exist but none have an open status")
            void hasOpenReactivationRequest_ShouldReturnFalse_WhenNoMatchingStatus() {
                UUID userId = UUID.randomUUID();
                UUID profileId = UUID.randomUUID();

                UserProfile profile = mock(UserProfile.class);
                when(profile.getId()).thenReturn(profileId);

                EntraUser entraUser = mock(EntraUser.class);
                when(entraUser.getUserProfiles()).thenReturn(Set.of(profile));
                when(entraUserRepository.findById(userId)).thenReturn(Optional.of(entraUser));

                UserActivationRequest request = mock(UserActivationRequest.class);
                when(request.getStatus()).thenReturn(ReactivationRequestStatus.APPROVED);

                when(userActivationRequestRepository.findTopForEachUserProfileId(List.of(profileId)))
                        .thenReturn(List.of(request));

                boolean result = service.hasOpenReactivationRequest(userId);

                assertThat(result).isFalse();
            }

            @Test
            @DisplayName("Should return false when user has no profiles or no activation requests found")
            void hasOpenReactivationRequest_ShouldReturnFalse_WhenNoRequestsFound() {
                UUID userId = UUID.randomUUID();

                EntraUser entraUser = mock(EntraUser.class);
                when(entraUser.getUserProfiles()).thenReturn(Collections.emptySet());
                when(entraUserRepository.findById(userId)).thenReturn(Optional.of(entraUser));
                when(userActivationRequestRepository.findTopForEachUserProfileId(Collections.emptyList()))
                        .thenReturn(Collections.emptyList());

                boolean result = service.hasOpenReactivationRequest(userId);

                assertThat(result).isFalse();
            }

            @Test
            @DisplayName("Should throw NoSuchElementException when EntraUser is not found")
            void hasOpenReactivationRequest_ShouldThrowException_WhenUserNotFound() {
                UUID userId = UUID.randomUUID();
                when(entraUserRepository.findById(userId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.hasOpenReactivationRequest(userId))
                        .isInstanceOf(NoSuchElementException.class);

                verifyNoInteractions(userActivationRequestRepository);
            }
        }

        // ==========================================
        // 3. getPageMode & resolvePageMode
        // ==========================================
        @Nested
        @DisplayName("getPageMode & resolvePageMode")
        class PageModeTests {

            @Test
            @DisplayName("Should return NONE mode when current user is null")
            void shouldReturnManageWhenUserIsNull() {
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(null);

                ReactivationRequestPageMode pageMode = service.getPageMode(authentication);

                assertThat(pageMode).isEqualTo(ReactivationRequestPageMode.NONE);
            }

            @Test
            @DisplayName("Should return TRACK mode when user has FIRM_USER_MANAGER role and NO manage roles")
            void shouldReturnTrackForProviderAdminOnly() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);

                try (MockedStatic<AccessControlService> accessControlMock = mockStatic(AccessControlService.class)) {
                    accessControlMock.when(() -> AccessControlService.userHasAuthzRole(eq(currentUser), any())).thenReturn(false);
                    accessControlMock.when(() -> AccessControlService.userHasAuthzRole(currentUser, AuthzRole.FIRM_USER_MANAGER.getRoleName())).thenReturn(true);

                    ReactivationRequestPageMode pageMode = service.getPageMode(authentication);

                    assertThat(pageMode).isEqualTo(ReactivationRequestPageMode.TRACK);
                }
            }

            @Test
            @DisplayName("Should return MANAGE mode when user has both FIRM_USER_MANAGER and a Manage role")
            void shouldReturnManageWhenUserHasManageAndProviderAdminRole() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);

                try (MockedStatic<AccessControlService> accessControlMock = mockStatic(AccessControlService.class)) {
                    accessControlMock.when(() -> AccessControlService.userHasAuthzRole(currentUser, AuthzRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
                    accessControlMock.when(() -> AccessControlService.userHasAuthzRole(currentUser, AuthzRole.FIRM_USER_MANAGER.getRoleName())).thenReturn(true);

                    ReactivationRequestPageMode pageMode = service.getPageMode(authentication);

                    assertThat(pageMode).isEqualTo(ReactivationRequestPageMode.MANAGE);
                }
            }
        }

        // ==========================================
        // 4. getPage & buildRequests & Pagination & Filtering
        // ==========================================
        @Nested
        @DisplayName("getPage")
        class GetPageTests {

            @Test
            @DisplayName("Should return empty paginatedRequests data when no latest requests are found")
            void shouldReturnEmptyPageDataWhenNoRequestsExist() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);
                when(userActivationRequestRepository.findAllLatestRequests()).thenReturn(List.of());

                ReactivationRequestsPageData result = service.getPage(authentication, "", null, false, false, false, 1, 10, "requestId", "asc");

                assertThat(result.pageMode()).isEqualTo(ReactivationRequestPageMode.NONE);
                assertThat(result.paginatedRequests().getRequests()).isEmpty();
                assertThat(result.paginatedRequests().getTotalRequests()).isEqualTo(0);
            }

            @Test
            @DisplayName("Should process, filter, sort and paginate requests in MANAGE mode")
            void shouldBuildFilterSortAndPaginateRequestsInManageMode() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);
                stubGlobalAdmin(currentUser);

                // Set up test entities
                UUID request1Id = UUID.randomUUID();
                UUID profile1Id = UUID.randomUUID();
                String actor1Oid = "actor-oid-1";

                UserActivationRequest request1 = mock(UserActivationRequest.class);
                when(request1.getId()).thenReturn(UUID.randomUUID());
                when(request1.getRequestId()).thenReturn(request1Id);
                when(request1.getUserEntraId()).thenReturn(USER_ENTRA_ID);
                when(request1.getActorEntraOid()).thenReturn(actor1Oid);
                when(request1.getStatus()).thenReturn(ReactivationRequestStatus.IN_REVIEW);
                when(request1.getVersion()).thenReturn(1);
                when(request1.getCreatedAt()).thenReturn(Instant.now());
                when(request1.getComments()).thenReturn("Sample request comment");

                when(userActivationRequestRepository.findAllLatestRequests()).thenReturn(List.of(request1));

                // Target Profile & User
                EntraUser targetUser = EntraUser.builder().id(USER_ENTRA_ID)
                        .firstName("Jane").lastName("Doe").email("jane.doe@example.com").build();

                Firm targetFirm = Firm.builder().id(UUID.randomUUID()).build();

                UserProfile profile1 = UserProfile.builder().id(profile1Id).activeProfile(true)
                        .entraUser(targetUser).firm(targetFirm).build();
                targetUser.setUserProfiles(Set.of(profile1));

                when(entraUserRepository.findAllById(Set.of(USER_ENTRA_ID))).thenReturn(List.of(targetUser));

                // Actor User
                EntraUser actor1 = mock(EntraUser.class);
                when(actor1.getEntraOid()).thenReturn(actor1Oid);
                when(actor1.getFirstName()).thenReturn("John");
                when(actor1.getLastName()).thenReturn("Smith");
                when(actor1.getEmail()).thenReturn("john.smith@example.com");

                when(entraUserRepository.findByEntraOidIn(Set.of(actor1Oid))).thenReturn(List.of(actor1));

                // Submission timestamp lookup
                UserActivationRequest firstVerRequest = mock(UserActivationRequest.class);
                when(firstVerRequest.getRequestId()).thenReturn(request1Id);
                when(firstVerRequest.getCreatedAt()).thenReturn(Instant.now());

                when(userActivationRequestRepository.findAllFirstVersionsByRequestIdIn(Set.of(request1Id))).thenReturn(List.of(firstVerRequest));

                // Execute method call
                ReactivationRequestsPageData result = service.getPage(authentication, "Jane", // Search string matches target user name
                    List.of(ReactivationRequestStatus.IN_REVIEW), false, false, false, 1, 10, "actorName", "asc");

                assertThat(result.paginatedRequests().getTotalRequests()).isEqualTo(1);
                ReactivationRequestListItem item = result.paginatedRequests().getRequests().getFirst();
                assertThat(item.userName()).isEqualTo("Jane Doe");
                assertThat(item.actorName()).isEqualTo("John Smith");
                assertThat(item.userType()).isEqualTo("Provider User");
                assertThat(item.firmId()).isEqualTo(targetFirm.getId());
            }

            @Test
            @DisplayName("Should show a multi-firm request to the External User Manager who raised it")
            void multiFirmRequest_isVisibleToRequestingEum() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);

                UUID requestId = UUID.randomUUID();
                UserActivationRequest request = mock(UserActivationRequest.class);
                when(request.getId()).thenReturn(UUID.randomUUID());
                when(request.getRequestId()).thenReturn(requestId);
                when(request.getUserEntraId()).thenReturn(USER_ENTRA_ID);
                when(request.getActorEntraOid()).thenReturn(ACTOR_ENTRA_OID);
                when(request.getActorRoleType()).thenReturn(ReactivationRoleType.LAA_OST);
                when(request.getStatus()).thenReturn(ReactivationRequestStatus.IN_REVIEW);
                when(request.getCreatedAt()).thenReturn(Instant.now());
                when(userActivationRequestRepository.findAllLatestRequests()).thenReturn(List.of(request));

                EntraUser multiFirmTarget = EntraUser.builder().id(USER_ENTRA_ID).build();
                when(userActivationRequestRepository.findAllFirstVersionsByRequestIdIn(Set.of(requestId)))
                        .thenReturn(List.of(request));
                when(entraUserRepository.findAllById(Set.of(USER_ENTRA_ID))).thenReturn(List.of(multiFirmTarget));

                try (MockedStatic<AccessControlService> accessControlMock = mockStatic(AccessControlService.class)) {
                    accessControlMock.when(() -> AccessControlService.userHasAuthzRole(
                            currentUser, AuthzRole.EXTERNAL_USER_MANAGER.getRoleName())).thenReturn(true);

                    ReactivationRequestsPageData result = service.getPage(
                            authentication, "", null, false, false, false, 1, 10, null, "asc");

                    assertThat(result.paginatedRequests().getRequests()).hasSize(1);
                }
            }

            @Test
            @DisplayName("Should show a multi-firm request to an External User Admin")
            void multiFirmRequest_isVisibleToExternalUserAdmin() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);

                UUID requestId = UUID.randomUUID();
                UserActivationRequest request = mock(UserActivationRequest.class);
                when(request.getId()).thenReturn(UUID.randomUUID());
                when(request.getRequestId()).thenReturn(requestId);
                when(request.getActorEntraOid()).thenReturn(ACTOR_ENTRA_OID);
                when(request.getActorRoleType()).thenReturn(ReactivationRoleType.LAA_OST);
                when(request.getStatus()).thenReturn(ReactivationRequestStatus.IN_REVIEW);
                when(request.getCreatedAt()).thenReturn(Instant.now());
                when(userActivationRequestRepository.findAllLatestRequests()).thenReturn(List.of(request));

                when(userActivationRequestRepository.findAllFirstVersionsByRequestIdIn(Set.of(requestId)))
                        .thenReturn(List.of(request));

                try (MockedStatic<AccessControlService> accessControlMock = mockStatic(AccessControlService.class)) {
                    accessControlMock.when(() -> AccessControlService.userHasAuthzRole(
                            currentUser, AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())).thenReturn(true);

                    ReactivationRequestsPageData result = service.getPage(
                            authentication, "", null, false, false, false, 1, 10, null, "asc");

                    assertThat(result.paginatedRequests().getRequests()).hasSize(1);
                }
            }

            @Test
            @DisplayName("Should show EUM and EUS originated requests to External User Support")
            void eumAndEusRequests_areVisibleToExternalUserSupport() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);

                UUID eumRequestId = UUID.randomUUID();
                UUID eusRequestId = UUID.randomUUID();
                UUID providerRequestId = UUID.randomUUID();
                UUID eumProfileId = UUID.randomUUID();
                UUID eusProfileId = UUID.randomUUID();
                UUID providerProfileId = UUID.randomUUID();

                UserActivationRequest eumRequest = mock(UserActivationRequest.class);
                when(eumRequest.getId()).thenReturn(UUID.randomUUID());
                when(eumRequest.getRequestId()).thenReturn(eumRequestId);
                when(eumRequest.getUserEntraId()).thenReturn(USER_ENTRA_ID);
                when(eumRequest.getStatus()).thenReturn(ReactivationRequestStatus.IN_REVIEW);
                when(eumRequest.getActorRoleType()).thenReturn(ReactivationRoleType.LAA_OST);

                UserActivationRequest eusRequest = mock(UserActivationRequest.class);
                when(eusRequest.getId()).thenReturn(UUID.randomUUID());
                when(eusRequest.getRequestId()).thenReturn(eusRequestId);
                when(eusRequest.getUserEntraId()).thenReturn(USER_ENTRA_ID);
                when(eusRequest.getStatus()).thenReturn(ReactivationRequestStatus.IN_REVIEW);
                when(eusRequest.getActorRoleType()).thenReturn(ReactivationRoleType.LAA_SUPPORT);

                UserActivationRequest providerRequest = mock(UserActivationRequest.class);
                when(providerRequest.getRequestId()).thenReturn(providerRequestId);
                when(providerRequest.getUserEntraId()).thenReturn(USER_ENTRA_ID);

                when(userActivationRequestRepository.findAllLatestRequests())
                        .thenReturn(List.of(eumRequest, eusRequest, providerRequest));

                EntraUser targetUser = EntraUser.builder().id(USER_ENTRA_ID).build();
                UserProfile eumProfile = UserProfile.builder().id(eumProfileId).entraUser(targetUser).build();
                UserProfile eusProfile = UserProfile.builder().id(eusProfileId).entraUser(targetUser).build();
                UserProfile providerProfile = UserProfile.builder().id(providerProfileId).entraUser(targetUser).build();
                targetUser.setUserProfiles(Set.of(eumProfile, eusProfile, providerProfile));

                when(entraUserRepository.findAllById(Set.of(USER_ENTRA_ID))).thenReturn(List.of(targetUser));

                UserActivationRequest eumFirstVersion = mock(UserActivationRequest.class);
                when(eumFirstVersion.getRequestId()).thenReturn(eumRequestId);
                when(eumFirstVersion.getCreatedAt()).thenReturn(Instant.now());
                when(eumFirstVersion.getActorRoleType()).thenReturn(ReactivationRoleType.LAA_OST);
                UserActivationRequest eusFirstVersion = mock(UserActivationRequest.class);
                when(eusFirstVersion.getRequestId()).thenReturn(eusRequestId);
                when(eusFirstVersion.getCreatedAt()).thenReturn(Instant.now());
                when(eusFirstVersion.getActorRoleType()).thenReturn(ReactivationRoleType.LAA_SUPPORT);
                UserActivationRequest providerFirstVersion = mock(UserActivationRequest.class);
                when(providerFirstVersion.getRequestId()).thenReturn(providerRequestId);
                when(providerFirstVersion.getCreatedAt()).thenReturn(Instant.now());
                when(providerFirstVersion.getActorRoleType()).thenReturn(ReactivationRoleType.PROVIDER_ADMIN);
                when(userActivationRequestRepository.findAllFirstVersionsByRequestIdIn(Set.of(eumRequestId, eusRequestId, providerRequestId)))
                        .thenReturn(List.of(eumFirstVersion, eusFirstVersion, providerFirstVersion));

                try (MockedStatic<AccessControlService> accessControlMock = mockStatic(AccessControlService.class)) {
                    accessControlMock.when(() -> AccessControlService.userHasAuthzRole(
                            currentUser, AuthzRole.EXTERNAL_USER_SUPPORT.getRoleName())).thenReturn(true);

                    ReactivationRequestsPageData result = service.getPage(
                            authentication, "", null, false, false, false, 1, 10, null, "asc");

                    assertThat(result.paginatedRequests().getRequests())
                            .extracting(ReactivationRequestListItem::requestId)
                            .containsExactlyInAnyOrder(eumRequestId, eusRequestId);
                }
            }

            @Test
            @DisplayName("Should return empty list in TRACK mode if user has no allowed firms")
            void shouldReturnEmptyWhenTrackModeHasNoAllowedFirms() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);

                try (MockedStatic<AccessControlService> accessControlMock = mockStatic(AccessControlService.class)) {
                    accessControlMock.when(() -> AccessControlService.userHasAuthzRole(currentUser, AuthzRole.FIRM_USER_MANAGER.getRoleName())).thenReturn(true);

                    UserActivationRequest request1 = UserActivationRequest
                            .builder().userProfileId(USER_PROFILE_ID)
                            .actorEntraOid("actor-1").requestId(UUID.fromString(REQUEST_ID_STR))
                            .status(ReactivationRequestStatus.IN_REVIEW)
                            .build();

                    when(userActivationRequestRepository.findAllLatestRequests()).thenReturn(List.of(request1));
                    when(firmService.getUserActiveAllFirms(currentUser)).thenReturn(List.of());

                    ReactivationRequestsPageData result = service.getPage(authentication, null, null, false, false, false, 1, 10, null, "asc");

                    assertThat(result.pageMode()).isEqualTo(ReactivationRequestPageMode.TRACK);
                    assertThat(result.paginatedRequests().getRequests()).isEmpty();
                }
            }

            @Test
            @DisplayName("Should filter items by allowed firm IDs in TRACK mode")
            void shouldFilterByAllowedFirmsInTrackMode() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);

                try (MockedStatic<AccessControlService> accessControlMock = mockStatic(AccessControlService.class)) {
                    accessControlMock.when(() -> AccessControlService.userHasAuthzRole(currentUser, AuthzRole.FIRM_USER_MANAGER.getRoleName())).thenReturn(true);

                    UUID allowedFirmId = UUID.randomUUID();
                    FirmDto allowedFirmDto = FirmDto.builder().id(allowedFirmId).build();

                    when(firmService.getUserActiveAllFirms(currentUser)).thenReturn(List.of(allowedFirmDto));

                    UserActivationRequest request = mock(UserActivationRequest.class);
                    when(request.getId()).thenReturn(UUID.randomUUID());
                    when(request.getRequestId()).thenReturn(UUID.fromString(REQUEST_ID_STR));
                    when(request.getUserEntraId()).thenReturn(USER_ENTRA_ID);
                    when(request.getActorEntraOid()).thenReturn("actor-oid");
                    when(request.getStatus()).thenReturn(ReactivationRequestStatus.IN_REVIEW);

                    when(userActivationRequestRepository.findAllLatestRequests()).thenReturn(List.of(request));

                    Firm allowedFirm = Firm.builder().id(allowedFirmId).build();

                    EntraUser targetUser = EntraUser.builder().id(USER_ENTRA_ID).build();
                    UserProfile profile = UserProfile.builder().id(USER_PROFILE_ID).activeProfile(true).entraUser(targetUser).firm(allowedFirm).build();
                    targetUser.setUserProfiles(Set.of(profile));

                    when(entraUserRepository.findAllById(Set.of(USER_ENTRA_ID))).thenReturn(List.of(targetUser));

                    ReactivationRequestsPageData result = service.getPage(authentication, "", null, false, false, false, 1, 10, null, "asc");

                    assertThat(result.paginatedRequests().getRequests()).hasSize(1);
                    assertThat(result.paginatedRequests().getRequests().getFirst().firmId()).isEqualTo(allowedFirmId);
                }
            }

            @Test
            @DisplayName("Should handle missing optional entity references cleanly and fallback to UNKNOWN_USER_NAME")
            void shouldHandleMissingActorAndProfileGracefully() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);
                stubGlobalAdmin(currentUser);

                UserActivationRequest request = mock(UserActivationRequest.class);
                when(request.getId()).thenReturn(UUID.randomUUID());
                when(request.getRequestId()).thenReturn(UUID.fromString(REQUEST_ID_STR));
                when(request.getActorEntraOid()).thenReturn(null);
                when(request.getStatus()).thenReturn(ReactivationRequestStatus.APPROVED);

                when(userActivationRequestRepository.findAllLatestRequests()).thenReturn(List.of(request));

                ReactivationRequestsPageData result = service.getPage(authentication, "", null, false, false, false, 1, 10, null, "asc");

                assertThat(result.paginatedRequests().getRequests()).hasSize(1);
                ReactivationRequestListItem item = result.paginatedRequests().getRequests().getFirst();

                assertThat(item.actorName()).isEqualTo(UNKNOWN_USER_NAME);
                assertThat(item.userName()).isEqualTo(UNKNOWN_USER_NAME);
                assertThat(item.actorEmail()).isNull();
                assertThat(item.userEmail()).isNull();
                assertThat(item.firmId()).isNull();

                verify(entraUserRepository).findAllById(Set.of());
            }
        }

        @Nested
        @DisplayName("Sorting & Search Branch Tests")
        class SortingAndSearchTests {

            @Test
            @DisplayName("Should test all comparator sort branches and reverse direction")
            void shouldTestComparatorsAndDescSorting() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);
                stubGlobalAdmin(currentUser);

                UserActivationRequest req1 = UserActivationRequest.builder()
                        .id(UUID.randomUUID())
                        .requestId(UUID.randomUUID())
                        .userEntraId(UUID.randomUUID())
                        .userProfileId(UUID.randomUUID())
                        .status(ReactivationRequestStatus.IN_REVIEW)
                        .version(1)
                        .build();

                UserActivationRequest req2 = UserActivationRequest.builder()
                        .id(UUID.randomUUID())
                        .requestId(UUID.randomUUID())
                        .userEntraId(UUID.randomUUID())
                        .userProfileId(UUID.randomUUID())
                        .status(ReactivationRequestStatus.REJECTED)
                        .version(1)
                        .build();

                when(userActivationRequestRepository.findAllLatestRequests()).thenReturn(List.of(req1, req2));

                String[] sortFields = {"requestId", "userProfileId", "version", "requestStatus", "actorName", "actorRoleType", "userType", "lastActivity", "invalidSortDefault"};

                for (String sortField : sortFields) {
                    ReactivationRequestsPageData data = service.getPage(authentication, "", null, false, false, false, 1, 10, sortField, "desc");

                    assertThat(data.paginatedRequests().getRequests()).hasSize(2);
                }
            }

            @Test
            @DisplayName("Should accurately perform pagination paginatedRequests boundary checks")
            void shouldHandlePaginationBoundaries() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);
                stubGlobalAdmin(currentUser);

                UserActivationRequest req1 = mock(UserActivationRequest.class);
                when(req1.getId()).thenReturn(UUID.randomUUID());
                when(req1.getRequestId()).thenReturn(UUID.randomUUID());
                when(req1.getStatus()).thenReturn(ReactivationRequestStatus.IN_REVIEW);

                when(userActivationRequestRepository.findAllLatestRequests()).thenReturn(List.of(req1));

                // Request out-of-bounds paginatedRequests index (e.g., paginatedRequests 55)
                ReactivationRequestsPageData pageData = service.getPage(authentication, "", null, false, false, false, 55, 10, null, "asc");

                PaginatedReactivationRequests paginated = pageData.paginatedRequests();
                assertThat(paginated.getCurrentPage()).isEqualTo(1); // Bounded back to max total pages (1)
                assertThat(paginated.getRequests()).hasSize(1);
            }
        }

    }

    private void stubGlobalAdmin(EntraUser currentUser) {
        AppRole globalAdminRole = mock(AppRole.class);
        when(globalAdminRole.isAuthzRole()).thenReturn(true);
        when(globalAdminRole.getName()).thenReturn(AuthzRole.GLOBAL_ADMIN.getRoleName());
        UserProfile profile = mock(UserProfile.class);
        when(profile.isActiveProfile()).thenReturn(true);
        when(profile.getAppRoles()).thenReturn(Set.of(globalAdminRole));
        when(currentUser.getUserProfiles()).thenReturn(Set.of(profile));
    }
}
