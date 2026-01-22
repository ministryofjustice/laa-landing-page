package uk.gov.justice.laa.portal.landingpage.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuration for PDA (Provider Data API) client.
 */
@Configuration
public class DataProviderConfig {

    @Value("${app.data.provider.base-url}")
    private String dataProviderBaseUrl;

    @Value("${app.data.provider.api-key}")
    private String dataProviderApiKey;

    @Value("${app.data.provider.req.read.timeout:30}")
    private int dataProviderReqReadTimeout;

    @Value("${app.data.provider.req.connect.timeout:30}")
    private int dataProviderReqConnectTimeout;

    @Bean
    public RestClient dataProviderRestClient() {
        return RestClient.builder()
                .requestFactory(getDataProviderClientHttpRequestFactory())
                .baseUrl(dataProviderBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("x-authorization", dataProviderApiKey)
                .build();
    }

    private ClientHttpRequestFactory getDataProviderClientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(dataProviderReqReadTimeout));
        factory.setConnectTimeout(Duration.ofSeconds(dataProviderReqConnectTimeout));
        return factory;
    }
}
