package uk.gov.justice.laa.portal.landingpage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
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
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserRequest;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsRequest;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
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
    private CacheManager cacheManager;
    @InjectMocks
    private TechServicesClient techServicesClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;
    @Mock
    private JwtDecoder jwtDecoder;

    @BeforeEach
    public void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(TechServicesClient.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        ReflectionTestUtils.setField(techServicesClient, "accessTokenRequestScope", "scope");
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

        techServicesClient.updateRoleAssignment(userId);

        assertLogMessage(Level.INFO, "Sending update security groups request to tech services:");
        assertLogMessage(Level.INFO, "Security Groups assigned successfully for firstName lastName");
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
        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(entraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restClient.patch()).thenThrow(new RuntimeException("Error sending request to Tech services"));
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> techServicesClient.updateRoleAssignment(userId),
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
                .userStatus(UserStatus.ACTIVE).startDate(LocalDateTime.now())
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
        ResponseEntity<UpdateSecurityGroupsResponse> responseEntity = ResponseEntity.badRequest().build();
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class))
                .thenReturn(responseEntity);
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

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
        when(responseSpec.toEntity(UpdateSecurityGroupsResponse.class))
                .thenReturn(responseEntity);
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

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
        when(responseSpec.toEntity(String.class))
                .thenReturn(ResponseEntity.ok("{}"));

        EntraUserDto user = EntraUserDto.builder().email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName").build();
        App app = App.builder().securityGroupOid("securityGroupOid").build();
        AppRole appRole = AppRole.builder().name("name").app(app).build();

        techServicesClient.registerNewUser(user, List.of(appRole));

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
        when(responseSpec.toEntity(String.class))
                .thenReturn(ResponseEntity.ok("{}"));
        ConcurrentMapCache concurrentMapCache = new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE);
        concurrentMapCache.put("access_token", "techServicesDetails");
        when(jwtDecoder.decode(anyString())).thenReturn(Jwt.withTokenValue("techServicesDetails")
                .header("alg", "none").claim("exp", Instant.now().plusSeconds(120)).build());
        when(cacheManager.getCache(anyString())).thenReturn(concurrentMapCache);

        EntraUserDto user = EntraUserDto.builder().email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName").build();
        App app = App.builder().securityGroupOid("securityGroupOid").build();
        AppRole appRole = AppRole.builder().name("name").app(app).build();

        techServicesClient.registerNewUser(user, List.of(appRole));

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
        App app = App.builder().securityGroupOid("securityGroupOid").build();
        AppRole appRole = AppRole.builder().name("name").app(app).build();

        when(restClient.post()).thenThrow(new RuntimeException("Error sending request to Tech services"));
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> techServicesClient.registerNewUser(user, List.of(appRole)),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage()).contains("Error while create user request to Tech Services.");
        assertLogMessage(Level.INFO, "Sending create new user request with security groups to tech services:");
        assertLogMessage(Level.ERROR, "Error while create user request to Tech Services.");
    }

    @Test
    void testRegisterUserError4Xx() {
        EntraUserDto user = EntraUserDto.builder().email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName").build();
        App app = App.builder().securityGroupOid("securityGroupOid").build();
        AppRole appRole = AppRole.builder().name("name").app(app).build();

        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(RegisterUserRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class))
                .thenReturn(ResponseEntity.badRequest().build());
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> techServicesClient.registerNewUser(user, List.of(appRole)),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage())
                .contains("Error while create user request to Tech Services.");
        assertLogMessage(Level.INFO, "Sending create new user request with security groups to tech services:");
        assertLogMessage(Level.ERROR,
                "Failed to create new user by Tech Services for user firstName lastName with error code 400 BAD_REQUEST");
        verify(restClient, times(1)).post();
    }

    @Test
    void testRegisterUserError5Xx() {
        EntraUserDto user = EntraUserDto.builder().email("test@email.com").entraOid("entraOid")
                .firstName("firstName").lastName("lastName").build();
        App app = App.builder().securityGroupOid("securityGroupOid").build();
        AppRole appRole = AppRole.builder().name("name").app(app).build();

        AccessToken token = new AccessToken("token", null);
        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(token));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(RegisterUserRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class))
                .thenReturn(ResponseEntity.internalServerError().build());
        when(cacheManager.getCache(anyString())).thenReturn(new ConcurrentMapCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE));

        RuntimeException rtEx = assertThrows(RuntimeException.class,
                () -> techServicesClient.registerNewUser(user, List.of(appRole)),
                "RuntimeException expected");

        Assertions.assertThat(rtEx).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(rtEx.getMessage())
                .contains("Error while create user request to Tech Services.");
        assertLogMessage(Level.INFO, "Sending create new user request with security groups to tech services:");
        assertLogMessage(Level.ERROR,
                "Failed to create new user by Tech Services for user firstName lastName with error code 500 INTERNAL_SERVER_ERROR");
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
