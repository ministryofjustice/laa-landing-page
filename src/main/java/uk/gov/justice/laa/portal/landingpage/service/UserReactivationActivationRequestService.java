package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserActivationRequestSummaryDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRequestStatus;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRoleType;
import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserActivationRequestRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class UserReactivationActivationRequestService {
    private final UserService userService;
    private final UserActivationRequestRepository requestRepository;
    private final UserProfileRepository userProfileRepository;
    private final EntraUserRepository entraUserRepository;
    private final ReactivationTypeResolver roleTypeResolver;

    public UserReactivationActivationRequestService(UserService userService,
                                                    UserActivationRequestRepository requestRepository,
                                                    UserProfileRepository userRepository,
                                                    EntraUserRepository entraUserRepository,
                                                    ReactivationTypeResolver roleTypeResolver) {
        this.userService = userService;
        this.requestRepository = requestRepository;
        this.userProfileRepository = userRepository;
        this.entraUserRepository = entraUserRepository;
        this.roleTypeResolver = roleTypeResolver;
    }

    public Optional<UserActivationRequest> findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(UUID profileId) {
        return requestRepository.findFirstByUserProfileIdOrderByVersionDesc(profileId);
    }

    public UserActivationRequest createNewRequest(UUID requestId, UUID profileId, String reason, String actorEntraOid) {
        Optional<UserActivationRequest> request = requestRepository.findFirstByUserProfileIdOrderByVersionDesc(profileId);

        if (request.isPresent() && !(ReactivationRequestStatus.REJECTED.equals(request.get().getStatus()) || ReactivationRequestStatus.APPROVED.equals(request.get().getStatus()))) {
            log.error("Request already being processed for user {}", profileId);
            throw new IllegalStateException("Request already being processed for user " + profileId);
        }

        UserActivationRequest newRecord = new UserActivationRequest();
        newRecord.setRequestId(requestId);
        newRecord.setUserProfileId(profileId);
        newRecord.setStatus(ReactivationRequestStatus.IN_REVIEW);
        newRecord.setComments(reason);
        newRecord.setActorEntraOid(actorEntraOid);
        newRecord.setCreatedAt(Instant.now());
        newRecord.setActorRoleType(ReactivationRoleType.PROVIDER_ADMIN);

        requestRepository.save(newRecord);

        return newRecord;
    }

    public UserActivationRequest saveRequestState(UUID requestId, UUID userId, ReactivationRequestStatus status, String comments, String actorEntraOid) {

        if (!userProfileRepository.existsById(userId)) {
            throw new EntityNotFoundException("Target user not found with ID: " + userId);
        }

        int nextVersion = 1;
        UUID activeRequestId = (requestId != null) ? requestId : UUID.randomUUID();

        if (requestId != null) {
            Optional<UserActivationRequest> firstByRequestIdOrderByVersionDesc = requestRepository.findFirstByRequestIdOrderByVersionDesc(requestId);
            if (firstByRequestIdOrderByVersionDesc.isPresent()) {
                UserActivationRequest existingRecord = firstByRequestIdOrderByVersionDesc.get();
                if (ReactivationRequestStatus.APPROVED.equals(existingRecord.getStatus()) || ReactivationRequestStatus.REJECTED.equals(existingRecord.getStatus())) {
                    log.error("Reactivation request already processed for user {}. Request ID: {}", userId, requestId);
                    throw new IllegalStateException(String.format("Request already processed for user %s. Request ID: %s", userId, requestId));
                }
                nextVersion = firstByRequestIdOrderByVersionDesc.get().getVersion() + 1;
            }
        }

        EntraUser entraUser = entraUserRepository.findByEntraOid(actorEntraOid).orElseThrow();
        ReactivationRoleType roleType = roleTypeResolver.resolve(entraUser);

        UserActivationRequest newRecord = new UserActivationRequest();
        newRecord.setRequestId(activeRequestId);
        newRecord.setUserProfileId(userId);
        newRecord.setVersion(nextVersion);
        newRecord.setStatus(status);
        newRecord.setComments(comments);
        newRecord.setActorEntraOid(actorEntraOid);
        newRecord.setActorRoleType(roleType);
        newRecord.setCreatedAt(Instant.now());

        return requestRepository.save(newRecord);
    }

    @Transactional(readOnly = true)
    public List<UserActivationRequestSummaryDto> getLatestRequestHistoryForUserProfile(UUID userProfileId) {
        log.debug("Fetching latest activation request history for user profile ID: {}", userProfileId);

        UserActivationRequest latestRequest = requestRepository.findTopByUserProfileIdOrderByCreatedAtDescVersionDesc(userProfileId);

        List<UserActivationRequestSummaryDto> history = requestRepository.findRequestHistoryByRequestId(latestRequest.getRequestId());

        if (history.isEmpty()) {
            log.info("No activation request history found for user profile ID: {}", userProfileId);
            return Collections.emptyList();
        }

        return history;
    }

    public ReactivationRequestStatus calculateNextReactivationRequestStatus(UUID profileId) {
        List<AppRoleDto> userAppRolesByUserId = userService.getUserAppRolesByUserId(profileId.toString());
        List<String> userRoles = userAppRolesByUserId.stream().map(AppRoleDto::getName).toList();
        ReactivationRoleType roleType = roleTypeResolver.resolveFromRoles(userRoles);

        return roleType == ReactivationRoleType.PROVIDER_ADMIN ? ReactivationRequestStatus.IN_REVIEW : ReactivationRequestStatus.INFORMATION_REQUIRED;
    }
}
