package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

/**
 * Command to disable a firm that no longer exists in PDA.
 * Unlike deactivation, this is a soft disable that preserves:
 * - The firm record itself
 * - All offices belonging to the firm
 * - All user profile associations
 *
 * This allows the firm to be automatically re-enabled if it returns in PDA
 * (e.g., after a contract gap is resolved).
 *
 * Note: Disabled firms are blocked from user access via FirmDisabledFilter.
 */
@Slf4j
@RequiredArgsConstructor
public class DisableFirmCommand implements PdaSyncCommand {

    private final FirmRepository firmRepository;
    private final Firm firm;

    @Override
    public void execute(PdaSyncResultDto result) {
        try {
            // Skip if already disabled (idempotent behavior)
            if (Boolean.FALSE.equals(firm.getEnabled())) {
                return;
            }

            // Mark firm as disabled (soft delete)
            firm.setEnabled(false);

            // Clear parent firm reference to simplify hierarchy
            // Disabled firms are removed from parent-child relationships
            if (firm.getParentFirm() != null) {
                log.debug("Clearing parent firm reference for disabled firm {}", firm.getCode());
                firm.setParentFirm(null);
            }

            // Clear this firm as parent from all child firms
            if (firm.getChildFirms() != null && !firm.getChildFirms().isEmpty()) {
                    log.debug("Clearing parent reference from {} child firms for disabled firm {}",
                    firm.getChildFirms().size(), firm.getCode());
                for (Firm childFirm : firm.getChildFirms()) {
                    childFirm.setParentFirm(null);
                    firmRepository.save(childFirm);
                }
                firm.getChildFirms().clear();
            }

            // Save the disabled firm
            firmRepository.save(firm);
            result.setFirmsDisabled(result.getFirmsDisabled() + 1);

            log.info("Disabled firm: {} (name: {}) - preserving offices and user associations",
                firm.getCode(), firm.getName());
        } catch (Exception e) {
            log.error("Failed to disable firm {}: {}", firm.getCode(), e.getMessage());
            result.addError("Failed to disable firm " + firm.getCode() + ": " + e.getMessage());
        }
    }
}
