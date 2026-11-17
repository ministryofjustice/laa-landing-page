package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.io.Serial;
import java.io.Serializable;

public class CreateUserAuditEvent extends AuditEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;
    private final String entraOid;
    private final String selectedFirmId;
    private final boolean isUserManager;

    private static final String CREATE_USER_TEMPLATE = """
            New user created, entra oid %s, with firm %s and user type %s
            """;

    public CreateUserAuditEvent(CurrentUserDto currentUserDto, EntraUser user,
                                String selectedFirmId, boolean isUserManager) {
        this(currentUserDto, user.getEntraOid(), selectedFirmId, isUserManager);
    }

    public CreateUserAuditEvent(CurrentUserDto currentUserDto, String entraOid,
                                String selectedFirmId, boolean isUserManager) {
        this.userId = currentUserDto.getUserId();
        this.entraOid = entraOid;
        this.selectedFirmId = selectedFirmId;
        this.isUserManager = isUserManager;
    }

    @Override
    public EventType getEventType() {
        return EventType.CREATE_USER;
    }

    @Override
    public String getDescription() {
        return String.format(CREATE_USER_TEMPLATE, entraOid, selectedFirmId, isUserManager ? "External User Manager" : "Provider User");
    }
}
