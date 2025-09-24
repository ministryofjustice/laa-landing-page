package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Getter;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public class UpdateUserAuditEvent extends AuditEvent implements Serializable {

    private final EntraUserDto user;
    private final String field;
    private final String changeString;
    private final UUID profileId;
    private static final String UPDATE_USER_ROLE_TEMPLATE = """
            Existing user id %s updated, profile id %s, with %s %s
            """;

    public UpdateUserAuditEvent(UUID profileId, CurrentUserDto currentUserDto, EntraUserDto user, String changeString, String field) {
        this.profileId = profileId;
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
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
        String userName = user.getFullName();
        return String.format(UPDATE_USER_ROLE_TEMPLATE, user.getId(), profileId, field, changeString);
    }
}
