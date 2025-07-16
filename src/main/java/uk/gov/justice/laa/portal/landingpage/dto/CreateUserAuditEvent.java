package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.List;

public class CreateUserAuditEvent extends AuditEvent {
    private final EntraUser user;
    private final String displayRoles;
    private final String selectedFirm;
    private final List<String> selectedOfficesDisplay;

    private static final String CREATE_USER_TEMPLATE = """
            New user %s created, user id %s, with role %s, office %s, firm %s
            """;

    public CreateUserAuditEvent(CurrentUserDto currentUserDto, EntraUser user,
                                String displayRoles, List<String> selectedOfficesDisplay, String selectedFirm) {
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.user = user;
        this.displayRoles = displayRoles;
        this.selectedFirm = selectedFirm;
        this.selectedOfficesDisplay = selectedOfficesDisplay;
    }

    @Override
    public EventType getEventType() {
        return EventType.CREATE_USER;
    }

    @Override
    public String getDescription() {
        String userName = user.getFirstName() + " " + user.getLastName();
        String offices = String.join(", ", selectedOfficesDisplay);
        return String.format(CREATE_USER_TEMPLATE, userName, user.getId(), displayRoles, offices, selectedFirm);
    }
}