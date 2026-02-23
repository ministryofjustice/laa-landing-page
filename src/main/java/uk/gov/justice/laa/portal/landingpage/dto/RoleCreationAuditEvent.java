package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.io.Serial;

public class RoleCreationAuditEvent extends AuditEvent {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String roleName;
    private final String parentAppName;
    private final String description;
    private final String ccmsCode;
    private final String userTypeRestriction;
    private final String userProfileId;
    private final String entraOid;

    private static final String ROLE_CREATION_TEMPLATE = """
            New app role created by user entra oid: %s user profile id: %s, for app %s with role details:
            Name: %s
            Description: %s
            CCMS Code: %s
            User Type Restriction: %s
            """;

    public RoleCreationAuditEvent(String roleName, String parentAppName,
                                  String description, String ccmsCode, String userTypeRestriction, 
                                  CurrentUserDto currentUserDto, String userProfileId, String entraOid) {
        this.roleName = roleName;
        this.parentAppName = parentAppName;
        this.description = description;
        this.ccmsCode = ccmsCode != null ? ccmsCode : "N/A";
        this.userTypeRestriction = userTypeRestriction;
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.userProfileId = userProfileId != null ? userProfileId : "N/A";
        this.entraOid = entraOid != null ? entraOid : "N/A";
    }

    @Override
    public EventType getEventType() {
        return EventType.CREATE_ROLE;
    }

    @Override
    public String getDescription() {
        return String.format(ROLE_CREATION_TEMPLATE, entraOid, userProfileId, parentAppName, roleName, description, ccmsCode, userTypeRestriction);
    }
}
