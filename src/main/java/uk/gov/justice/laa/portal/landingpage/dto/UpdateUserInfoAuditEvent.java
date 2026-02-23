package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Getter;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
public class UpdateUserInfoAuditEvent extends AuditEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private final EntraUser user;
    private final EntraUser updatedUser;

    private static final String UPDATE_USER_INFO_TEMPLATE = """
            Existing user id %s updated, email: %s, first name: %s, last name: %s, with %s %s %s
            """;

    public UpdateUserInfoAuditEvent(EntraUser user, EntraUser updatedUser) {
        this.user = user;
        this.updatedUser = updatedUser;
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_USER;
    }

    @Override
    public String getDescription() {
        return String.format(UPDATE_USER_INFO_TEMPLATE,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                updatedUser.getEmail(),
                updatedUser.getFirstName(),
                updatedUser.getLastName());
    }
}
