package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.UUID;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

public class DeleteFirmProfileAuditEvent extends AuditEvent {
    private final UUID userProfileId;
    private final UUID firmId;
    private final String entraOid;
    private final int removedRolesCount;
    private final int detachedOfficesCount;

    private static final String DELETE_FIRM_PROFILE_TEMPLATE = """
            Firm profile deleted for multi-firm user. EntraOid: %s, User Profile ID: %s, FirmId: %s. \
            %d roles removed, %d offices detached
            """;

    public DeleteFirmProfileAuditEvent(UUID userId, UUID userProfileId, String entraOid,
                                       UUID firmId,
                                       int removedRolesCount, int detachedOfficesCount) {
        this.userId = userId;
        this.userProfileId = userProfileId;
        this.entraOid = entraOid;
        this.firmId = firmId;
        this.removedRolesCount = removedRolesCount;
        this.detachedOfficesCount = detachedOfficesCount;
    }

    @Override
    public EventType getEventType() {
        return EventType.USER_DELETE_ATTEMPT;
    }

    @Override
    public String getDescription() {
        return String.format(DELETE_FIRM_PROFILE_TEMPLATE,
                entraOid,
                userProfileId,
                firmId,
                removedRolesCount,
                detachedOfficesCount);
    }
}
