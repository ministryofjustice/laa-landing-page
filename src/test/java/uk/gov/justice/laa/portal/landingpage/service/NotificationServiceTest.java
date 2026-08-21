package uk.gov.justice.laa.portal.landingpage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring.addListAppenderToLogger;

/**
 * Unit tests for the NotificationService class
 */
@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private EmailService emailService;

    private NotificationService notificationService;

    private static final String ACTOR_USER_ID = "actor-123";
    private static final String ACTOR_USER_UUID = "11111111-1111-1111-1111-111111111111";
    private static final String RECIPIENT_FIRST_NAME = "John";
    private static final String RECIPIENT_EMAIL = "john.doe@example.com";
    private static final String RECIPIENT_ID = "recipient-456";
    private static final String TARGET_USER_PROFILE_ID = "target-profile-789";
    private static final String TARGET_EMAIL = "target.user@example.com";

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
        String url = "url.com";
        // Add list appender to logger to capture and verify logs
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // When
        notificationService.notifyCreateUser(username, userId, email, url);

        // Then
        // Check send mail was invoked and two info logs were generated.
        verify(emailService, Mockito.times(1)).sendMail(any(), any(), any(), any());
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
        notificationService.notifyCreateUser(username, userId, email, userId);

        // Then
        // Check send mail was not invoked and only one info log was generated.
        verify(emailService, Mockito.times(0)).sendMail(any(), any(), any(), any());
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
        verify(emailService, Mockito.times(1)).sendMail(any(), any(), any(), any());
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
        verify(emailService, Mockito.times(0)).sendMail(any(), any(), any(), any());
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
        verify(emailService, Mockito.times(0)).sendMail(any(), any(), any(), any());
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
        verify(emailService, Mockito.times(1)).sendMail(any(), any(), any(), any());
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
        verify(emailService, Mockito.times(0)).sendMail(any(), any(), any(), any());
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
        verify(emailService, Mockito.times(0)).sendMail(any(), any(), any(), any());
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
        notificationsProperties.setReactivationRequestSubmittedEmailTemplate("testReactivationRequestSubmittedEmailTemplate");
        notificationsProperties.setReactivationRequestInfoRequestedEmailTemplate("testReactivationRequestInfoRequestedEmailTemplate");
        notificationsProperties.setReactivationRequestApprovedEmailTemplate("testReactivationRequestApprovedEmailTemplate");
        notificationsProperties.setReactivationRequestRejectedEmailTemplate("testReactivationRequestRejectedEmailTemplate");
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
        String firmName = "Test Firm";

        // Act
        notificationService.notifyUserAccessChange(userProfileId, firstName, email, firmName);

        // Assert - emailService should be called once
        verify(emailService, Mockito.times(1)).sendMail(
                eq(email),
                eq("testUserAccessChangeEmailTemplate"),
                eq(Map.of("first_name", firstName, "firm_name", firmName)),
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
        String firmName = "Test Firm";

        // Act
        notificationService.notifyUserAccessChange(userProfileId, firstName, email, firmName);

        // Assert
        Mockito.verify(emailService, Mockito.times(0)).sendMail(any(), any(), any(), any());
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
        String firmName = "Test Firm";

        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // Act
        notificationService.notifyUserAccessChange(userProfileId, firstName, email, firmName);

        // Assert
        verify(emailService, Mockito.times(0)).sendMail(any(), any(), any(), any());
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
        String firmName = "Test Firm";

        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // Act
        notificationService.notifyUserAccessChange(userProfileId, firstName, email, firmName);

        // Assert – emailService is not called when email is null
        Mockito.verify(emailService, Mockito.times(0)).sendMail(any(), any(), any(), any());

        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());

        // Verify the log message content
        assertThat(infoLogs)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly(
                        String.format("No email address provided, skipping access change notification for User: %s", userProfileId));
    }


    @Nested
    @DisplayName("notifyReactivationRequestSubmitted Tests")
    class NotifyReactivationRequestSubmittedTests {

        private static final String SUBMIT_TEMPLATE_ID = "testReactivationRequestSubmittedEmailTemplate";

        @Test
        @DisplayName("Should send email when recipientEmail is provided")
        void shouldSendEmail_WhenRecipientEmailIsNotNull() {
            // When
            notificationService.notifyReactivationRequestSubmitted(
                    ACTOR_USER_ID, RECIPIENT_FIRST_NAME, RECIPIENT_EMAIL, RECIPIENT_ID, TARGET_USER_PROFILE_ID, TARGET_EMAIL
            );

            // Then
            verify(emailService).sendMail(
                    RECIPIENT_EMAIL,
                    SUBMIT_TEMPLATE_ID,
                    Map.of("first_name", RECIPIENT_FIRST_NAME, "email", TARGET_EMAIL),
                    "laa-portal-notice-of-delegate-reactivation-request-submit-" + TARGET_USER_PROFILE_ID
            );
            verifyNoMoreInteractions(emailService);
        }

        @Test
        @DisplayName("Should skip sending email when recipientEmail is null")
        void shouldSkipEmail_WhenRecipientEmailIsNull() {
            // When
            notificationService.notifyReactivationRequestSubmitted(
                    ACTOR_USER_ID, RECIPIENT_FIRST_NAME, null, RECIPIENT_ID, TARGET_USER_PROFILE_ID, TARGET_EMAIL
            );

            // Then
            verifyNoInteractions(emailService);
        }
    }

    @Nested
    @DisplayName("notifyReactivationRequestInfoRequested Tests")
    class NotifyReactivationRequestInfoRequestedTests {

        private static final String INFO_REQ_TEMPLATE_ID = "testReactivationRequestInfoRequestedEmailTemplate";

        @Test
        @DisplayName("Should send email when recipientEmail is provided")
        void shouldSendEmail_WhenRecipientEmailIsNotNull() {
            // When
            notificationService.notifyReactivationRequestInfoRequested(
                    ACTOR_USER_ID, RECIPIENT_FIRST_NAME, RECIPIENT_EMAIL, RECIPIENT_ID, TARGET_USER_PROFILE_ID, TARGET_EMAIL
            );

            // Then
            verify(emailService).sendMail(
                    RECIPIENT_EMAIL,
                    INFO_REQ_TEMPLATE_ID,
                    Map.of("first_name", RECIPIENT_FIRST_NAME, "email", TARGET_EMAIL),
                    "laa-portal-notice-of-reactivation-request-info-req-" + TARGET_USER_PROFILE_ID
            );
            verifyNoMoreInteractions(emailService);
        }

        @Test
        @DisplayName("Should skip sending email when recipientEmail is null")
        void shouldSkipEmail_WhenRecipientEmailIsNull() {
            // When
            notificationService.notifyReactivationRequestInfoRequested(
                    ACTOR_USER_ID, RECIPIENT_FIRST_NAME, null, RECIPIENT_ID, TARGET_USER_PROFILE_ID, TARGET_EMAIL
            );

            // Then
            verifyNoInteractions(emailService);
        }
    }

    @Nested
    @DisplayName("notifyReactivationRequestApproved Tests")
    class NotifyReactivationRequestApprovedTests {

        private static final String APPROVED_TEMPLATE_ID = "testReactivationRequestApprovedEmailTemplate";

        @Test
        @DisplayName("Should send email when recipientEmail is provided")
        void shouldSendEmail_WhenRecipientEmailIsNotNull() {
            // When
            notificationService.notifyReactivationRequestApproved(
                    ACTOR_USER_ID, RECIPIENT_FIRST_NAME, RECIPIENT_EMAIL, RECIPIENT_ID, TARGET_USER_PROFILE_ID, TARGET_EMAIL
            );

            // Then
            verify(emailService).sendMail(
                    RECIPIENT_EMAIL,
                    APPROVED_TEMPLATE_ID,
                    Map.of("first_name", RECIPIENT_FIRST_NAME, "email", TARGET_EMAIL),
                    "laa-portal-notice-of-reactivation-request-approved-" + TARGET_USER_PROFILE_ID
            );
            verifyNoMoreInteractions(emailService);
        }

        @Test
        @DisplayName("Should skip sending email when recipientEmail is null")
        void shouldSkipEmail_WhenRecipientEmailIsNull() {
            // When
            notificationService.notifyReactivationRequestApproved(
                    ACTOR_USER_ID, RECIPIENT_FIRST_NAME, null, RECIPIENT_ID, TARGET_USER_PROFILE_ID, TARGET_EMAIL
            );

            // Then
            verifyNoInteractions(emailService);
        }
    }

    @Nested
    @DisplayName("notifyReactivationRequestRejected Tests")
    class NotifyReactivationRequestRejectedTests {

        private static final String REJECTED_TEMPLATE_ID = "testReactivationRequestRejectedEmailTemplate";

        @Test
        @DisplayName("Should send email when recipientEmail is provided")
        void shouldSendEmail_WhenRecipientEmailIsNotNull() {
            // When
            notificationService.notifyReactivationRequestRejected(
                    ACTOR_USER_UUID, RECIPIENT_FIRST_NAME, RECIPIENT_EMAIL, RECIPIENT_ID, TARGET_USER_PROFILE_ID, TARGET_EMAIL
            );

            // Then
            verify(emailService).sendMail(
                    RECIPIENT_EMAIL,
                    REJECTED_TEMPLATE_ID,
                    Map.of("first_name", RECIPIENT_FIRST_NAME, "email", TARGET_EMAIL),
                    "laa-portal-notice-of-reactivation-request-rejected-" + TARGET_USER_PROFILE_ID
            );
            verifyNoMoreInteractions(emailService);
        }

        @Test
        @DisplayName("Should skip sending email when recipientEmail is null")
        void shouldSkipEmail_WhenRecipientEmailIsNull() {
            // When
            notificationService.notifyReactivationRequestRejected(
                    ACTOR_USER_UUID, RECIPIENT_FIRST_NAME, null, RECIPIENT_ID, TARGET_USER_PROFILE_ID, TARGET_EMAIL
            );

            // Then
            verifyNoInteractions(emailService);
        }
    }
}
