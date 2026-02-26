package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.UUID;

public class UpdateAppDisplayOrderAuditEvent extends AuditEvent {

    private final UUID userProfileId;

    public UpdateAppDisplayOrderAuditEvent(CurrentUserDto currentUserDto, UUID userProfileId) {
        super();
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.userProfileId = userProfileId;
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_LAA_APP_METADATA;
    }

    @Override
    public String getDescription() {
        return String.format("User (Profile ID: '%s' Entra OID: '%s') has updated App display order",
                userProfileId, userId);
    }
}
