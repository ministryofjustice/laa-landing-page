package uk.gov.justice.laa.portal.landingpage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRole implements Serializable {
    private String appId;
    private String appName;
    private String appRoleId;
    private String roleName;
    private String assignmentId;
    private String appRoleName;
    private String appRoleAssignmentId;
    private boolean selected;
    private String url;
}
