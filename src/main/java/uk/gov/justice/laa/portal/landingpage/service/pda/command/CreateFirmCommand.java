package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.dto.PdaFirmData;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

/**
 * Command to create a new firm from PDA data.
 */
@Slf4j
@RequiredArgsConstructor
public class CreateFirmCommand implements PdaSyncCommand {

    private final FirmRepository firmRepository;
    private final PdaFirmData pdaFirm;

    @Override
    public void execute(PdaSyncResultDto result) {
        try {
            Firm firm = Firm.builder()
                .code(pdaFirm.getFirmNumber())
                .name(pdaFirm.getFirmName())
                .type(FirmType.valueOf(pdaFirm.getFirmType().toUpperCase().replace(" ", "_")))
                .build();

            // Handle parent firm if exists
            if (pdaFirm.getParentFirmNumber() != null && !pdaFirm.getParentFirmNumber().isEmpty()) {
                Firm parentFirm = firmRepository.findByCode(pdaFirm.getParentFirmNumber());
                if (parentFirm != null) {
                    firm.setParentFirm(parentFirm);
                }
            }

            // firmRepository.save(firm);  // COMMENTED OUT FOR TESTING
            result.setFirmsCreated(result.getFirmsCreated() + 1);
            log.info("Would create firm: {} (name: {}, type: {})",
                pdaFirm.getFirmNumber(), pdaFirm.getFirmName(), pdaFirm.getFirmType());
        } catch (Exception e) {
            log.error("Failed to create firm {}: {}", pdaFirm.getFirmNumber(), e.getMessage());
            result.addError("Failed to create firm " + pdaFirm.getFirmNumber() + ": " + e.getMessage());
        }
    }
}
