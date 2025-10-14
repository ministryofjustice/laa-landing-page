package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;
import uk.gov.justice.laa.portal.landingpage.model.DeletedUser;

import java.io.Serializable;
import java.util.UUID;

public class DeleteUserSuccessAuditEvent extends AuditEvent implements Serializable {
    private final DeletedUser deletedUser;
    private final String reason;

    private static final String DELETE_USER_TEMPLATE = """
            User deleted successfully, deleted user id %s for reason %s. %s roles removed. %s offices detached
            """;

    public DeleteUserSuccessAuditEvent(String reason, UUID userId, DeletedUser deletedUser) {
        this.deletedUser = deletedUser;
        this.reason = reason;
        this.userId = userId;
    }

    @Override
    public EventType getEventType() {
        return EventType.USER_DELETE_EXECUTED;
    }

    @Override
    public String getDescription() {
        return String.format(DELETE_USER_TEMPLATE, deletedUser.getDeletedUserId(), reason, deletedUser.getRemovedRolesCount(), deletedUser.getDetachedOfficesCount());
    }
}