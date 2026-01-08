package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.config.NotificationsProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    private static final String REFERENCE_TEMPLATE_ADD_MF_PROFILE = "laa-portal-notice-of-delegate-firm-access-%s";
    private static final String REFERENCE_TEMPLATE_REVOKE_FIRM_ACCESS = "laa-portal-notice-of-revoke-firm-access-%s";
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

    public void notifyDeleteFirmAccess(UUID userProfileId, String firstName, String email, String firmName) {
        log.info("Starting Multi Firm Profile creation notification for User: {}", userProfileId);
        if (null != email) {
            emailService.sendMail(
                    email,
                    notificationProperties.getDelegateFirmAccessEmailTemplate(),
                    Map.of("first_name", firstName, "firm_name", firmName),
                    String.format(
                            REFERENCE_TEMPLATE_ADD_MF_PROFILE,
                            firstName
                    )
            );
            log.info("Multi Firm profile created notification sent to: {} for User ID: {}", email, userProfileId);
        }
    }

    public void notifyRevokeFirmAccess(UUID userProfileId, String firstName, String email, String firmName) {
        log.info("Sending revoke firm access notification for User: {}", userProfileId);
        if (null != email) {
            emailService.sendMail(
                    email,
                    notificationProperties.getRevokeFirmAccessEmailTemplate(),
                    Map.of("first_name", firstName, "firm_name", firmName),
                    String.format(
                            REFERENCE_TEMPLATE_REVOKE_FIRM_ACCESS,
                            firstName
                    )
            );
            log.info("Revoke firm access notification sent to: {} for User ID: {}", email, userProfileId);
        }
    }

    public Map<String, String> addProperties(String username, String invitationUrl) {

        customProps.put(USER_NAME, username);
        customProps.put(INVITATION_URL, invitationUrl);
        customProps.put(PORTAL_URL, notificationProperties.getPortalUrl());
        return customProps;
    }
}
