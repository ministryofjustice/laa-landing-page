package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.UUID;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

public class UserFirmReassignmentEvent extends AuditEvent {
    
    private final String targetUserId;
    private final String targetUserEntraOid;
    private final UUID oldFirmId;
    private final UUID newFirmId;
    private final String reason;

    public UserFirmReassignmentEvent(UUID modifierUserId,
                                     String targetUserId, String targetUserEntraOid, UUID oldFirmId, UUID newFirmId, String reason) {
        super();
        this.oldFirmId = oldFirmId;
        this.newFirmId = newFirmId;
        this.userId = modifierUserId;
        this.targetUserId = targetUserId;
        this.targetUserEntraOid = targetUserEntraOid;
        this.reason = reason;
    }

    @Override
    public EventType getEventType() {
        return EventType.REASSIGN_USER_FIRM;
    }

    @Override
    public String getDescription() {
        return String.format("User %s and entraOid %s, was reassigned from firmID '%s' to firmID '%s'. Reason: %s",
             targetUserId, targetUserEntraOid, oldFirmId, newFirmId, reason);
    }
}
