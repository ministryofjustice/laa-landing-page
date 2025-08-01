package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.model.CCMSMessage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleChangeNotificationService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${app.sqs.queue.url}")
    private String queueUrl;

    /**
     * Send a message to the configured SQS queue
     *
     * @param userProfile The user profile
     * @param newPuiRoles new roles filtered by PUI
     * @param oldPuiRoles old roled filtered by PUI
     */
    public void sendMessage(UserProfile userProfile, Set<AppRole> newPuiRoles, Set<AppRole> oldPuiRoles) {
        EntraUser entraUser = userProfile.getEntraUser();
        if (!newPuiRoles.equals(oldPuiRoles)
                && !UserType.INTERNAL_TYPES.contains(userProfile.getUserType())) {
            try {
                CCMSMessage message = CCMSMessage.builder()
                        .userName(userProfile.getLegacyUserId().toString())
                        .vendorNumber(userProfile.getFirm().getCode())
                        .firstName(entraUser.getFirstName())
                        .lastName(entraUser.getLastName())
                        .timestamp(LocalDateTime.now())
                        .email(entraUser.getEmail())
                        .responsibilityKey(newPuiRoles.stream().map(AppRole::getCcmsCode).toList())
                        .build();

                String messageBodyJson = objectMapper.writeValueAsString(message);

                log.info("message body json: {}", messageBodyJson);

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBodyJson)
                    .messageDeduplicationId(generateDeduplicationId(entraUser.getEntraOid()))
                    .build();

            SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);

            log.info("Successfully sent SQS message with messageId: {}",
                response.messageId());

            } catch (JsonProcessingException e) {
                log.error("Failed to serialize message payload for user: {}", entraUser.getEntraOid(), e);
                throw new RuntimeException("Failed to send message", e);
            } catch (Exception e) {
                log.error("Failed to send message for user: {}", entraUser.getEntraOid(), e);
                throw new RuntimeException("Failed to send message", e);
            }
        }
    }

    private String generateDeduplicationId(String entraUserId) {
        return String.format("%s-%d",
                entraUserId,
                Instant.now().toEpochMilli());
    }
}
