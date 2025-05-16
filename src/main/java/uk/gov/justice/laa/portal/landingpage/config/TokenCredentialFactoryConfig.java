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
            @Value("${azure.client-id}") String clientId,
            @Value("${azure.client-secret}") String clientSecret,
            @Value("${azure.tenant-id}") String tenantId) {
        return new DefaultTokenCredentialFactory(clientId, clientSecret, tenantId);
    }
}
