package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.config.NotificationsProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * A service to handle email notification specifically for new user creation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final NotificationsProperties notificationProperties;
    private final Map<String, String> customProps = new HashMap<>();
    private static final String REFERENCE_TEMPLATE_NEW_USER = "laa-portal-notice-of-new-user-%s";
    private static final String USER_NAME = "name";
    private static final String INVITATION_URL = "invitationURL";
    private static final String PORTAL_URL = "portalURL";

    public void notifyCreateUser(String name, String email, String invitationUrl) {
        log.info("Starting add new user notification for User ID: {}", name);
        if (null != email) {
            emailService.sendMail(
                    email,
                    notificationProperties.getAddNewUserEmailTemplate(),
                    addProperties(name, invitationUrl),
                    String.format(
                            REFERENCE_TEMPLATE_NEW_USER,
                            name
                    )
            );
            log.info("Welcome user notification sent to: {} for User ID: {}", email, name);
        }
    }

    public Map<String, String> addProperties(String username, String invitationUrl) {

        customProps.put(USER_NAME, username);
        customProps.put(INVITATION_URL, invitationUrl);
        customProps.put(PORTAL_URL, notificationProperties.getPortalUrl());
        return customProps;
    }
}
