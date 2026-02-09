package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.PdaFirmData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
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
    private final Map<String, Firm> firmsByCode;

    @Override
    public void execute(PdaSyncResultDto result) {
        try {
            boolean updated = false;

            // Re-enable firm if it was previously disabled
            if (!firm.getEnabled()) {
                log.info("Re-enabling previously disabled firm {} as it has returned in PDA data",
                    pdaFirm.getFirmNumber());
                firm.setEnabled(true);
                result.setFirmsReactivated(result.getFirmsReactivated() + 1);
                updated = true;
            }

            // Check firmType - CRITICAL: Type should NOT change (Python script requirement)
            // Handle empty or null firmType gracefully
            if (pdaFirm.getFirmType() == null || pdaFirm.getFirmType().trim().isEmpty()) {
                log.error("Failed to update firm {}: firmType is empty or null", pdaFirm.getFirmNumber());
                result.addError("Failed to update firm " + pdaFirm.getFirmNumber() + ": firmType is empty or null");
                return;
            }

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
                // Check for duplicate name BEFORE attempting update to avoid aborting transaction
                Firm existingFirmWithName = firmRepository.findFirmByName(pdaFirm.getFirmName());
                if (existingFirmWithName != null && !existingFirmWithName.getId().equals(firm.getId())) {
                    log.debug("Duplicate firm name '{}' detected for firm {} - skipping name update",
                        pdaFirm.getFirmName(), pdaFirm.getFirmNumber());
                    result.addWarning("Duplicate firm name '" + pdaFirm.getFirmName()
                        + "' for firm " + pdaFirm.getFirmNumber() + " - name update skipped");
                    // Don't update the name, but continue with other updates
                } else {
                    log.debug("Updating firm {}: name '{}' -> '{}'",
                        pdaFirm.getFirmNumber(), firm.getName(), pdaFirm.getFirmName());
                    firm.setName(pdaFirm.getFirmName());
                    updated = true;
                }
            }

            // Update parent if changed
            String currentParentCode = firm.getParentFirm() != null ? firm.getParentFirm().getCode() : null;
            String newParentCode = (pdaFirm.getParentFirmNumber() != null
                && !pdaFirm.getParentFirmNumber().trim().isEmpty()
                && !pdaFirm.getParentFirmNumber().trim().equalsIgnoreCase("null"))
                ? pdaFirm.getParentFirmNumber().trim() : null;

            // Normalize parent code to null if parent doesn't exist in database (prevents infinite update loops)
            if (newParentCode != null) {
                Firm parentFirm = firmsByCode.get(newParentCode);
                if (parentFirm == null || parentFirm.getType() == FirmType.ADVOCATE || parentFirm.getParentFirm() != null) {
                    // Parent doesn't exist, is invalid type, or has its own parent - treat as null
                    if (parentFirm == null) {
                        log.debug("Parent firm {} not found for firm {} - treating as null parent", newParentCode, pdaFirm.getFirmNumber());
                        result.addWarning("Parent firm " + newParentCode + " not found for firm " + pdaFirm.getFirmNumber());
                    } else if (parentFirm.getType() == FirmType.ADVOCATE) {
                        log.debug("Parent firm {} is ADVOCATE type for firm {} - treating as null parent (ADVOCATE firms cannot be parents)",
                            newParentCode, pdaFirm.getFirmNumber());
                        result.addWarning("Parent firm " + newParentCode + " is ADVOCATE type and cannot be a parent for firm "
                            + pdaFirm.getFirmNumber());
                    } else if (parentFirm.getParentFirm() != null) {
                        log.debug("Parent firm {} already has parent {} for firm {} - treating as null parent (multi-level hierarchy not allowed)",
                            newParentCode, parentFirm.getParentFirm().getCode(), pdaFirm.getFirmNumber());
                        result.addWarning("Parent firm " + newParentCode + " already has parent - multi-level hierarchy not allowed for firm "
                            + pdaFirm.getFirmNumber());
                    }
                    newParentCode = null;  // Normalize to null to prevent re-detection on next sync
                }
            }

            if (!equals(currentParentCode, newParentCode)) {
                log.debug("Updating firm {}: parentFirm '{}' -> '{}'",
                    pdaFirm.getFirmNumber(),
                    currentParentCode != null ? currentParentCode : "null",
                    newParentCode != null ? newParentCode : "null");

                if (newParentCode != null) {
                    Firm parentFirm = firmsByCode.get(newParentCode);
                    // Parent existence and validity already validated above, this lookup should always succeed
                    firm.setParentFirm(parentFirm);
                    updated = true;
                } else {
                    // Setting parent to null - only mark as updated if it wasn't already null
                    if (currentParentCode != null) {
                        firm.setParentFirm(null);
                        updated = true;
                    }
                }
            }

            if (updated) {
                firmRepository.save(firm);
                result.setFirmsUpdated(result.getFirmsUpdated() + 1);
                log.debug("Firm {} update complete", pdaFirm.getFirmNumber());
            }
        } catch (DataIntegrityViolationException e) {
            // Check if this is a duplicate name constraint violation
            if (e.getMessage() != null && e.getMessage().contains("firm_name_key")) {
                log.debug("Duplicate firm name '{}' for firm {} - skipping entire update to avoid constraint violation",
                    pdaFirm.getFirmName(), pdaFirm.getFirmNumber());
                result.addWarning("Duplicate firm name '" + pdaFirm.getFirmName()
                    + "' for firm " + pdaFirm.getFirmNumber() + " - update skipped to avoid constraint violation");
                // Don't add error, just warning - allow sync to continue
            } else {
                // For other data integrity violations, add as error
                log.error("Data integrity violation updating firm {}: {}", pdaFirm.getFirmNumber(), e.getMessage());
                result.addError("Data integrity violation for firm " + pdaFirm.getFirmNumber() + ": " + e.getMessage());
            }
        } catch (Exception e) {
            // Check if this is a duplicate name constraint violation (wrapped in different exception type)
            if (e.getMessage() != null && e.getMessage().contains("firm_name_key")) {
                log.debug("Duplicate firm name '{}' for firm {} - skipping entire update to avoid constraint violation",
                    pdaFirm.getFirmName(), pdaFirm.getFirmNumber());
                result.addWarning("Duplicate firm name '" + pdaFirm.getFirmName()
                    + "' for firm " + pdaFirm.getFirmNumber() + " - update skipped to avoid constraint violation");
                // Don't add error, just warning - allow sync to continue
            } else {
                log.error("Failed to update firm {}: {}", pdaFirm.getFirmNumber(), e.getMessage());
                result.addError("Failed to update firm " + pdaFirm.getFirmNumber() + ": " + e.getMessage());
            }
        }
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
