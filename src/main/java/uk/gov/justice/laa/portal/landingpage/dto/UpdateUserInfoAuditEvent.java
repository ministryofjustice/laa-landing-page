package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Getter;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.io.Serial;
import java.io.Serializable;

@Getter
public class UpdateUserInfoAuditEvent extends AuditEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private final String userOid;
    private final String actorOid;
    private final String actorProfileId;

    private static final String UPDATE_USER_INFO_TEMPLATE = """
            User details updated for existing user entra oid: %s by user entra oid: %s profile id: %s
            """;

    public UpdateUserInfoAuditEvent(EntraUser user, String actorOid, String actorProfileId) {
        this.userId = user.getId();
        this.userOid = user.getEntraOid();
        this.actorOid = actorOid;
        this.actorProfileId = actorProfileId;
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_USER;
    }

    @Override
    public String getDescription() {
        return String.format(UPDATE_USER_INFO_TEMPLATE,
                userOid,
                actorOid,
                actorProfileId
        );

    }
}
