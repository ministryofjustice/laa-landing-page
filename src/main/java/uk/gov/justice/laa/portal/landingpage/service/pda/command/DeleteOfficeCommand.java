package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

/**
 * Command to delete an office that no longer exists in PDA.
 */
@Slf4j
@RequiredArgsConstructor
public class DeleteOfficeCommand implements PdaSyncCommand {

    private final OfficeRepository officeRepository;
    private final UserProfileRepository userProfileRepository;
    private final Office office;

    @Override
    public void execute(PdaSyncResultDto result) {
        try {
            // CRITICAL RULE: MUST remove user associations before deleting office
            List<UserProfile> profiles = userProfileRepository.findByOfficeId(office.getId());

            if (!profiles.isEmpty()) {
                // Remove office from all profiles in memory
                for (UserProfile profile : profiles) {
                    profile.getOffices().remove(office);
                }
                // Batch save all modified profiles
                userProfileRepository.saveAll(profiles);
                log.debug("Removed office {} from {} user profiles", office.getCode(), profiles.size());
            }

            officeRepository.delete(office);
            result.setOfficesDeleted(result.getOfficesDeleted() + 1);
            log.debug("Deleted office: {} (firm: {})", office.getCode(), office.getFirm().getCode());
        } catch (Exception e) {
            log.error("Failed to delete office {}: {}", office.getCode(), e.getMessage());
            result.addError("Failed to delete office " + office.getCode() + ": " + e.getMessage());
        }
    }
}
