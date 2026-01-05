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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    public void notifyDeleteFirmAccessShouldSendMailWhenEmailPresent() {
        // Arrange
        UUID userProfileId = UUID.randomUUID();
        String firstName = "Alice";
        String email = "alice@example.com";
        String firmName = "Contoso LLP";
        // Add list appender to logger to capture and verify logs
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // Act
        notificationService.notifyDeleteFirmAccess(userProfileId, firstName, email, firmName);

        // Assert – capture and verify emailService.sendMail() params
        Mockito.verify(emailService, Mockito.times(1)).sendMail(any(), any(), any(), any());
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(2, infoLogs.size());
    }

    @Test
    public void notifyDeleteFirmAccessShouldNotSendMailWhenEmailIsNull() {
        // Arrange
        UUID userProfileId = UUID.randomUUID();
        String firstName = "Bob";
        String email = null; // <- important
        String firmName = "Fabrikam Inc";
        // Add list appender to logger to capture and verify logs
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // Act
        notificationService.notifyDeleteFirmAccess(userProfileId, firstName, email, firmName);

        // Assert – emailService must not be called
        Mockito.verify(emailService, Mockito.times(0)).sendMail(any(), any(), any(), any());
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());
    }

    @Test
    public void notifyRevokeFirmAccessShouldSendMailWhenEmailPresent() {
        // Arrange
        UUID userProfileId = UUID.randomUUID();
        String firstName = "Alice";
        String email = "alice@example.com";
        String firmName = "Contoso LLP";
        // Add list appender to logger to capture and verify logs
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // Act
        notificationService.notifyRevokeFirmAccess(userProfileId, firstName, email, firmName);

        // Assert – capture and verify emailService.sendMail() params
        Mockito.verify(emailService, Mockito.times(1)).sendMail(any(), any(), any(), any());
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(2, infoLogs.size());

        assertThat(infoLogs)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly(
                        String.format("Sending revoke firm access notification for User: %s", userProfileId),
                        String.format("Revoke firm access notification sent to: alice@example.com for User ID: %s", userProfileId));
    }

    @Test
    public void notifyRevokeFirmAccessShouldNotSendMailWhenEmailIsNull() {
        // Arrange
        UUID userProfileId = UUID.randomUUID();
        String firstName = "Bob";
        String email = null; // <- important
        String firmName = "Fabrikam Inc";
        // Add list appender to logger to capture and verify logs
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // Act
        notificationService.notifyRevokeFirmAccess(userProfileId, firstName, email, firmName);

        // Assert – emailService must not be called
        Mockito.verify(emailService, Mockito.times(0)).sendMail(any(), any(), any(), any());
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());

        assertThat(infoLogs)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly(
                        String.format("Sending revoke firm access notification for User: %s", userProfileId));
    }

    private static NotificationsProperties buildTestNotificationsProperties() {
        NotificationsProperties notificationsProperties = new NotificationsProperties();
        notificationsProperties.setPortalUrl("testPortalUrl");
        notificationsProperties.setGovNotifyApiKey("testGovNotifyApiKey");
        notificationsProperties.setAddNewUserEmailTemplate("testAddNewUserEmailTemplate");
        return notificationsProperties;
    }

}
