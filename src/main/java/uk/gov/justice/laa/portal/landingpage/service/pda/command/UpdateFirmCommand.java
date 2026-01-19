package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.dto.PdaFirmData;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

/**
 * Command to update an existing firm with PDA data.
 */
@Slf4j
@RequiredArgsConstructor
public class UpdateFirmCommand implements PdaSyncCommand {

    private final FirmRepository firmRepository;
    private final Firm firm;
    private final PdaFirmData pdaFirm;

    @Override
    public void execute(PdaSyncResultDto result) {
        try {
            boolean updated = false;

            // Check firmType - CRITICAL: Type should NOT change (Python script requirement)
            FirmType newType = FirmType.valueOf(pdaFirm.getFirmType().toUpperCase().replace(" ", "_"));
            if (!firm.getType().equals(newType)) {
                // This violates SiLAS data requirements - log as ERROR
                log.error("CRITICAL: Firm {} attempting to change type from {} to {} - THIS SHOULD NOT HAPPEN",
                    pdaFirm.getFirmNumber(), firm.getType(), newType);
                result.addWarning("CRITICAL: Firm " + pdaFirm.getFirmNumber() + " type change rejected: "
                    + firm.getType() + " -> " + newType + " (violates SiLAS data requirement)");
                return;
            }

            // Update name if changed
            if (!firm.getName().equals(pdaFirm.getFirmName())) {
                firm.setName(pdaFirm.getFirmName());
                updated = true;
            }

            // Update parent if changed
            String currentParentCode = firm.getParentFirm() != null ? firm.getParentFirm().getCode() : null;
            if ((pdaFirm.getParentFirmNumber() == null && currentParentCode != null)
                || (pdaFirm.getParentFirmNumber() != null && !pdaFirm.getParentFirmNumber().equals(currentParentCode))) {

                if (pdaFirm.getParentFirmNumber() != null && !pdaFirm.getParentFirmNumber().isEmpty()) {
                    Firm parentFirm = firmRepository.findByCode(pdaFirm.getParentFirmNumber());
                    firm.setParentFirm(parentFirm);
                } else {
                    firm.setParentFirm(null);
                }
                updated = true;
            }

            if (updated) {
                // firmRepository.save(firm);  // COMMENTED OUT FOR TESTING
                result.setFirmsUpdated(result.getFirmsUpdated() + 1);
                log.info("Would update firm: {} (name: {})", pdaFirm.getFirmNumber(), pdaFirm.getFirmName());
            }
        } catch (Exception e) {
            log.error("Failed to update firm {}: {}", pdaFirm.getFirmNumber(), e.getMessage());
            result.addError("Failed to update firm " + pdaFirm.getFirmNumber() + ": " + e.getMessage());
        }
    }
}
