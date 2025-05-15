
package uk.gov.justice.laa.portal.landingpage.config;

import com.azure.core.credential.TokenCredential;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.authentication.AuthenticationProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import uk.gov.justice.laa.portal.landingpage.auth.TokenCredentialFactory;

@Configuration
public class GraphClientConfig {

    private final TokenCredentialFactory credentialFactory;

    public GraphClientConfig(TokenCredentialFactory credentialFactory) {
        this.credentialFactory = credentialFactory;
    }

    @Bean
    public GraphServiceClient graphServiceClient() {
        TokenCredential credential = credentialFactory.createCredential();
        String[] scopes = new String[]{"https://graph.microsoft.com/.default"};
        return new GraphServiceClient(credential, scopes);
    }

    @Bean(name = "graphicServiceClientByAccessToken")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public GraphServiceClient graphClientByAccessToken(String accessToken) {
        AuthenticationProvider authProvider = new TokenAuthAccessProvider(accessToken);
        return new GraphServiceClient(authProvider);
    }
}
