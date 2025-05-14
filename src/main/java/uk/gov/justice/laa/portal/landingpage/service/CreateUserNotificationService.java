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
public class CreateUserNotificationService {

    private final NotificationService notificationService;
    private final NotificationsProperties notificationProperties;
    private final Map<String, String> customProps = new HashMap<>();
    private static final String REFERENCE_TEMPLATE_NEW_USER = "laa-portal-notice-of-new-user-%s";
    private static final String USER_NAME = "name";
    private static final String PASSWORD = "password";
    private static final String PORTAL_URL = "portalURL";

    public void notifyCreateUser(String username, String email, String password, String userId) {
        log.info("Starting add new user notification for User ID: {}", userId);
        if (null != email) {
            notificationService.sendMail(
                    email,
                    notificationProperties.getAddNewUserEmailTemplate(),
                    addProperties(username, password),
                    String.format(
                            REFERENCE_TEMPLATE_NEW_USER,
                            userId
                    )
            );
            log.info("Welcome user notification sent to: {} for User ID: {}", email, userId);
        }
    }

    public Map<String, String> addProperties(String username, String password) {

        customProps.put(USER_NAME, username);
        customProps.put(PASSWORD, password);
        customProps.put(PORTAL_URL, notificationProperties.getPortalUrl());
        return customProps;
    }
}
