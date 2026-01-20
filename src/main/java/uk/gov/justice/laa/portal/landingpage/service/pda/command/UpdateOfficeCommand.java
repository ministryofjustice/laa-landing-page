package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaOfficeData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Office.Address;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

@Slf4j
@RequiredArgsConstructor
public class UpdateOfficeCommand {
    private final OfficeRepository officeRepository;
    private final Office office;
    private final PdaOfficeData pdaOffice;
    private final Firm firm;

    public void execute(PdaSyncResultDto result) {
        try {
            boolean hasChanges = false;

            // Check if firm has changed
            if (!office.getFirm().getId().equals(firm.getId())) {
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

        } catch (Exception e) {
            log.error("Failed to update office {}: {}", office.getCode(), e.getMessage(), e);
            result.addError("Failed to update office " + office.getCode() + ": " + e.getMessage());
        }
    }

    private boolean equals(String s1, String s2) {
        if (s1 == null) return s2 == null;
        return s1.equals(s2);
    }

    private String emptyToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value;
    }
}
