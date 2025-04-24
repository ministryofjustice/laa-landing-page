package uk.gov.justice.laa.portal.landingpage.model;

import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * UserSessionData
 */
@Data
@AllArgsConstructor
@Builder
public class UserSessionData {
    private String name;
    private String accessToken;
    private List<AppRoleAssignment> appRoleAssignments;
    private List<AppRole> userAppRoles;
    private Set<LaaApplication> userAccLaaApps;
    private User user;
    private String lastLogin;
}
