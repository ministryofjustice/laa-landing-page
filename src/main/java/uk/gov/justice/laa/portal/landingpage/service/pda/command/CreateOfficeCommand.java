package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaOfficeData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

/**
 * Command to create a new office from PDA data.
 */
@Slf4j
@RequiredArgsConstructor
public class CreateOfficeCommand implements PdaSyncCommand {

    private final OfficeRepository officeRepository;
    private final PdaOfficeData pdaOffice;
    private final Firm firm;

    @Override
    public void execute(PdaSyncResultDto result) {
        try {
            Office office = Office.builder()
                .code(pdaOffice.getOfficeAccountNo())
                .firm(firm)
                .address(Office.Address.builder()
                    .addressLine1(emptyToNull(pdaOffice.getAddressLine1()))
                    .addressLine2(emptyToNull(pdaOffice.getAddressLine2()))
                    .addressLine3(emptyToNull(pdaOffice.getAddressLine3()))
                    .city(emptyToNull(pdaOffice.getCity()))
                    .postcode(emptyToNull(pdaOffice.getPostcode()))
                    .build())
                .build();

            officeRepository.save(office);
            result.setOfficesCreated(result.getOfficesCreated() + 1);
            log.debug("Created office: {} for firm {} (address: {}, {})",
                pdaOffice.getOfficeAccountNo(), firm.getCode(), pdaOffice.getAddressLine1(), pdaOffice.getCity());
        } catch (Exception e) {
            log.error("Failed to create office {}: {}", pdaOffice.getOfficeAccountNo(), e.getMessage());
            result.addError("Failed to create office " + pdaOffice.getOfficeAccountNo() + ": " + e.getMessage());
        }
    }

    private String emptyToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value;
    }
}
