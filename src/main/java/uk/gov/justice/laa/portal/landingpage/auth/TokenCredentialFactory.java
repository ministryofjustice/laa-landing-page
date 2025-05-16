package uk.gov.justice.laa.portal.landingpage.auth;

import com.azure.core.credential.TokenCredential;

public interface TokenCredentialFactory {
    TokenCredential createCredential();
}
