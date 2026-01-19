package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.dto.PdaOfficeData;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

/**
 * Command to update an existing office with PDA data.
 */
@Slf4j
@RequiredArgsConstructor
public class UpdateOfficeCommand implements PdaSyncCommand {

    private final OfficeRepository officeRepository;
    private final UserProfileRepository userProfileRepository;
    private final Office office;
    private final PdaOfficeData pdaOffice;
    private final Firm firm;

    @Override
    public void execute(PdaSyncResultDto result) {
        try {
            boolean updated = false;

            // CRITICAL RULE: Check if firm changed - MUST remove user_profile_office associations first
            // This matches Python script logic: offices that switch firms lose all user associations
            if (!office.getFirm().getId().equals(firm.getId())) {
                log.warn("Office {} switching firms: {} -> {} - will delete user associations",
                    office.getCode(), office.getFirm().getCode(), firm.getCode());
                // removeUserProfileOfficeAssociations(office);  // COMMENTED OUT FOR TESTING
                int associationsCount = countUserProfileOfficeAssociations(office);
                log.info("Would remove {} user profile associations for office: {}", associationsCount, office.getCode());
                result.addWarning("Office " + office.getCode() + " switched firms: " + associationsCount
                    + " user associations will be deleted");
                office.setFirm(firm);
                updated = true;
            }

            // Update address if changed
            Office.Address currentAddress = office.getAddress();
            if (currentAddress == null
                || !equals(currentAddress.getAddressLine1(), pdaOffice.getAddressLine1())
                || !equals(currentAddress.getAddressLine2(), pdaOffice.getAddressLine2())
                || !equals(currentAddress.getAddressLine3(), pdaOffice.getAddressLine3())
                || !equals(currentAddress.getCity(), pdaOffice.getCity())
                || !equals(currentAddress.getPostcode(), pdaOffice.getPostcode())) {

                office.setAddress(Office.Address.builder()
                    .addressLine1(pdaOffice.getAddressLine1())
                    .addressLine2(pdaOffice.getAddressLine2())
                    .addressLine3(pdaOffice.getAddressLine3())
                    .city(pdaOffice.getCity())
                    .postcode(pdaOffice.getPostcode())
                    .build());
                updated = true;
            }

            if (updated) {
                // officeRepository.save(office);  // COMMENTED OUT FOR TESTING
                result.setOfficesUpdated(result.getOfficesUpdated() + 1);
                log.info("Would update office: {} (firm: {}, address: {})",
                    pdaOffice.getOfficeAccountNo(), firm.getCode(), pdaOffice.getAddressLine1());
            }
        } catch (Exception e) {
            log.error("Failed to update office {}: {}", pdaOffice.getOfficeAccountNo(), e.getMessage());
            result.addError("Failed to update office " + pdaOffice.getOfficeAccountNo() + ": " + e.getMessage());
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

    private boolean equals(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.equals(s2);
    }
}
