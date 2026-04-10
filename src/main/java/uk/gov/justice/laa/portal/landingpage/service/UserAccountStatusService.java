package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.BulkDisableUserAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.DisableUserReasonDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.CountFirms;
import uk.gov.justice.laa.portal.landingpage.entity.DisableType;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;
import uk.gov.justice.laa.portal.landingpage.entity.DisableUserReason;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserTypeReasonDisable;
import uk.gov.justice.laa.portal.landingpage.exception.TechServicesClientException;
import uk.gov.justice.laa.portal.landingpage.repository.UserAccountStatusAuditRepository;
import uk.gov.justice.laa.portal.landingpage.repository.DisableUserReasonRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.ChangeAccountEnabledResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccountStatusService {

    private final DisableUserReasonRepository disableUserReasonRepository;
    private final UserAccountStatusAuditRepository userAccountStatusAuditRepository;
    private final ModelMapper mapper;
    private final EntraUserRepository entraUserRepository;
    private final TechServicesClient techServicesClient;
    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final EventService eventService;
    private final DisableTypeResolver disableTypeResolver;
    private final UserEnablementPolicy userEnablementPolicy;

    public List<DisableUserReasonDto> getDisableUserReasons(UserTypeReasonDisable userTypeReasonDisable) {
        List<DisableUserReason> reasons = disableUserReasonRepository.findAll();
        List<DisableUserReasonDto> disableUserReasonDtos = new java.util.ArrayList<>(reasons.stream()
                .filter(DisableUserReason::isUserSelectable)
                .map(reason -> mapper.map(reason, DisableUserReasonDto.class))
                .toList());

        if (userTypeReasonDisable.equals(UserTypeReasonDisable.IS_USER_DISABLE)) {
            Set<String> keepReasons = Set.of("Absence", "Provider Discretion");
            disableUserReasonDtos.removeIf(u -> !keepReasons.contains(u.getName()));
        } else if (userTypeReasonDisable.equals(UserTypeReasonDisable.BULK_DISABLE)) {
            Set<String> keepReasonsBulk = Set.of(
                    "Compliance Breach",
                    "Contract Ended",
                    "Cyber Risk",
                    "Firm Closure / Merger",
                    "Investigation Pending",
                    "User Request"
            );
            disableUserReasonDtos.removeIf(u -> !keepReasonsBulk.contains(u.getName()));
        }

        return disableUserReasonDtos;
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void disableUser(UUID disabledUserId, UUID disableReasonId, UUID disabledById) {
        if (disabledUserId.equals(disabledById)) {
            throw new RuntimeException(String.format("User %s can not be disabled by themselves", disabledUserId));
        }

        // Fetch entities
        EntraUser disabledUser = entraUserRepository.findByIdWithAssociations(disabledUserId)
                .orElseThrow(() -> new RuntimeException(String.format("Could not find a user account to disable with id \"%s\"", disabledUserId)));
        EntraUser disabledByUser = entraUserRepository.findByIdWithAssociations(disabledById)
                .orElseThrow(() -> new RuntimeException(String.format("Could not find a user account with id \"%s\"", disabledById)));
        DisableUserReason reason = disableUserReasonRepository.findById(disableReasonId)
                .orElseThrow(() -> new RuntimeException(String.format("Could not find a disable user reason with id \"%s\"", disableReasonId)));

        boolean isDisabledByAnInternalUser = userService.isInternal(disabledById);
        if (!isDisabledByAnInternalUser && disabledUser.isMultiFirmUser()) {
            throw new RuntimeException(String.format("Multi firm user %s can not be disabled", disabledUserId));
        }

        Firm disabledByUserFirm = disabledByUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .findFirst()
                .map(UserProfile::getFirm)
                .orElse(null);
        Firm disabledUserFirm = disabledUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .findFirst()
                .map(UserProfile::getFirm).orElse(null);

        if (userService.isInternal(disabledById)
                || (disabledUserFirm != null && disabledByUserFirm != null && disabledByUserFirm.getId().equals(disabledUserFirm.getId()))) {
            // Disable user in Entra via tech services.
            TechServicesApiResponse<ChangeAccountEnabledResponse> changeAccountEnabledResponse
                    = techServicesClient.disableUser(mapper.map(disabledUser, EntraUserDto.class), reason.getEntraDescription());
            if (!changeAccountEnabledResponse.isSuccess()) {
                throw new TechServicesClientException(changeAccountEnabledResponse.getError().getMessage(),
                        changeAccountEnabledResponse.getError().getCode(),
                        changeAccountEnabledResponse.getError().getErrors());
            }

            // Determine and store the disable type from the actor's roles
            DisableType disableType = disableTypeResolver.resolve(disabledByUser);

            // Perform disable
            disabledUser.setDisabledBy(disabledById);
            disabledUser.setEnabled(false);
            disabledUser.setDisableType(disableType);
            entraUserRepository.saveAndFlush(disabledUser);

            // Add audit entry
            UserAccountStatusAudit userAccountStatusAudit = UserAccountStatusAudit.builder()
                    .entraUser(disabledUser)
                    .disableUserReason(reason)
                    .statusChange(UserAccountStatus.DISABLED)
                    .statusChangedBy(disabledByUser.getFirstName() + " " + disabledByUser.getLastName())
                    .statusChangedDate(LocalDateTime.now())
                    .disableType(disableType)
                    .build();
            userAccountStatusAuditRepository.saveAndFlush(userAccountStatusAudit);
        } else {
            throw new RuntimeException(String.format("Unable to disable the user %s by %s", disabledUserId, disabledById));
        }
    }

    public Map<String, Long> getUserCountsForFirm(String firmId) {
        Map<String, Long> result = new HashMap<>();
        List<CountFirms> countFirmsList = userProfileRepository.countFirmsById(UUID.fromString(firmId));

        for (CountFirms count : countFirmsList) {
            if (count.getIsMultifirm()) {
                result.put("totalOfMultiFirm", count.getUserCount());
            } else {
                result.put("totalOfSingleFirm", count.getUserCount());
            }
        }

        return result;

    }

    public boolean hasActiveUserByFirmId(String firmId) {
        return userProfileRepository.hasActiveUserByFirmId(UUID.fromString(firmId));
    }

    public void disableUserAllUserByFirmIdWithCyberRisk(String firmId, UUID disabledById) {
        log.info("Started Bulk disable users with cyber risk reason");
        DisableUserReason reason = disableUserReasonRepository.findByName("Cyber Risk")
                .orElseThrow(() -> new RuntimeException("Could not find 'Cyber Risk' disable user reason"));

        disableUserAllUserByFirmId(firmId, reason.getId(), disabledById);
    }

    public void disableUserAllUserByFirmId(String firmId, UUID disableReasonId, UUID disabledById) {
        log.info("Started Bulk disable users");
        // Fetch entities
        EntraUser disabledByUser = entraUserRepository.findByIdWithAssociations(disabledById)
                .orElseThrow(() -> new RuntimeException(String.format("Could not find a user account with id \"%s\"", disabledById)));
        DisableUserReason reason = disableUserReasonRepository.findById(disableReasonId)
                .orElseThrow(() -> new RuntimeException(String.format("Could not find a disable user reason with id \"%s\"", disableReasonId)));

        List<EntraUser> entraUsers = userProfileRepository.findByFirmId(UUID.fromString(firmId)).stream()
                .map(UserProfile::getEntraUser)
                .filter(EntraUser::isEnabled)
                .toList();
        DisableType bulkDisableType = disableTypeResolver.resolve(disabledByUser);
        Integer totalOfUsersDisabled = 0;
        for (EntraUser entraUser : entraUsers) {
            // Disable user in Entra via tech services.
            TechServicesApiResponse<ChangeAccountEnabledResponse> changeAccountEnabledResponse
                    = techServicesClient.disableUser(mapper.map(entraUser, EntraUserDto.class), reason.getEntraDescription());
            if (!changeAccountEnabledResponse.isSuccess()) {
                throw new TechServicesClientException(changeAccountEnabledResponse.getError().getMessage(),
                        changeAccountEnabledResponse.getError().getCode(),
                        changeAccountEnabledResponse.getError().getErrors());
            }
            // Perform disable
            entraUser.setEnabled(false);
            entraUser.setDisabledBy(disabledById);
            entraUser.setDisableType(bulkDisableType);
            entraUserRepository.saveAndFlush(entraUser);
            totalOfUsersDisabled++;
            log.info("User with entra oid: {} has been disabled successfully with reason: {} By actor entra oid: {}",
                    entraUser.getEntraOid(),
                    reason.getEntraDescription(),
                    disabledByUser.getEntraOid());

            UserAccountStatusAudit userAccountStatusAudit = UserAccountStatusAudit.builder()
                    .entraUser(entraUser)
                    .disableUserReason(reason)
                    .statusChange(UserAccountStatus.DISABLED)
                    .statusChangedBy(disabledByUser.getFirstName() + " " + disabledByUser.getLastName())
                    .statusChangedDate(LocalDateTime.now())
                    .disableType(bulkDisableType)
                    .build();

            userAccountStatusAuditRepository.saveAndFlush(userAccountStatusAudit);
        }

        BulkDisableUserAuditEvent auditEvent = new BulkDisableUserAuditEvent(
                UUID.fromString(disabledByUser.getEntraOid()),
                UUID.fromString(firmId),
                totalOfUsersDisabled);
        eventService.logEvent(auditEvent);
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void enableUser(UUID enabledUserId, UUID enabledById) {
        if (enabledUserId.equals(enabledById)) {
            throw new RuntimeException(String.format("User %s can not be enabled by themselves", enabledUserId));
        }
        // Fetch entities
        EntraUser enabledUser = entraUserRepository.findByIdWithAssociations(enabledUserId)
                .orElseThrow(() -> new RuntimeException(String.format("Could not find a user account to disable with id \"%s\"", enabledUserId)));
        EntraUser enabledByUser = entraUserRepository.findByIdWithAssociations(enabledById)
                .orElseThrow(() -> new RuntimeException(String.format("Could not find a user account with id \"%s\"", enabledById)));

        boolean isUserEnablementAllowed = isUserEnablementAllowed(enabledUser, enabledByUser);

        if (isUserEnablementAllowed) {
            // Enable user in Entra via tech services.
            TechServicesApiResponse<ChangeAccountEnabledResponse> changeAccountEnabledResponse
                    = techServicesClient.enableUser(mapper.map(enabledUser, EntraUserDto.class));
            if (!changeAccountEnabledResponse.isSuccess()) {
                throw new TechServicesClientException(changeAccountEnabledResponse.getError().getMessage(),
                        changeAccountEnabledResponse.getError().getCode(),
                        changeAccountEnabledResponse.getError().getErrors());
            }

            // Perform enable
            enabledUser.setDisabledBy(null);
            enabledUser.setEnabled(true);
            enabledUser.setDisableType(null);
            entraUserRepository.saveAndFlush(enabledUser);

            // Add audit entry
            UserAccountStatusAudit userAccountStatusAudit = UserAccountStatusAudit.builder()
                    .entraUser(enabledUser)
                    .statusChange(UserAccountStatus.ENABLED)
                    .statusChangedBy(enabledByUser.getFirstName() + " " + enabledByUser.getLastName())
                    .statusChangedDate(LocalDateTime.now())
                    .build();
            userAccountStatusAuditRepository.saveAndFlush(userAccountStatusAudit);
        } else {
            throw new RuntimeException(String.format("Unable to enable the user %s by %s", enabledUserId, enabledById));
        }
    }

    private boolean isUserEnablementAllowed(EntraUser targetUser, EntraUser actor) {
        if (targetUser.isEnabled()) {
            log.info("The user {} is enabled already", targetUser.getId());
            return false;
        }

        if (!userService.isInternal(actor.getId()) && targetUser.isMultiFirmUser()) {
            log.info("Multi-firm user {} can not be enabled by non-internal user {}", targetUser.getId(), actor.getId());
            return false;
        }

        UserProfile actorUserProfile = actor.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("Could not find an active user profile for user with id \"%s\"", actor.getId())));

        List<String> actingUserRoles = Optional.ofNullable(actorUserProfile.getAppRoles()).orElse(Set.of())
                .stream().map(AppRole::getName).toList();
        DisableType disableType = targetUser.getDisableType();

        if (!userEnablementPolicy.canEnable(disableType, actingUserRoles)) {
            log.info("Enable blocked by hierarchy: actor {} with roles {} cannot re-enable user {} with disableType {}",
                    actor.getId(), actingUserRoles, targetUser.getId(), disableType);
            return false;
        }

        if (userEnablementPolicy.requiresSameFirmCheck(disableType, actingUserRoles)) {
            Firm enabledByUserFirm = actorUserProfile.getFirm();
            Firm enabledUserFirm = targetUser.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
                    .findFirst()
                    .map(UserProfile::getFirm).orElse(null);

            boolean sameFirm = enabledUserFirm != null && enabledByUserFirm != null
                    && enabledByUserFirm.getId().equals(enabledUserFirm.getId());
            if (!sameFirm) {
                log.info("FUM {} cannot enable user {} — different firm", actor.getId(), targetUser.getId());
                return false;
            }
        }

        return true;
    }

}
