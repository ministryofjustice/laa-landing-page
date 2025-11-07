package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig
class RoleChangeNotificationServiceTest {

    @Mock
    private SqsClient sqsClient;
    
    @Mock
    private ObjectMapper objectMapper;
    
    private RoleChangeNotificationService roleChangeNotificationService;
    
    private static final String QUEUE_URL = "https://sqs.eu-west-2.amazonaws.com/123456789/test-queue.fifo";
    private static final String QUEUE_URL_NONE = "none";

    private UserProfile userProfile;
    private EntraUser entraUser;
    private Firm firm;
    private AppRole puiRole1;
    private AppRole puiRole2;
    private AppRole nonPuiRole;
    private Set<String> oldPuiRoles;
    private Set<String> newPuiRoles;
    private Set<String> emptyRoles;

    @BeforeEach
    void setUp() {
        roleChangeNotificationService = new RoleChangeNotificationService(sqsClient, objectMapper, QUEUE_URL);
        entraUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .entraOid("test-entra-oid")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("FIRM001")
                .name("Test Firm")
                .build();

        puiRole1 = AppRole.builder()
                .id(UUID.randomUUID())
                .name("PUI_ROLE_1")
                .description("PUI Role 1 Description")
                .ccmsCode("CCMS_PUI_001")
                .legacySync(true)
                .build();

        puiRole2 = AppRole.builder()
                .id(UUID.randomUUID())
                .name("PUI_ROLE_2")
                .description("PUI Role 2 Description")
                .ccmsCode("CCMS_PUI_002")
                .legacySync(true)
                .build();

        nonPuiRole = AppRole.builder()
                .id(UUID.randomUUID())
                .name("NON_PUI_ROLE")
                .description("Non-PUI Role Description")
                .ccmsCode("NON_CCMS_001")
                .build();

        userProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .legacyUserId(UUID.randomUUID())
                .userType(UserType.EXTERNAL)
                .entraUser(entraUser)
                .firm(firm)
                .appRoles(Set.of(puiRole1, puiRole2, nonPuiRole))
                .build();

        oldPuiRoles = Set.of(puiRole1.getCcmsCode());
        newPuiRoles = Set.of(puiRole1.getCcmsCode(), puiRole2.getCcmsCode());
        emptyRoles = Set.of();
    }

    @Test
    void shouldNotSendMessage_whenPuiRolesUnchangedForExternalUser() {
        Set<String> unchangedRoles = Set.of(puiRole1.getCcmsCode());

        boolean result = roleChangeNotificationService.sendMessage(userProfile, unchangedRoles, unchangedRoles);

        assertThat(result).isTrue();
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldNotSendMessage_whenUserIsInternal() {
        userProfile.setUserType(UserType.INTERNAL);

        boolean result = roleChangeNotificationService.sendMessage(userProfile, newPuiRoles, oldPuiRoles);

        assertThat(result).isTrue();
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldSendCorrectSqsMessage_whenPuiRolesChanged() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"message\"}");
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().messageId("test-message-id").build());

        boolean result = roleChangeNotificationService.sendMessage(userProfile, newPuiRoles, oldPuiRoles);

        assertThat(result).isTrue();
        verify(sqsClient).sendMessage(argThat((SendMessageRequest request) -> {
            return request.queueUrl().equals(QUEUE_URL)
                    && request.messageGroupId().equals(userProfile.getLegacyUserId().toString())
                    && request.messageDeduplicationId() != null
                    && request.messageBody().equals("{\"test\":\"message\"}");
        }));
    }

    @Test
    void shouldReturnFalse_whenRecoverMethodCalled() {
        boolean result = roleChangeNotificationService.recoverFromRetryFailure(
            new RuntimeException("SQS send failed"), userProfile);

        assertThat(result).isFalse();
    }

    @Test
    void shouldHandleEmptyOldRoles() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"message\"}");
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().messageId("test-message-id").build());

        boolean result = roleChangeNotificationService.sendMessage(userProfile, newPuiRoles, emptyRoles);

        assertThat(result).isTrue();
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldHandleEmptyNewRoles() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"message\"}");
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().messageId("test-message-id").build());

        boolean result = roleChangeNotificationService.sendMessage(userProfile, emptyRoles, oldPuiRoles);

        assertThat(result).isTrue();
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldNotSendMessage_whenBothRoleSetsAreEmpty() {
        boolean result = roleChangeNotificationService.sendMessage(userProfile, emptyRoles, emptyRoles);

        assertThat(result).isTrue();
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }
    
    @Test
    void shouldSkipSqsOperations_whenQueueUrlIsNone() {
        RoleChangeNotificationService serviceWithNoneQueue = 
            new RoleChangeNotificationService(sqsClient, objectMapper, QUEUE_URL_NONE);
        
        boolean result = serviceWithNoneQueue.sendMessage(userProfile, newPuiRoles, oldPuiRoles);
        
        assertThat(result).isFalse();
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }
}
