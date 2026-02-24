package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.UUID;

public class DeleteAppRoleEvent extends AuditEvent {

    private final UUID entraUserId;
    private final String appName;
    private final String appRoleName;
    private final String reason;

    public DeleteAppRoleEvent(UUID modifierUserId, UUID entraUserId,
                              String appName, String appRoleName, String reason) {
        super();
        this.userId = modifierUserId;
        this.entraUserId = entraUserId;
        this.appName = appName;
        this.appRoleName = appRoleName;
        this.reason = reason;
    }

    @Override
    public EventType getEventType() {
        return EventType.DELETE_LAA_APP_ROLE;
    }

    @Override
    public String getDescription() {
        return String.format("User '%s' (Entra OID: %s) has deleted app role %s from app %s. Reason: %s",
                userId, entraUserId, appRoleName, appName, reason);
    }
}
