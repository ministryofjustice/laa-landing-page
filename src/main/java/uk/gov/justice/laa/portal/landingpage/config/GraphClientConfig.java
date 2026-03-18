package uk.gov.justice.laa.portal.landingpage.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.justice.laa.portal.landingpage.auth.TokenCredentialFactory;

@Configuration
public class GraphClientConfig {

    private final TokenCredentialFactory credentialFactory;

    @Value("${report.service.client.id}")
    private String reportClientId;

    @Value("${report.service.tenant.id}")
    private String reportTenantId;

    @Value("${report.service.secret}")
    private String reportSecret;

    public GraphClientConfig(TokenCredentialFactory credentialFactory) {
        this.credentialFactory = credentialFactory;
    }

    @Bean
    @Primary
    public GraphServiceClient graphServiceClient() {
        TokenCredential credential = credentialFactory.createCredential();
        String[] scopes = new String[]{"https://graph.microsoft.com/.default"};
        return new GraphServiceClient(credential, scopes);
    }

    @Bean
    public GraphServiceClient graphUploadClient() {
        ClientSecretCredential credential =
                new ClientSecretCredentialBuilder()
                        .clientId(reportClientId)
                        .clientSecret(reportSecret)
                        .tenantId(reportTenantId)
                        .build();
        String[] scopes = new String[]{"https://graph.microsoft.com/.default"};
        return new GraphServiceClient(credential, scopes);
    }
}
