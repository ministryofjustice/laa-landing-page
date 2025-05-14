package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.service.notify.NotificationClient;

/**
 * Config class for setting up beans related to notifications
 */
@Configuration
public class NotificationsConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "notifications")
    public NotificationsProperties notificationsProperties() {
        return new NotificationsProperties();
    }

    @Bean
    public NotificationClient notificationClient(NotificationsProperties notificationsProperties) {
        return new NotificationClient(notificationsProperties.getGovNotifyApiKey());
    }

}
