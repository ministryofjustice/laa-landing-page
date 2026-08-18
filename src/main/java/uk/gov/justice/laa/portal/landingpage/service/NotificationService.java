package uk.gov.justice.laa.portal.landingpage.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.config.NotificationsProperties;

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
    private static final String REFERENCE_TEMPLATE_ACCESS_CHANGE = "laa-portal-notice-of-access-change-%s";
    private static final String REFERENCE_TEMPLATE_EXISTING_USER = "laa-portal-notice-of-existing-user-%s";

    private static final String REFERENCE_TEMPLATE_REACTIVATION_REQUEST_SUBMIT = "laa-portal-notice-of-delegate-reactivation-request-submit-%s";
    private static final String REFERENCE_TEMPLATE_REACTIVATION_REQUEST_INFO_REQ = "laa-portal-notice-of-reactivation-request-info-req-%s";
    private static final String REFERENCE_TEMPLATE_REACTIVATION_REQUEST_APPROVED = "laa-portal-notice-of-reactivation-request-approved-%s";
    private static final String REFERENCE_TEMPLATE_REACTIVATION_REQUEST_REJECTED = "laa-portal-notice-of-reactivation-request-rejected-%s";

    private static final String USER_NAME = "name";
    private static final String INVITATION_URL = "invitationURL";
    private static final String PORTAL_URL = "portalURL";

    public void notifyCreateUser(String name, String userId, String email, String invitationUrl) {
        log.info("Starting add new user notification for User ID: {}", userId);
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
            log.info("Welcome user notification sent to user: {}", userId);
        }
    }

    public void notifyDeleteFirmAccess(UUID userProfileId, String firstName, String email, String firmName) {
        if ("NONE".equalsIgnoreCase(notificationProperties.getDelegateFirmAccessEmailTemplate())) {
            log.info("Email template for delegate firm access is not ready, skipping notification email for User: {}", userProfileId);
            return;
        }

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
            log.info("Multi Firm profile created notification sent for User ID: {}", userProfileId);
        }
    }

    public void notifyRevokeFirmAccess(UUID userProfileId, String firstName, String email, String firmName) {
        if ("NONE".equalsIgnoreCase(notificationProperties.getRevokeFirmAccessEmailTemplate())) {
            log.info("Email template for revoke firm access is not ready, skipping notification email for User: {}", userProfileId);
            return;
        }

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
            log.info("Revoke firm access notification sent for User ID: {}", userProfileId);
        }
    }

    public void notifyUserAccessChange(UUID userProfileId, String firstName, String email, String firmName) {
        if ("NONE".equalsIgnoreCase(notificationProperties.getUserAccessChangeEmailTemplate())) {
            log.info("Email template for user access change is not ready, skipping notification email for User: {}", userProfileId);
            return;
        }

        if (email == null || email.isBlank()) {
            log.info("No email address provided, skipping access change notification for User: {}", userProfileId);
            return;
        }

        log.info("Sending user access change notification for User: {}", userProfileId);

        emailService.sendMail(
                email,
                notificationProperties.getUserAccessChangeEmailTemplate(),
                Map.of("first_name", firstName, "firm_name", firmName),
                String.format(
                        REFERENCE_TEMPLATE_ACCESS_CHANGE,
                        userProfileId
                )
        );
        log.info("User access change notification sent for User ID: {}", userProfileId);
    }


    public void notifyExistingUser(UUID userProfileId, String firstName, String email) {
        log.info("Sending existing user notification for User: {}", userProfileId);
        if (null != email) {
            emailService.sendMail(
                    email,
                    notificationProperties.getExistingUserEmailTemplate(),
                    Map.of("first_name", firstName, "email", email, "portal_url", notificationProperties.getPortalUrl()),
                    String.format(
                            REFERENCE_TEMPLATE_EXISTING_USER,
                            firstName
                    )
            );
            log.info("Existing user notification email sent for User ID: {}", userProfileId);
        }
    }

    public void notifyReactivationRequestSubmitted(String actorUserId, String recipientFirstName, String recipientEmail, String recipientId, String targetUserProfileId, String targetEmail) {
        log.info("Starting submit user reactivation request notification by Actor User ID {} for User Profile ID: {} to User ID: {}",
                actorUserId, targetUserProfileId, recipientId);
        if (null != recipientEmail) {
            emailService.sendMail(
                    recipientEmail,
                    notificationProperties.getReactivationRequestSubmittedEmailTemplate(),
                    Map.of("first_name", recipientFirstName,
                            "email", targetEmail),
                    String.format(
                            REFERENCE_TEMPLATE_REACTIVATION_REQUEST_SUBMIT,
                            targetUserProfileId
                    )
            );
            log.info("Reactivate user request by User ID {} on User Profile ID {} notification sent to User ID: {}",
                    actorUserId, targetUserProfileId, recipientId);
        } else {
            log.info("Skipping the submit user reactivation request notification by Actor User ID {} for User Profile ID: {} "
                    + "to User ID: {}, because email is empty.", actorUserId, targetUserProfileId, recipientId);
        }
    }

    public void notifyReactivationRequestInfoRequested(String actorUserId, String recipientFirstName, String recipientEmail, String recipientId, String targetUserProfileId, String targetEmail) {
        log.info("Reactivate request - Starting more info requested for reactivation request notification by Actor User ID {} for User Profile ID: {}",
                actorUserId, targetUserProfileId);
        if (null != recipientEmail) {
            emailService.sendMail(
                    recipientEmail,
                    notificationProperties.getReactivationRequestInfoRequestedEmailTemplate(),
                    Map.of("first_name", recipientFirstName,
                            "email", targetEmail),
                    String.format(
                            REFERENCE_TEMPLATE_REACTIVATION_REQUEST_INFO_REQ,
                            targetUserProfileId
                    )
            );
            log.info("Reactivate request - More info requested by User ID {} on User Profile ID {} notification sent to User ID: {}",
                    actorUserId, targetUserProfileId, recipientId);
        } else {
            log.info("Reactivate request - Skipping More info requested by User ID {} on User Profile ID {} notification sent to User ID: {}, because email id empty.",
                    actorUserId, targetUserProfileId, recipientId);
        }
    }

    public void notifyReactivationRequestApproved(String actorUserId, String recipientFirstName, String recipientEmail, String recipientId, String targetUserProfileId, String targetEmail) {
        log.info("Reactivate request - Starting request approved notification by Actor User ID {} for User Profile ID: {} to User ID: {}",
                actorUserId, targetUserProfileId, recipientId);
        if (null != recipientEmail) {
            emailService.sendMail(
                    recipientEmail,
                    notificationProperties.getReactivationRequestApprovedEmailTemplate(),
                    Map.of("first_name", recipientFirstName,
                            "email", targetEmail),
                    String.format(
                            REFERENCE_TEMPLATE_REACTIVATION_REQUEST_APPROVED,
                            targetUserProfileId
                    )
            );
            log.info("Reactivate request - Request approved by User ID {} on User Profile ID {} notification sent to User ID: {}",
                    actorUserId, targetUserProfileId, recipientId);
        } else {
            log.info("Reactivate request - Skipping Request approved by User ID {} on User Profile ID {} notification sent to User ID: {}, because email is empty.",
                    actorUserId, targetUserProfileId, recipientId);
        }
    }

    public void notifyReactivationRequestRejected(UUID actorUserId, String recipientFirstName, String recipientEmail, String recipientId, String targetUserProfileId, String targetEmail) {
        log.info("Reactivate request - Starting request rejected notification by Actor User ID {} for User Profile ID: {} to User ID: {}",
                actorUserId, targetUserProfileId, recipientId);
        if (null != recipientEmail) {
            emailService.sendMail(
                    recipientEmail,
                    notificationProperties.getReactivationRequestRejectedEmailTemplate(),
                    Map.of("first_name", recipientFirstName,
                            "email", targetEmail),
                    String.format(
                            REFERENCE_TEMPLATE_REACTIVATION_REQUEST_REJECTED,
                            targetUserProfileId
                    )
            );
            log.info("Reactivate request - Request rejection by User ID {} on User Profile ID {} notification sent to User ID: {}",
                    actorUserId, targetUserProfileId, recipientId);
        } else {
            log.info("Reactivate request - Skipping Request rejection by User ID {} on User Profile ID {} notification sent to User ID: {}, because email is empty.",
                    actorUserId, targetUserProfileId, recipientId);
        }
    }

    public Map<String, String> addProperties(String username, String invitationUrl) {

        customProps.put(USER_NAME, username);
        customProps.put(INVITATION_URL, invitationUrl);
        customProps.put(PORTAL_URL, notificationProperties.getPortalUrl());
        return customProps;
    }
}
