package uk.gov.justice.laa.portal.landingpage.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.service.TechServicesClient;

@Configuration
public class TechServicesConfig {

    @Bean
    public ClientSecretCredential techServicesClientSecretCredential(
            @Value("${spring.security.tech.services.credentials.client-id}") String clientId,
            @Value("${spring.security.tech.services.credentials.client-secret}") String clientSecret,
            @Value("${spring.security.tech.services.credentials.tenant-id}") String tenantId
    ) {
        return new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();
    }

    @Bean
    public RestClient restClient(@Value("${spring.security.tech.services.credentials.base-url}") String techServicesBaseUrl) {
        return RestClient.builder()
                .baseUrl(techServicesBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public TechServicesClient techServicesClient(ClientSecretCredential clientSecretCredential, RestClient restClient,
                                                 EntraUserRepository entraUserRepository) {
        return new TechServicesClient(clientSecretCredential, restClient, entraUserRepository);
    }
}
