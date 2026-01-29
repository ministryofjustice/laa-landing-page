package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.DisableUserReasonDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;
import uk.gov.justice.laa.portal.landingpage.entity.DisableUserReason;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
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

    public List<DisableUserReasonDto> getDisableUserReasons() {
        List<DisableUserReason> reasons = disableUserReasonRepository.findAll();
        return reasons.stream()
                .map(reason -> mapper.map(reason, DisableUserReasonDto.class))
                .toList();
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void disableUser(UUID disabledUserId, UUID disableReasonId, UUID disabledById) {
        // Fetch entities
        EntraUser disabledUser = entraUserRepository.findById(disabledUserId)
                .orElseThrow(() -> new RuntimeException(String.format("Could not find a user account to disable with id \"%s\"", disabledUserId)));
        EntraUser disabledByUser = entraUserRepository.findById(disabledById)
                .orElseThrow(() -> new RuntimeException(String.format("Could not find a user account with id \"%s\"", disabledById)));
        DisableUserReason reason = disableUserReasonRepository.findById(disableReasonId)
                .orElseThrow(() -> new RuntimeException(String.format("Could not find a disable user reason with id \"%s\"", disableReasonId)));

        // Disable user in Entra via tech services.
        TechServicesApiResponse<ChangeAccountEnabledResponse> changeAccountEnabledResponse = techServicesClient.disableUser(mapper.map(disabledUser, EntraUserDto.class), reason.getEntraDescription());
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
                .disabledBy(disabledByUser.getFirstName() + " " + disabledByUser.getLastName())
                .disabledDate(LocalDateTime.now())
                .build();
        userAccountStatusAuditRepository.saveAndFlush(userAccountStatusAudit);
    }
}
