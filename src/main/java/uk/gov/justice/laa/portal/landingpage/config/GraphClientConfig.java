package uk.gov.justice.laa.portal.landingpage.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.justice.laa.portal.landingpage.auth.TokenCredentialFactory;

@Configuration
public class GraphClientConfig {

    private final TokenCredentialFactory credentialFactory;

    @Value("${report.service.client.id}")
    private String REPORT_CLIENT_ID;

    @Value("${report.service.tenant.id}")
    private String REPORT_TENANT_ID;

    @Value("${report.service.secret}")
    private String REPORT_SECRET;


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
                        .clientId(REPORT_CLIENT_ID)
                        .clientSecret(REPORT_SECRET)
                        .tenantId(REPORT_TENANT_ID)
                        .build();
        String[] scopes = new String[]{"https://graph.microsoft.com/.default"};
        return new GraphServiceClient(credential, scopes);
    }
}
