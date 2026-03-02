package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.UUID;

public class AppSynchronizationAuditEvent extends AuditEvent {

    private final UUID userProfileId;
    private final String message;

    public AppSynchronizationAuditEvent(CurrentUserDto currentUserDto, UUID userProfileId, String message) {
        super();
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.userProfileId = userProfileId;
        this.message = message;
    }

    @Override
    public EventType getEventType() {
        return EventType.SYNCHRONISE_LAA_APPS;
    }

    @Override
    public String getDescription() {
        return String.format("User (Profile ID: '%s; Entra OID: %s) has syncronised apps. The status message is: %s",
                userProfileId, userId, message);
    }
}
