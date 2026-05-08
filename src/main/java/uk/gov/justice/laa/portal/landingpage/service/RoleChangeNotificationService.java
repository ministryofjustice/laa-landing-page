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
import uk.gov.justice.laa.portal.landingpage.registry.SqsClientRegistry;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleChangeNotificationService {

    private final SqsClientRegistry sqsClientRegistry;
    private final ObjectMapper objectMapper;
    private final UserProfileRepository userProfileRepository;

    private static final String USER_TYPE_ATTRIBUTE = "userType";

    /**
     * This method will automatically retry up to 3 times with 0.1 second delays
     *
     * @param userProfile The user profile
     * @param appEntraOid the app entra oid to determine which queue to send to
     * @param newPuiRoles new roles filtered by PUI
     * @param oldPuiRoles old roled filtered by PUI
     * @return true if successful
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    public boolean sendMessage(UserProfile userProfile, String appEntraOid, Set<String> newPuiRoles, Set<String> oldPuiRoles) {
        Optional<String> sqsQueueUrlOpt = sqsClientRegistry.getSqsQueueUrl(appEntraOid);
        // Skip until queue is ready for env
        if (sqsQueueUrlOpt.isEmpty() || "NONE".equalsIgnoreCase(sqsQueueUrlOpt.get())) {
            log.info("Skipping CCMS update for user: {}",
                userProfile.getEntraUser().getEntraOid());
            return false;
        }
        
        try {
            log.info("Initializing CCMS role change message for user: {}", userProfile.getEntraUser().getEntraOid());
            sendRoleChangeNotificationToSqs(userProfile, appEntraOid, newPuiRoles, oldPuiRoles);
            return true;
        } catch (Exception e) {
            log.warn("CCMS notification attempt failed for user: {}: {}",
                userProfile.getEntraUser().getEntraOid(), e.getMessage());
            throw new RuntimeException("CCMS notification failed", e);
        }
    }
    

    private void sendRoleChangeNotificationToSqs(UserProfile userProfile, String appEntraOid, Set<String> newPuiRoles, Set<String> oldPuiRoles) throws Exception {
        Optional<SqsClient> sqsClientOpt = sqsClientRegistry.getSqsClient(appEntraOid);
        Optional<String> sqsQueueUrlOpt = sqsClientRegistry.getSqsQueueUrl(appEntraOid);
        if (sqsClientOpt.isEmpty() || sqsQueueUrlOpt.isEmpty() || "NONE".equalsIgnoreCase(sqsQueueUrlOpt.get())) {
            log.info("Skipping CCMS update for user: {}", userProfile.getEntraUser().getEntraOid());
            return;
        }

        SqsClient sqsClient = sqsClientOpt.get();
        String sqsQueueUrl = sqsQueueUrlOpt.get();
        EntraUser entraUser = userProfile.getEntraUser();
        if (!newPuiRoles.equals(oldPuiRoles)) {
            log.info("CCMS roles updated for user: {} with entra oid: {}, generating message", userProfile.getId(), userProfile.getEntraUser().getEntraOid());
            CcmsMessage.CcmsMessageBuilder ccmsMessageBuilder = CcmsMessage.builder()
                    .userName(userProfile.getLegacyUserId().toString())
                    .firstName(entraUser.getFirstName())
                    .lastName(entraUser.getLastName())
                    .timestamp(LocalDateTime.now())
                    .email(entraUser.getEmail())
                    .responsibilityKey(newPuiRoles.stream().toList());

            if (UserType.EXTERNAL.equals(userProfile.getUserType())) {
                ccmsMessageBuilder.vendorNumber(userProfile.getFirm().getCode());
            }

            String messageBody = objectMapper.writeValueAsString(ccmsMessageBuilder.build());
            log.info("CCMS role change generated for user profile: {} with entra oid: {} and legacy profile id: {} containing CCMS roles: {}",
                    userProfile.getId(), userProfile.getEntraUser().getEntraOid(), userProfile.getLegacyUserId().toString(), newPuiRoles);

            Map<String, MessageAttributeValue> userTypeAttribute = Map.of(USER_TYPE_ATTRIBUTE, MessageAttributeValue.builder()
                    .stringValue(userProfile.getUserType().toString())
                    .dataType("String")
                    .build());

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(sqsQueueUrl)
                    .messageBody(messageBody)
                    .messageAttributes(userTypeAttribute)
                    .messageGroupId(userProfile.getLegacyUserId().toString())
                    .messageDeduplicationId(generateDeduplicationId(userProfile, newPuiRoles))
                    .build();

            SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);
            log.info("CCMS role change message sent to queue for user: {} with entra oid: {}, messageId: {}",
                    userProfile.getId(), entraUser.getEntraOid(), response.messageId());
        } else {
            log.info("No CCMS roles updated for user: {} with entra oid: {}, skipping", userProfile.getId(), userProfile.getEntraUser().getEntraOid());
        }
    }

    @Recover
    public boolean recoverFromRetryFailure(RuntimeException e, UserProfile userProfile) {
        log.error("All retry attempts failed for CCMS notification for user: {} with entra oid: {}, saving roles to db and moving on",
                userProfile.getId(), userProfile.getEntraUser().getEntraOid(), e);
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
            try {
                Map<String, Set<String>> ccmsRolesMap = profile.getAppRoles().stream()
                        .filter(AppRole::isLegacySync)
                        .collect(Collectors.groupingBy(role -> role.getApp().getEntraOid(),
                                Collectors.collectingAndThen(
                                        Collectors.mapping(AppRole::getCcmsCode, Collectors.toSet()),
                                        HashSet::new
                                )));

                boolean notificationSuccess = profile.isLastCcmsSyncSuccessful();
                boolean ccmsSyncResult = true;
                for (Map.Entry<String, Set<String>> ccmsRoles : ccmsRolesMap.entrySet()) {
                    ccmsSyncResult = ccmsSyncResult && sendMessage(profile, ccmsRoles.getKey(), ccmsRoles.getValue(), Collections.emptySet());
                    notificationSuccess = ccmsSyncResult;

                    log.info("CCMS role sync for user: {} with entra oid: {} {}", profile.getId(), profile.getEntraUser().getEntraOid(), notificationSuccess);
                }

                profile.setLastCcmsSyncSuccessful(notificationSuccess);
                userProfileRepository.save(profile);

                if (notificationSuccess) {
                    successful.incrementAndGet();
                } else {
                    unsuccessful.incrementAndGet();
                }
            } catch (Exception e) {
                log.error("Error syncing roles for user: {} with entra oid: {}", profile.getId(), profile.getEntraUser().getEntraOid(), e);
                unsuccessful.incrementAndGet();
            }

        });

        log.info("CCMS role sync complete. Successful: {}, Unsuccessful: {}",
                successful.get(), unsuccessful.get());
    }

}
