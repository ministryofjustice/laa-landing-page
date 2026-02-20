package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaOfficeData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.Office.Address;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

@Slf4j
@RequiredArgsConstructor
public class UpdateOfficeCommand {
    private final OfficeRepository officeRepository;
    private final UserProfileRepository userProfileRepository;
    private final Office office;
    private final PdaOfficeData pdaOffice;
    private final Firm firm;

    public void execute(PdaSyncResultDto result) {
        try {
            boolean hasChanges = false;

            // Check if firm has changed - if so, remove all user associations
            if (!office.getFirm().getId().equals(firm.getId())) {
                log.debug("Office {} switching from firm {} to firm {} - removing user associations",
                    office.getCode(), office.getFirm().getCode(), firm.getCode());

                // Remove all user profile associations for this office
                List<UserProfile> profiles = userProfileRepository.findByOfficeId(office.getId());
                if (!profiles.isEmpty()) {
                    // Remove office from all profiles by matching ID
                    // Cannot rely on Office.equals() as it uses reference equality
                    for (UserProfile profile : profiles) {
                        profile.getOffices().removeIf(o -> o.getId().equals(office.getId()));
                    }
                    // Batch save all modified profiles
                    userProfileRepository.saveAll(profiles);

                    log.debug("Removed {} user associations from office {} due to firm switch",
                        profiles.size(), office.getCode());
                    result.addWarning("Office " + office.getCode() + " switched firms - removed "
                        + profiles.size() + " user association(s)");
                }

                office.setFirm(firm);
                hasChanges = true;
            }

            // Check if address has changed
            if (office.getAddress() == null) {
                office.setAddress(new Address());
            }

            Address address = office.getAddress();
            if (!equals(address.getAddressLine1(), emptyToNull(pdaOffice.getAddressLine1()))) {
                address.setAddressLine1(emptyToNull(pdaOffice.getAddressLine1()));
                hasChanges = true;
            }
            if (!equals(address.getAddressLine2(), emptyToNull(pdaOffice.getAddressLine2()))) {
                address.setAddressLine2(emptyToNull(pdaOffice.getAddressLine2()));
                hasChanges = true;
            }
            if (!equals(address.getAddressLine3(), emptyToNull(pdaOffice.getAddressLine3()))) {
                address.setAddressLine3(emptyToNull(pdaOffice.getAddressLine3()));
                hasChanges = true;
            }
            if (!equals(address.getCity(), emptyToNull(pdaOffice.getCity()))) {
                address.setCity(emptyToNull(pdaOffice.getCity()));
                hasChanges = true;
            }
            if (!equals(address.getPostcode(), emptyToNull(pdaOffice.getPostcode()))) {
                address.setPostcode(emptyToNull(pdaOffice.getPostcode()));
                hasChanges = true;
            }

            if (hasChanges) {
                officeRepository.save(office);
                result.setOfficesUpdated(result.getOfficesUpdated() + 1);
                log.debug("Updated office: {}", office.getCode());
            }

        } catch (org.springframework.dao.InvalidDataAccessApiUsageException e) {
            // Office was deleted in this transaction, skip the update
            if (e.getMessage() != null && e.getMessage().contains("deleted instance passed to merge")) {
                log.debug("Office {} was deleted in this transaction, skipping update", office.getCode());
                return;
            }
            log.error("Failed to update office {}: {}", office.getCode(), e.getMessage(), e);
            result.addError("Failed to update office " + office.getCode() + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update office {}: {}", office.getCode(), e.getMessage(), e);
            result.addError("Failed to update office " + office.getCode() + ": " + e.getMessage());
        }
    }

    private boolean equals(String s1, String s2) {
        if (s1 == null) {
            return s2 == null;
        }
        return s1.equals(s2);
    }

    private String emptyToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value;
    }
}
