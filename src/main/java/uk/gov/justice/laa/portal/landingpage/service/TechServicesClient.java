package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsRequest;
import uk.gov.justice.laa.portal.landingpage.techservices.UpdateSecurityGroupsResponse;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TechServicesClient {

    private static final String TECH_SERVICES_ENDPOINT = "/businessUnit/%s/users/%s";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RestClient restClient;
    private final EntraUserRepository entraUserRepository;
    private final ObjectMapper objectMapper;
    @Value("${app.laa.business.unit}")
    private static String LAA_BUSINESS_UNIT;


    public TechServicesClient(RestClient restClient, EntraUserRepository entraUserRepository, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.entraUserRepository = entraUserRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    public void updateRoleAssignment(UUID userId) {
        EntraUser entraUser = entraUserRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        try {
            UpdateSecurityGroupsRequest.UpdateSecurityGroupsRequestBuilder builder = UpdateSecurityGroupsRequest.builder();

            Set<String> securityGroups = entraUser.getUserProfiles().stream()
                    .flatMap(profile -> profile.getAppRoles().stream())
                    .filter(appRole -> Objects.nonNull(appRole.getApp().getSecurityGroupOid()))
                    .map(appRole -> appRole.getApp().getSecurityGroupOid())
                    .collect(Collectors.toSet());
            builder.requiredGroups(securityGroups);

            UpdateSecurityGroupsRequest request = builder.build();
            String jsonRequest = objectMapper.convertValue(request, String.class);

            logger.info("Sending update security groups request to tech services: {}", jsonRequest);

            String uri = String.format(TECH_SERVICES_ENDPOINT, LAA_BUSINESS_UNIT, entraUser.getEntraOid());

            ResponseEntity<UpdateSecurityGroupsResponse> response = restClient
                    .patch()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonRequest)
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

}
