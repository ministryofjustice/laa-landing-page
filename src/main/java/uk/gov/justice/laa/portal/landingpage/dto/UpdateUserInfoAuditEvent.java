package uk.gov.justice.laa.portal.landingpage.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

@Getter
public class UpdateUserInfoAuditEvent extends AuditEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private final String userOid;
    private final String actorOid;
    private final String actorProfileId;
    private final List<String> changedFields;

    private static final String UPDATE_USER_INFO_TEMPLATE = """
            User details updated for existing user entra oid: %s by user entra oid: %s profile id: %s. Fields changed: %s
            """;

    public UpdateUserInfoAuditEvent(EntraUser user, String newFirstName, String newLastName, String newEmail,
                                    String actorOid, String actorProfileId) {
        this.userId = user.getId();
        this.userOid = user.getEntraOid();
        this.actorOid = actorOid;
        this.actorProfileId = actorProfileId;
        List<String> fields = new ArrayList<>();
        if (!Objects.equals(user.getFirstName(), newFirstName)) {
            fields.add("firstName");
        }
        if (!Objects.equals(user.getLastName(), newLastName)) {
            fields.add("lastName");
        }
        if (!Objects.equals(user.getEmail(), newEmail)) {
            fields.add("email");
        }
        this.changedFields = fields;
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
                actorProfileId,
                String.join(", ", changedFields)
        );
    }
}
