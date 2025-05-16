package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.justice.laa.portal.landingpage.auth.DefaultTokenCredentialFactory;
import uk.gov.justice.laa.portal.landingpage.auth.TokenCredentialFactory;

@Configuration
public class TokenCredentialFactoryConfig {

    @Bean
    public TokenCredentialFactory tokenCredentialFactory(
            @Value("${spring.security.oauth2.client.registration.azure.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.azure.client-secret}") String clientSecret,
            @Value("${spring.security.oauth2.client.registration.azure.tenant-id}") String tenantId) {
        return new DefaultTokenCredentialFactory(clientId, clientSecret, tenantId);
    }
}
