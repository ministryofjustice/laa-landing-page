package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.UUID;

import lombok.Getter;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

@Getter
public class ConvertToMultiFirmAuditEvent extends AuditEvent {

    private final UUID convertedEntraOId;
    private final String convertedUserId;

    public ConvertToMultiFirmAuditEvent(CurrentUserDto currentUserDto, EntraUserDto convertedUser, String id) {
        super();
        this.userId = currentUserDto.getUserId();
        this.convertedUserId = id;
        this.convertedEntraOId = UUID.fromString(convertedUser.getEntraOid());
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_USER;
    }

    @Override
    public String getDescription() {
        return String.format("User converted to multi-firm, converted user entra oid %s, user id %s",
                convertedEntraOId, convertedUserId);
    }
}
