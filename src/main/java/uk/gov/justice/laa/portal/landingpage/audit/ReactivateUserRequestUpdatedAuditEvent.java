package uk.gov.justice.laa.portal.landingpage.audit;

import uk.gov.justice.laa.portal.landingpage.dto.AuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

public class ReactivateUserRequestUpdatedAuditEvent extends AuditEvent {

    private final String targetUserId;
    private final String targetUserProfileId;
    private final String activity;


    public ReactivateUserRequestUpdatedAuditEvent(CurrentUserDto currentUserDto, String targetUserId, String targetUserProfileId, String activity) {
        super();
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.targetUserId = targetUserId;
        this.targetUserProfileId = targetUserProfileId;
        this.activity = activity;
    }

    @Override
    public EventType getEventType() {
        return EventType.REACTIVATE_REQ_DELEGATE_ENABLE_USER_UPDATED;
    }

    @Override
    public String getDescription() {
        return String.format("User (Entra OID: %s) has %s reactivation request for (User Entra ID: %s; User Profile ID: %s)",
                userId, activity, targetUserId, targetUserProfileId);
    }
}
