package uk.gov.justice.laa.portal.landingpage.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configures RestClient beans for laa-data-user-api communication.
 *
 * <p>Two beans are registered:
 * <ul>
 *   <li>{@code userDataApiRestClient} — pre-configured with the data API base URL,
 *       used by {@link uk.gov.justice.laa.portal.landingpage.client.LiveUserDataApiClient}.</li>
 *   <li>{@code oboRestClient} — plain RestClient without a base URL, used by
 *       {@link uk.gov.justice.laa.portal.landingpage.service.OboTokenService} to POST to
 *       the Entra token endpoint.</li>
 * </ul>
 *
 * <p>Environment variables:
 * <ul>
 *   <li>{@code USER_DATA_API_BASE_URL} — internal cluster URL (default: {@code http://laa-data-user-api})</li>
 *   <li>{@code USER_DATA_API_REQ_READ_TIMEOUT} / {@code USER_DATA_API_REQ_CONNECT_TIMEOUT} — timeouts in seconds</li>
 * </ul>
 */
@Configuration
public class UserDataApiConfig {

    @Value("${user.data.api.base-url}")
    private String baseUrl;

    @Value("${user.data.api.req.read.timeout:30}")
    private int readTimeoutSeconds;

    @Value("${user.data.api.req.connect.timeout:30}")
    private int connectTimeoutSeconds;

    @Bean
    public RestClient userDataApiRestClient() {
        HttpClient jdkHttpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
            .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkHttpClient);
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        return RestClient.builder()
            .requestFactory(factory)
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            .build();
    }

    @Bean
    public RestClient oboRestClient() {
        return RestClient.builder()
            .requestFactory(buildRequestFactory())
            .defaultHeader("Accept", "application/json")
            .build();
    }

    private HttpComponentsClientHttpRequestFactory buildRequestFactory() {
        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(connectTimeoutSeconds))
                        .build())
                    .build())
            .setDefaultRequestConfig(RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(readTimeoutSeconds))
                .build())
            .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
