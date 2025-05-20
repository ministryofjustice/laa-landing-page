package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.service.notify.NotificationClient;

/**
 * Unit tests for the NotificationsConfiguration class.
 */
public class NotificationsConfigurationTest {

    private NotificationsConfiguration notificationsConfiguration;

    @BeforeEach
    public void setup() {
        notificationsConfiguration = new NotificationsConfiguration();
    }

    @Test
    public void checkNotificationsConfigurationCreatesNewNotificationsPropertiesObject() {
        // When
        NotificationsProperties notificationsProperties = notificationsConfiguration.notificationsProperties();

        // Then
        Assertions.assertNotNull(notificationsProperties);
    }

    @Test
    public void checkNotificationsConfigurationCreatesNewNotificationClientWithProperties() {
        // Given
        NotificationsProperties notificationsProperties = new NotificationsProperties();
        notificationsProperties.setGovNotifyApiKey("testGovNotifyApiKey");

        // When
        NotificationClient notificationClient = notificationsConfiguration.notificationClient(notificationsProperties);

        // Then
        Assertions.assertNotNull(notificationClient);
        Assertions.assertEquals("testGovNotifyApiKey", notificationClient.getApiKey());
    }

}
