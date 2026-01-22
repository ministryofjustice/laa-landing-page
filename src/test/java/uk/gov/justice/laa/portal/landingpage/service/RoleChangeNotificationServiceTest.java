package uk.gov.justice.laa.portal.landingpage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig
class RoleChangeNotificationServiceTest {

    private static final String QUEUE_URL = "https://sqs.eu-west-2.amazonaws.com/123456789/test-queue.fifo";
    private static final String QUEUE_URL_NONE = "none";
    private ListAppender<ILoggingEvent> logAppender;
    @Mock
    private SqsClient sqsClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private UserProfileRepository userProfileRepository;
    private RoleChangeNotificationService roleChangeNotificationService;
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
        Logger logger = (Logger) LoggerFactory.getLogger(RoleChangeNotificationService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        roleChangeNotificationService = new RoleChangeNotificationService(sqsClient, objectMapper, QUEUE_URL, userProfileRepository);
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

    @AfterEach
    public void tearDown() {
        logAppender.stop();
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
        ReflectionTestUtils.setField(roleChangeNotificationService, "sqsQueueUrl", QUEUE_URL_NONE);

        boolean result = roleChangeNotificationService.sendMessage(userProfile, newPuiRoles, oldPuiRoles);

        assertThat(result).isFalse();
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldBulkProcessCcmsRoleSync() {
        // Arrange
        EntraUser entraUser2 = EntraUser.builder().entraOid("OID123").firstName("first").lastName("last").email("test@email.com").build();
        Firm firm2 = Firm.builder().code("FIRM001").build();
        UserProfile userProfile2 = UserProfile.builder()
                .entraUser(entraUser2)
                .userType(UserType.EXTERNAL)
                .legacyUserId(UUID.randomUUID())
                .appRoles(Set.of(puiRole2))
                .firm(firm2)
                .build();

        SendMessageResponse response = SendMessageResponse.builder().messageId("test-message-id").build();

        when(userProfileRepository.findUserProfilesForCcmsSync()).thenReturn(List.of(userProfile, userProfile2));
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(response);

        // Act
        roleChangeNotificationService.ccmsRoleSync();

        // Assert
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository, times(2)).save(captor.capture());
        List<UserProfile> savedProfile = captor.getAllValues();

        assertThat(savedProfile).hasSize(2);
        assertThat(savedProfile.getFirst().isLastCcmsSyncSuccessful()).isTrue();
        assertThat(savedProfile.get(1).isLastCcmsSyncSuccessful()).isTrue();
        verify(sqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
        assertLogMessage("Initializing CCMS role change message for user: test-entra-oid");
        assertLogMessage("CCMS roles updated for user: test-entra-oid");
        assertLogMessage("CCMS role sync for user test-entra-oid: true");
        assertLogMessage("Initializing CCMS role change message for user: OID123");
        assertLogMessage("CCMS roles updated for user: OID123");
        assertLogMessage("CCMS role sync for user OID123: true");
        assertLogMessage("CCMS role sync complete. Successful: 2, Unsuccessful: 0");
    }

    @Test
    void shouldSetLastSyncToFalseWhenSendMessageReturnsFalse() {
        // Arrange
        ReflectionTestUtils.setField(roleChangeNotificationService, "sqsQueueUrl", QUEUE_URL_NONE);

        when(userProfileRepository.findUserProfilesForCcmsSync()).thenReturn(List.of(userProfile));

        // Act
        roleChangeNotificationService.ccmsRoleSync();

        // Assert
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        UserProfile savedProfile = captor.getValue();

        assertThat(savedProfile.isLastCcmsSyncSuccessful()).isFalse();
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
        assertLogMessage("Skipping CCMS update for user: test-entra-oid");
        assertLogMessage("CCMS role sync for user test-entra-oid: false");
        assertLogMessage("CCMS role sync complete. Successful: 0, Unsuccessful: 1");
    }

    @Test
    void shouldHandleExceptionCases() {
        // Arrange
        AppRole appRole = AppRole.builder().ccmsCode("CCMS_ROLE").legacySync(true).build();
        EntraUser entraUser = EntraUser.builder().entraOid("OID123").firstName("first").lastName("last").email("test@email.com").build();
        Firm firm = Firm.builder().code("FIRM001").build();
        UserProfile profile = UserProfile.builder()
                .entraUser(entraUser)
                .userType(UserType.EXTERNAL)
                .legacyUserId(UUID.randomUUID())
                .appRoles(Set.of(appRole))
                .firm(firm)
                .build();

        when(userProfileRepository.findUserProfilesForCcmsSync()).thenReturn(List.of(profile));
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenThrow(new RuntimeException());

        // Act
        roleChangeNotificationService.ccmsRoleSync();

        // Assert
        verify(userProfileRepository, never()).save(any());
        assertLogMessage("Initializing CCMS role change message for user: OID123");
        assertLogMessage("CCMS roles updated for user: OID123");
        assertLogMessage("CCMS role change message:");
        assertLogMessage("CCMS notification attempt failed for user: OID123", Level.WARN);
        assertLogMessage("Error syncing roles for user OID123", Level.ERROR);
        assertLogMessage("CCMS role sync complete. Successful: 0, Unsuccessful: 1");
    }

    @Test
    void testCcmsRoleSync_WhenNoProfiles_ShouldDoNothing() {
        // Arrange
        when(userProfileRepository.findUserProfilesForCcmsSync()).thenReturn(List.of());

        // Act
        roleChangeNotificationService.ccmsRoleSync();

        // Assert
        verify(userProfileRepository, never()).save(any());
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
        assertLogMessage("No profiles found for CCMS role sync");
    }

    private void assertLogMessage(String message) {
        assertLogMessage(message, Level.INFO);
    }

    private void assertLogMessage(String message, Level level) {
        assertTrue(logAppender.list.stream()
                        .anyMatch(logEvent -> logEvent.getLevel() == level
                                && logEvent.getFormattedMessage().contains(message)),
                String.format("Log message not found with level %s and message %s. Actual Logs are: %s", level, message,
                        logAppender.list.stream().map(e -> String.format("[%s] %s", level, e.getFormattedMessage()))
                                .toList()));
    }

}
