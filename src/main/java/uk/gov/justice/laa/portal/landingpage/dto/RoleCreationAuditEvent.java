package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.io.Serial;

public class RoleCreationAuditEvent extends AuditEvent {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String roleName;
    private final String roleId;
    private final String parentAppName;
    private final String description;
    private final String ccmsCode;
    private final String userTypeRestriction;

    private static final String ROLE_CREATION_TEMPLATE = """
            Role created: %s (ID: %s) for app %s
            Description: %s
            CCMS Code: %s
            User Type Restriction: %s
            """;

    public RoleCreationAuditEvent(String roleName, String roleId, String parentAppName, 
                                  String description, String ccmsCode, String userTypeRestriction, CurrentUserDto currentUserDto) {
        this.roleName = roleName;
        this.roleId = roleId;
        this.parentAppName = parentAppName;
        this.description = description;
        this.ccmsCode = ccmsCode != null ? ccmsCode : "N/A";
        this.userTypeRestriction = userTypeRestriction;
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
    }

    @Override
    public EventType getEventType() {
        return EventType.CREATE_ROLE;
    }

    @Override
    public String getDescription() {
        return String.format(ROLE_CREATION_TEMPLATE, roleName, roleId, parentAppName, description, ccmsCode, userTypeRestriction);
    }
}
