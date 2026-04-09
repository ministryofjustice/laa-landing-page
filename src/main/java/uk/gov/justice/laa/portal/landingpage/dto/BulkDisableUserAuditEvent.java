package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.io.Serial;
import java.util.UUID;

public class BulkDisableUserAuditEvent extends AuditEvent {
    @Serial
    private static final long serialVersionUID = 1L;
    private final UUID firmId;
    private final int totalUsersDisabled;

    public BulkDisableUserAuditEvent(UUID userId, UUID firmId, int totalUsersDisabled) {
        super();
        this.userId = userId;
        this.firmId = firmId;
        this.totalUsersDisabled = totalUsersDisabled;
    }

    @Override
    public EventType getEventType() {
        return EventType.BULK_DISABLE_FIRM_USERS;
    }

    @Override
    public String getDescription() {
        return String.format("User (Entra OID: %s) has bulk disabled %s users for firm id %s for reason BULK_DISABLE_CYBERBREACH",
                userId, totalUsersDisabled, firmId);
    }
}
