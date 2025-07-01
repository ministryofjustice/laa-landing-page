package uk.gov.justice.laa.portal.landingpage.config;

import com.azure.core.credential.TokenCredential;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

}
