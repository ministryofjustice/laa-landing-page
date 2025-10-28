package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.UUID;

import lombok.Getter;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

@Getter
public class ConvertToMultiFirmAuditEvent extends AuditEvent {

    private final UUID convertedUserId;
    private final String convertedUserEmail;
    private final String convertedUserName;

    private static final String CONVERT_TO_MULTI_FIRM_TEMPLATE = """
            User converted to multi-firm by %s (ID: %s). Converted user: %s (email: %s, converted user id %s)
            """;

    public ConvertToMultiFirmAuditEvent(CurrentUserDto currentUserDto, EntraUserDto convertedUser) {
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.convertedUserId = UUID.fromString(convertedUser.getId());
        this.convertedUserEmail = convertedUser.getEmail();
        this.convertedUserName = convertedUser.getFullName();
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_USER;
    }

    @Override
    public String getDescription() {
        return String.format(CONVERT_TO_MULTI_FIRM_TEMPLATE,
                userName,
                userId,
                convertedUserName,
                convertedUserEmail,
                convertedUserId);
    }
}
