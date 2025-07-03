package uk.gov.justice.laa.portal.landingpage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.service.TechServicesNotifier;

@Configuration
public class TechServicesConfig {

    @Value("${app.tech.services.base.url}")
    private String TECH_SERVICES_BASE_URL;

    @Bean
    public RestClient restClient() {
        return RestClient.create(TECH_SERVICES_BASE_URL);
    }

    @Bean
    public TechServicesNotifier techServicesNotifier(RestClient restClient, EntraUserRepository entraUserRepository, ObjectMapper objectMapper) {
        return new TechServicesNotifier(restClient, entraUserRepository, objectMapper);
    }
}
