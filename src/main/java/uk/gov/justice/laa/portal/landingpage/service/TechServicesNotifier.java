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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TechServicesNotifier {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final RestClient restClient;
    private final EntraUserRepository entraUserRepository;
    private final ObjectMapper objectMapper;
    @Value("${app.tech.services.sec.group.uri}")
    private String TECH_SERVICES_SEC_GROUP_URI;

    public TechServicesNotifier(RestClient restClient, EntraUserRepository entraUserRepository, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.entraUserRepository = entraUserRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    public void notifyRoleChange(UUID userId) {
        Optional<EntraUser> entraUser = entraUserRepository.findById(userId);
        if (entraUser.isPresent()) {
            UpdateSecurityGroupsRequest.UpdateSecurityGroupsRequestBuilder builder = UpdateSecurityGroupsRequest.builder();
            EntraUser user = entraUser.get();
            builder.firstName(user.getFirstName()).lastName(user.getLastName()).email(user.getEmail());

            Set<String> securityGroups = user.getUserProfiles().stream()
                    .flatMap(profile -> profile.getAppRoles().stream())
                    .map(appRole -> appRole.getApp().getSecurityGroupName() + ":" + appRole.getApp().getSecurityGroupOid())
                    .collect(Collectors.toSet());
            builder.securityGroups(securityGroups);

            UpdateSecurityGroupsRequest request = builder.build();
            String jsonRequest = objectMapper.convertValue(request, String.class);
            System.out.println("Sending update security groups request to tech services: " + jsonRequest);

            ResponseEntity<UpdateSecurityGroupsResponse> response = restClient
                    .put()
                    .uri(TECH_SERVICES_SEC_GROUP_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonRequest)
                    .retrieve()
                    .toEntity(UpdateSecurityGroupsResponse.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                UpdateSecurityGroupsResponse respBody = response.getBody();
                LOGGER.info("Security Groups assigned successfully for {}", user.getFirstName() + " " + user.getLastName());
            } else {
                LOGGER.error("Failed to assign security groups for user {} with error code {}", user.getFirstName() + " " + user.getLastName(), response.getStatusCode());
                throw new RuntimeException("Failed to assign security groups for user " + user.getFirstName() + " " + user.getLastName() + " with error code " + response.getStatusCode());
            }

        }


    }
}
