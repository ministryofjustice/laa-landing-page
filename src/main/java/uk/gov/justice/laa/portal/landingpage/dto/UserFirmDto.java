package uk.gov.justice.laa.portal.landingpage.dto;

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
    
    // User Profile fields
    private UUID userProfileId;
    private boolean isActiveProfile;
    
    // Firm fields
    private UUID firmId;
    private String firmName;
    private String firmCode;
    private String firmType;
    
    /**
     * Returns the display name for the firm, combining name and code if available.
     * 
     * @return formatted firm display name
     */
    public String getFirmDisplayName() {
        StringBuilder displayName = new StringBuilder(firmName);
        
        if (firmCode != null && !firmCode.isEmpty()) {
            displayName.append(" (").append(firmCode).append(")");
        }
        
        if (isActiveProfile) {
            displayName.append(" - Active");
        }
        
        return displayName.toString();
    }
}
