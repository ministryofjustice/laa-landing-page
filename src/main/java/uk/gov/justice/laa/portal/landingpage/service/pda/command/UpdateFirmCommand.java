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
                // Check for duplicate name BEFORE attempting update to avoid aborting transaction
                Firm existingFirmWithName = firmRepository.findFirmByName(pdaFirm.getFirmName());
                if (existingFirmWithName != null && !existingFirmWithName.getId().equals(firm.getId())) {
                    log.warn("Duplicate firm name '{}' detected for firm {} - skipping name update",
                        pdaFirm.getFirmName(), pdaFirm.getFirmNumber());
                    result.addWarning("Duplicate firm name '" + pdaFirm.getFirmName() +
                        "' for firm " + pdaFirm.getFirmNumber() + " - name update skipped");
                    // Don't update the name, but continue with other updates
                } else {
                    log.info("Updating firm {}: name '{}' -> '{}'",
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

            if (!equals(currentParentCode, newParentCode)) {
                log.info("Updating firm {}: parentFirm '{}' -> '{}'",
                    pdaFirm.getFirmNumber(),
                    currentParentCode != null ? currentParentCode : "null",
                    newParentCode != null ? newParentCode : "null");

                if (newParentCode != null) {
                    Firm parentFirm = firmRepository.findByCode(newParentCode);
                    if (parentFirm == null) {
                        log.warn("Parent firm {} not found for firm {} - keeping parent as null", newParentCode, pdaFirm.getFirmNumber());
                        result.addWarning("Parent firm " + newParentCode + " not found for firm " + pdaFirm.getFirmNumber());
                        // Don't set to null if already null - no change needed
                        if (currentParentCode != null) {
                            firm.setParentFirm(null);
                            updated = true;
                        }
                    } else if (parentFirm.getType() == FirmType.ADVOCATE) {
                        log.warn("Parent firm {} is ADVOCATE type for firm {} - keeping parent as null (ADVOCATE firms cannot be parents)",
                            newParentCode, pdaFirm.getFirmNumber());
                        result.addWarning("Parent firm " + newParentCode + " is ADVOCATE type and cannot be a parent for firm " +
                            pdaFirm.getFirmNumber());
                        // Don't set to null if already null - no change needed
                        if (currentParentCode != null) {
                            firm.setParentFirm(null);
                            updated = true;
                        }
                    } else if (parentFirm.getParentFirm() != null) {
                        // Check if proposed parent already has a parent (database constraint: only one level allowed)
                        log.warn("Parent firm {} already has parent {} for firm {} - keeping parent as null (multi-level hierarchy not allowed)",
                            newParentCode, parentFirm.getParentFirm().getCode(), pdaFirm.getFirmNumber());
                        result.addWarning("Parent firm " + newParentCode + " already has parent " +
                            parentFirm.getParentFirm().getCode() + " - multi-level hierarchy not allowed for firm " +
                            pdaFirm.getFirmNumber());
                        // Don't set to null if already null - no change needed
                        if (currentParentCode != null) {
                            firm.setParentFirm(null);
                            updated = true;
                        }
                    } else {
                        firm.setParentFirm(parentFirm);
                        updated = true;
                    }
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
                log.info("Firm {} update complete", pdaFirm.getFirmNumber());
            }
        } catch (DataIntegrityViolationException e) {
            // Check if this is a duplicate name constraint violation
            if (e.getMessage() != null && e.getMessage().contains("firm_name_key")) {
                log.warn("Duplicate firm name '{}' for firm {} - skipping entire update to avoid constraint violation",
                    pdaFirm.getFirmName(), pdaFirm.getFirmNumber());
                result.addWarning("Duplicate firm name '" + pdaFirm.getFirmName() +
                    "' for firm " + pdaFirm.getFirmNumber() + " - update skipped to avoid constraint violation");
                // Don't add error, just warning - allow sync to continue
            } else {
                // For other data integrity violations, add as error
                log.error("Data integrity violation updating firm {}: {}", pdaFirm.getFirmNumber(), e.getMessage());
                result.addError("Data integrity violation for firm " + pdaFirm.getFirmNumber() + ": " + e.getMessage());
            }
        } catch (Exception e) {
            // Check if this is a duplicate name constraint violation (wrapped in different exception type)
            if (e.getMessage() != null && e.getMessage().contains("firm_name_key")) {
                log.warn("Duplicate firm name '{}' for firm {} - skipping entire update to avoid constraint violation",
                    pdaFirm.getFirmName(), pdaFirm.getFirmNumber());
                result.addWarning("Duplicate firm name '" + pdaFirm.getFirmName() +
                    "' for firm " + pdaFirm.getFirmNumber() + " - update skipped to avoid constraint violation");
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
