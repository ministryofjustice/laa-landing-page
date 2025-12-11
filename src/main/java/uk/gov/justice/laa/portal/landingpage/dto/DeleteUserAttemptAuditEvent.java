package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public class DeleteUserAttemptAuditEvent extends AuditEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String deletedUserId;
    private final String reason;
    private final String error;

    private static final String DELETE_USER_TEMPLATE = """
            User delete attempted, user id %s for reason %s, error: %s
            """;

    public DeleteUserAttemptAuditEvent(String deletedUserId, String reason, UUID userId, String error) {
        this.deletedUserId = deletedUserId;
        this.reason = reason;
        this.userId = userId;
        this.error = error;
    }

    @Override
    public EventType getEventType() {
        return EventType.USER_DELETE_ATTEMPT;
    }

    @Override
    public String getDescription() {
        return String.format(DELETE_USER_TEMPLATE, deletedUserId, reason, error);
    }
}
