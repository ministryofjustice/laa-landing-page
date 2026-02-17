package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

public class UpdateAppRoleDisplayOrderAuditEvent extends AuditEvent {

    private final String appId;
    private final String appName;

    public UpdateAppRoleDisplayOrderAuditEvent(CurrentUserDto currentUserDto, String appId, String appName) {
        super();
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.appId = appId;
        this.appName = appName;
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_LAA_APP_METADATA;
    }

    @Override
    public String getDescription() {
        return String.format("User '%s' (ID: %s) has updated App role display order for App '%s' (ID: %s)",
                userName, userId, appName, appId);
    }
}
