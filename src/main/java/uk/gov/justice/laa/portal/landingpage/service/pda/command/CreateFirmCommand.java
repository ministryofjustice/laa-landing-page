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
            // Validate firmType
            if (pdaFirm.getFirmType() == null || pdaFirm.getFirmType().trim().isEmpty()) {
                log.error("Failed to create firm {}: firmType is empty or null", pdaFirm.getFirmNumber());
                result.addError("Failed to create firm " + pdaFirm.getFirmNumber() + ": firmType is empty or null");
                return;
            }

            // Check for duplicate code BEFORE attempting to create to avoid aborting transaction
            Firm existingFirmWithCode = firmRepository.findByCode(pdaFirm.getFirmNumber());
            if (existingFirmWithCode != null) {
                log.error("Cannot create firm {}: code {} already exists (existing firm ID: {})",
                    pdaFirm.getFirmNumber(), pdaFirm.getFirmNumber(), existingFirmWithCode.getId());
                result.addError("Cannot create firm " + pdaFirm.getFirmNumber()
                    + ": code already exists (duplicate in PDA data or database)");
                return;
            }

            // Check for duplicate name BEFORE attempting to create to avoid aborting transaction
            Firm existingFirmWithName = firmRepository.findFirmByName(pdaFirm.getFirmName());
            String finalName = pdaFirm.getFirmName();

            if (existingFirmWithName != null) {
                log.debug("Duplicate firm name '{}' detected when creating firm {} - appending firm code to make unique",
                    pdaFirm.getFirmName(), pdaFirm.getFirmNumber());
                finalName = pdaFirm.getFirmName() + " (" + pdaFirm.getFirmNumber() + ")";
                result.addWarning("Duplicate firm name '" + pdaFirm.getFirmName()
                    + "' for firm " + pdaFirm.getFirmNumber() + " - appended firm code to make unique");
            }

            Firm firm = Firm.builder()
                .code(pdaFirm.getFirmNumber())
                .name(finalName)
                .type(FirmType.valueOf(pdaFirm.getFirmType().toUpperCase().replace(" ", "_")))
                .build();

            // Note: Parent firm is NOT set here to avoid circular dependencies
            // Parent relationships are set in a second pass after all firms are created

            firmRepository.save(firm);
            result.setFirmsCreated(result.getFirmsCreated() + 1);
            log.debug("Created firm: {} (name: {}, type: {})",
                pdaFirm.getFirmNumber(), firm.getName(), pdaFirm.getFirmType());
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation creating firm {}: {}", pdaFirm.getFirmNumber(), e.getMessage());
            result.addError("Data integrity violation for firm " + pdaFirm.getFirmNumber() + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create firm {}: {}", pdaFirm.getFirmNumber(), e.getMessage(), e);
            result.addError("Failed to create firm " + pdaFirm.getFirmNumber() + ": " + e.getMessage());
        }
    }
}
