package uk.gov.justice.laa.portal.landingpage.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.service.DoNothingTechServicesClient;
import uk.gov.justice.laa.portal.landingpage.service.LiveTechServicesClient;
import uk.gov.justice.laa.portal.landingpage.service.TechServicesClient;

import java.time.Duration;

@Configuration
public class TechServicesConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${app.tech.services.req.read.timeout:30}")
    private int technicalServicesReqReadTimeout;

    @Value("${app.tech.services.req.connect.timeout:30}")
    private int technicalServicesReqConnectTimeout;

    @Bean
    public ClientSecretCredential techServicesClientSecretCredential(
            @Value("${spring.security.tech.services.credentials.client-id}") String clientId,
            @Value("${spring.security.tech.services.credentials.client-secret}") String clientSecret,
            @Value("${spring.security.tech.services.credentials.tenant-id}") String tenantId) {
        return new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();
    }

    @Bean
    public RestClient restClient(@Value("${spring.security.tech.services.credentials.base-url}") String techServicesBaseUrl) {
        return RestClient.builder()
                .requestFactory(getClientHttpRequestFactory())
                .baseUrl(techServicesBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public ClientHttpRequestFactory getClientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(technicalServicesReqReadTimeout));
        factory.setConnectTimeout(Duration.ofSeconds(technicalServicesReqConnectTimeout));
        return factory;
    }

    @Bean
    @ConditionalOnProperty(
            value = "app.enable.tech.services.call",
            havingValue = "true",
            matchIfMissing = true
    )
    public TechServicesClient liveTechServicesClient(ClientSecretCredential clientSecretCredential, RestClient restClient,
                                                     EntraUserRepository entraUserRepository, CacheManager cacheManager,
                                                     @Qualifier("tokenExpiryJwtDecoder") JwtDecoder jwtDecoder, ObjectMapper objectMapper) {
        return new LiveTechServicesClient(clientSecretCredential, restClient, entraUserRepository, cacheManager, jwtDecoder, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(
            value = "app.enable.tech.services.call",
            havingValue = "false",
            matchIfMissing = false
    )
    public TechServicesClient doNothingTechServicesClient() {
        return new DoNothingTechServicesClient();
    }

    @Bean("tokenExpiryJwtDecoder")
    public JwtDecoder jwtDecoderForTokenExpiry() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();

        OAuth2TokenValidator<Jwt> withAudienceAndTimestamp =
                new DelegatingOAuth2TokenValidator<>(timestampValidator);

        jwtDecoder.setJwtValidator(withAudienceAndTimestamp);
        return jwtDecoder;
    }
}
