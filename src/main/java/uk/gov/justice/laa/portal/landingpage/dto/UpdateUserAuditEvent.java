package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Getter;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
public class UpdateUserAuditEvent extends AuditEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private final EntraUserDto user;
    private final String field;
    private final String changeString;
    private final UUID profileId;
    private static final String UPDATE_USER_ROLE_TEMPLATE = """
            Existing entra oid %s, user profile id %s updated, with %s %s
            """;

    public UpdateUserAuditEvent(UUID profileId, CurrentUserDto currentUserDto, EntraUserDto user, String changeString, String field) {
        this.profileId = profileId;
        this.userId = currentUserDto.getUserId();
        this.user = user;
        this.field = field;
        this.changeString = changeString;
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_USER;
    }

    @Override
    public String getDescription() {
        return String.format(UPDATE_USER_ROLE_TEMPLATE, user.getEntraOid(), profileId, field, changeString);
    }
}
