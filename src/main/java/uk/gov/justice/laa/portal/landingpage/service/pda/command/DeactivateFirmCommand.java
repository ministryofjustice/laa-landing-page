package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
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
            // First, handle user profile firm associations
            // EXTERNAL users cannot exist without a firm (database constraint), so we delete their profiles
            // INTERNAL users can have null firm, so we just clear the association
            List<UserProfile> profilesWithFirm = userProfileRepository.findByFirmId(firm.getId());
            if (!profilesWithFirm.isEmpty()) {
                log.info("Handling {} user profiles associated with firm {} before deleting",
                    profilesWithFirm.size(), firm.getCode());
                for (UserProfile profile : profilesWithFirm) {
                    if (profile.getUserType() == UserType.EXTERNAL) {
                        // EXTERNAL users must have a firm - delete the profile entirely
                        log.info("Deleting EXTERNAL user profile {} (firm {} being deactivated)",
                            profile.getId(), firm.getCode());
                        userProfileRepository.delete(profile);
                    } else {
                        // INTERNAL users can have null firm - just clear the association
                        profile.setFirm(null);
                        userProfileRepository.save(profile);
                        log.debug("Removed firm {} from INTERNAL user profile {} during firm deactivation",
                            firm.getCode(), profile.getId());
                    }
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
                result.setOfficesDeleted(result.getOfficesDeleted() + 1);
                    log.debug("Deleted office {} belonging to firm {}", office.getCode(), firm.getCode());
                }
            }

            // Now delete the firm
            firmRepository.delete(firm);
            result.setFirmsDeleted(result.getFirmsDeleted() + 1);
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
