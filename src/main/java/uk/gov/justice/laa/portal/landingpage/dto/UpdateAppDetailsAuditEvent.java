package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.UUID;

public class UpdateAppDetailsAuditEvent extends AuditEvent {

    private final UUID userProfileId;
    private final String appName;
    private final boolean updatedAppStatus;
    private final boolean previousAppStatus;
    private final String updatedAppDescription;
    private final String previousAppDescription;

    public UpdateAppDetailsAuditEvent(CurrentUserDto currentUserDto, UUID userProfileId, String appName,
                                   boolean updatedAppStatus, boolean previousAppStatus,
                                   String updatedAppDescription, String previousAppDescription) {
        super();
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.userProfileId = userProfileId;
        this.appName = appName;
        this.updatedAppStatus = updatedAppStatus;
        this.previousAppStatus = previousAppStatus;
        this.updatedAppDescription = updatedAppDescription;
        this.previousAppDescription = previousAppDescription;
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_LAA_APP_METADATA;
    }

    @Override
    public String getDescription() {
        return String.format("User (Profile Id: '%s'; Entra OID: %s) has updated App \"%s\" details from status '%s' to '%s' and description from '%s' to '%s'",
                userProfileId, userId, appName, previousAppStatus ? "Yes" : "No", updatedAppStatus ? "Yes" : "No", previousAppDescription, updatedAppDescription);
    }
}
