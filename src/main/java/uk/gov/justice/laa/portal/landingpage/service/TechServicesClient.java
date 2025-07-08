package uk.gov.justice.laa.portal.landingpage.service;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsRequest;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsResponse;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TechServicesClient {

    private static final String TECH_SERVICES_ENDPOINT = "%s/users/%s";
    @Value("${app.tech.services.laa.business.unit}")
    private String laaBusinessUnit;
    @Value("${spring.security.tech.services.credentials.scope}")
    private String accessTokenRequestScope;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ClientSecretCredential clientSecretCredential;
    private final RestClient restClient;
    private final EntraUserRepository entraUserRepository;


    public TechServicesClient(ClientSecretCredential clientSecretCredential, RestClient restClient,
                              EntraUserRepository entraUserRepository) {
        this.clientSecretCredential = clientSecretCredential;
        this.restClient = restClient;
        this.entraUserRepository = entraUserRepository;
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

            String uri = String.format(TECH_SERVICES_ENDPOINT, laaBusinessUnit, entraUser.getEntraOid());

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

    private String getAccessToken() {
        return Objects.requireNonNull(clientSecretCredential.getToken(new TokenRequestContext()
                .setScopes(List.of(accessTokenRequestScope))).block()).getToken();
    }

}
