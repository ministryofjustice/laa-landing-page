package uk.gov.justice.laa.portal.landingpage.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID id;
    private boolean activeProfile;
    private UserType userType;
    private String legacyUserId;
    private UserProfileStatus userProfileStatus;

    // Related entities as DTOs
    private EntraUserDto entraUser;
    private FirmDto firm;
    private List<OfficeDto> offices;
    private List<AppRoleDto> appRoles;

    // Audit fields
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime lastModified;
    private String lastModifiedBy;

    // Helper method for compatibility with tests
    public String getFullName() {
        if (entraUser != null) {
            return entraUser.getFullName();
        }
        return null;
    }
}
