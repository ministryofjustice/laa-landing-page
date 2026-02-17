package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Role Assignment administration display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentAdminDto {
    private String assigningRoleId;
    private String assigningRoleName;
    private String assignableRoleId;
    private String assignableRoleName;
}
