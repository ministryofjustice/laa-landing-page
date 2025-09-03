package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.model.CcmsMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleChangeNotificationService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String sqsQueueUrl;

    /**
     * This method will automatically retry up to 3 times with 0.1 second delays
     *
     * @param userProfile The user profile
     * @param newPuiRoles new roles filtered by PUI
     * @param oldPuiRoles old roled filtered by PUI
     * @return true if successful
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    public boolean sendMessage(UserProfile userProfile, Set<AppRole> newPuiRoles, Set<AppRole> oldPuiRoles) {
        // Skip until queue is ready for env
        if ("none".equalsIgnoreCase(sqsQueueUrl)) {
            log.info("Skipping CCMS update for user: {}",
                userProfile.getEntraUser().getEntraOid());
            return false;
        }
        
        try {
            sendRoleChangeNotificationToSqs(userProfile, newPuiRoles, oldPuiRoles);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send CCMS role change message to SQS for user: {}: {}, saving roles to db and moving on",
                userProfile.getEntraUser().getEntraOid(), e.getMessage());
            return false;
        }
    }
    

    private void sendRoleChangeNotificationToSqs(UserProfile userProfile, Set<AppRole> newPuiRoles, Set<AppRole> oldPuiRoles) throws Exception {
        EntraUser entraUser = userProfile.getEntraUser();
        if (!newPuiRoles.equals(oldPuiRoles)
                && !UserType.INTERNAL_TYPES.contains(userProfile.getUserType())) {
            CcmsMessage message = CcmsMessage.builder()
                    .userName(userProfile.getLegacyUserId().toString())
                    .vendorNumber(userProfile.getFirm().getCode())
                    .firstName(entraUser.getFirstName())
                    .lastName(entraUser.getLastName())
                    .timestamp(LocalDateTime.now())
                    .email(entraUser.getEmail())
                    .responsibilityKey(newPuiRoles.stream().map(AppRole::getCcmsCode).toList())
                    .build();

            String messageBody = objectMapper.writeValueAsString(message);

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(sqsQueueUrl)
                    .messageBody(messageBody)
                    .messageGroupId(userProfile.getLegacyUserId().toString())
                    .messageDeduplicationId(generateDeduplicationId(userProfile, newPuiRoles))
                    .build();

            SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);
            log.info("CCMS role change message sent to queue for user: {}, messageId: {}",
                entraUser.getEntraOid(), response.messageId());
        }
    }

    private String generateDeduplicationId(UserProfile userProfile, Set<AppRole> newPuiRoles) {
        String content = userProfile.getEntraUser().getEntraOid() + "-"
                + userProfile.getLegacyUserId() + "-"
                + newPuiRoles.stream()
                            .map(AppRole::getCcmsCode)
                            .sorted()
                            .reduce("", (a, b) -> a + b) + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));

        return UUID.nameUUIDFromBytes(content.getBytes()).toString();
    }
}
