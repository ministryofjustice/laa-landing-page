package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * DTO combining user profile and firm information for multi-firm users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirmDirectoryDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // Firm fields
    private UUID firmId;
    private String firmName;
    private String firmCode;
    private String firmType;
}
