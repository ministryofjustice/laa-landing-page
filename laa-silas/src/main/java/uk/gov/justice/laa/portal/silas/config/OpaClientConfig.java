package uk.gov.justice.laa.portal.silas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OpaClientConfig {

    @Bean
    public RestClient opaRestClient(@Value("${opa.base-url:http://localhost:8181}") String opaBaseUrl) {
        return RestClient.builder()
                .baseUrl(opaBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
