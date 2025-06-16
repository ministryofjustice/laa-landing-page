package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.models.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.controller.LoginController;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.Event;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private static final String EVENT_TEMPLATE = """
            %s: Audit event %s, by User %s with user id %s, %s
            """;
    private static final String CREATE_USER_TEMPLATE = """
            New user % created, user id %, with role %s
            """;
    private static final String UPDATE_USER_ROLE_TEMPLATE = """
            Existing user % updated, user id %, with new role %s
            """;
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    public void auditUserCreate(CurrentUserDto currentUserDto, User user, List<String> selectedRoles) {
        String userName = user.getGivenName() + " " + user.getSurname();
        String roles = String.join(", ", selectedRoles);
        String description = CREATE_USER_TEMPLATE.formatted(user.getId(), userName, roles);
        Event createEvent = auditEvent(currentUserDto.getUserId(), currentUserDto.getName(), EventType.CREATE_USER, description);
        logEvent(createEvent);
    }

    public void auditUpdateRole(CurrentUserDto currentUserDto, User user, List<String> selectedRoles) {
        String userName = user.getGivenName() + " " + user.getSurname();
        String roles = String.join(", ", selectedRoles);
        String description = UPDATE_USER_ROLE_TEMPLATE.formatted(user.getId(), userName, roles);
        Event createEvent = auditEvent(currentUserDto.getUserId(), currentUserDto.getName(), EventType.CREATE_USER, description);
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
        String eventString = EVENT_TEMPLATE.format(dateTime, event.getEventType().toString(),
                event.getUserName(), event.getUserId().toString(), event.getDescription());
        logger.info(eventString);
    }
}
