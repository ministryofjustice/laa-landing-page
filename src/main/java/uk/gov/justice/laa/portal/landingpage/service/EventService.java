package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.Event;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private static final String EVENT_TEMPLATE = """
            %s: Audit event %s, by User %s with user id %s, %s
            """;
    private static final String CREATE_USER_TEMPLATE = """
            New user %s created, user id %s, with role %s, office %s, firm %s
            """;
    private static final String UPDATE_USER_ROLE_TEMPLATE = """
            Existing user %s updated, user id %s, with new role %s
            """;
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    public void auditUserCreate(CurrentUserDto currentUserDto, EntraUser user,
                                List<String> selectedRoles, List<String> selectedOfficesDisplay, String selectedFirm) {
        String userName = user.getUserName();
        String roles = String.join(", ", selectedRoles);
        String offices = String.join(", ", selectedOfficesDisplay);
        String description = String.format(CREATE_USER_TEMPLATE, userName, user.getId(), roles, offices, selectedFirm);
        Event createEvent = auditEvent(currentUserDto.getUserId(), currentUserDto.getName(), EventType.CREATE_USER, description);
        logEvent(createEvent);
    }

    public void auditUpdateRole(CurrentUserDto currentUserDto, EntraUserDto user, List<String> selectedRoles) {
        String userName = user.getFullName();
        String roles = String.join(", ", selectedRoles);
        String description = String.format(UPDATE_USER_ROLE_TEMPLATE, userName, user.getId(), roles);
        Event createEvent = auditEvent(currentUserDto.getUserId(), currentUserDto.getName(), EventType.UPDATE_USER, description);
        logEvent(createEvent);
    }

    protected Event auditEvent(UUID userId, String userName, EventType eventType, String description) {
        LocalDateTime now = LocalDateTime.now();
        Event event = new Event();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setDescription(description);
        event.setCreatedDate(now);
        event.setUserName(userName);
        return event;
    }

    protected void logEvent(Event event) {
        String dateTime = event.getCreatedDate().format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
        String eventString = String.format(EVENT_TEMPLATE, dateTime, event.getEventType().toString(),
                event.getUserName(), event.getUserId().toString(), event.getDescription());
        logger.info(eventString);
    }
}
