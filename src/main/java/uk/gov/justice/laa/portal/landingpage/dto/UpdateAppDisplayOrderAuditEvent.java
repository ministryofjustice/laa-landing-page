package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

public class UpdateAppDisplayOrderAuditEvent extends AuditEvent {


    public UpdateAppDisplayOrderAuditEvent(CurrentUserDto currentUserDto) {
        super();
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_LAA_APP_METADATA;
    }

    @Override
    public String getDescription() {
        return String.format("User '%s' (ID: %s) has updated App display order",
                userName, userId);
    }
}
