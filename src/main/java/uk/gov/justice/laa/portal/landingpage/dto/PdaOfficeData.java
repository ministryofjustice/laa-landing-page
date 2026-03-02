package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data transfer object for PDA office information.
 * Used across multiple office synchronization commands.
 */
@Data
@Builder
public class PdaOfficeData {
    private String officeAccountNo;
    private String firmNumber;
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String city;
    private String postcode;
}
