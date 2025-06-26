package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.util.Map;

/**
 * A service to send email notifications to users
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final NotificationClient notificationClient;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendMail(
            String targetEmail,
            String emailTemplate,
            Map<String, String> parameters,
            String reference
    )  {
        try {
            log.info("EmailService::sendMail::templateID: {}", emailTemplate);
            notificationClient.sendEmail(emailTemplate, targetEmail, parameters, reference);
        } catch (Exception e) {
            log.error("Error sending mail: {}", e.getMessage());
        }
    }
}
