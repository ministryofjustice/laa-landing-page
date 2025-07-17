package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.List;

public class CreateUserAuditEvent extends AuditEvent {
    private final EntraUser user;
    private final String selectedFirm;
    private final UserType userType;

    private static final String CREATE_USER_TEMPLATE = """
            New user %s created, user id %s, with firm %s and user type %s
            """;

    public CreateUserAuditEvent(CurrentUserDto currentUserDto, EntraUser user,
                                String selectedFirm, UserType userType) {
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.user = user;
        this.selectedFirm = selectedFirm;
        this.userType = userType;
    }

    @Override
    public EventType getEventType() {
        return EventType.CREATE_USER;
    }

    @Override
    public String getDescription() {
        String userName = user.getFirstName() + " " + user.getLastName();
        return String.format(CREATE_USER_TEMPLATE, userName, user.getId(), selectedFirm, userType.getFriendlyName());
    }
}