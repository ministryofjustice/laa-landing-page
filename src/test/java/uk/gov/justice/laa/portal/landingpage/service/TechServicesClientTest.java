package uk.gov.justice.laa.portal.landingpage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsRequest;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsResponse;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TechServicesClientTest {

    private ListAppender<ILoggingEvent> logAppender;
    @Mock
    private ClientSecretCredential clientSecretCredential;
    @Mock
    private RestClient restClient;
    @Mock
    private EntraUserRepository entraUserRepository;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private TechServicesClient techServicesClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    public void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(TechServicesClient.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        ReflectionTestUtils.setField(techServicesClient, "accessTokenRequestScope", "scope");
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
    }

    @Test
    void testUpdateRoleAssignmentUserNotFound() {
        UUID userId = UUID.randomUUID();
        restClient = Mockito.mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(entraUserRepository.findById(userId)).thenThrow(new RuntimeException("User not found"));

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> techServicesClient.updateRoleAssignment(userId),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage()).contains("Error while sending security group changes to Tech Services.");
    }

    @Test
    void testUpdateRoleAssignmentUserError() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(HashSet.newHashSet(11))
                .firstName("firstName").lastName("lastName")
                .userStatus(UserStatus.ACTIVE).startDate(LocalDateTime.now())
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenThrow(new RuntimeException("Error sending request to Tech services"));

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> techServicesClient.updateRoleAssignment(userId),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage()).contains("Error while sending security group changes to Tech Services.");
        assertLogMessage(Level.INFO, "Sending update security groups request to tech services:");
        assertLogMessage(Level.ERROR, "Error while sending security group changes to Tech Services.");
    }

    @Test
    void testUpdateRoleAssignment() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(HashSet.newHashSet(11))
                .firstName("firstName").lastName("lastName")
                .userStatus(UserStatus.ACTIVE).startDate(LocalDateTime.now())
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
        String reqStr = "{\"requiredGroups\": []}";
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(UpdateSecurityGroupsRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class))
                .thenReturn(ResponseEntity.ok(UpdateSecurityGroupsResponse.builder().build()));

        techServicesClient.updateRoleAssignment(userId);

        assertLogMessage(Level.INFO, "Sending update security groups request to tech services:");
        assertLogMessage(Level.INFO, "Security Groups assigned successfully for firstName lastName");
        verify(restClient, times(1)).patch();
    }

    @Test
    void testUpdateRoleAssignmentError4Xx() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(HashSet.newHashSet(11))
                .firstName("firstName").lastName("lastName")
                .userStatus(UserStatus.ACTIVE).startDate(LocalDateTime.now())
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(UpdateSecurityGroupsRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        ResponseEntity<UpdateSecurityGroupsResponse> responseEntity = ResponseEntity.badRequest().build();
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class))
                .thenReturn(responseEntity);

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> techServicesClient.updateRoleAssignment(userId),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage())
                .contains("Error while sending security group changes to Tech Services.");
        assertLogMessage(Level.INFO, "Sending update security groups request to tech services:");
        assertLogMessage(Level.ERROR,
                "Failed to assign security groups for user firstName lastName with error code 400 BAD_REQUEST");
        verify(restClient, times(1)).patch();
    }

    @Test
    void testUpdateRoleAssignmentError5Xx() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(HashSet.newHashSet(11))
                .firstName("firstName").lastName("lastName")
                .userStatus(UserStatus.ACTIVE).startDate(LocalDateTime.now())
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
        String reqStr = "{\"requiredGroups\": []}";
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(UpdateSecurityGroupsRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        ResponseEntity<UpdateSecurityGroupsResponse> responseEntity = ResponseEntity.internalServerError().build();
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class))
                .thenReturn(responseEntity);

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> techServicesClient.updateRoleAssignment(userId),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage())
                .contains("Error while sending security group changes to Tech Services.");
        assertLogMessage(Level.INFO, "Sending update security groups request to tech services:");
        assertLogMessage(Level.ERROR,
                "Failed to assign security groups for user firstName lastName with error code 500 INTERNAL_SERVER_ERROR");
        verify(restClient, times(1)).patch();
    }

    private void assertLogMessage(ch.qos.logback.classic.Level logLevel, String message) {
        assertTrue(logAppender.list.stream()
                        .anyMatch(logEvent -> logEvent.getLevel() == logLevel
                                && logEvent.getFormattedMessage().contains(message)),
                String.format("Log message not found with level %s and message %s. Actual Logs are: %s", logLevel, message,
                        logAppender.list.stream().map(e -> String.format("[%s] %s", logLevel, e.getFormattedMessage()))
                                .toList()));
    }

    @AfterEach
    public void tearDown() {
        logAppender.stop();
        logAppender.list.clear();
    }

}
