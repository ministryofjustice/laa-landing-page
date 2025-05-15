
package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.justice.laa.portal.landingpage.auth.DefaultTokenCredentialFactory;
import uk.gov.justice.laa.portal.landingpage.auth.TokenCredentialFactory;

@Configuration
public class TokenCredentialFactoryConfig {

    @Bean
    public TokenCredentialFactory tokenCredentialFactory() {
        return new DefaultTokenCredentialFactory(
            System.getenv("AZURE_CLIENT_ID"),
            System.getenv("AZURE_CLIENT_SECRET"),
            System.getenv("AZURE_TENANT_ID")
        );
    }
}
