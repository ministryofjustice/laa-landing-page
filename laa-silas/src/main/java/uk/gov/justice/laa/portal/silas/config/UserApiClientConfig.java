package uk.gov.justice.laa.portal.silas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UserApiClientConfig {

    @Value("${user.api.base-url}")
    private String userApiBaseUrl;

    @Bean
    public RestClient userApiRestClient() {
        return RestClient.builder()
                .baseUrl(userApiBaseUrl)
                .build();
    }
}
