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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.exception.BadRequestException;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserRequest;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.SendUserVerificationEmailRequest;
import uk.gov.justice.laa.portal.landingpage.techservices.SendUserVerificationEmailResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsRequest;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LiveTechServicesClientTest {

    private ListAppender<ILoggingEvent> logAppender;
    @Mock
    private ClientSecretCredential clientSecretCredential;
    @Mock
    private RestClient restClient;
    @Mock
    private EntraUserRepository entraUserRepository;
    @Mock
    private CacheManager cacheManager;
    @InjectMocks
    private LiveTechServicesClient liveTechServicesClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;
    @Mock
    private JwtDecoder jwtDecoder;
    @Spy
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(LiveTechServicesClient.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        ReflectionTestUtils.setField(liveTechServicesClient, "accessTokenRequestScope", "scope");
    }

    @Test
    void testDeleteRoleAssignment_404NotFoundContinues() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(new java.util.HashSet<>())
                .firstName("firstName").lastName("lastName").build();
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(UpdateSecurityGroupsRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        String errorBody = "{\n  \"success\": false, \n  \"code\": \"NOT_FOUND\", \n  \"message\": \"User not found\"\n}";
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.NOT_FOUND,
                "Not Found", null, errorBody.getBytes(), null);
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class)).thenThrow(exception);
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        // Should not throw error
        liveTechServicesClient.deleteRoleAssignment(userId);

        assertLogMessage(Level.WARN, "404 Not Found");
    }

    @Test
    void testUpdateRoleAssignment() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(HashSet.newHashSet(11))
                .firstName("firstName").lastName("lastName")
                .userStatus(UserStatus.ACTIVE)
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(UpdateSecurityGroupsRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class))
                .thenReturn(ResponseEntity.ok(UpdateSecurityGroupsResponse.builder().build()));
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        liveTechServicesClient.updateRoleAssignment(userId);

        assertLogMessage(Level.INFO, "Sending update security groups request to tech services:");
        assertLogMessage(Level.INFO, "Security Groups assigned successfully for firstName lastName");
        verify(restClient, times(1)).patch();
    }

    @Test
    void testDeleteRoleAssignment_sendsEmptyGroupsPayload() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(new java.util.HashSet<>())
                .firstName("firstName").lastName("lastName").build();
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        ArgumentCaptor<UpdateSecurityGroupsRequest> captor = ArgumentCaptor.forClass(UpdateSecurityGroupsRequest.class);
        when(requestBodySpec.body(captor.capture())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class))
                .thenReturn(ResponseEntity.ok(UpdateSecurityGroupsResponse.builder().build()));
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        liveTechServicesClient.deleteRoleAssignment(userId);

        UpdateSecurityGroupsRequest sent = captor.getValue();
        assertThat(sent).isNotNull();
        assertThat(sent.getGroups()).isNotNull();
        assertThat(sent.getGroups()).isEmpty();
    }

    @Test
    void testDeleteRoleAssignment_httpClientErrorExceptionLogsBody() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(new java.util.HashSet<>())
                .firstName("firstName").lastName("lastName").build();
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(UpdateSecurityGroupsRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        String errorBody = "{\n  \"success\": false, \n  \"code\": \"BAD_REQUEST\", \n  \"message\": \"Validation failed\"\n}";
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.BAD_REQUEST,
                "Bad Request", null, errorBody.getBytes(), null);
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class)).thenThrow(exception);
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        assertThrows(RuntimeException.class, () -> liveTechServicesClient.deleteRoleAssignment(userId));
        assertLogMessage(Level.ERROR, "status=400");
        assertLogMessage(Level.ERROR, "Validation failed");
    }

    @Test
    void testDeleteRoleAssignment_success() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(new java.util.HashSet<>())
                .firstName("firstName").lastName("lastName").build();
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(UpdateSecurityGroupsRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class))
                .thenReturn(ResponseEntity.ok(UpdateSecurityGroupsResponse.builder().build()));
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        liveTechServicesClient.deleteRoleAssignment(userId);

        assertLogMessage(Level.INFO, "Sending request to tech services to remove group memberships for deleting:");
        assertLogMessage(Level.INFO, "Security Groups removed successfully for firstName lastName");
        verify(restClient, times(1)).patch();
    }

    @Test
    void testDeleteRoleAssignmentUserNotFound() {
        UUID userId = UUID.randomUUID();
        AccessToken token = new AccessToken("token", null);
        when(entraUserRepository.findById(userId)).thenThrow(new RuntimeException("User not found"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> liveTechServicesClient.deleteRoleAssignment(userId));
        Assertions.assertThat(ex.getMessage()).contains("Error while sending security group removal to Tech Services.");
        assertLogMessage(Level.ERROR, "Error while sending security group removal to Tech Services.");
    }

    @Test
    void testDeleteRoleAssignmentError() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(new java.util.HashSet<>())
                .firstName("firstName").lastName("lastName").build();
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenThrow(new RuntimeException("Rest error"));
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> liveTechServicesClient.deleteRoleAssignment(userId));
        Assertions.assertThat(ex.getMessage()).contains("Error while sending security group removal to Tech Services.");
        assertLogMessage(Level.ERROR, "Error while sending security group removal to Tech Services.");
    }

    @Test
    void testDeleteRoleAssignment4xxResponse() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(new java.util.HashSet<>())
                .firstName("firstName").lastName("lastName").build();
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(UpdateSecurityGroupsRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class)).thenReturn(ResponseEntity.badRequest().build());
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> liveTechServicesClient.deleteRoleAssignment(userId));
        Assertions.assertThat(ex.getMessage()).contains("Error while sending security group removal to Tech Services.");
        assertLogMessage(Level.ERROR, "Failed to remove security groups for user firstName lastName with error code 400 BAD_REQUEST");
        verify(restClient, times(1)).patch();
    }

    @Test
    void testDeleteRoleAssignment5xxResponse() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(new java.util.HashSet<>())
                .firstName("firstName").lastName("lastName").build();
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(UpdateSecurityGroupsRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class)).thenReturn(ResponseEntity.internalServerError().build());
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> liveTechServicesClient.deleteRoleAssignment(userId));
        Assertions.assertThat(ex.getMessage()).contains("Error while sending security group removal to Tech Services.");
        assertLogMessage(Level.ERROR, "Failed to remove security groups for user firstName lastName with error code 500 INTERNAL_SERVER_ERROR");
        verify(restClient, times(1)).patch();
    }

    @Test
    void testUpdateRoleAssignmentUserNotFound() {
        UUID userId = UUID.randomUUID();
        restClient = Mockito.mock(RestClient.class, RETURNS_DEEP_STUBS);
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(entraUserRepository.findById(userId)).thenThrow(new RuntimeException("User not found"));
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> liveTechServicesClient.updateRoleAssignment(userId),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage()).contains("Error while sending security group changes to Tech Services.");
    }

    @Test
    void testUpdateRoleAssignmentUserError() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(HashSet.newHashSet(11))
                .firstName("firstName").lastName("lastName").userStatus(UserStatus.ACTIVE)
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenThrow(new RuntimeException("Error sending request to Tech services"));
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> liveTechServicesClient.updateRoleAssignment(userId),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage()).contains("Error while sending security group changes to Tech Services.");
        assertLogMessage(Level.INFO, "Sending update security groups request to tech services:");
        assertLogMessage(Level.ERROR, "Error while sending security group changes to Tech Services.");
    }

    @Test
    void testUpdateRoleAssignmentError4Xx() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(HashSet.newHashSet(11))
                .firstName("firstName").lastName("lastName")
                .userStatus(UserStatus.ACTIVE)
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(UpdateSecurityGroupsRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class)).thenReturn(ResponseEntity.badRequest().build());
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        BadRequestException rtEx = assertThrows(BadRequestException.class,
                () -> liveTechServicesClient.updateRoleAssignment(userId),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(BadRequestException.class);
        Assertions.assertThat(rtEx.getMessage())
                .contains("Failed to assign security groups for user");
        assertLogMessage(Level.INFO, "Sending update security groups request to tech services:");
        assertLogMessage(Level.INFO,
                "Failed to assign security groups for user firstName lastName with error code 400 BAD_REQUEST");
        verify(restClient, times(1)).patch();
    }

    @Test
    void testUpdateRoleAssignmentError5Xx() {
        UUID userId = UUID.randomUUID();
        EntraUser user = EntraUser.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .userProfiles(HashSet.newHashSet(11))
                .firstName("firstName").lastName("lastName")
                .userStatus(UserStatus.ACTIVE)
                .createdDate(LocalDateTime.now()).createdBy("Test").build();

        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(UpdateSecurityGroupsRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        ResponseEntity<UpdateSecurityGroupsResponse> responseEntity = ResponseEntity.internalServerError().build();
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class)).thenReturn(responseEntity);
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> liveTechServicesClient.updateRoleAssignment(userId),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage())
                .contains("Error while sending security group changes to Tech Services.");
        assertLogMessage(Level.INFO, "Sending update security groups request to tech services:");
        assertLogMessage(Level.ERROR,
                "Failed to assign security groups for user firstName lastName with error code 500 INTERNAL_SERVER_ERROR");
        verify(restClient, times(1)).patch();
    }

    @Test
    void testSendEmailVerification() {
        String userId = UUID.randomUUID().toString();
        EntraUserDto user = EntraUserDto.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName")
                .userStatus(UserStatus.ACTIVE).build();
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(SendUserVerificationEmailRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class))
                .thenReturn(ResponseEntity.ok("""
                        {
                          "success": true,
                          "message": "Activation code has been generated and sent successfully via email."
                        }"""));
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        TechServicesApiResponse<SendUserVerificationEmailResponse> response = liveTechServicesClient.sendEmailVerification(user);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getError()).isNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getMessage()).isEqualTo("Activation code has been generated and sent successfully via email.");

        assertLogMessage(Level.INFO, "Sending Resend verification email request to tech services");
        assertLogMessage(Level.INFO, "Resend user verification email by Tech Services is successful for firstName lastName and response is");

        verify(restClient, times(1)).post();
    }

    @Test
    void testSendEmailVerificationUserNotFound() {
        restClient = Mockito.mock(RestClient.class, RETURNS_DEEP_STUBS);

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> liveTechServicesClient.sendEmailVerification(null),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage()).contains("Error while sending verification email request to Tech Services.");
        assertLogMessage(Level.ERROR, "Error while sending verification email request to Tech Services.");
    }

    @Test
    void testSendEmailVerificationUserEntraOidNotFound() {
        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> liveTechServicesClient.sendEmailVerification(null),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage()).contains("Error while sending verification email request to Tech Services.");
        assertLogMessage(Level.ERROR, "Error while sending verification email request to Tech Services.");
    }

    @Test
    void testSendEmailVerificationUserError() {
        String userId = UUID.randomUUID().toString();
        EntraUserDto user = EntraUserDto.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName")
                .userStatus(UserStatus.ACTIVE).build();
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(restClient.post()).thenThrow(new RuntimeException("Error sending request to Tech services"));
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> liveTechServicesClient.sendEmailVerification(user),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage()).contains("Error while sending verification email request to Tech Services.");
        assertLogMessage(Level.ERROR, "Error while sending verification email request to Tech Services.");
    }

    @Test
    void testSendEmailVerificationReturns4Xx() {
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(SendUserVerificationEmailRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class))
                .thenReturn(ResponseEntity.badRequest().body("""
                        {
                          "success": false,
                          "code": "BAD_REQUEST",
                          "message": "Validation failed"
                        }"""));
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        String userId = UUID.randomUUID().toString();
        EntraUserDto user = EntraUserDto.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName")
                .userStatus(UserStatus.ACTIVE).build();

        liveTechServicesClient.sendEmailVerification(user);

        assertLogMessage(Level.INFO, "Sending Resend verification email request to tech services");
        assertLogMessage(Level.ERROR, "Failed to send verification email fo");

        verify(restClient, times(1)).post();
    }

    @Test
    void testSendEmailVerificationReturns425() {
        AccessToken token = new AccessToken("token", null);

        String errorBody = """
                {
                    "success": false,
                    "code": "RECENTLY_TRIGGERED",
                    "message": "An activation code was recently sent. Please wait before requesting another."
                }""";
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.TOO_EARLY,
                "Too early", null, errorBody.getBytes(), null);

        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(restClient.post()).thenThrow(exception);
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        String userId = UUID.randomUUID().toString();
        EntraUserDto user = EntraUserDto.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName")
                .userStatus(UserStatus.ACTIVE).build();

        liveTechServicesClient.sendEmailVerification(user);

        assertLogMessage(Level.INFO, "Sending Resend verification email request to tech services");
        assertLogMessage(Level.INFO, "Failed to send verification email for firstName lastName");

        verify(restClient, times(1)).post();
    }

    @Test
    void testSendEmailVerificationThrows5Xx() {
        String userId = UUID.randomUUID().toString();
        EntraUserDto user = EntraUserDto.builder().id(userId).email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName")
                .userStatus(UserStatus.ACTIVE).build();

        String errorBody = """
                {
                  "success": false,
                  "code": "INTERNAL_SERVER_ERROR",
                  "message": "An unexpected error occurred during verification process"
                }""";
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR,
                "Server Error", null, errorBody.getBytes(), null);

        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(restClient.post()).thenThrow(exception);
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        TechServicesApiResponse<SendUserVerificationEmailResponse> response = liveTechServicesClient.sendEmailVerification(user);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getError().getMessage()).isEqualTo("An unexpected error occurred during verification process");

        assertLogMessage(Level.INFO, "Sending Resend verification email request to tech services");
        assertLogMessage(Level.ERROR, "Failed to send verification email fo");
        verify(restClient, times(1)).post();
    }

    @Test
    void testRegisterUser() {
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(RegisterUserRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        ConcurrentMapCache concurrentMapCache = new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE);
        concurrentMapCache.put("access_token", "techServicesDetails");
        when(cacheManager.getCache(anyString())).thenReturn(concurrentMapCache);
        when(jwtDecoder.decode(anyString())).thenThrow(new RuntimeException("Error decoding JWT token"));
        String responseJson = """
                                {
                                "success": true,
                                "message": "User created successfully. An email has been sent to the user with their activation code",
                                "entraObject": {
                            "id": "12345678-1234-1234-1234-123456789012",
                                    "displayName": "John Smith",
                                    "mail": "john@example.com",
                                    "accountEnabled": false,
                                    "createdDateTime": "2025-01-15T10:30:00Z"
                        }
                }""";
        when(responseSpec.toEntity(String.class))
                .thenReturn(ResponseEntity.ok(responseJson));

        EntraUserDto user = EntraUserDto.builder().email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName").build();

        liveTechServicesClient.registerNewUser(user);

        assertLogMessage(Level.INFO, "Sending create new user request with security groups to tech services:");
        assertLogMessage(Level.INFO, "New User creation by Tech Services is successful for firstName lastName");
        verify(restClient, times(1)).post();
    }

    @Test
    void testRegisterUserTokenFromCache() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(RegisterUserRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        String responseJson = """
                                {
                                "success": true,
                                "message": "User created successfully. An email has been sent to the user with their activation code",
                                "entraObject": {
                            "id": "12345678-1234-1234-1234-123456789012",
                                    "displayName": "John Smith",
                                    "mail": "john@example.com",
                                    "accountEnabled": false,
                                    "createdDateTime": "2025-01-15T10:30:00Z"
                        }
                }""";
        when(responseSpec.toEntity(String.class))
                .thenReturn(ResponseEntity.ok(responseJson));
        ConcurrentMapCache concurrentMapCache = new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE);
        concurrentMapCache.put("access_token", "techServicesDetails");
        when(jwtDecoder.decode(anyString())).thenReturn(Jwt.withTokenValue("techServicesDetails")
                .header("alg", "none").claim("exp", Instant.now().plusSeconds(120)).build());
        when(cacheManager.getCache(anyString())).thenReturn(concurrentMapCache);

        EntraUserDto user = EntraUserDto.builder().email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName").build();

        liveTechServicesClient.registerNewUser(user);

        assertLogMessage(Level.INFO, "Sending create new user request with security groups to tech services:");
        assertLogMessage(Level.INFO, "New User creation by Tech Services is successful for firstName lastName");
        verify(restClient, times(1)).post();
    }

    @Test
    void testRegisterUserError() {
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        EntraUserDto user = EntraUserDto.builder().email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName").build();

        when(restClient.post()).thenThrow(new RuntimeException("Error sending request to Tech services"));
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> liveTechServicesClient.registerNewUser(user),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage()).contains("Unexpected error while sending new user creation request to Tech Services.");
        assertLogMessage(Level.INFO, "Sending create new user request with security groups to tech services:");
        assertLogMessage(Level.ERROR, "Unexpected error while sending new user creation request to Tech Services.");
    }

    @Test
    void testRegisterUserError409() {
        EntraUserDto user = EntraUserDto.builder().email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName").build();

        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(RegisterUserRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        String errorBody = """
                {
                   "success": false,
                   "code": "USER_ALREADY_EXISTS",
                   "message": "A user with this email already exists"
                 }""";
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.CONFLICT,
                "Server Error", null, errorBody.getBytes(), null);
        when(responseSpec.toEntity(String.class))
                .thenThrow(exception);
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        TechServicesApiResponse<RegisterUserResponse> result = liveTechServicesClient.registerNewUser(user);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.isSuccess()).isFalse();
        Assertions.assertThat(result.getError()).isNotNull();
        Assertions.assertThat(result.getError().getCode()).isEqualTo("USER_ALREADY_EXISTS");
        Assertions.assertThat(result.getError().getMessage()).isEqualTo("A user with this email already exists");
        assertLogMessage(Level.INFO, "Sending create new user request with security groups to tech services:");
        assertLogMessage(Level.INFO,
                "Error while sending new user creation request to Tech Services for firstName lastName");
        verify(restClient, times(1)).post();
    }

    @Test
    void testRegisterUserError400() {
        EntraUserDto user = EntraUserDto.builder().email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName").build();

        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(RegisterUserRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        String errorBody = """
                {
                    "success": false,
                    "code": "BAD_REQUEST",
                    "message": "givenName is required and must be a non-empty string"
                  }""";
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.BAD_REQUEST,
                "Server Error", null, errorBody.getBytes(), null);
        when(responseSpec.toEntity(String.class)).thenThrow(exception);
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        TechServicesApiResponse<RegisterUserResponse> result = liveTechServicesClient.registerNewUser(user);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.isSuccess()).isFalse();
        Assertions.assertThat(result.getError()).isNotNull();
        Assertions.assertThat(result.getError().getCode()).isEqualTo("BAD_REQUEST");
        Assertions.assertThat(result.getError().getMessage()).isEqualTo("givenName is required and must be a non-empty string");
        assertLogMessage(Level.INFO, "Sending create new user request with security groups to tech services:");
        assertLogMessage(Level.INFO,
                "Error while sending new user creation request to Tech Services for firstName lastName");
        verify(restClient, times(1)).post();
    }

    @Test
    void testRegisterUserError5Xx() {
        EntraUserDto user = EntraUserDto.builder().email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName").build();

        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(RegisterUserRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(RegisterUserResponse.class))
                .thenReturn(ResponseEntity.internalServerError().build());
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> liveTechServicesClient.registerNewUser(user),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage())
                .contains("Unexpected error while sending new user creation request to Tech Services.");
        assertLogMessage(Level.INFO, "Sending create new user request with security groups to tech services:");
        assertLogMessage(Level.ERROR,
                "Unexpected error while sending new user creation request to Tech Services");
        verify(restClient, times(1)).post();
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
