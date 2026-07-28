package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserActivationRequestSummaryDto;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRoleType;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRequestStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;
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
    private final UserActivationRequestRepository requestRepository;
    private final UserProfileRepository userProfileRepository;

    public UserReactivationActivationRequestService(UserActivationRequestRepository requestRepository,
                                                    UserProfileRepository userRepository) {
        this.requestRepository = requestRepository;
        this.userProfileRepository = userRepository;
    }

    public Optional<UserActivationRequest> findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(UUID profileId) {
        return requestRepository.findFirstByUserProfileIdOrderByVersionDesc(profileId);
    }

    public UserActivationRequest createNewRequest(UUID requestId, UUID profileId, String reason, EntraUserDto user) {
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
        newRecord.setActorEntraOid(user.getEntraOid());
        newRecord.setCreatedAt(Instant.now());
        newRecord.setActorRoleType(AuthzRoleType.PROVIDER_ADMIN);

        requestRepository.save(newRecord);

        return newRecord;
    }

    public UserActivationRequest saveRequestState(UUID requestId, UUID userId, ReactivationRequestStatus status, String comments, String actionByUserId) {

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

        UserActivationRequest newRecord = new UserActivationRequest();
        newRecord.setRequestId(activeRequestId);
        newRecord.setUserProfileId(userId);
        newRecord.setVersion(nextVersion);
        newRecord.setStatus(status);
        newRecord.setComments(comments);
        newRecord.setActorEntraOid(actionByUserId);
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

}
