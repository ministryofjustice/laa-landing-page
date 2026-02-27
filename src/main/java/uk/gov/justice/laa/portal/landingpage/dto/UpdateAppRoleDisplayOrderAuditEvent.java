package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.UUID;

public class UpdateAppRoleDisplayOrderAuditEvent extends AuditEvent {

    private final UUID userProfileId;
    private final String appId;
    private final String appName;

    public UpdateAppRoleDisplayOrderAuditEvent(CurrentUserDto currentUserDto, UUID userProfileId, String appId, String appName) {
        super();
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.userProfileId = userProfileId;
        this.appId = appId;
        this.appName = appName;
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_LAA_APP_METADATA;
    }

    @Override
    public String getDescription() {
        return String.format("User (Profile ID: '%s'; Entra OID: %s) has updated App role display order for App '%s' (ID: %s)",
                userProfileId, userId, appName, appId);
    }
}
