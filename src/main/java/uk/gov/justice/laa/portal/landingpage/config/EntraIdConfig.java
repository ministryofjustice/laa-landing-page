package uk.gov.justice.laa.portal.landingpage.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Microsoft Entra ID (Azure AD) integration.
 */
@Configuration
public class EntraIdConfig {

    @Value("${azure.tenant-id}")
    private String tenantId;

    @Value("${azure.client-id}")
    private String clientId;

    @Value("${azure.client-secret}")
    private String clientSecret;

    /**
     * Creates a GraphServiceClient for interacting with Microsoft Graph API.
     * @return Configured GraphServiceClient instance
     */
    @Bean
    public GraphServiceClient graphServiceClient() {
        final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();

        return new GraphServiceClient(credential);
    }

    /**
     * Creates a ClientSecretCredential for authenticating with Azure AD.
     * @return Configured ClientSecretCredential instance
     */
    @Bean
    public ClientSecretCredential clientSecretCredential() {
        return new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();
    }
}