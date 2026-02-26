package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data transfer object for PDA firm information.
 * Used across multiple firm synchronization commands.
 */
@Data
@Builder
public class PdaFirmData {
    private String firmNumber;
    private String firmName;
    private String firmType;
    private String parentFirmNumber;
}
