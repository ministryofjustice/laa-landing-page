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

import org.apache.commons.lang3.StringUtils;
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

@Service
@Transactional
@Slf4j
public class UserReactivationRequestService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String UNKNOWN_USER_NAME = "Unknown user";
    private static final String USER_TYPE_PROVIDER_USER = "Provider User";
    private static final String USER_TYPE_PROVIDER_ADMIN = "Provider Admin";
    private static final String USER_TYPE_THIRD_PARTY = "3rd Party";

    private final LoginService loginService;
    private final FirmService firmService;
    private final UserActivationRequestRepository userActivationRequestRepository;
    private final EntraUserRepository entraUserRepository;
    private final ReactivationTypeResolver roleTypeResolver;
    private final NotificationService notificationService;

    public UserReactivationRequestService(LoginService loginService,
                                          FirmService firmService,
                                          UserActivationRequestRepository userActivationRequestRepository,
                                          EntraUserRepository entraUserRepository,
                                          ReactivationTypeResolver roleTypeResolver,
                                          NotificationService notificationService) {
        this.loginService = loginService;
        this.firmService = firmService;
        this.userActivationRequestRepository = userActivationRequestRepository;
        this.entraUserRepository = entraUserRepository;
        this.roleTypeResolver = roleTypeResolver;
        this.notificationService = notificationService;
    }

    public Optional<UserActivationRequest> findFirstByUserEntraIdOrderByCreatedAtDescVersionDesc(String userId) {
        log.debug("Fetching latest activation request for User ID: {}", userId);
        return userActivationRequestRepository.findFirstByUserEntraIdOrderByCreatedAtDescVersionDesc(parseUuid(userId));
    }

    public Optional<UserActivationRequest> findFirstByUserEntraIdAndRequestIdOrderByVersionDesc(String userId, String requestId) {
        log.debug("Fetching latest activation request for User ID: {}, Request ID: {}", userId, requestId);
        return userActivationRequestRepository.findFirstByUserEntraIdAndRequestIdOrderByVersionDesc(parseUuid(userId), parseUuid(requestId));
    }

    public UserActivationRequest createReactivationRequest(String userId, String profileId, String comments, String actorEntraOid) {
        log.info("A new user activation request is being submitted for user {} by User Entra Oid: {}",
                userId, actorEntraOid);

        userActivationRequestRepository.findFirstByUserEntraIdOrderByCreatedAtDescVersionDesc(parseUuid(userId))
                .ifPresent(existing -> {
                    ReactivationRequestStatus status = existing.getStatus();
                    if (status != ReactivationRequestStatus.REJECTED && status != ReactivationRequestStatus.APPROVED) {
                        log.error("Request already being processed for profile ID: {}", userId);
                        throw new IllegalStateException("Request already being processed for user " + userId);
                    }
                });

        EntraUser actor = entraUserRepository.findByEntraOid(actorEntraOid).orElseThrow();
        ReactivationRoleType actorRoleType = roleTypeResolver.resolve(actor);

        EntraUser providerUser = entraUserRepository.findById(UUID.fromString(userId)).orElseThrow();

        final UserActivationRequest newRequest = createReactivationRequestEntry(null, userId, profileId,
                ReactivationRequestStatus.IN_REVIEW, comments, actorEntraOid, actorRoleType);

        if (ReactivationRoleType.PROVIDER_ADMIN.equals(actorRoleType)) {
            log.debug("Sending submission notification to Provider Admin: {}", actor.getEmail());
            notificationService.notifyReactivationRequestSubmitted(
                    actor.getId().toString(), actor.getFirstName(), actor.getEmail(),
                    actor.getId().toString(), userId, providerUser.getEmail()
            );
        }

        log.debug("Sending submission notification to target user: {}", providerUser.getEmail());
        notificationService.notifyReactivationRequestSubmitted(
                actor.getId().toString(), providerUser.getFirstName(), providerUser.getEmail(), providerUser.getId().toString(), userId, providerUser.getEmail()
        );

        log.info("Successfully created reactivation request ID: {} (Version 1) for profile ID: {}",
                newRequest.getRequestId(), userId);
        return newRequest;
    }

    public UserActivationRequest approveReactivationRequest(String requestId, String userEntraId, String userProfileId, String actorEntraOid) {
        log.info("Approving reactivation request ID: {} for user ID: {} by actor OID: {}", requestId, userEntraId, actorEntraOid);
        return processReactivationState(requestId, userEntraId, userProfileId, "Approved", actorEntraOid, ReactivationRequestStatus.APPROVED, true);
    }

    public UserActivationRequest rejectReactivationRequest(String requestId, String userEntraId, String userProfileId, String comments, String actorEntraOid) {
        log.info("Rejecting reactivation request ID: {} for user ID: {} by actor OID: {}", requestId, userEntraId, actorEntraOid);
        return processReactivationState(requestId, userEntraId, userProfileId, comments, actorEntraOid, ReactivationRequestStatus.REJECTED, false);
    }

    public UserActivationRequest updateReactivateRequestState(String requestId, String userEntraId, String userProfileId, String comments, String actorEntraOid) {
        log.info("Updating reactivation request ID: {} for user ID: {} by actor OID: {}", requestId, userEntraId, actorEntraOid);

        EntraUser entraUser = entraUserRepository.findByEntraOid(actorEntraOid).orElseThrow();
        ReactivationRoleType roleType = roleTypeResolver.resolve(entraUser);

        validateActiveReactivationRequestPresent(requestId);

        ReactivationRequestStatus reactivationRequestStatus = calculateNextReactivationRequestStatus(entraUser);
        log.debug("Calculated next status: {} for request ID: {}", reactivationRequestStatus, requestId);

        UserActivationRequest result = createReactivationRequestEntry(requestId, userEntraId, userProfileId, reactivationRequestStatus, comments, actorEntraOid, roleType);

        if (ReactivationRequestStatus.INFORMATION_REQUIRED.equals(reactivationRequestStatus)) {
            UserActivationRequest initialRequest = userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(parseUuid(requestId)).orElseThrow();
            if (initialRequest.getActorRoleType() == ReactivationRoleType.PROVIDER_ADMIN) {
                EntraUser providerAdmin = entraUserRepository.findByEntraOid(initialRequest.getActorEntraOid()).orElseThrow();
                EntraUser providerUser = entraUserRepository.findById(parseUuid(userEntraId)).orElseThrow();
                log.debug("Notifying provider admin {} that information is required for request ID: {}", providerAdmin.getEmail(), requestId);
                notificationService.notifyReactivationRequestInfoRequested(
                        entraUser.getId().toString(), providerAdmin.getFirstName(), providerAdmin.getEmail(), providerAdmin.getId().toString(),
                        userEntraId, providerUser.getEmail()
                );
            }
        }

        log.info("Successfully updated reactivation request ID: {} to status: {} (Version {})", requestId, reactivationRequestStatus, result.getVersion());
        return result;
    }

    private UserActivationRequest processReactivationState(String requestId, String userId, String userProfileId, String comments,
                                                           String actorEntraOid, ReactivationRequestStatus status, boolean isApproved) {
        String actor;
        ReactivationRoleType roleType;
        if ("SYNC".equalsIgnoreCase(actorEntraOid)) {
            actor = "SYSTEM";
            roleType = ReactivationRoleType.SYNC;
        } else {
            EntraUser entraUser = entraUserRepository.findByEntraOid(actorEntraOid).orElseThrow();
            actor = String.valueOf(entraUser.getId());
            roleType = roleTypeResolver.resolve(entraUser);
        }

        UserActivationRequest initialRequest = userActivationRequestRepository.findFirstByRequestIdOrderByVersionAsc(parseUuid(requestId)).orElseThrow();

        final UserActivationRequest result = createReactivationRequestEntry(requestId, userId, userProfileId, status, comments, actorEntraOid, roleType);

        EntraUser providerUser = entraUserRepository.findById(parseUuid(userId)).orElseThrow();

        if (initialRequest.getActorRoleType() == ReactivationRoleType.PROVIDER_ADMIN) {
            EntraUser providerAdmin = entraUserRepository.findByEntraOid(initialRequest.getActorEntraOid()).orElseThrow();
            if (isApproved) {
                log.debug("Notifying Provider Admin {} of approval for request ID: {}", providerAdmin.getEmail(), requestId);
                notificationService.notifyReactivationRequestApproved(actor, providerAdmin.getFirstName(),
                        providerAdmin.getEmail(), providerAdmin.getId().toString(), userId, providerUser.getEmail());
            } else {
                log.debug("Notifying Provider Admin {} of rejection for request ID: {}", providerAdmin.getEmail(), requestId);
                notificationService.notifyReactivationRequestRejected(actor, providerAdmin.getFirstName(),
                        providerAdmin.getEmail(), providerAdmin.getId().toString(), userId, providerUser.getEmail());
            }
        }

        if (isApproved) {
            log.debug("Notifying target user {} of approval for request ID: {}", providerUser.getEmail(), requestId);
            notificationService.notifyReactivationRequestApproved(actor, providerUser.getFirstName(),
                    providerUser.getEmail(), providerUser.getId().toString(), providerUser.getId().toString(), providerUser.getEmail());
        } else {
            log.debug("Notifying target user {} of rejection for request ID: {}", providerUser.getEmail(), requestId);
            notificationService.notifyReactivationRequestRejected(actor, providerUser.getFirstName(),
                    providerUser.getEmail(), providerUser.getId().toString(), providerUser.getId().toString(), providerUser.getEmail());
        }

        log.info("Completed processing state change for request ID: {} to status: {}", requestId, status);
        return result;
    }

    private UserActivationRequest createReactivationRequestEntry(String requestId, String userId, String profileId,
                                                                 ReactivationRequestStatus status, String comments,
                                                                 String actorEntraOid, ReactivationRoleType roleType) {

        if (!isUserIdsValid(userId, profileId)) {
            log.error("Failed to create request entry. User id not found for ID: {}", userId);
            throw new EntityNotFoundException("Target user not found with ID: " + userId);
        }

        int nextVersion = 1;
        UUID activeRequestId = requestId != null ? parseUuid(requestId) : UUID.randomUUID();

        if (requestId != null) {
            UserActivationRequest firstByRequestIdOrderByVersionDesc = userActivationRequestRepository
                    .findFirstByRequestIdOrderByVersionDesc(parseUuid(requestId)).orElseThrow();
            nextVersion = firstByRequestIdOrderByVersionDesc.getVersion() + 1;
        }

        log.debug("Creating reactivation request entry. Request ID: {}, User ID: {}, Version: {}, Status: {}",
                activeRequestId, userId, nextVersion, status);

        UserActivationRequest newRecord = new UserActivationRequest();
        newRecord.setRequestId(activeRequestId);
        newRecord.setUserProfileId(safeParseUuid(profileId));
        newRecord.setUserEntraId(parseUuid(userId));
        newRecord.setVersion(nextVersion);
        newRecord.setStatus(status);
        newRecord.setComments(comments);
        newRecord.setActorEntraOid(actorEntraOid);
        newRecord.setActorRoleType(roleType);
        newRecord.setCreatedAt(Instant.now());

        return userActivationRequestRepository.save(newRecord);
    }

    private boolean isUserIdsValid(String userId, String profileId) {
        if (userId == null) {
            return false;
        }

        EntraUser user = entraUserRepository.findById(parseUuid(userId)).orElse(null);
        if (user == null) {
            return false;
        }

        if (user.isMultiFirmUser() && StringUtils.isEmpty(profileId)) {
            return true;
        }

        return user.getUserProfiles().stream()
                .anyMatch(profile -> profile != null && profile.getId().equals(parseUuid(profileId)));
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
    public List<UserActivationRequestSummaryDto> getRequestHistoryForUserIdAndRequestId(String userId, String requestId) {
        log.debug("Fetching activation request history for user ID: {} and request ID: {}", userId, requestId);

        if (StringUtils.isBlank(requestId)) {
            log.info("No activation request found for user ID: {}", userId);
            return Collections.emptyList();
        }

        List<UserActivationRequestSummaryDto> history = userActivationRequestRepository.findRequestHistoryByRequestId(parseUuid(requestId));

        if (history.isEmpty()) {
            log.info("No activation request history found for user ID: {}, request ID: {}", userId, requestId);
            return Collections.emptyList();
        }

        log.debug("Found {} history records for user ID: {}, request ID: {}", history.size(), userId, requestId);
        return history;
    }

    public ReactivationRequestStatus calculateNextReactivationRequestStatus(EntraUser entraUser) {
        ReactivationRoleType roleType = roleTypeResolver.resolve(entraUser);
        log.debug("Resolving next status for user {} with role type: {}", entraUser.getId(), roleType);

        return ReactivationRoleType.PROVIDER_ADMIN.equals(roleType)
                || ReactivationRoleType.LAA_SUPPORT.equals(roleType)
                || ReactivationRoleType.LAA_OST.equals(roleType)
                ? ReactivationRequestStatus.IN_REVIEW : ReactivationRequestStatus.INFORMATION_REQUIRED;
    }

    @Transactional(readOnly = true)
    public ReactivationRequestsPageData getPage(
            Authentication authentication,
            String search,
            List<ReactivationRequestStatus> selectedStatuses,
            boolean showFirmAdmins,
            boolean showMultiFirmUsers,
            boolean showProviderUsers,
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

        List<ReactivationRequestListItem> requests = filterAndSortRequests(
                buildRequests(currentUser, pageMode),
                normalizeSearch(search),
                effectiveStatuses,
                showFirmAdmins,
                showMultiFirmUsers,
                showProviderUsers,
                sort,
                direction);

        PaginatedReactivationRequests paginated = paginate(requests, page, size);
        log.debug("Returning {} filtered items across {} pages for mode: {}",
                paginated.getTotalRequests(), paginated.getTotalPages(), pageMode);

        return new ReactivationRequestsPageData(pageMode, effectiveStatuses, showFirmAdmins,
            showMultiFirmUsers, showProviderUsers, paginated);
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
            || AccessControlService.userHasAuthzRole(currentUser, AuthzRole.EXTERNAL_USER_SUPPORT.getRoleName())
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

        Set<UUID> userIds = latestRequests.stream()
                .map(UserActivationRequest::getUserEntraId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, EntraUser> userByIds = entraUserRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(EntraUser::getId, user -> user));

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
        List<UserActivationRequest> firstVersions = userActivationRequestRepository
            .findAllFirstVersionsByRequestIdIn(requestIds);
        Map<UUID, Instant> submittedAtByRequestId = firstVersions.stream()
                .collect(Collectors.toMap(UserActivationRequest::getRequestId, UserActivationRequest::getCreatedAt));
        Map<UUID, ReactivationRoleType> submittedByRoleByRequestId = firstVersions.stream()
            .filter(firstVersion -> firstVersion.getActorRoleType() != null)
            .collect(Collectors.toMap(UserActivationRequest::getRequestId, UserActivationRequest::getActorRoleType));

        boolean isGlobalAdminOrSecurityResponse = AccessControlService.userHasAuthzRole(currentUser, AuthzRole.SECURITY_RESPONSE.getRoleName())
                || AccessControlService.userHasAuthzRole(currentUser, AuthzRole.GLOBAL_ADMIN.getRoleName());
        boolean isExternalUserAdmin = AccessControlService.userHasAuthzRole(currentUser, AuthzRole.EXTERNAL_USER_ADMIN.getRoleName());
        boolean isExternalUserManager = AccessControlService.userHasAuthzRole(currentUser, AuthzRole.EXTERNAL_USER_MANAGER.getRoleName());
        boolean isExternalUserSupport = AccessControlService.userHasAuthzRole(currentUser, AuthzRole.EXTERNAL_USER_SUPPORT.getRoleName());
        boolean isProviderAdmin = AccessControlService.userHasAuthzRole(currentUser, AuthzRole.FIRM_USER_MANAGER.getRoleName());
        Set<UUID> viewerFirmIds = isProviderAdmin && currentUser != null
                ? firmService.getUserActiveAllFirms(currentUser).stream()
                .map(FirmDto::getId)
                .collect(Collectors.toSet())
                : Set.of();

        List<ReactivationRequestListItem> items = latestRequests.stream()
                .filter(request -> isVisibleToViewer(request, userByIds.get(request.getUserEntraId()), currentUser,
                        isGlobalAdminOrSecurityResponse, isExternalUserAdmin, isExternalUserManager,
                    isExternalUserSupport, isProviderAdmin, viewerFirmIds, submittedByRoleByRequestId.get(request.getRequestId())))
                .map(request -> toListItem(request, userByIds.get(request.getUserEntraId()),
                        actorsByEntraOid.get(request.getActorEntraOid()),
                        submittedAtByRequestId.get(request.getRequestId())))
                .toList();

        return items;
    }

    private boolean isVisibleToViewer(UserActivationRequest request, EntraUser targetUser,
                                      EntraUser currentUser, boolean isGlobalAdminOrSecurityResponse,
                                      boolean isExternalUserAdmin, boolean isExternalUserManager,
                                      boolean isExternalUserSupport, boolean isProviderAdmin,
                                      Set<UUID> viewerFirmIds, ReactivationRoleType submittedByRole) {
        if (isGlobalAdminOrSecurityResponse) {
            return true;
        }

        if (isExternalUserAdmin) {
            return true;
        }

        if (targetUser == null) {
            return false;
        }

        if (isExternalUserManager || isExternalUserSupport) {
            return submittedByRole == ReactivationRoleType.LAA_OST
                    || submittedByRole == ReactivationRoleType.LAA_SUPPORT;
        }

        if (isProviderAdmin) {
            boolean isMultiFirmTarget = targetUser.isMultiFirmUser();
            if (isMultiFirmTarget) {
                return false;
            }
            UserProfile activeProfile = activeUserProfile(targetUser);
            UUID targetFirmId = activeProfile != null && activeProfile.getFirm() != null ? activeProfile.getFirm().getId() : null;
            return targetFirmId != null && viewerFirmIds.contains(targetFirmId);
        }

        return false;
    }

    private ReactivationRequestListItem toListItem(UserActivationRequest request, EntraUser entraUser, EntraUser actor,
                                                   Instant submittedAt) {
        UserProfile targetUserProfile = activeUserProfile(entraUser);
        UUID firmId = targetUserProfile != null && targetUserProfile.getFirm() != null ? targetUserProfile.getFirm().getId() : null;
        String actorName = actor != null
                ? (nullToEmpty(actor.getFirstName()) + " " + nullToEmpty(actor.getLastName())).trim()
                : UNKNOWN_USER_NAME;
        String actorEmail = actor != null ? actor.getEmail() : null;
        String userName = entraUser != null
                ? (nullToEmpty(entraUser.getFirstName()) + " " + nullToEmpty(entraUser.getLastName())).trim()
                : UNKNOWN_USER_NAME;
        String userEmail = entraUser != null ? entraUser.getEmail() : null;
        String userType = determineTargetUserType(targetUserProfile, entraUser);
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
                request.getUserEntraId(),
                targetUserProfile != null ? targetUserProfile.getId() : null,
                request.getVersion(),
                status,
                request.getComments(),
                request.getActorEntraOid(),
                actorRoleType,
                actorName.isBlank() ? UNKNOWN_USER_NAME : actorName,
                actorEmail,
                userName.isBlank() ? UNKNOWN_USER_NAME : userName,
                userEmail,
                userType,
                dateSubmitted,
                lastActivity,
                firmId);
    }

    private UserProfile activeUserProfile(EntraUser entraUser) {
        return Optional.ofNullable(entraUser)
                .map(EntraUser::getUserProfiles)
                .orElse(Set.of())
                .stream()
                .filter(profile -> profile != null && profile.isActiveProfile())
                .findFirst()
                .orElse(null);
    }

    private String determineTargetUserType(UserProfile profile, EntraUser targetUser) {
        if (targetUser != null && targetUser.isMultiFirmUser()) {
            return USER_TYPE_THIRD_PARTY;
        }

        if (profile != null && isProviderAdminProfile(profile)) {
            return USER_TYPE_PROVIDER_ADMIN;
        }

        return USER_TYPE_PROVIDER_USER;
    }

    private boolean isProviderAdminProfile(UserProfile profile) {
        return Optional.ofNullable(profile.getAppRoles()).orElse(Set.of()).stream()
                .anyMatch(appRole -> appRole.isAuthzRole()
                        && (AuthzRole.EXTERNAL_USER_MANAGER.getRoleName().equals(appRole.getName())
                        || AuthzRole.FIRM_USER_MANAGER.getRoleName().equals(appRole.getName())));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<ReactivationRequestListItem> filterAndSortRequests(
            List<ReactivationRequestListItem> requests,
            String search,
            List<ReactivationRequestStatus> selectedStatuses,
            boolean showFirmAdmins,
            boolean showMultiFirmUsers,
            boolean showProviderUsers,
            String sort,
            String direction) {

        Set<ReactivationRequestStatus> statusFilter = selectedStatuses == null
                ? Set.of()
                : new HashSet<>(selectedStatuses);
        boolean filterByUserType = showFirmAdmins || showMultiFirmUsers || showProviderUsers;

        Comparator<ReactivationRequestListItem> comparator = resolveComparator(sort);
        if (!"asc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }

        return requests.stream()
                .filter(item -> search.isBlank() || matchesSearch(item, search))
                .filter(item -> statusFilter.isEmpty() || statusFilter.contains(item.requestStatus()))
                .filter(item -> !filterByUserType || matchesUserTypeFilter(item,
                        showFirmAdmins, showMultiFirmUsers, showProviderUsers))
                .sorted(comparator)
                .toList();
    }

    private boolean matchesUserTypeFilter(ReactivationRequestListItem item,
                                          boolean showFirmAdmins,
                                          boolean showMultiFirmUsers,
                                          boolean showProviderUsers) {
        return (showFirmAdmins && USER_TYPE_PROVIDER_ADMIN.equals(item.userType()))
                || (showMultiFirmUsers && USER_TYPE_THIRD_PARTY.equals(item.userType()))
                || (showProviderUsers && USER_TYPE_PROVIDER_USER.equals(item.userType()));
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
            case "userProfileId" -> Comparator.comparing(item -> item.userId().toString(), String.CASE_INSENSITIVE_ORDER);
            case "version" -> Comparator.comparing(ReactivationRequestListItem::version, Comparator.nullsLast(Integer::compareTo));
            case "requestStatus" -> Comparator.comparing(item -> item.requestStatus().name(), String.CASE_INSENSITIVE_ORDER);
            case "actorName" -> Comparator.comparing(ReactivationRequestListItem::actorName, String.CASE_INSENSITIVE_ORDER);
            case "actorRoleType" -> Comparator.comparing(ReactivationRequestListItem::actorRoleType,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "userType" -> Comparator.comparing(item -> item.userType(),
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
            return UUID.fromString(uuidStr);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid UUID format: " + uuidStr, ex);
        }
    }

    private UUID safeParseUuid(String uuidStr) {
        try {
            return StringUtils.isEmpty(uuidStr) ? null : UUID.fromString(uuidStr);
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean hasOpenReactivationRequest(UUID id) {
        EntraUser entraUser = entraUserRepository.findById(id).orElseThrow();
        List<UUID> userProfileIds = entraUser.getUserProfiles().stream().map(UserProfile::getId).toList();
        List<UserActivationRequest> requestsByUserProfileIds = userActivationRequestRepository.findTopForEachUserProfileId(userProfileIds);
        return requestsByUserProfileIds.stream()
                .anyMatch(request -> (request.getStatus() == ReactivationRequestStatus.IN_REVIEW
                        || request.getStatus() == ReactivationRequestStatus.INFORMATION_REQUIRED));
    }
}
