package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.UUID;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

public class DeleteFirmProfileAuditEvent extends AuditEvent {
    private final UUID userProfileId;
    private final String firmName;
    private final String firmCode;
    private final String userEmail;
    private final int removedRolesCount;
    private final int detachedOfficesCount;

    private static final String DELETE_FIRM_PROFILE_TEMPLATE = """
            Firm profile deleted for multi-firm user. User: %s, User Profile ID: %s, Firm: %s (%s). \
            %d roles removed, %d offices detached. Deleted by user ID: %s
            """;

    public DeleteFirmProfileAuditEvent(UUID userId, UUID userProfileId, String userEmail,
                                       String firmName, String firmCode,
                                       int removedRolesCount, int detachedOfficesCount) {
        this.userId = userId;
        this.userProfileId = userProfileId;
        this.userEmail = userEmail;
        this.firmName = firmName;
        this.firmCode = firmCode;
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
                userEmail,
                userProfileId,
                firmName,
                firmCode != null ? firmCode : "N/A",
                removedRolesCount,
                detachedOfficesCount,
                userId);
    }
}
