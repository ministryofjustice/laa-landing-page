package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.List;

public class UpdateRoleAuditEvent extends AuditEvent {

    private final EntraUserDto user;
    private final List<String> selectedRoles;
    private static final String UPDATE_USER_ROLE_TEMPLATE = """
            Existing user %s updated, user id %s, with new role %s
            """;

    public UpdateRoleAuditEvent(CurrentUserDto currentUserDto, EntraUserDto user, List<String> selectedRoles) {
        this.userId = currentUserDto.getUserId();
        this.userName = currentUserDto.getName();
        this.user = user;
        this.selectedRoles = selectedRoles;
    }

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_USER;
    }

    @Override
    public String getDescription() {
        String userName = user.getFullName();
        String roles = String.join(", ", selectedRoles);
        return String.format(UPDATE_USER_ROLE_TEMPLATE, userName, user.getId(), roles);
    }
}
