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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    public void notifyDelegateFirmAccessShouldNotSendMailWhenTemplateIsNone() {
        // Arrange
        NotificationsProperties notificationsProperties = new NotificationsProperties();
        notificationsProperties.setDelegateFirmAccessEmailTemplate("none");
        notificationService = new NotificationService(emailService, notificationsProperties);

        UUID userProfileId = UUID.randomUUID();
        String firstName = "Bob";
        String email = "test@email.com"; // <- important
        String firmName = "Fabrikam Inc";
        // Add list appender to logger to capture and verify logs
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // Act
        notificationService.notifyDeleteFirmAccess(userProfileId, firstName, email, firmName);

        // Assert – emailService must not be called
        Mockito.verify(emailService, Mockito.times(0)).sendMail(any(), any(), any(), any());
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());

        assertThat(infoLogs)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly(
                        String.format("Email template for delegate firm access is not ready, skipping notification email for User: %s", userProfileId));
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
                        String.format("Revoke firm access notification sent for User ID: %s", userProfileId));
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

    @Test
    public void notifyRevokeFirmAccessShouldNotSendMailWhenTemplateIsNone() {
        // Arrange
        NotificationsProperties notificationsProperties = new NotificationsProperties();
        notificationsProperties.setRevokeFirmAccessEmailTemplate("none");
        notificationService = new NotificationService(emailService, notificationsProperties);

        UUID userProfileId = UUID.randomUUID();
        String firstName = "Bob";
        String email = "test@email.com"; // <- important
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
                        String.format("Email template for revoke firm access is not ready, skipping notification email for User: %s", userProfileId));
    }

    private static NotificationsProperties buildTestNotificationsProperties() {
        NotificationsProperties notificationsProperties = new NotificationsProperties();
        notificationsProperties.setPortalUrl("testPortalUrl");
        notificationsProperties.setGovNotifyApiKey("testGovNotifyApiKey");
        notificationsProperties.setAddNewUserEmailTemplate("testAddNewUserEmailTemplate");
        notificationsProperties.setRevokeFirmAccessEmailTemplate("testRevokeFirmAccessEmailTemplate");
        notificationsProperties.setDelegateFirmAccessEmailTemplate("testDelegateFirmAccessEmailTemplate");
        notificationsProperties.setUserAccessChangeEmailTemplate("testUserAccessChangeEmailTemplate");
        return notificationsProperties;
    }

    @Test
    void notifyUserAccessChangeShouldSendMailWhenEmailPresent() {
        // Arrange
        NotificationsProperties notificationsProperties = buildTestNotificationsProperties();
        notificationService = new NotificationService(emailService, notificationsProperties);

        UUID userProfileId = UUID.randomUUID();
        String firstName = "Bob";
        String email = "bob@example.com";
        String changeType = "Service roles";
        String changes = "Added: Role1, Removed: Role2";

        // Act
        notificationService.notifyUserAccessChange(userProfileId, firstName, email, changeType, changes);

        // Assert - emailService should be called once
        Mockito.verify(emailService, Mockito.times(1)).sendMail(
                eq(email),
                eq("testUserAccessChangeEmailTemplate"),
                eq(Map.of("first_name", firstName, "change_type", changeType, "changes", changes, "portal_url", "testPortalUrl")),
                eq(String.format("laa-portal-notice-of-access-change-%s", userProfileId))
        );
    }

    @Test
    void notifyUserAccessChangeShouldNotSendMailWhenEmailIsNull() {
        // Arrange
        NotificationsProperties notificationsProperties = buildTestNotificationsProperties();
        notificationService = new NotificationService(emailService, notificationsProperties);

        UUID userProfileId = UUID.randomUUID();
        String firstName = "Bob";
        String email = null;
        String changeType = "Offices";
        String changes = "Added: Office1";

        // Act
        notificationService.notifyUserAccessChange(userProfileId, firstName, email, changeType, changes);

        // Assert
        Mockito.verify(emailService, Mockito.times(1)).sendMail(
                eq(null),
                eq("testUserAccessChangeEmailTemplate"),
                eq(Map.of("first_name", firstName, "change_type", changeType, "changes", changes, "portal_url", "testPortalUrl")),
                eq(String.format("laa-portal-notice-of-access-change-%s", userProfileId))
        );
    }

    @Test
    void notifyUserAccessChangeShouldNotSendMailWhenTemplateIsPlaceholder() {
        // Arrange
        NotificationsProperties notificationsProperties = buildTestNotificationsProperties();
        notificationsProperties.setUserAccessChangeEmailTemplate("none");
        notificationService = new NotificationService(emailService, notificationsProperties);

        UUID userProfileId = UUID.randomUUID();
        String firstName = "Bob";
        String email = "test@email.com";
        String changeType = "Access granted";
        String changes = "You have been granted access to services and offices";

        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // Act
        notificationService.notifyUserAccessChange(userProfileId, firstName, email, changeType, changes);

        // Assert
        Mockito.verify(emailService, Mockito.times(0)).sendMail(any(), any(), any(), any());
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());

        assertThat(infoLogs)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly(
                        String.format("Email template for user access change is not ready, skipping notification email for User: %s", userProfileId));
    }

    @Test
    void notifyUserAccessChangeShouldLogCorrectlyWhenEmailIsNull() {
        // Arrange
        NotificationsProperties notificationsProperties = buildTestNotificationsProperties();
        notificationService = new NotificationService(emailService, notificationsProperties);

        UUID userProfileId = UUID.randomUUID();
        String firstName = "Charlie";
        String email = null;
        String changeType = "Services";
        String changes = "Removed: Service A, Added: Service B";

        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // Act
        notificationService.notifyUserAccessChange(userProfileId, firstName, email, changeType, changes);

        // Assert – emailService is called even when email is null
        Mockito.verify(emailService, Mockito.times(1)).sendMail(
                eq(null),
                eq("testUserAccessChangeEmailTemplate"),
                eq(Map.of("first_name", firstName, "change_type", changeType, "changes", changes, "portal_url", "testPortalUrl")),
                eq(String.format("laa-portal-notice-of-access-change-%s", userProfileId))
        );

        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(2, infoLogs.size());

        // Verify the log message content
        assertThat(infoLogs)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly(
                        String.format("Sending user access change notification for User: %s (change type: %s)", userProfileId, changeType),
                        String.format("User access change notification sent for User ID: %s", userProfileId));
    }
}
