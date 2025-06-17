package uk.gov.justice.laa.portal.landingpage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.dto.Event;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring.addListAppenderToLogger;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @InjectMocks
    private EventService eventService;

    @Test
    void auditUserCreate() {
    }

    @Test
    void auditUpdateRole() {
    }

    @Test
    void auditEvent() {
        UUID userId = UUID.randomUUID();
        String userName = "user";
        EventType eventType = EventType.CREATE_USER;
        String description = "test";
        Event event = eventService.auditEvent(userId, userName, eventType, description);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getUserName()).isEqualTo("user");
        assertThat(event.getEventType()).isEqualTo(EventType.CREATE_USER);
        assertThat(event.getDescription()).isEqualTo("test");
        assertThat(event.getCreatedDate()).isNotNull();
    }

    @Test
    void logEvent() {
        LocalDateTime now = LocalDateTime.now();
        UUID userId = UUID.randomUUID();
        Event event = new Event();
        event.setUserId(userId);
        event.setEventType(EventType.CREATE_USER);
        event.setDescription("test description");
        event.setCreatedDate(now);
        event.setUserName("admin");
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(EventService.class);
        eventService.logEvent(event);
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());
        assertThat(infoLogs.get(0).getFormattedMessage()).contains("Audit event CREATE_USER, by User admin with user id");
        assertThat(infoLogs.get(0).getFormattedMessage()).contains("test description");
    }
}