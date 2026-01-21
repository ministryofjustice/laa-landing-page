package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import org.springframework.dao.DataIntegrityViolationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaFirmData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
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

            // Note: Parent firm is NOT set here to avoid circular dependencies
            // Parent relationships are set in a second pass after all firms are created

            try {
                firmRepository.save(firm);
                result.setFirmsCreated(result.getFirmsCreated() + 1);
                log.info("Created firm: {} (name: {}, type: {})",
                    pdaFirm.getFirmNumber(), pdaFirm.getFirmName(), pdaFirm.getFirmType());
            } catch (DataIntegrityViolationException e) {
                // Check if this is a duplicate name constraint violation
                if (e.getMessage() != null && e.getMessage().contains("firm_name_key")) {
                    log.warn("Duplicate firm name '{}' when creating firm {} - appending firm code to make unique",
                        pdaFirm.getFirmName(), pdaFirm.getFirmNumber());

                    // Try again with firm code appended to name
                    firm.setName(pdaFirm.getFirmName() + " (" + pdaFirm.getFirmNumber() + ")");
                    firmRepository.save(firm);
                    result.setFirmsCreated(result.getFirmsCreated() + 1);
                    result.addWarning("Duplicate firm name '" + pdaFirm.getFirmName() +
                        "' for firm " + pdaFirm.getFirmNumber() + " - appended firm code to make unique");
                    log.info("Created firm with modified name: {} (name: {}, type: {})",
                        pdaFirm.getFirmNumber(), firm.getName(), pdaFirm.getFirmType());
                } else {
                    // For other data integrity violations, propagate the error
                    throw e;
                }
            }
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation creating firm {}: {}", pdaFirm.getFirmNumber(), e.getMessage());
            result.addError("Data integrity violation for firm " + pdaFirm.getFirmNumber() + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create firm {}: {}", pdaFirm.getFirmNumber(), e.getMessage(), e);
            result.addError("Failed to create firm " + pdaFirm.getFirmNumber() + ": " + e.getMessage());
        }
    }
}
