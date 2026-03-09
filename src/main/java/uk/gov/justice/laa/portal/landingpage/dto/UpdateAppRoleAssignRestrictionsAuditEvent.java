package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.UUID;

public class UpdateAppRoleAssignRestrictionsAuditEvent extends AuditEvent {

    private final UUID userProfileId;
    private final String appRoleName;

    public UpdateAppRoleAssignRestrictionsAuditEvent(CurrentUserDto currentUserDto, UUID userProfileId, String appRoleName) {
        super();
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.userProfileId = userProfileId;
        this.appRoleName = appRoleName;
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_LAA_APP_METADATA;
    }

    @Override
    public String getDescription() {
        return String.format("User (Profile ID: '%s'; Entra OID: %s) has updated App Role \"%s\" assignment restrictions",
                userProfileId, userId, appRoleName);
    }
}
