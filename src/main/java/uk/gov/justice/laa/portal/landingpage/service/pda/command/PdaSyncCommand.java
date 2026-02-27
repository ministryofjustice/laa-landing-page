package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;

/**
 * Command interface for PDA synchronization operations.
 * Implements the Command pattern to encapsulate data synchronization actions.
 */
@FunctionalInterface
public interface PdaSyncCommand {

    /**
     * Execute the synchronization command.
     *
     * @param result The result DTO to update with operation statistics and errors
     */
    void execute(PdaSyncResultDto result);
}
