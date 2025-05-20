package uk.gov.justice.laa.portal.landingpage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring.addListAppenderToLogger;
import static uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring.getLogsByLevel;

/**
 * Unit tests for the NotificationService class
 */
@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationClient notificationClient;

    @Test
    public void checkSendingEmailRunsWithoutErrors() throws NotificationClientException {
        // Given
        String targetEmail = "test@test.com";
        String emailTemplate = "testTemplate";
        Map<String, String> parameters = new HashMap<>();
        String reference = "testReference";
        // Add list appender to logger to capture and verify logs
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // When
        notificationService.sendMail(targetEmail, emailTemplate, parameters, reference);

        // Then
        // Assert mail is sent with no error logs
        Mockito.verify(notificationClient, Mockito.times(1)).sendEmail(any(), any(), any(), any());
        List<ILoggingEvent> errorLogs = getLogsByLevel(listAppender, Level.ERROR);
        assertEquals(0, errorLogs.size());
    }

    @Test
    public void checkErrorIsLoggedWhenExceptionIsThrown() throws NotificationClientException {
        // Given
        String targetEmail = "test@test.com";
        String emailTemplate = "testTemplate";
        Map<String, String> parameters = new HashMap<>();
        String reference = "testReference";
        // Throw exception when trying to send an email
        Mockito.when(notificationClient.sendEmail(any(), any(), any(), any())).thenThrow(NotificationClientException.class);
        // Add list appender to logger to capture and verify logs
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(NotificationService.class);

        // When
        notificationService.sendMail(targetEmail, emailTemplate, parameters, reference);

        // Then
        // Assert mail is attempted to be sent but throws an error.
        Mockito.verify(notificationClient, Mockito.times(1)).sendEmail(any(), any(), any(), any());
        List<ILoggingEvent> errorLogs = getLogsByLevel(listAppender, Level.ERROR);
        assertEquals(1, errorLogs.size());
    }




}
