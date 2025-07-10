package uk.gov.justice.laa.portal.landingpage.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.service.TechServicesClient;

@Configuration
public class TechServicesConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

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
                .baseUrl(techServicesBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public TechServicesClient techServicesClient(ClientSecretCredential clientSecretCredential, RestClient restClient,
                                                 EntraUserRepository entraUserRepository, CacheManager cacheManager,
                                                 @Qualifier("tokenExpiryJwtDecoder") JwtDecoder jwtDecoder) {
        return new TechServicesClient(clientSecretCredential, restClient, entraUserRepository, cacheManager, jwtDecoder);
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
