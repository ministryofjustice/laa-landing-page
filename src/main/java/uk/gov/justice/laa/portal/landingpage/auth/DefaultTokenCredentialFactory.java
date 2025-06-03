package uk.gov.justice.laa.portal.landingpage.auth;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.core.credential.TokenCredential;

public class DefaultTokenCredentialFactory implements TokenCredentialFactory {

    private final String clientId;
    private final String clientSecret;
    private final String tenantId;

    public DefaultTokenCredentialFactory(String clientId, String clientSecret, String tenantId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tenantId = tenantId;
    }

    @Override
    public TokenCredential createCredential() {
        return new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();
    }
}
