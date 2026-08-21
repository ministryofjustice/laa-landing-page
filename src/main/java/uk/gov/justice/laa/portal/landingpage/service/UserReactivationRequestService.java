package uk.gov.justice.laa.portal.landingpage.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.ReactivationRequestsPageData;
import uk.gov.justice.laa.portal.landingpage.dto.UserActivationRequestSummaryDto;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
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

@Service
@Transactional
@Slf4j
public class UserReactivationRequestService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String UNKNOWN_USER_NAME = "Unknown user";

    private final LoginService loginService;
    private final FirmService firmService;
    private final UserActivationRequestRepository userActivationRequestRepository;
    private final UserProfileRepository userProfileRepository;
    private final EntraUserRepository entraUserRepository;
    private final ReactivationTypeResolver roleTypeResolver;
    private final NotificationService notificationService;

    public UserReactivationRequestService(LoginService loginService,
                                          FirmService firmService,
                                          UserActivationRequestRepository userActivationRequestRepository,
                                          UserProfileRepository userRepository,
                                          EntraUserRepository entraUserRepository,
                                          ReactivationTypeResolver roleTypeResolver,
                                          NotificationService notificationService) {
        this.loginService = loginService;
        this.firmService = firmService;
        this.userActivationRequestRepository = userActivationRequestRepository;
        this.userProfileRepository = userRepository;
        this.entraUserRepository = entraUserRepository;
        this.roleTypeResolver = roleTypeResolver;
        this.notificationService = notificationService;
    }

    public Optional<UserActivationRequest> findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(String profileId) {
        log.debug("Fetching latest activation request for profile ID: {}", profileId);
        return userActivationRequestRepository.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(parseUuid(profileId));
    }

    public UserActivationRequest createReactivationRequest(String userId, String profileId, String comments, String actorEntraOid) {
        log.info("A new user activation request is being submitted for user {} with profile ID: {} by User Entra Oid: {}",
                userId, profileId, actorEntraOid);

        userActivationRequestRepository.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(parseUuid(profileId))
                .ifPresent(existing -> {
                    ReactivationRequestStatus status = existing.getStatus();
                    if (status != ReactivationRequestStatus.REJECTED && status != ReactivationRequestStatus.APPROVED) {
                        log.error("Request already being processed for profile ID: {}", profileId);
                        throw new IllegalStateException("Request already being processed for user " + profileId);
                    }
                });

        EntraUser actor = entraUserRepository.findByEntraOid(actorEntraOid).orElseThrow();
        ReactivationRoleType actorRoleType = roleTypeResolver.resolve(actor);

        EntraUser providerUser = entraUserRepository.findById(UUID.fromString(userId)).orElseThrow();

        final UserActivationRequest newRequest = createReactivationRequestEntry(null, profileId,
                ReactivationRequestStatus.IN_REVIEW, comments, actorEntraOid, actorRoleType);

        if (ReactivationRoleType.PROVIDER_ADMIN.equals(actorRoleType)) {
            log.debug("Sending submission notification to Provider Admin: {}", actor.getEmail());
            notificationService.notifyReactivationRequestSubmitted(
                    actor.getId().toString(), actor.getFirstName(), actor.getEmail(),
                    actor.getId().toString(), profileId, providerUser.getEmail()
            );
        }

        log.debug("Sending submission notification to target user: {}", providerUser.getEmail());
        notificationService.notifyReactivationRequestSubmitted(
                actor.getId().toString(), providerUser.getFirstName(), providerUser.getEmail(), providerUser.getId().toString(), profileId, providerUser.getEmail()
        );

        log.info("Successfully created reactivation request ID: {} (Version 1) for profile ID: {}",
                newRequest.getRequestId(), profileId);
        return newRequest;
    }

    public UserActivationRequest approveReactivationRequest(String requestId, String userEntraId, String userProfileId, String actorEntraOid) {
        log.info("Approving reactivation request ID: {} for user profile ID: {} by actor OID: {}", requestId, userProfileId, actorEntraOid);
        return processReactivationState(requestId, userEntraId, userProfileId, "Approved", actorEntraOid, ReactivationRequestStatus.APPROVED, true);
    }

    public UserActivationRequest rejectReactivationRequest(String requestId, String userEntraId, String userProfileId, String comments, String actorEntraOid) {
        log.info("Rejecting reactivation request ID: {} for user profile ID: {} by actor OID: {}", requestId, userProfileId, actorEntraOid);
        return processReactivationState(requestId, userEntraId, userProfileId, comments, actorEntraOid, ReactivationRequestStatus.REJECTED, false);
    }

    public UserActivationRequest updateReactivateRequestState(String requestId, String userEntraId, String userProfileId, String comments, String actorEntraOid) {
        log.info("Updating reactivation request ID: {} for user profile ID: {} by actor OID: {}", requestId, userProfileId, actorEntraOid);

        EntraUser entraUser = entraUserRepository.findByEntraOid(actorEntraOid).orElseThrow();
        ReactivationRoleType roleType = roleTypeResolver.resolve(entraUser);

        validateActiveReactivationRequestPresent(requestId);

        ReactivationRequestStatus reactivationRequestStatus = calculateNextReactivationRequestStatus(entraUser);
        log.debug("Calculated next status: {} for request ID: {}", reactivationRequestStatus, requestId);

        UserActivationRequest result = createReactivationRequestEntry(requestId, userProfileId, reactivationRequestStatus, comments, actorEntraOid, roleType);

        if (ReactivationRequestStatus.INFORMATION_REQUIRED.equals(reactivationRequestStatus)) {
            UserActivationRequest initialRequest = userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(parseUuid(requestId)).orElseThrow();
            if (initialRequest.getActorRoleType() == ReactivationRoleType.PROVIDER_ADMIN) {
                EntraUser providerAdmin = entraUserRepository.findByEntraOid(initialRequest.getActorEntraOid()).orElseThrow();
                EntraUser providerUser = entraUserRepository.findById(parseUuid(userEntraId)).orElseThrow();
                log.debug("Notifying provider admin {} that information is required for request ID: {}", providerAdmin.getEmail(), requestId);
                notificationService.notifyReactivationRequestInfoRequested(
                        entraUser.getId().toString(), providerAdmin.getFirstName(), providerAdmin.getEmail(), providerAdmin.getId().toString(),
                        userProfileId, providerUser.getEmail()
                );
            }
        }

        log.info("Successfully updated reactivation request ID: {} to status: {} (Version {})", requestId, reactivationRequestStatus, result.getVersion());
        return result;
    }

    private UserActivationRequest processReactivationState(String requestId, String userEntraId, String userProfileId, String comments,
                                                           String actorEntraOid, ReactivationRequestStatus status, boolean isApproved) {
        EntraUser entraUser = entraUserRepository.findByEntraOid(actorEntraOid).orElseThrow();
        ReactivationRoleType roleType = roleTypeResolver.resolve(entraUser);
        UserActivationRequest initialRequest = userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(parseUuid(requestId)).orElseThrow();

        final UserActivationRequest result = createReactivationRequestEntry(requestId, userProfileId, status, comments, actorEntraOid, roleType);

        EntraUser providerUser = entraUserRepository.findById(parseUuid(userEntraId)).orElseThrow();

        if (initialRequest.getActorRoleType() == ReactivationRoleType.PROVIDER_ADMIN) {
            EntraUser providerAdmin = entraUserRepository.findByEntraOid(initialRequest.getActorEntraOid()).orElseThrow();
            if (isApproved) {
                log.debug("Notifying Provider Admin {} of approval for request ID: {}", providerAdmin.getEmail(), requestId);
                notificationService.notifyReactivationRequestApproved(entraUser.getId().toString(), providerAdmin.getFirstName(),
                        providerAdmin.getEmail(), providerAdmin.getId().toString(), userProfileId, providerUser.getEmail());
            } else {
                log.debug("Notifying Provider Admin {} of rejection for request ID: {}", providerAdmin.getEmail(), requestId);
                notificationService.notifyReactivationRequestRejected(entraUser.getId(), providerAdmin.getFirstName(),
                        providerAdmin.getEmail(), providerAdmin.getId().toString(), userProfileId, providerUser.getEmail());
            }
        }

        if (isApproved) {
            log.debug("Notifying target user {} of approval for request ID: {}", providerUser.getEmail(), requestId);
            notificationService.notifyReactivationRequestApproved(entraUser.getId().toString(), providerUser.getFirstName(),
                    providerUser.getEmail(), providerUser.getId().toString(), providerUser.getId().toString(), providerUser.getEmail());
        } else {
            log.debug("Notifying target user {} of rejection for request ID: {}", providerUser.getEmail(), requestId);
            notificationService.notifyReactivationRequestRejected(entraUser.getId(), providerUser.getFirstName(),
                    providerUser.getEmail(), providerUser.getId().toString(), providerUser.getId().toString(), providerUser.getEmail());
        }

        log.info("Completed processing state change for request ID: {} to status: {}", requestId, status);
        return result;
    }

    private UserActivationRequest createReactivationRequestEntry(String requestId, String userProfileId,
                                                                 ReactivationRequestStatus status, String comments,
                                                                 String actorEntraOid, ReactivationRoleType roleType) {

        if (userProfileId == null || !userProfileRepository.existsById(parseUuid(userProfileId))) {
            log.error("Failed to create request entry. User profile not found for ID: {}", userProfileId);
            throw new EntityNotFoundException("Target user not found with ID: " + userProfileId);
        }

        int nextVersion = 1;
        UUID activeRequestId = requestId != null ? parseUuid(requestId) : UUID.randomUUID();

        if (requestId != null) {
            UserActivationRequest firstByRequestIdOrderByVersionDesc = userActivationRequestRepository
                    .findFirstByRequestIdOrderByVersionDesc(parseUuid(requestId)).orElseThrow();
            nextVersion = firstByRequestIdOrderByVersionDesc.getVersion() + 1;
        }

        log.debug("Creating reactivation request entry. Request ID: {}, Profile ID: {}, Version: {}, Status: {}",
                activeRequestId, userProfileId, nextVersion, status);

        UserActivationRequest newRecord = new UserActivationRequest();
        newRecord.setRequestId(activeRequestId);
        newRecord.setUserProfileId(parseUuid(userProfileId));
        newRecord.setVersion(nextVersion);
        newRecord.setStatus(status);
        newRecord.setComments(comments);
        newRecord.setActorEntraOid(actorEntraOid);
        newRecord.setActorRoleType(roleType);
        newRecord.setCreatedAt(Instant.now());

        return userActivationRequestRepository.save(newRecord);
    }

    private void validateActiveReactivationRequestPresent(String requestId) {
        log.debug("Validating active reactivation request for ID: {}", requestId);
        UserActivationRequest request = userActivationRequestRepository.findFirstByRequestIdOrderByVersionDesc(parseUuid(requestId))
                .orElseThrow(() -> {
                    log.error("Reactivation request not found for ID: {}", requestId);
                    return new EntityNotFoundException("Reactivation request not found for ID: " + requestId);
                });

        if (request.getStatus() == ReactivationRequestStatus.APPROVED || request.getStatus() == ReactivationRequestStatus.REJECTED) {
            log.warn("Attempted operation on already finalized request ID: {} in status: {}", requestId, request.getStatus());
            throw new IllegalStateException("Reactivation request already processed for ID: " + requestId);
        }
    }

    @Transactional(readOnly = true)
    public List<UserActivationRequestSummaryDto> getLatestRequestHistoryForUserProfile(String userProfileId) {
        log.debug("Fetching latest activation request history for user profile ID: {}", userProfileId);

        Optional<UserActivationRequest> latestRequest = userActivationRequestRepository.findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(parseUuid(userProfileId));

        if (latestRequest.isEmpty()) {
            log.info("No activation request found for user profile ID: {}", userProfileId);
            return Collections.emptyList();
        }

        List<UserActivationRequestSummaryDto> history = userActivationRequestRepository.findRequestHistoryByRequestId(latestRequest.get().getRequestId());

        if (history.isEmpty()) {
            log.info("No activation request history found for user profile ID: {}", userProfileId);
            return Collections.emptyList();
        }

        log.debug("Found {} history records for user profile ID: {}", history.size(), userProfileId);
        return history;
    }

    public ReactivationRequestStatus calculateNextReactivationRequestStatus(EntraUser entraUser) {
        ReactivationRoleType roleType = roleTypeResolver.resolve(entraUser);
        log.debug("Resolving next status for user {} with role type: {}", entraUser.getId(), roleType);

        return ReactivationRoleType.PROVIDER_ADMIN.equals(roleType)
                || ReactivationRoleType.LAA_USER_REGISTRATION.equals(roleType)
                || ReactivationRoleType.LAA_OST.equals(roleType)
                ? ReactivationRequestStatus.IN_REVIEW : ReactivationRequestStatus.INFORMATION_REQUIRED;
    }

    @Transactional(readOnly = true)
    public ReactivationRequestsPageData getPage(
            Authentication authentication,
            String search,
            List<ReactivationRequestStatus> selectedStatuses,
            List<ReactivationRoleType> selectedActorRoleTypes,
            int page,
            int size,
            String sort,
            String direction) {

        log.debug("Building reactivation requests page. Page: {}, Size: {}, Sort: {}, Direction: {}", page, size, sort, direction);

        EntraUser currentUser = loginService.getCurrentEntraUser(authentication);
        ReactivationRequestPageMode pageMode = resolvePageMode(currentUser);
        List<ReactivationRequestStatus> effectiveStatuses = selectedStatuses == null
                ? List.of()
                : List.copyOf(selectedStatuses);
        List<ReactivationRoleType> effectiveActorRoleTypes = selectedActorRoleTypes == null
                ? List.of()
                : List.copyOf(selectedActorRoleTypes);

        List<ReactivationRequestListItem> requests = filterAndSortRequests(
                buildRequests(currentUser, pageMode),
                normalizeSearch(search),
                effectiveStatuses,
                effectiveActorRoleTypes,
                sort,
                direction);

        PaginatedReactivationRequests paginated = paginate(requests, page, size);
        log.debug("Returning {} filtered items across {} pages for mode: {}",
                paginated.getTotalRequests(), paginated.getTotalPages(), pageMode);

        return new ReactivationRequestsPageData(pageMode, effectiveStatuses, effectiveActorRoleTypes, paginated);
    }

    public ReactivationRequestPageMode getPageMode(Authentication authentication) {
        return resolvePageMode(loginService.getCurrentEntraUser(authentication));
    }

    private ReactivationRequestPageMode resolvePageMode(EntraUser currentUser) {
        if (currentUser == null) {
            log.debug("No current user provided; defaulting page mode to MANAGE");
            return ReactivationRequestPageMode.MANAGE;
        }

        boolean isManageRole = AccessControlService.userHasAuthzRole(currentUser, AuthzRole.EXTERNAL_USER_MANAGER.getRoleName())
                || AccessControlService.userHasAuthzRole(currentUser, AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())
                || AccessControlService.userHasAuthzRole(currentUser, AuthzRole.EXTERNAL_USER_VIEWER.getRoleName())
                || AccessControlService.userHasAuthzRole(currentUser, AuthzRole.GLOBAL_ADMIN.getRoleName())
                || AccessControlService.userHasAuthzRole(currentUser, AuthzRole.SECURITY_RESPONSE.getRoleName());

        boolean isProviderAdminOnly = AccessControlService.userHasAuthzRole(currentUser, AuthzRole.FIRM_USER_MANAGER.getRoleName())
                && !isManageRole;

        ReactivationRequestPageMode resolvedMode = isProviderAdminOnly ? ReactivationRequestPageMode.TRACK : ReactivationRequestPageMode.MANAGE;
        log.debug("Resolved page mode: {} for user: {}", resolvedMode, currentUser.getId());
        return resolvedMode;
    }

    private List<ReactivationRequestListItem> buildRequests(EntraUser currentUser, ReactivationRequestPageMode pageMode) {
        List<UserActivationRequest> latestRequests = userActivationRequestRepository.findAllLatestRequests();

        if (latestRequests.isEmpty()) {
            log.debug("No latest reactivation requests found in database");
            return List.of();
        }

        Set<UUID> profileIds = latestRequests.stream()
                .map(UserActivationRequest::getUserProfileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, UserProfile> profilesById = userProfileRepository.findAllByIdInWithFirm(profileIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, profile -> profile));

        Set<String> actorEntraOids = latestRequests.stream()
                .map(UserActivationRequest::getActorEntraOid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, EntraUser> actorsByEntraOid = new HashMap<>();
        if (!actorEntraOids.isEmpty()) {
            entraUserRepository.findByEntraOidIn(actorEntraOids)
                    .forEach(actor -> actorsByEntraOid.put(actor.getEntraOid(), actor));
        }

        Set<UUID> requestIds = latestRequests.stream()
                .map(UserActivationRequest::getRequestId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, Instant> submittedAtByRequestId = userActivationRequestRepository
                .findAllFirstVersionsByRequestIdIn(requestIds).stream()
                .collect(Collectors.toMap(UserActivationRequest::getRequestId, UserActivationRequest::getCreatedAt));

        List<ReactivationRequestListItem> items = latestRequests.stream()
                .filter(request -> isVisibleToViewer(request, profilesById.get(request.getUserProfileId()), currentUser, pageMode))
                .map(request -> toListItem(request, profilesById.get(request.getUserProfileId()),
                        actorsByEntraOid.get(request.getActorEntraOid()),
                        submittedAtByRequestId.get(request.getRequestId())))
                .toList();

        if (pageMode == ReactivationRequestPageMode.TRACK && currentUser != null) {
            Set<UUID> allowedFirmIds = firmService.getUserActiveAllFirms(currentUser).stream()
                    .map(FirmDto::getId)
                    .collect(Collectors.toSet());

            if (allowedFirmIds.isEmpty()) {
                log.debug("User {} has no active firms; returning empty request list in TRACK mode", currentUser.getId());
                return List.of();
            }

            return items.stream()
                    .filter(item -> item.firmId() != null && allowedFirmIds.contains(item.firmId()))
                    .toList();
        }

        return items;
    }

    private boolean isVisibleToViewer(UserActivationRequest request, UserProfile profile,
                                      EntraUser currentUser, ReactivationRequestPageMode pageMode) {
        if (currentUser == null || profile == null || profile.getEntraUser() == null
                || !profile.getEntraUser().isMultiFirmUser()) {
            return true;
        }

        boolean isProviderAdmin = AccessControlService.userHasAuthzRole(currentUser, AuthzRole.FIRM_USER_MANAGER.getRoleName());
        if (pageMode == ReactivationRequestPageMode.TRACK || isProviderAdmin) {
            return false;
        }

        boolean isLaaViewer = AccessControlService.userHasAuthzRole(currentUser, AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())
                || AccessControlService.userHasAuthzRole(currentUser, AuthzRole.SECURITY_RESPONSE.getRoleName())
                || AccessControlService.userHasAuthzRole(currentUser, AuthzRole.GLOBAL_ADMIN.getRoleName());
        boolean isRequestingEum = AccessControlService.userHasAuthzRole(currentUser, AuthzRole.EXTERNAL_USER_MANAGER.getRoleName())
                && request.getActorRoleType() == ReactivationRoleType.LAA_OST
                && Objects.equals(currentUser.getEntraOid(), request.getActorEntraOid());

        return isLaaViewer || isRequestingEum;
    }

    private ReactivationRequestListItem toListItem(UserActivationRequest request, UserProfile profile, EntraUser actor,
                                                   Instant submittedAt) {
        UUID firmId = profile != null && profile.getFirm() != null ? profile.getFirm().getId() : null;
        String actorName = actor != null
                ? (nullToEmpty(actor.getFirstName()) + " " + nullToEmpty(actor.getLastName())).trim()
                : UNKNOWN_USER_NAME;
        String actorEmail = actor != null ? actor.getEmail() : null;
        EntraUser targetUser = profile != null ? profile.getEntraUser() : null;
        String userName = targetUser != null
                ? (nullToEmpty(targetUser.getFirstName()) + " " + nullToEmpty(targetUser.getLastName())).trim()
                : UNKNOWN_USER_NAME;
        String userEmail = targetUser != null ? targetUser.getEmail() : null;
        String actorRoleType = request.getActorRoleType() != null ? request.getActorRoleType().getDisplayName() : null;
        ReactivationRequestStatus status = ReactivationRequestStatus.valueOf(request.getStatus().name());
        // dateSubmitted reflects when the request was originally raised (version 1),
        // while lastActivity reflects the most recent version's timestamp (this row).
        Instant originalSubmission = submittedAt != null ? submittedAt : request.getCreatedAt();
        LocalDate dateSubmitted = originalSubmission != null
                ? LocalDate.ofInstant(originalSubmission, ZoneId.systemDefault())
                : null;
        LocalDate lastActivity = request.getCreatedAt() != null
                ? LocalDate.ofInstant(request.getCreatedAt(), ZoneId.systemDefault())
                : null;

        return new ReactivationRequestListItem(
                request.getId(),
                request.getRequestId(),
                request.getUserProfileId(),
                targetUser != null ? targetUser.getId() : null,
                request.getVersion(),
                status,
                request.getComments(),
                request.getActorEntraOid(),
                actorRoleType,
                actorName.isBlank() ? UNKNOWN_USER_NAME : actorName,
                actorEmail,
                userName.isBlank() ? UNKNOWN_USER_NAME : userName,
                userEmail,
                dateSubmitted,
                lastActivity,
                firmId);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<ReactivationRequestListItem> filterAndSortRequests(
            List<ReactivationRequestListItem> requests,
            String search,
            List<ReactivationRequestStatus> selectedStatuses,
            List<ReactivationRoleType> selectedActorRoleTypes,
            String sort,
            String direction) {

        Set<ReactivationRequestStatus> statusFilter = selectedStatuses == null
                ? Set.of()
                : new HashSet<>(selectedStatuses);
        Set<String> actorRoleTypeLabelFilter = selectedActorRoleTypes == null || selectedActorRoleTypes.isEmpty()
                ? Set.of()
                : selectedActorRoleTypes.stream().map(ReactivationRoleType::getDisplayName).collect(Collectors.toSet());

        Comparator<ReactivationRequestListItem> comparator = resolveComparator(sort);
        if (!"asc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }

        return requests.stream()
                .filter(item -> search.isBlank() || matchesSearch(item, search))
                .filter(item -> statusFilter.isEmpty() || statusFilter.contains(item.requestStatus()))
                .filter(item -> actorRoleTypeLabelFilter.isEmpty() || actorRoleTypeLabelFilter.contains(item.actorRoleType()))
                .sorted(comparator)
                .toList();
    }

    private boolean matchesSearch(ReactivationRequestListItem item, String search) {
        return containsIgnoreCase(item.requestId(), search)
                || containsIgnoreCase(item.userProfileId(), search)
                || containsIgnoreCase(item.userName(), search)
                || containsIgnoreCase(item.userEmail(), search)
                || containsIgnoreCase(item.actorName(), search)
                || containsIgnoreCase(item.actorEntraOid(), search)
                || containsIgnoreCase(item.comments(), search);
    }

    private boolean containsIgnoreCase(Object value, String search) {
        return value != null && value.toString().toLowerCase(Locale.UK).contains(search);
    }

    private PaginatedReactivationRequests paginate(List<ReactivationRequestListItem> requests, int page, int requestedSize) {
        int pageSize = requestedSize > 0 ? requestedSize : DEFAULT_PAGE_SIZE;
        int safePage = Math.max(1, page);

        int totalItems = requests.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
        int boundedPage = totalPages == 0 ? 0 : Math.min(safePage, totalPages);
        int startIndex = boundedPage == 0 ? 0 : (boundedPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);

        List<ReactivationRequestListItem> pageItems = startIndex >= totalItems
                ? List.of()
                : requests.subList(startIndex, endIndex);

        PaginatedReactivationRequests paginated = new PaginatedReactivationRequests();
        paginated.setRequests(pageItems);
        paginated.setTotalRequests(totalItems);
        paginated.setTotalPages(totalPages);
        paginated.setCurrentPage(boundedPage);
        paginated.setPageSize(pageSize);
        return paginated;
    }

    private Comparator<ReactivationRequestListItem> resolveComparator(String sort) {
        if (sort == null) {
            return Comparator.comparing(ReactivationRequestListItem::dateSubmitted, Comparator.nullsLast(LocalDate::compareTo));
        }

        return switch (sort) {
            case "requestId" -> Comparator.comparing(item -> item.requestId().toString(), String.CASE_INSENSITIVE_ORDER);
            case "userProfileId" -> Comparator.comparing(item -> item.userProfileId().toString(), String.CASE_INSENSITIVE_ORDER);
            case "version" -> Comparator.comparing(ReactivationRequestListItem::version, Comparator.nullsLast(Integer::compareTo));
            case "requestStatus" -> Comparator.comparing(item -> item.requestStatus().name(), String.CASE_INSENSITIVE_ORDER);
            case "actorName" -> Comparator.comparing(ReactivationRequestListItem::actorName, String.CASE_INSENSITIVE_ORDER);
            case "actorRoleType" -> Comparator.comparing(ReactivationRequestListItem::actorRoleType,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "lastActivity" -> Comparator.comparing(ReactivationRequestListItem::lastActivity,
                    Comparator.nullsLast(LocalDate::compareTo));
            case "userName" -> Comparator.comparing(ReactivationRequestListItem::userName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "userEmail" -> Comparator.comparing(ReactivationRequestListItem::userEmail,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> Comparator.comparing(ReactivationRequestListItem::dateSubmitted,
                    Comparator.nullsLast(LocalDate::compareTo));
        };
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.UK);
    }

    private UUID parseUuid(String uuidStr) {
        try {
            return uuidStr == null ? null : UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
