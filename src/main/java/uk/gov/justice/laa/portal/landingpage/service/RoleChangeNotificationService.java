package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.model.CcmsMessage;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleChangeNotificationService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String sqsQueueUrl;
    private final UserProfileRepository userProfileRepository;

    private static final String USER_TYPE_ATTRIBUTE = "userType";

    /**
     * This method will automatically retry up to 3 times with 0.1 second delays
     *
     * @param userProfile The user profile
     * @param newPuiRoles new roles filtered by PUI
     * @param oldPuiRoles old roled filtered by PUI
     * @return true if successful
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    public boolean sendMessage(UserProfile userProfile, Set<String> newPuiRoles, Set<String> oldPuiRoles) {
        // Skip until queue is ready for env
        if ("none".equalsIgnoreCase(sqsQueueUrl)) {
            log.info("Skipping CCMS update for user: {}",
                userProfile.getEntraUser().getEntraOid());
            return false;
        }
        
        try {
            log.info("Initializing CCMS role change message for user: {}", userProfile.getEntraUser().getEntraOid());
            sendRoleChangeNotificationToSqs(userProfile, newPuiRoles, oldPuiRoles);
            return true;
        } catch (Exception e) {
            log.warn("CCMS notification attempt failed for user: {}: {}",
                userProfile.getEntraUser().getEntraOid(), e.getMessage());
            throw new RuntimeException("CCMS notification failed", e);
        }
    }
    

    private void sendRoleChangeNotificationToSqs(UserProfile userProfile, Set<String> newPuiRoles, Set<String> oldPuiRoles) throws Exception {
        EntraUser entraUser = userProfile.getEntraUser();
        if (!newPuiRoles.equals(oldPuiRoles)
                && userProfile.getUserType() != UserType.INTERNAL) {
            log.info("CCMS roles updated for user: {}, generating message", userProfile.getEntraUser().getEntraOid());
            CcmsMessage message = CcmsMessage.builder()
                    .userName(userProfile.getLegacyUserId().toString())
                    .vendorNumber(userProfile.getFirm().getCode())
                    .firstName(entraUser.getFirstName())
                    .lastName(entraUser.getLastName())
                    .timestamp(LocalDateTime.now())
                    .email(entraUser.getEmail())
                    .responsibilityKey(newPuiRoles.stream().toList())
                    .build();

            String messageBody = objectMapper.writeValueAsString(message);
            log.info("CCMS role change message: {}", messageBody);

            Map<String, MessageAttributeValue> userTypeAttribute = Map.of(USER_TYPE_ATTRIBUTE, MessageAttributeValue.builder()
                    .stringValue(userProfile.getUserType().toString())
                    .build());

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(sqsQueueUrl)
                    .messageBody(messageBody)
                    .messageAttributes(userTypeAttribute)
                    .messageGroupId(userProfile.getLegacyUserId().toString())
                    .messageDeduplicationId(generateDeduplicationId(userProfile, newPuiRoles))
                    .build();

            SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);
            log.info("CCMS role change message sent to queue for user: {}, messageId: {}",
                entraUser.getEntraOid(), response.messageId());
        } else {
            log.info("No CCMS roles updated for user: {}, skipping", userProfile.getEntraUser().getEntraOid());
        }
    }

    @Recover
    public boolean recoverFromRetryFailure(RuntimeException e, UserProfile userProfile) {
        log.error("All retry attempts failed for CCMS notification for user: {}, saving roles to db and moving on",
            userProfile.getEntraUser().getEntraOid(), e);
        return false;
    }

    private String generateDeduplicationId(UserProfile userProfile, Set<String> newPuiRoles) {
        String content = userProfile.getEntraUser().getEntraOid() + "-"
                + userProfile.getLegacyUserId() + "-"
                + newPuiRoles.stream()
                            .sorted()
                            .reduce("", (a, b) -> a + b) + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));

        return UUID.nameUUIDFromBytes(content.getBytes()).toString();
    }


    @Async
    public void ccmsRoleSync() {
        List<UserProfile> profiles = userProfileRepository.findUserProfilesForCcmsSync();

        if (profiles.isEmpty()) {
            log.info("No profiles found for CCMS role sync");
            return;
        }

        AtomicInteger successful = new AtomicInteger();
        AtomicInteger unsuccessful = new AtomicInteger();

        profiles.forEach(profile -> {
            boolean notificationSuccess = false;
            try {
                Set<String> ccmsRoles = profile.getAppRoles().stream()
                        .filter(AppRole::isLegacySync)
                        .map(AppRole::getCcmsCode)
                        .collect(Collectors.toSet());
                notificationSuccess = sendMessage(profile, ccmsRoles, Collections.emptySet());

                log.info("CCMS role sync for user {}: {}", profile.getEntraUser().getEntraOid(), notificationSuccess);

                profile.setLastCcmsSyncSuccessful(notificationSuccess);
                userProfileRepository.save(profile);
            } catch (Exception e) {
                log.error("Error syncing roles for user {}", profile.getEntraUser().getEntraOid(), e);
            }

            if (notificationSuccess) {
                successful.incrementAndGet();
            } else {
                unsuccessful.incrementAndGet();
            }
        });

        log.info("CCMS role sync complete. Successful: {}, Unsuccessful: {}",
                successful.get(), unsuccessful.get());
    }

}
