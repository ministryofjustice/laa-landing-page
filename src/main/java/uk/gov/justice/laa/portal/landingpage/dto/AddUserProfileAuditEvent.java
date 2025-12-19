package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Getter;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
public class AddUserProfileAuditEvent extends AuditEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final String UPDATE_USER_ROLE_TEMPLATE
            = "New user profile with id %s added to user id %s, for the firm %s, added by %s and added %s (%s)";
    private final EntraUserDto user;
    private final UUID newUserProfileId;
    private final UUID firmId;
    private final String field;
    private final String changeString;

    public AddUserProfileAuditEvent(CurrentUserDto currentUserDto, UUID newUserProfileId, EntraUserDto user,
                                    UUID firmId, String field, String changeString) {
        this.newUserProfileId = newUserProfileId;
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.user = user;
        this.firmId = firmId;
        this.field = field;
        this.changeString = changeString;
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_USER;
    }

    @Override
    public String getDescription() {
        return String.format(UPDATE_USER_ROLE_TEMPLATE, newUserProfileId, user.getId(), firmId, userId, field, changeString);
    }
}
