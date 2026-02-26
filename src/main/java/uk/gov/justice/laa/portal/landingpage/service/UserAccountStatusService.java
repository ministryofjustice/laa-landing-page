package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.DisableUserReasonDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;
import uk.gov.justice.laa.portal.landingpage.entity.DisableUserReason;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.exception.TechServicesClientException;
import uk.gov.justice.laa.portal.landingpage.repository.UserAccountStatusAuditRepository;
import uk.gov.justice.laa.portal.landingpage.repository.DisableUserReasonRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.ChangeAccountEnabledResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAccountStatusService {

    private final DisableUserReasonRepository disableUserReasonRepository;
    private final UserAccountStatusAuditRepository userAccountStatusAuditRepository;
    private final ModelMapper mapper;
    private final EntraUserRepository entraUserRepository;
    private final TechServicesClient techServicesClient;
    private final UserService userService;

    public List<DisableUserReasonDto> getDisableUserReasons() {
        List<DisableUserReason> reasons = disableUserReasonRepository.findAll();
        return reasons.stream()
                .filter(DisableUserReason::isUserSelectable)
                .map(reason -> mapper.map(reason, DisableUserReasonDto.class))
                .toList();
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void disableUser(UUID disabledUserId, UUID disableReasonId, UUID disabledById) {
        if (disabledUserId.equals(disabledById)) {
            throw new RuntimeException(String.format("User %s can not be disabled by themselves", disabledUserId));
        }

        // Fetch entities
        EntraUser disabledUser = entraUserRepository.findById(disabledUserId)
                .orElseThrow(() -> new RuntimeException(String.format("Could not find a user account to disable with id \"%s\"", disabledUserId)));
        EntraUser disabledByUser = entraUserRepository.findById(disabledById)
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

            // Perform disable
            disabledUser.setEnabled(false);
            entraUserRepository.saveAndFlush(disabledUser);

            // Add audit entry
            UserAccountStatusAudit userAccountStatusAudit = UserAccountStatusAudit.builder()
                    .entraUser(disabledUser)
                    .disableUserReason(reason)
                    .statusChange(UserAccountStatus.DISABLED)
                    .statusChangedBy(disabledByUser.getFirstName() + " " + disabledByUser.getLastName())
                    .statusChangedDate(LocalDateTime.now())
                    .build();
            userAccountStatusAuditRepository.saveAndFlush(userAccountStatusAudit);
        } else {
            throw new RuntimeException(String.format("Unable to disable the user %s by %s", disabledUserId, disabledById));
        }
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void enableUser(UUID enabledUserId, UUID enabledById) {
        if (enabledUserId.equals(enabledById)) {
            throw new RuntimeException(String.format("User %s can not be enabled by themselves", enabledUserId));
        }
        // Fetch entities
        EntraUser enabledUser = entraUserRepository.findById(enabledUserId)
                .orElseThrow(() -> new RuntimeException(String.format("Could not find a user account to disable with id \"%s\"", enabledUserId)));
        EntraUser enabledByUser = entraUserRepository.findById(enabledById)
                .orElseThrow(() -> new RuntimeException(String.format("Could not find a user account with id \"%s\"", enabledById)));
        if (!userService.isInternal(enabledById) && enabledUser.isMultiFirmUser()) {
            throw new RuntimeException(String.format("Multi firm user %s can not be enabled", enabledUserId));
        }

        Firm enabledByUserFirm = enabledByUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .findFirst()
                .map(UserProfile::getFirm)
                .orElse(null);
        Firm enabledUserFirm = enabledUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .findFirst()
                .map(UserProfile::getFirm).orElse(null);

        if (userService.isInternal(enabledById)
                || (enabledUserFirm != null && enabledByUserFirm != null && enabledByUserFirm.getId().equals(enabledUserFirm.getId()))) {
            // Enable user in Entra via tech services.
            TechServicesApiResponse<ChangeAccountEnabledResponse> changeAccountEnabledResponse
                    = techServicesClient.enableUser(mapper.map(enabledUser, EntraUserDto.class));
            if (!changeAccountEnabledResponse.isSuccess()) {
                throw new TechServicesClientException(changeAccountEnabledResponse.getError().getMessage(),
                        changeAccountEnabledResponse.getError().getCode(),
                        changeAccountEnabledResponse.getError().getErrors());
            }

            // Perform enable
            enabledUser.setEnabled(true);
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
}
