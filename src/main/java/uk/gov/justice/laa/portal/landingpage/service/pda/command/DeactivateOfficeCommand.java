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
 * Command to deactivate/delete an office that no longer exists in PDA.
 */
@Slf4j
@RequiredArgsConstructor
public class DeactivateOfficeCommand implements PdaSyncCommand {

    private final OfficeRepository officeRepository;
    private final UserProfileRepository userProfileRepository;
    private final Office office;

    @Override
    public void execute(PdaSyncResultDto result) {
        try {
            // CRITICAL RULE: MUST remove user associations before deleting office
            removeUserProfileOfficeAssociations(office);
            int associationsCount = countUserProfileOfficeAssociations(office);
            log.info("Would remove {} user profile associations for office: {}", associationsCount, office.getCode());
            result.addWarning("Office " + office.getCode() + " being deleted: "
                + associationsCount + " user associations will be removed");

            officeRepository.delete(office);
            result.setOfficesDeactivated(result.getOfficesDeactivated() + 1);
            log.info("Would deactivate/delete office: {} (firm: {})", office.getCode(), office.getFirm().getCode());
        } catch (Exception e) {
            log.error("Failed to deactivate office {}: {}", office.getCode(), e.getMessage());
            result.addError("Failed to deactivate office " + office.getCode() + ": " + e.getMessage());
        }
    }

    /**
     * Removes all user profile associations for an office before deletion.
     */
    private void removeUserProfileOfficeAssociations(Office office) {
        List<UserProfile> profiles = userProfileRepository.findAll();
        for (UserProfile profile : profiles) {
            if (profile.getOffices() != null && profile.getOffices().contains(office)) {
                profile.getOffices().remove(office);
                userProfileRepository.save(profile);
                log.info("Removed office {} from user profile {}", office.getCode(), profile.getId());
            }
        }
    }

    /**
     * Counts user profile associations for an office.
     */
    private int countUserProfileOfficeAssociations(Office office) {
        List<UserProfile> profiles = userProfileRepository.findAll();
        int count = 0;
        for (UserProfile profile : profiles) {
            if (profile.getOffices() != null && profile.getOffices().contains(office)) {
                count++;
            }
        }
        return count;
    }
}
