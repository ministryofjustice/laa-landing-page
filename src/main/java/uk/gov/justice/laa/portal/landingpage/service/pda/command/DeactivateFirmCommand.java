package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

/**
 * Command to deactivate/delete a firm that no longer exists in PDA.
 */
@Slf4j
@RequiredArgsConstructor
public class DeactivateFirmCommand implements PdaSyncCommand {

    private final FirmRepository firmRepository;
    private final Firm firm;

    @Override
    public void execute(PdaSyncResultDto result) {
        try {
            // firmRepository.delete(firm);  // COMMENTED OUT FOR TESTING - could delete instead
            result.setFirmsDeactivated(result.getFirmsDeactivated() + 1);
            log.info("Would deactivate/delete firm: {} (name: {})", firm.getCode(), firm.getName());
        } catch (Exception e) {
            log.error("Failed to deactivate firm {}: {}", firm.getCode(), e.getMessage());
            result.addError("Failed to deactivate firm " + firm.getCode() + ": " + e.getMessage());
        }
    }
}
