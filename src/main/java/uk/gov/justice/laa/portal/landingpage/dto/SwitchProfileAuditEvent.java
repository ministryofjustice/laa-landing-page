package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public class SwitchProfileAuditEvent extends AuditEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String oldFirm;
    private final String newFirm;

    private static final String SWITCH_FIRM_TEMPLATE = """
            User firm switched, user id %s, from firm %s to firm %s
            """;

    public SwitchProfileAuditEvent(UUID userId, String oldFirm, String newFirm) {
        this.userId = userId;
        this.oldFirm = oldFirm;
        this.newFirm = newFirm;
    }

    @Override
    public EventType getEventType() {
        return EventType.SWITCH_FIRM;
    }

    @Override
    public String getDescription() {
        return String.format(SWITCH_FIRM_TEMPLATE, userId, oldFirm, newFirm);
    }
}
