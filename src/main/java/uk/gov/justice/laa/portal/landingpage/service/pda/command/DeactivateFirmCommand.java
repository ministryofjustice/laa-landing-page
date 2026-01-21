package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

/**
 * Command to deactivate/delete a firm that no longer exists in PDA.
 * Note: This will also delete all offices belonging to the firm to maintain referential integrity.
 */
@Slf4j
@RequiredArgsConstructor
public class DeactivateFirmCommand implements PdaSyncCommand {

    private final FirmRepository firmRepository;
    private final OfficeRepository officeRepository;
    private final UserProfileRepository userProfileRepository;
    private final Firm firm;

    @Override
    public void execute(PdaSyncResultDto result) {
        try {
            // First, remove user profile firm associations to avoid foreign key constraint violations
            List<UserProfile> profilesWithFirm = userProfileRepository.findByFirmId(firm.getId());
            if (!profilesWithFirm.isEmpty()) {
                log.info("Removing firm association from {} user profiles for firm {} before deleting",
                    profilesWithFirm.size(), firm.getCode());
                for (UserProfile profile : profilesWithFirm) {
                    profile.setFirm(null);
                    userProfileRepository.save(profile);
                    log.debug("Removed firm {} from user profile {} during firm deactivation",
                        firm.getCode(), profile.getId());
                }
            }

            // Second, delete all offices belonging to this firm to avoid foreign key constraint violations
            List<Office> offices = officeRepository.findByFirm(firm);
            if (!offices.isEmpty()) {
                log.info("Deleting {} offices for firm {} before deleting firm", offices.size(), firm.getCode());
                for (Office office : offices) {
                    // Remove user associations before deleting office
                    removeUserProfileOfficeAssociations(office);

                    officeRepository.delete(office);
                    result.setOfficesDeactivated(result.getOfficesDeactivated() + 1);
                    log.debug("Deleted office {} belonging to firm {}", office.getCode(), firm.getCode());
                }
            }

            // Now delete the firm
            firmRepository.delete(firm);
            result.setFirmsDeactivated(result.getFirmsDeactivated() + 1);
            log.info("Deactivate/delete firm: {} (name: {})", firm.getCode(), firm.getName());
        } catch (Exception e) {
            log.error("Failed to deactivate firm {}: {}", firm.getCode(), e.getMessage());
            result.addError("Failed to deactivate firm " + firm.getCode() + ": " + e.getMessage());
        }
    }

    /**
     * Removes all user profile associations for an office before deletion.
     */
    private void removeUserProfileOfficeAssociations(Office office) {
        List<UserProfile> profiles = userProfileRepository.findByOfficeId(office.getId());
        for (UserProfile profile : profiles) {
            profile.getOffices().remove(office);
            userProfileRepository.save(profile);
            log.debug("Removed office {} from user profile {} during firm deactivation", office.getCode(), profile.getId());
        }
    }
}
