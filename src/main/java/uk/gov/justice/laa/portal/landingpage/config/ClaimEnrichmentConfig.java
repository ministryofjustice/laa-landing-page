package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ClaimEnrichmentConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}