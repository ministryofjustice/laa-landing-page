package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

/**
 * Command to re-enable a firm that has returned in PDA data.
 * This is the counterpart to DisableFirmCommand and restores access to firms
 * that were temporarily disabled due to contract gaps.
 *
 * Since all offices and user associations are preserved during disabling,
 * re-enabling simply sets the enabled flag back to true.
 */
@Slf4j
@RequiredArgsConstructor
public class EnableFirmCommand implements PdaSyncCommand {

    private final FirmRepository firmRepository;
    private final Firm firm;

    @Override
    public void execute(PdaSyncResultDto result) {
        try {
            // Mark firm as enabled (restore from soft disable)
            firm.setEnabled(true);

            // Save the enabled firm
            firmRepository.save(firm);
            result.setFirmsReactivated(result.getFirmsReactivated() + 1);

            log.info("Re-enabled firm: {} (name: {}) - restoring user access",
                firm.getCode(), firm.getName());
        } catch (Exception e) {
            log.error("Failed to enable firm {}: {}", firm.getCode(), e.getMessage());
            result.addError("Failed to enable firm " + firm.getCode() + ": " + e.getMessage());
        }
    }
}
