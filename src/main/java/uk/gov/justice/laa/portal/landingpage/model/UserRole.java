package uk.gov.justice.laa.portal.landingpage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRole implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String appId;
    private String appName;
    private String appRoleId;
    private String roleName;
    private String assignmentId;
    private String appRoleName;
    private String appRoleAssignmentId;
    private boolean selected;
    private String url;
    private String serviceUrl;
    private boolean legacySync;
    private AppType appType;
    private boolean authzRole;
    private int appIndex;
    private long totalAppRoles;
}
