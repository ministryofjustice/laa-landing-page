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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
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
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserReactivationRequestServiceTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final String ACTOR_ID = UUID.randomUUID().toString();
    private static final String UNKNOWN_USER_NAME = "Unknown user";
    private UUID userProfileId;
    private UUID requestId;

    @Mock
    private UserActivationRequestRepository requestRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
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

    @BeforeEach
    void setUp() {
        userProfileId = UUID.randomUUID();
        requestId = UUID.randomUUID();
    }


    private UserActivationRequest buildUserActivationRequest(ReactivationRequestStatus status, int version) {
        return UserActivationRequest.builder().id(UUID.randomUUID()).requestId(REQUEST_ID).userProfileId(PROFILE_ID).status(status).version(version).actorEntraOid(ACTOR_ID).build();
    }

    @Nested
    @DisplayName("findFirstByUserProfileIdOrderByVersionDesc")
    class FindFirstByUserProfileIdOrderByVersionDescTests {

        @Test
        @DisplayName("Should return empty optional when no request exists for profile ID")
        void noRequestFound_returnsEmptyOptional() {
            given(requestRepository.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID)).willReturn(Optional.empty());

            Optional<UserActivationRequest> result = service.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID);

            assertThat(result).isEmpty();
            verify(requestRepository).findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID);
        }

        @Test
        @DisplayName("Should return request when found for profile ID")
        void requestFound_returnsRequest() {
            UserActivationRequest existing = buildUserActivationRequest(ReactivationRequestStatus.IN_REVIEW, 1);
            given(requestRepository.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID)).willReturn(Optional.of(existing));

            Optional<UserActivationRequest> result = service.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID);

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
            UserProfile profile = UserProfile.builder().activeProfile(true).appRoles(Set.of(role)).build();
            EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).userProfiles(Set.of(profile)).build();
            given(entraUserRepository.findByEntraOid(any())).willReturn(Optional.of(entraUser));
            given(requestRepository.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID)).willReturn(Optional.empty());

            UserActivationRequest result = service.createNewRequest(REQUEST_ID, PROFILE_ID, "Reactivation comment", ACTOR_ID);

            ArgumentCaptor<UserActivationRequest> captor = ArgumentCaptor.forClass(UserActivationRequest.class);
            verify(requestRepository).save(captor.capture());

            UserActivationRequest saved = captor.getValue();
            assertThat(saved.getRequestId()).isEqualTo(REQUEST_ID);
            assertThat(saved.getUserProfileId()).isEqualTo(PROFILE_ID);
            assertThat(saved.getStatus()).isEqualTo(ReactivationRequestStatus.IN_REVIEW);
            assertThat(saved.getComments()).isEqualTo("Reactivation comment");
            assertThat(saved.getActorEntraOid()).isEqualTo(ACTOR_ID);
            assertThat(saved.getCreatedAt()).isNotNull();

            assertThat(result).isEqualTo(saved);
        }

        @Test
        @DisplayName("Should create new request when existing request is REJECTED")
        void existingRejectedRequest_createsNewRequest() {
            AppRole role = AppRole.builder().id(UUID.randomUUID()).name("Provider Admin").build();
            UserProfile profile = UserProfile.builder().activeProfile(true).appRoles(Set.of(role)).build();
            EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).userProfiles(Set.of(profile)).build();
            given(entraUserRepository.findByEntraOid(any())).willReturn(Optional.of(entraUser));
            UserActivationRequest rejected = buildUserActivationRequest(ReactivationRequestStatus.REJECTED, 1);
            given(requestRepository.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID)).willReturn(Optional.of(rejected));

            UserActivationRequest result = service.createNewRequest(REQUEST_ID, PROFILE_ID, "Retry comment", ACTOR_ID);

            assertThat(result).isNotNull();
            verify(requestRepository).save(any(UserActivationRequest.class));
        }

        @Test
        @DisplayName("Should create new request when existing request is APPROVED")
        void existingApprovedRequest_createsNewRequest() {
            AppRole role = AppRole.builder().id(UUID.randomUUID()).name("Provider Admin").build();
            UserProfile profile = UserProfile.builder().activeProfile(true).appRoles(Set.of(role)).build();
            EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).userProfiles(Set.of(profile)).build();
            given(entraUserRepository.findByEntraOid(any())).willReturn(Optional.of(entraUser));
            UserActivationRequest approved = buildUserActivationRequest(ReactivationRequestStatus.APPROVED, 1);
            given(requestRepository.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID)).willReturn(Optional.of(approved));


            UserActivationRequest result = service.createNewRequest(REQUEST_ID, PROFILE_ID, "New request", ACTOR_ID);

            assertThat(result).isNotNull();
            verify(requestRepository).save(any(UserActivationRequest.class));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when request is currently IN_REVIEW")
        void activeRequestInReview_throwsIllegalStateException() {
            UserActivationRequest inReview = buildUserActivationRequest(ReactivationRequestStatus.IN_REVIEW, 1);
            given(requestRepository.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(PROFILE_ID)).willReturn(Optional.of(inReview));

            assertThatThrownBy(() -> service.createNewRequest(REQUEST_ID, PROFILE_ID, "Comment", ACTOR_ID))
                    .isInstanceOf(IllegalStateException.class).hasMessage("Request already being processed for user " + PROFILE_ID);

            verify(requestRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("saveRequestState")
    class SaveRequestStateTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException when target user profile does not exist")
        void targetUserNotFound_throwsException() {
            given(userProfileRepository.existsById(PROFILE_ID)).willReturn(false);

            assertThatThrownBy(() -> service.saveRequestState(REQUEST_ID, PROFILE_ID, ReactivationRequestStatus.APPROVED, "Comments", ACTOR_ID))
                    .isInstanceOf(EntityNotFoundException.class).hasMessage("Target user not found with ID: " + PROFILE_ID);

            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should save state with version 1 when requestId is null")
        void nullRequestId_generatesUuidAndVersionOne() {
            given(requestRepository.save(any(UserActivationRequest.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(entraUserRepository.findByEntraOid(ACTOR_ID)).willReturn(Optional.of(EntraUser.builder().id(UUID.randomUUID()).build()));
            given(userProfileRepository.existsById(PROFILE_ID)).willReturn(true);
            given(roleTypeResolver.resolve(any())).willReturn(ReactivationRoleType.PROVIDER_ADMIN);

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
            given(entraUserRepository.findByEntraOid(ACTOR_ID)).willReturn(Optional.of(EntraUser.builder().id(UUID.randomUUID()).build()));
            given(userProfileRepository.existsById(PROFILE_ID)).willReturn(true);
            given(roleTypeResolver.resolve(any())).willReturn(ReactivationRoleType.PROVIDER_ADMIN);

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
            given(entraUserRepository.findByEntraOid(ACTOR_ID)).willReturn(Optional.of(EntraUser.builder().id(UUID.randomUUID()).build()));
            given(userProfileRepository.existsById(PROFILE_ID)).willReturn(true);
            given(roleTypeResolver.resolve(any())).willReturn(ReactivationRoleType.PROVIDER_ADMIN);

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
            given(userProfileRepository.existsById(PROFILE_ID)).willReturn(true);

            assertThatThrownBy(() -> service.saveRequestState(REQUEST_ID, PROFILE_ID, ReactivationRequestStatus.REJECTED, "Cannot reject", ACTOR_ID))
                    .isInstanceOf(IllegalStateException.class).hasMessage(String.format("Request already processed for user %s. Request ID: %s", PROFILE_ID, REQUEST_ID));

            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when request has already been REJECTED")
        void alreadyRejected_throwsIllegalStateException() {
            UserActivationRequest existing = buildUserActivationRequest(ReactivationRequestStatus.REJECTED, 1);
            given(requestRepository.findFirstByRequestIdOrderByVersionDesc(REQUEST_ID)).willReturn(Optional.of(existing));
            given(userProfileRepository.existsById(PROFILE_ID)).willReturn(true);

            assertThatThrownBy(() -> service.saveRequestState(REQUEST_ID, PROFILE_ID, ReactivationRequestStatus.APPROVED,
                    "Cannot approve", ACTOR_ID)).isInstanceOf(IllegalStateException.class)
                    .hasMessage(String.format("Request already processed for user %s. Request ID: %s", PROFILE_ID, REQUEST_ID));

            verify(requestRepository, never()).save(any());
        }

        @Nested
        @DisplayName("getLatestRequestHistoryForUserProfile")
        class GetLatestRequestHistoryForUserProfile {

            @Test
            @DisplayName("Should return empty list when history query yields no records")
            void shouldReturnEmptyListWhenHistoryIsEmpty() {
                UserActivationRequest latestRequest = mock(UserActivationRequest.class);
                when(latestRequest.getRequestId()).thenReturn(requestId);

                when(requestRepository.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(userProfileId)).thenReturn(Optional.of(latestRequest));
                when(requestRepository.findRequestHistoryByRequestId(requestId)).thenReturn(Collections.emptyList());

                List<UserActivationRequestSummaryDto> result = service.getLatestRequestHistoryForUserProfile(userProfileId);

                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("Should return history when records are found")
            void shouldReturnHistoryListWhenFound() {
                UserActivationRequest latestRequest = mock(UserActivationRequest.class);
                when(latestRequest.getRequestId()).thenReturn(requestId);

                UserActivationRequestSummaryDto dto = mock(UserActivationRequestSummaryDto.class);

                when(requestRepository.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(userProfileId)).thenReturn(Optional.of(latestRequest));
                when(requestRepository.findRequestHistoryByRequestId(requestId)).thenReturn(List.of(dto));

                List<UserActivationRequestSummaryDto> result = service.getLatestRequestHistoryForUserProfile(userProfileId);

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
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(actor);
                when(roleTypeResolver.resolve(actor)).thenReturn(ReactivationRoleType.PROVIDER_ADMIN);

                ReactivationRequestStatus status = service.calculateNextReactivationRequestStatus(authentication);

                assertThat(status).isEqualTo(ReactivationRequestStatus.IN_REVIEW);
            }

            @Test
            @DisplayName("Should return INFORMATION_REQUIRED when role is not PROVIDER_ADMIN")
            void shouldReturnInformationRequiredForNonProviderAdmin() {
                EntraUser actor = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(actor);
                when(roleTypeResolver.resolve(actor)).thenReturn(ReactivationRoleType.LAA);

                ReactivationRequestStatus status = service.calculateNextReactivationRequestStatus(authentication);

                assertThat(status).isEqualTo(ReactivationRequestStatus.INFORMATION_REQUIRED);
            }
        }

        // ==========================================
        // 3. getPageMode & resolvePageMode
        // ==========================================
        @Nested
        @DisplayName("getPageMode & resolvePageMode")
        class PageModeTests {

            @Test
            @DisplayName("Should return MANAGE mode when current user is null")
            void shouldReturnManageWhenUserIsNull() {
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(null);

                ReactivationRequestPageMode pageMode = service.getPageMode(authentication);

                assertThat(pageMode).isEqualTo(ReactivationRequestPageMode.MANAGE);
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
                when(requestRepository.findAllLatestRequests()).thenReturn(List.of());

                ReactivationRequestsPageData result = service.getPage(authentication, "", null, null, 1, 10, "requestId", "asc");

                assertThat(result.pageMode()).isEqualTo(ReactivationRequestPageMode.MANAGE);
                assertThat(result.paginatedRequests().getRequests()).isEmpty();
                assertThat(result.paginatedRequests().getTotalRequests()).isEqualTo(0);
            }

            @Test
            @DisplayName("Should process, filter, sort and paginate requests in MANAGE mode")
            void shouldBuildFilterSortAndPaginateRequestsInManageMode() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);

                // Set up test entities
                UUID request1Id = UUID.randomUUID();
                UUID profile1Id = UUID.randomUUID();
                String actor1Oid = "actor-oid-1";

                UserActivationRequest request1 = mock(UserActivationRequest.class);
                when(request1.getId()).thenReturn(UUID.randomUUID());
                when(request1.getRequestId()).thenReturn(request1Id);
                when(request1.getUserProfileId()).thenReturn(profile1Id);
                when(request1.getActorEntraOid()).thenReturn(actor1Oid);
                when(request1.getStatus()).thenReturn(ReactivationRequestStatus.IN_REVIEW);
                when(request1.getVersion()).thenReturn(1);
                when(request1.getCreatedAt()).thenReturn(Instant.now());
                when(request1.getComments()).thenReturn("Sample request comment");

                when(requestRepository.findAllLatestRequests()).thenReturn(List.of(request1));

                // Target Profile & User
                EntraUser targetUser = mock(EntraUser.class);
                when(targetUser.getFirstName()).thenReturn("Jane");
                when(targetUser.getLastName()).thenReturn("Doe");
                when(targetUser.getEmail()).thenReturn("jane.doe@example.com");

                Firm targetFirm = mock(Firm.class);
                UUID firmId = UUID.randomUUID();
                when(targetFirm.getId()).thenReturn(firmId);

                UserProfile profile1 = mock(UserProfile.class);
                when(profile1.getId()).thenReturn(profile1Id);
                when(profile1.getEntraUser()).thenReturn(targetUser);
                when(profile1.getFirm()).thenReturn(targetFirm);

                when(userProfileRepository.findAllByIdInWithFirm(Set.of(profile1Id))).thenReturn(List.of(profile1));

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

                when(requestRepository.findAllFirstVersionsByRequestIdIn(Set.of(request1Id))).thenReturn(List.of(firstVerRequest));

                // Execute method call
                ReactivationRequestsPageData result = service.getPage(authentication, "Jane", // Search string matches target user name
                        List.of(ReactivationRequestStatus.IN_REVIEW), null, 1, 10, "actorName", "asc");

                assertThat(result.paginatedRequests().getTotalRequests()).isEqualTo(1);
                ReactivationRequestListItem item = result.paginatedRequests().getRequests().get(0);
                assertThat(item.userName()).isEqualTo("Jane Doe");
                assertThat(item.actorName()).isEqualTo("John Smith");
                assertThat(item.firmId()).isEqualTo(firmId);
            }

            @Test
            @DisplayName("Should return empty list in TRACK mode if user has no allowed firms")
            void shouldReturnEmptyWhenTrackModeHasNoAllowedFirms() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);

                try (MockedStatic<AccessControlService> accessControlMock = mockStatic(AccessControlService.class)) {
                    accessControlMock.when(() -> AccessControlService.userHasAuthzRole(currentUser, AuthzRole.FIRM_USER_MANAGER.getRoleName())).thenReturn(true);

                    UserActivationRequest request1 = UserActivationRequest
                            .builder()
                            .userProfileId(userProfileId)
                            .actorEntraOid("actor-1")
                            .requestId(requestId)
                            .status(ReactivationRequestStatus.IN_REVIEW)
                            .build();

                    when(requestRepository.findAllLatestRequests()).thenReturn(List.of(request1));
                    when(firmService.getUserActiveAllFirms(currentUser)).thenReturn(List.of());

                    ReactivationRequestsPageData result = service.getPage(authentication, null, null, null, 1, 10, null, "asc");

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
                    FirmDto allowedFirmDto = mock(FirmDto.class);
                    when(allowedFirmDto.getId()).thenReturn(allowedFirmId);

                    when(firmService.getUserActiveAllFirms(currentUser)).thenReturn(List.of(allowedFirmDto));

                    UserActivationRequest request = mock(UserActivationRequest.class);
                    when(request.getId()).thenReturn(UUID.randomUUID());
                    when(request.getRequestId()).thenReturn(requestId);
                    when(request.getUserProfileId()).thenReturn(userProfileId);
                    when(request.getActorEntraOid()).thenReturn("actor-oid");
                    when(request.getStatus()).thenReturn(ReactivationRequestStatus.IN_REVIEW);

                    when(requestRepository.findAllLatestRequests()).thenReturn(List.of(request));

                    Firm allowedFirm = mock(Firm.class);
                    when(allowedFirm.getId()).thenReturn(allowedFirmId);

                    UserProfile profile = mock(UserProfile.class);
                    when(profile.getId()).thenReturn(userProfileId);
                    when(profile.getFirm()).thenReturn(allowedFirm);

                    when(userProfileRepository.findAllByIdInWithFirm(Set.of(userProfileId))).thenReturn(List.of(profile));

                    ReactivationRequestsPageData result = service.getPage(authentication, "", null, null, 1, 10, null, "asc");

                    assertThat(result.paginatedRequests().getRequests()).hasSize(1);
                    assertThat(result.paginatedRequests().getRequests().get(0).firmId()).isEqualTo(allowedFirmId);
                }
            }

            @Test
            @DisplayName("Should handle missing optional entity references cleanly and fallback to UNKNOWN_USER_NAME")
            void shouldHandleMissingActorAndProfileGracefully() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);

                UserActivationRequest request = mock(UserActivationRequest.class);
                when(request.getId()).thenReturn(UUID.randomUUID());
                when(request.getRequestId()).thenReturn(requestId);
                when(request.getUserProfileId()).thenReturn(null);
                when(request.getActorEntraOid()).thenReturn(null);
                when(request.getStatus()).thenReturn(ReactivationRequestStatus.APPROVED);

                when(requestRepository.findAllLatestRequests()).thenReturn(List.of(request));

                ReactivationRequestsPageData result = service.getPage(authentication, "", null, null, 1, 10, null, "asc");

                assertThat(result.paginatedRequests().getRequests()).hasSize(1);
                ReactivationRequestListItem item = result.paginatedRequests().getRequests().get(0);

                assertThat(item.actorName()).isEqualTo(UNKNOWN_USER_NAME);
                assertThat(item.userName()).isEqualTo(UNKNOWN_USER_NAME);
                assertThat(item.actorEmail()).isNull();
                assertThat(item.userEmail()).isNull();
                assertThat(item.firmId()).isNull();

                verifyNoInteractions(entraUserRepository);
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

                UserActivationRequest req1 = UserActivationRequest.builder()
                        .id(UUID.randomUUID())
                        .requestId(UUID.randomUUID())
                        .userProfileId(UUID.randomUUID())
                        .status(ReactivationRequestStatus.IN_REVIEW)
                        .version(1)
                        .build();

                UserActivationRequest req2 = UserActivationRequest.builder()
                        .id(UUID.randomUUID())
                        .requestId(UUID.randomUUID())
                        .userProfileId(UUID.randomUUID())
                        .status(ReactivationRequestStatus.REJECTED)
                        .version(1)
                        .build();

                when(requestRepository.findAllLatestRequests()).thenReturn(List.of(req1, req2));

                String[] sortFields = {"requestId", "userProfileId", "version", "requestStatus", "actorName", "actorRoleType", "lastActivity", "invalidSortDefault"};

                for (String sortField : sortFields) {
                    ReactivationRequestsPageData data = service.getPage(authentication, "", null, null, 1, 10, sortField, "desc");

                    assertThat(data.paginatedRequests().getRequests()).hasSize(2);
                }
            }

            @Test
            @DisplayName("Should accurately perform pagination paginatedRequests boundary checks")
            void shouldHandlePaginationBoundaries() {
                EntraUser currentUser = mock(EntraUser.class);
                when(loginService.getCurrentEntraUser(authentication)).thenReturn(currentUser);

                UserActivationRequest req1 = mock(UserActivationRequest.class);
                when(req1.getId()).thenReturn(UUID.randomUUID());
                when(req1.getRequestId()).thenReturn(UUID.randomUUID());
                when(req1.getStatus()).thenReturn(ReactivationRequestStatus.IN_REVIEW);

                when(requestRepository.findAllLatestRequests()).thenReturn(List.of(req1));

                // Request out-of-bounds paginatedRequests index (e.g., paginatedRequests 55)
                ReactivationRequestsPageData pageData = service.getPage(authentication, "", null, null, 55, 10, null, "asc");

                PaginatedReactivationRequests paginated = pageData.paginatedRequests();
                assertThat(paginated.getCurrentPage()).isEqualTo(1); // Bounded back to max total pages (1)
                assertThat(paginated.getRequests()).hasSize(1);
            }
        }

    }
}
