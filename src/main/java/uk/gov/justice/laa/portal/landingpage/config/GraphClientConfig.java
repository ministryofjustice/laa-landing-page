package uk.gov.justice.laa.portal.landingpage.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.authentication.AuthenticationProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

/**
 * Config file for graph client api
 */
@Configuration
public class GraphClientConfig {

    private static final String AZURE_CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
    private static final String AZURE_TENANT_ID = System.getenv("AZURE_TENANT_ID");
    private static final String AZURE_CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");

    /**
     * Get Authenticated Graph Client for API usage
     *
     * @return Usable and authenticated Graph Client
     */
    @Bean
    @Primary
    public static GraphServiceClient getGraphClient() {

        // Create secret
        final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(AZURE_CLIENT_ID).tenantId(AZURE_TENANT_ID).clientSecret(AZURE_CLIENT_SECRET).build();

        final String[] scopes = new String[]{"https://graph.microsoft.com/.default"};

        return new GraphServiceClient(credential, scopes);
    }

    /**
     * Get the GraphApiClient using access token
     * @param accessToken the access token
     * @return the Graph service client
     */
    @Bean(name = "graphicServiceClientByAccessToken")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public static GraphServiceClient getGraphClientByAccessToken(String accessToken) {

        AuthenticationProvider authProvider = new TokenAuthAccessProvider(accessToken);

        return new GraphServiceClient(authProvider);
    }

}

