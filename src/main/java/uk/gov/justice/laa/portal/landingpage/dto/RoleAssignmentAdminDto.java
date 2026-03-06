package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO for Role Assignment administration display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentAdminDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String assigningRoleId;
    private String assigningRoleName;
    private String assignableRoleId;
    private String assignableRoleName;
}
