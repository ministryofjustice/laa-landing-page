package uk.gov.justice.laa.portal.landingpage.audit;

import uk.gov.justice.laa.portal.landingpage.dto.AuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

public class ReactivateUserRequestRejectedAuditEvent extends AuditEvent {

    private final String targetUserId;
    private final String targetUserProfileId;
    private final String comments;


    public ReactivateUserRequestRejectedAuditEvent(CurrentUserDto currentUserDto, String targetUserId,
                                                   String targetUserProfileId, String comments) {
        super();
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.targetUserId = targetUserId;
        this.targetUserProfileId = targetUserProfileId;
        this.comments = comments;
    }

    @Override
    public EventType getEventType() {
        return EventType.REACTIVATE_REQ_DELEGATE_ENABLE_USER_REJECTED;
    }

    @Override
    public String getDescription() {
        return String.format("User (Entra OID: %s) has rejected reactivation request for (User Entra ID: %s; User Profile ID: %s), with comments: %s",
                userId, targetUserId, targetUserProfileId, comments);
    }
}
