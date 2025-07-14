package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Getter;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.List;

@Getter
public class UpdateUserAuditEvent extends AuditEvent {

    private final EntraUserDto user;
    private final List<String> changedValues;
    private final String field;
    private static final String UPDATE_USER_ROLE_TEMPLATE = """
            Existing user %s updated, user id %s, with new %s %s
            """;

    public UpdateUserAuditEvent(CurrentUserDto currentUserDto, EntraUserDto user, List<String> changedValues, String field) {
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.user = user;
        this.changedValues = changedValues;
        this.field = field;
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_USER;
    }

    @Override
    public String getDescription() {
        String userName = user.getFullName();
        String changedString = String.join(", ", changedValues);
        return String.format(UPDATE_USER_ROLE_TEMPLATE, userName, user.getId(), field, changedString);
    }
}
