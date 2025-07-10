package uk.gov.justice.laa.portal.landingpage.service;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserRequest;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsRequest;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsResponse;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TechServicesClient {

    public static final String TECH_SERVICES_VERIFICATION_METHOD = "activation_code_email";
    public static final String ACCESS_TOKEN = "access_token";
    private static final String TECH_SERVICES_UPDATE_USER_GRP_ENDPOINT = "%s/users/%s";
    private static final String TECH_SERVICES_REGISTER_USER_ENDPOINT = "%s/users";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ClientSecretCredential clientSecretCredential;
    private final RestClient restClient;
    private final CacheManager cacheManager;
    private final JwtDecoder jwtDecoder;
    private final EntraUserRepository entraUserRepository;
    @Value("${app.tech.services.laa.business.unit}")
    private String laaBusinessUnit;
    @Value("${spring.security.tech.services.credentials.scope}")
    private String accessTokenRequestScope;


    public TechServicesClient(ClientSecretCredential clientSecretCredential, RestClient restClient,
                              EntraUserRepository entraUserRepository, CacheManager cacheManager,
                              @Qualifier("tokenExpiryJwtDecoder") JwtDecoder jwtDecoder) {
        this.clientSecretCredential = clientSecretCredential;
        this.restClient = restClient;
        this.entraUserRepository = entraUserRepository;
        this.cacheManager = cacheManager;
        this.jwtDecoder = jwtDecoder;
    }

    public void updateRoleAssignment(UUID userId) {

        try {
            String accessToken = getAccessToken();

            EntraUser entraUser = entraUserRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

            UpdateSecurityGroupsRequest.UpdateSecurityGroupsRequestBuilder builder = UpdateSecurityGroupsRequest.builder();

            Set<String> securityGroups = entraUser.getUserProfiles().stream()
                    .flatMap(profile -> profile.getAppRoles().stream())
                    .filter(appRole -> Objects.nonNull(appRole.getApp().getSecurityGroupOid()))
                    .map(appRole -> appRole.getApp().getSecurityGroupOid())
                    .collect(Collectors.toSet());

            UpdateSecurityGroupsRequest request = builder.requiredGroups(securityGroups).build();

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
                UpdateSecurityGroupsResponse respBody = response.getBody();
                logger.info("Security Groups assigned successfully for {}", entraUser.getFirstName() + " " + entraUser.getLastName());
            } else {
                logger.error("Failed to assign security groups for user {} with error code {}", entraUser.getFirstName() + " " + entraUser.getLastName(), response.getStatusCode());
                throw new RuntimeException("Failed to assign security groups for user " + entraUser.getFirstName() + " " + entraUser.getLastName() + " with error code " + response.getStatusCode());
            }
        } catch (Exception ex) {
            logger.error("Error while sending security group changes to Tech Services.", ex);
            throw new RuntimeException("Error while sending security group changes to Tech Services.", ex);
        }

    }

    public RegisterUserResponse registerNewUser(EntraUserDto user, List<AppRole> validAppRoles) {
        try {
            String accessToken = getAccessToken();

            Set<String> securityGroups = validAppRoles.stream()
                    .map(appRole -> appRole.getApp().getSecurityGroupOid())
                    .collect(Collectors.toSet());

            RegisterUserRequest request = RegisterUserRequest.builder()
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .companyName("Test Company")
                    .verificationMethod(TECH_SERVICES_VERIFICATION_METHOD)
                    .requiredGroups(securityGroups).build();

            logger.info("Sending update security groups request to tech services: {}", request);

            String uri = String.format(TECH_SERVICES_REGISTER_USER_ENDPOINT, laaBusinessUnit);

            ResponseEntity<String> response = restClient
                    .post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ObjectMapper mapper = new ObjectMapper();
                RegisterUserResponse registerUserResponse = mapper.readValue(response.getBody(), RegisterUserResponse.class);
                logger.info("Security Groups assigned successfully for {}", user.getFirstName() + " " + user.getLastName());
                return registerUserResponse;
            } else {
                logger.error("Failed to assign security groups for user {} with error code {}", user.getFirstName() + " " + user.getLastName(), response.getStatusCode());
                throw new RuntimeException("Failed to assign security groups for user " + user.getFirstName() + " " + user.getLastName() + " with error code " + response.getStatusCode());
            }
        } catch (Exception ex) {
            logger.error("Error while sending security group changes to Tech Services.", ex);
            throw new RuntimeException("Error while sending security group changes to Tech Services.", ex);
        }
    }

    private String getAccessToken() {
        String accessToken = cacheManager.getCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE).get(ACCESS_TOKEN, String.class);

        if (accessToken != null) {
            try {
                Jwt jwt = jwtDecoder.decode(accessToken);
                assert jwt.getExpiresAt() != null;
                if (jwt.getExpiresAt().isAfter(Instant.now().plusSeconds(30))) {
                    return accessToken;
                }
            } catch (Exception ex) {
                logger.info("Error while getting access token from cache", ex);
            }

        }

        accessToken = Objects.requireNonNull(clientSecretCredential.getToken(new TokenRequestContext()
                .setScopes(List.of(accessTokenRequestScope))).timeout(Duration.of(60, ChronoUnit.SECONDS)).block()).getToken();
        cacheManager.getCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE).put(ACCESS_TOKEN, accessToken);

        return accessToken;
    }
}
