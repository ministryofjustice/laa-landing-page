package uk.gov.justice.laa.portal.landingpage.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
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

    @Value("${app.data.provider.use-local-file}")
    private boolean useLocalFile;

    @Value("${app.data.provider.local-file-path}")
    private String localFilePath;

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

        // 1. Connection Config (Socket layer connect timeout)
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(dataProviderReqConnectTimeout))
                .build();

        // 2. Connection Manager Pool
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .build();

        // 3. Request Config (Application layer read/response timeout)
        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(dataProviderReqReadTimeout))
                .build();

        // 4. Assemble the client
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    public boolean isUseLocalFile() {
        return useLocalFile;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }
}
