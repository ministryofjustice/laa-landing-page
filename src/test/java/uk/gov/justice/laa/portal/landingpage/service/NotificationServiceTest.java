package uk.gov.justice.laa.portal.landingpage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.config.NotificationsProperties;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring.addListAppenderToLogger;

/**
 * Unit tests for the NotificationService class
 */
@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private EmailService emailService;

    private NotificationService notificationService;

    @BeforeEach
    public void setup() {
        NotificationsProperties notificationsProperties = buildTestNotificationsProperties();
        notificationService = new NotificationService(emailService, notificationsProperties);
    }

    @Test
    public void testUserIsNotifiedWhenEmailIsIncluded() {
        // Given
        String username = "testUser";
        String email = "test@test.com";
        String userId = "testUserId";
        // Add list appender to logger to capture and verify logs
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // When
        notificationService.notifyCreateUser(username, email, userId);

        // Then
        // Check send mail was invoked and two info logs were generated.
        Mockito.verify(emailService, Mockito.times(1)).sendMail(any(), any(), any(), any());
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(2, infoLogs.size());
    }

    @Test
    public void testUserIsNotNotifiedWhenEmailIsNotIncluded() {
        // Given
        String username = "testUser";
        String email = null;
        String userId = "testUserId";
        // Add list appender to logger to capture and verify logs
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // When
        notificationService.notifyCreateUser(username, email, userId);

        // Then
        // Check send mail was not invoked and only one info log was generated.
        Mockito.verify(emailService, Mockito.times(0)).sendMail(any(), any(), any(), any());
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());
    }

    private static NotificationsProperties buildTestNotificationsProperties() {
        NotificationsProperties notificationsProperties = new NotificationsProperties();
        notificationsProperties.setPortalUrl("testPortalUrl");
        notificationsProperties.setGovNotifyApiKey("testGovNotifyApiKey");
        notificationsProperties.setAddNewUserEmailTemplate("testAddNewUserEmailTemplate");
        return notificationsProperties;
    }

}
