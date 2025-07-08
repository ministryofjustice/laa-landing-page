package uk.gov.justice.laa.portal.landingpage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.service.TechServicesClient;

@Configuration
public class TechServicesConfig {

    @Bean
    public RestClient restClient(@Value("${TECH_SERVICES_BASE_URL}") String techServicesBaseUrl) {
        return RestClient.builder()
                .baseUrl(techServicesBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public TechServicesClient techServicesNotifier(RestClient restClient, EntraUserRepository entraUserRepository, ObjectMapper objectMapper) {
        return new TechServicesClient(restClient, entraUserRepository, objectMapper);
    }
}
