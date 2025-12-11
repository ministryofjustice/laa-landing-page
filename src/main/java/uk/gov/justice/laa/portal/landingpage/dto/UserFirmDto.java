package uk.gov.justice.laa.portal.landingpage.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO combining user profile and firm information for multi-firm users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFirmDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    
    // User Profile fields
    private UUID userProfileId;
    private boolean isActiveProfile;
    
    // Firm fields
    private UUID firmId;
    private String firmName;
    private String firmCode;
    private String firmType;
}
