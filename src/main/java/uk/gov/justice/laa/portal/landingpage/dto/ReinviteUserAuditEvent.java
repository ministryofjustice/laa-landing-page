package uk.gov.justice.laa.portal.landingpage.dto;

import java.io.Serial;
import java.util.UUID;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

public class ReinviteUserAuditEvent extends AuditEvent {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum Outcome {
        SUCCESS,
        USER_NOT_FOUND,
        API_FAILURE
    }

    private final String targetUserId;
    private final String targetUserOid;
    private final Outcome outcome;

    public ReinviteUserAuditEvent(AuditUserDetailDto targetUser, UUID initiatedByOid, Outcome outcome) {
        this.userId = initiatedByOid;
        this.targetUserId = targetUser.getUserId();
        this.targetUserOid = targetUser.getEntraOid();
        this.outcome = outcome;
    }

    @Override
    public EventType getEventType() {
        return EventType.USER_REINVITED;
    }

    @Override
    public String getDescription() {
        return String.format("Action: User re-invited, target user id %s, target entra oid %s, outcome %s",
                targetUserId, targetUserOid, outcome);
    }
}
