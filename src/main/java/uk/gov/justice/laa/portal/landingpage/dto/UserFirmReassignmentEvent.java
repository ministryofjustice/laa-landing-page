package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.UUID;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

public class UserFirmReassignmentEvent extends AuditEvent {
    
    private final String targetUserId;
    private final String targetUserName;
    private final String previousFirmName;
    private final String newFirmName;
    private final String reason;

    public UserFirmReassignmentEvent(UUID modifierUserId, String modifierUserName, 
                                   String targetUserId, String targetUserName,
                                   String previousFirmName, String newFirmName, String reason) {
        super();
        this.userId = modifierUserId;
        this.userName = modifierUserName;
        this.targetUserId = targetUserId;
        this.targetUserName = targetUserName;
        this.previousFirmName = previousFirmName;
        this.newFirmName = newFirmName;
        this.reason = reason;
    }

    @Override
    public EventType getEventType() {
        return EventType.REASSIGN_USER_FIRM;
    }

    @Override
    public String getDescription() {
        return String.format("User '%s' (ID: %s) was reassigned from firm '%s' to firm '%s'. Reason: %s", 
            targetUserName, targetUserId, previousFirmName, newFirmName, reason);
    }
}
