package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

public class UpdateAppRoleDetailsAuditEvent extends AuditEvent {

    private final String updatedAppRoleName;
    private final String previousAppRoleName;
    private final String updatedAppRoleDescription;
    private final String previousAppRoleDescription;

    public UpdateAppRoleDetailsAuditEvent(CurrentUserDto currentUserDto,
                                          String updatedAppRoleName, String previousAppRoleName,
                                          String updatedAppRoleDescription, String previousAppRoleDescription) {
        super();
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.updatedAppRoleName = updatedAppRoleName;
        this.previousAppRoleName = previousAppRoleName;
        this.updatedAppRoleDescription = updatedAppRoleDescription;
        this.previousAppRoleDescription = previousAppRoleDescription;
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_LAA_APP_METADATA;
    }

    @Override
    public String getDescription() {
        return String.format("User '%s' (ID: %s) has updated App Role \"%s\" details - Name from '%s' to '%s' and description from '%s' to '%s'",
                userName, userId, previousAppRoleName, previousAppRoleName, updatedAppRoleName, previousAppRoleDescription, updatedAppRoleDescription);
    }
}
