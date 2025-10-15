package uk.gov.justice.laa.portal.landingpage.service;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserRequest;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.SendUserVerificationEmailRequest;
import uk.gov.justice.laa.portal.landingpage.techservices.SendUserVerificationEmailResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesErrorResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsRequest;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsResponse;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class LiveTechServicesClient implements TechServicesClient {

    public static final String ACCESS_TOKEN = "access_token";
    private static final String TECH_SERVICES_UPDATE_USER_GRP_ENDPOINT = "%s/users/%s";
    private static final String TECH_SERVICES_REGISTER_USER_ENDPOINT = "%s/users";
    private static final String TECH_SERVICES_RESEND_VERIFICATION_EMAIL_ENDPOINT = "%s/users/%s/verify";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ClientSecretCredential clientSecretCredential;
    private final RestClient restClient;
    private final CacheManager cacheManager;
    private final JwtDecoder jwtDecoder;
    private final EntraUserRepository entraUserRepository;
    private final ObjectMapper objectMapper;
    @Value("${app.tech.services.laa.verification.method}")
    public String techServicesVerificationMethod;
    @Value("${app.tech.services.laa.business.unit}")
    private String laaBusinessUnit;
    @Value("${spring.security.tech.services.credentials.scope}")
    private String accessTokenRequestScope;
    @Value("${app.laa.default.user.access.security.group}")
    private String defaultSecurityGroup;

    public LiveTechServicesClient(ClientSecretCredential clientSecretCredential, RestClient restClient,
                                  EntraUserRepository entraUserRepository, CacheManager cacheManager,
                                  @Qualifier("tokenExpiryJwtDecoder") JwtDecoder jwtDecoder, ObjectMapper objectMapper) {
        this.clientSecretCredential = clientSecretCredential;
        this.restClient = restClient;
        this.entraUserRepository = entraUserRepository;
        this.cacheManager = cacheManager;
        this.jwtDecoder = jwtDecoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public void updateRoleAssignment(UUID userId) {

        try {
            String accessToken = getAccessToken();

            EntraUser entraUser = entraUserRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

            UpdateSecurityGroupsRequest.UpdateSecurityGroupsRequestBuilder builder = UpdateSecurityGroupsRequest.builder();

            Set<String> securityGroups = entraUser.getUserProfiles().stream()
                    .flatMap(profile -> profile.getAppRoles().stream())
                    .filter(appRole -> !appRole.isAuthzRole())
                    .filter(appRole -> Objects.nonNull(appRole.getApp().getSecurityGroupOid()))
                    .map(appRole -> appRole.getApp().getSecurityGroupOid())
                    .collect(Collectors.toSet());

            // Add the default security group
            securityGroups.add(defaultSecurityGroup);

            UpdateSecurityGroupsRequest request = builder.groups(securityGroups).build();

            logger.info("Sending update security groups request to tech services: {}", request);

            String uri = String.format(TECH_SERVICES_UPDATE_USER_GRP_ENDPOINT, laaBusinessUnit, entraUser.getEntraOid());

            ResponseEntity<UpdateSecurityGroupsResponse> response = restClient
                    .patch()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(UpdateSecurityGroupsResponse.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                UpdateSecurityGroupsResponse responseBody = response.getBody();
                assert responseBody != null;
                logger.info("Security Groups assigned successfully for {} with security groups {} added and {} with security groups removed",
                        entraUser.getFirstName() + " " + entraUser.getLastName(), responseBody.getGroupsAdded(), responseBody.getGroupsRemoved());
            } else {
                logger.error("Failed to assign security groups for user {} with error code {}", entraUser.getFirstName() + " " + entraUser.getLastName(), response.getStatusCode());
                throw new RuntimeException("Failed to assign security groups for user " + entraUser.getFirstName() + " " + entraUser.getLastName() + " with error code " + response.getStatusCode());
            }
        } catch (Exception ex) {
            logger.error("Error while sending security group changes to Tech Services.", ex);
            throw new RuntimeException("Error while sending security group changes to Tech Services.", ex);
        }

    }

    @Override
    public void deleteRoleAssignment(UUID userId) {
        EntraUser entraUser = null;
        try {
            entraUser = entraUserRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

            UpdateSecurityGroupsRequest request = UpdateSecurityGroupsRequest
                    .builder()
                    .groups(Collections.emptySet())
                    .build();

            String uri = String.format(TECH_SERVICES_UPDATE_USER_GRP_ENDPOINT, laaBusinessUnit, entraUser.getEntraOid());

            logger.info("TechServices DELETE groups: userId={}, entraOid={}, uri={}", userId, entraUser.getEntraOid(), uri);
            logger.info("Sending request to tech services to remove group memberships for deleting: {}", request);

            String accessToken = getAccessToken();
            ResponseEntity<UpdateSecurityGroupsResponse> response = restClient
                    .patch()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(UpdateSecurityGroupsResponse.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                UpdateSecurityGroupsResponse responseBody = response.getBody();
                assert responseBody != null;
                logger.info("Security Groups removed successfully for {} {} with security groups removed",
                        entraUser.getFirstName() + " " + entraUser.getLastName(), responseBody.getGroupsRemoved());
            } else {
                logger.error("Failed to remove security groups for user {} with error code {}", entraUser.getFirstName() + " " + entraUser.getLastName(), response.getStatusCode());
                throw new RuntimeException("Failed to remove security groups for user " + entraUser.getFirstName() + " " + entraUser.getLastName() + " with error code " + response.getStatusCode());
            }
        } catch (HttpClientErrorException | HttpServerErrorException httpEx) {
            String errorJson = httpEx.getResponseBodyAsString();
            // Do not throw error for 404, user may already be deleted in entra
            if (httpEx.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.warn("Tech Services DELETE groups returned 404 Not Found for userId={}, entraOid={}. User will be deleted from Silas db. Body={}",
                        userId, (entraUser != null ? entraUser.getEntraOid() : null), errorJson);
                return;
            }
            logger.error("Tech Services DELETE groups failed for userId={}, entraOid={}, status={}, body={}",
                    userId, (entraUser != null ? entraUser.getEntraOid() : null), httpEx.getStatusCode(), errorJson, httpEx);
            throw new RuntimeException("Error while sending security group removal to Tech Services: " + httpEx.getStatusCode(), httpEx);
        } catch (Exception ex) {
            logger.error("Error while sending security group removal to Tech Services.", ex);
            throw new RuntimeException("Error while sending security group removal to Tech Services.", ex);
        }

    }

    @Override
    public TechServicesApiResponse<RegisterUserResponse> registerNewUser(EntraUserDto user) {
        ResponseEntity<String> response = null;
        try {
            String accessToken = getAccessToken();

            Set<String> securityGroups = new HashSet<>();

            // Add the default security group
            securityGroups.add(defaultSecurityGroup);

            RegisterUserRequest request = RegisterUserRequest.builder()
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .verificationMethod(techServicesVerificationMethod)
                    .groups(securityGroups).build();

            logger.info("Sending create new user request with security groups to tech services: {}", request);

            String uri = String.format(TECH_SERVICES_REGISTER_USER_ENDPOINT, laaBusinessUnit);

            response = restClient
                    .post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);

            logger.info("The create user response from TS: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                RegisterUserResponse responseBody = objectMapper.readValue(response.getBody(), RegisterUserResponse.class);
                logger.info("New User creation by Tech Services is successful for {} with security groups {} added",
                        user.getFirstName() + " " + user.getLastName(), securityGroups);
                return TechServicesApiResponse.success(responseBody);
            } else {
                logger.error("Error while sending new user creation request to Tech Services, the response is {}.", response.getBody());
                throw new RuntimeException(String.format("Error while sending verification email to Tech Services, the response is %s.", response.getBody()));
            }
        } catch (HttpClientErrorException | HttpServerErrorException httpEx) {
            String errorJson = httpEx.getResponseBodyAsString();
            try {
                TechServicesErrorResponse errorResponse = objectMapper.readValue(errorJson, TechServicesErrorResponse.class);
                if (httpEx.getStatusCode().is4xxClientError()) {
                    logger.info("Error while sending new user creation request to Tech Services for {}, the root cause is {} ({}) ",
                            user.getFirstName() + " " + user.getLastName(), errorResponse.getMessage(), errorResponse.getCode(), httpEx);
                    return TechServicesApiResponse.error(errorResponse);
                }
                logger.error("Error while sending new user creation request to Tech Services for {}, the root cause is {} ({}) ",
                        user.getFirstName() + " " + user.getLastName(), errorResponse.getMessage(), errorResponse.getCode(), httpEx);
                throw httpEx;
            } catch (Exception ex) {
                String responseBody = response != null ? response.getBody() : "Unknown";
                logger.error("Error while sending new user creation request to Tech Services. The response body is {}", responseBody, ex);
                throw new RuntimeException("Error while sending new user creation request to Tech Services.", ex);
            }
        } catch (Exception ex) {
            String responseBody = response != null ? response.getBody() : "Unknown";
            logger.error("Unexpected error while sending new user creation request to Tech Services. The response is {}", responseBody, ex);
            throw new RuntimeException("Unexpected error while sending new user creation request to Tech Services.", ex);
        }
    }

    @Override
    public TechServicesApiResponse<SendUserVerificationEmailResponse> sendEmailVerification(EntraUserDto user) {
        try {
            if (user == null || user.getEntraOid() == null) {
                throw new RuntimeException("Invalid user details supplied.");
            }

            String accessToken = getAccessToken();

            SendUserVerificationEmailRequest request = SendUserVerificationEmailRequest.builder()
                    .verificationMethod("activation_code_email").build();

            logger.info("Sending Resend verification email request to tech services: {}", request);

            String uri = String.format(TECH_SERVICES_RESEND_VERIFICATION_EMAIL_ENDPOINT, laaBusinessUnit, user.getEntraOid());

            ResponseEntity<String> response = restClient
                    .post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Resend user verification email by Tech Services is successful for {} and response is {}",
                        user.getFirstName() + " " + user.getLastName(), response);
                ObjectMapper mapper = new ObjectMapper();
                SendUserVerificationEmailResponse successResponse = mapper.readValue(response.getBody(), SendUserVerificationEmailResponse.class);
                return TechServicesApiResponse.success(successResponse);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                TechServicesErrorResponse errorResponse = mapper.readValue(response.getBody(), TechServicesErrorResponse.class);
                logger.error("Failed to send verification email for {}", user.getFirstName() + " " + user.getLastName());
                return TechServicesApiResponse.error(errorResponse);
            }

        } catch (HttpClientErrorException | HttpServerErrorException httpEx) {
            String errorJson = httpEx.getResponseBodyAsString();
            ObjectMapper mapper = new ObjectMapper();
            try {
                TechServicesErrorResponse errorResponse = mapper.readValue(errorJson, TechServicesErrorResponse.class);
                logger.error("Failed to send verification email for {}, the root cause is {} ({}) ",
                        user.getFirstName() + " " + user.getLastName(), errorResponse.getMessage(), errorResponse.getCode(), httpEx);
                return TechServicesApiResponse.error(errorResponse);
            } catch (Exception ex) {
                logger.error("Error while sending verification email request to Tech Services.", ex);
                throw new RuntimeException("Error while sending verification email request to Tech Services.", ex);
            }
        } catch (Exception ex) {
            logger.error("Error while sending verification email request to Tech Services.", ex);
            throw new RuntimeException("Error while sending verification email request to Tech Services.", ex);
        }
    }

    private String getAccessToken() {
        Cache cache = cacheManager.getCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE);
        if (cache != null) {
            try {
                String accessTokenFromCache = cache.get(ACCESS_TOKEN, String.class);
                if (accessTokenFromCache != null) {
                    Jwt jwt = jwtDecoder.decode(accessTokenFromCache);
                    assert jwt.getExpiresAt() != null;
                    if (jwt.getExpiresAt().isAfter(Instant.now().plusSeconds(30))) {
                        return accessTokenFromCache;
                    }
                }
            } catch (Exception ex) {
                logger.info("Error while getting access token from cache", ex);
            }

        }

        String accessToken = Objects.requireNonNull(clientSecretCredential.getToken(new TokenRequestContext()
                .setScopes(List.of(accessTokenRequestScope))).timeout(Duration.of(60, ChronoUnit.SECONDS)).block()).getToken();
        cacheManager.getCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE).put(ACCESS_TOKEN, accessToken);

        return accessToken;
    }


}
