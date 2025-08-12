package uk.gov.justice.laa.portal.landingpage.service;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class LogoutService {

    @Value("${AZURE_TENANT_ID}")
    private String tenantId;

    @Value("${BASE_URL}")
    private String baseUrl;

    /**
     * Builds the Azure AD logout URL with post-logout redirect
     * @return The complete Azure AD logout URL
     */
    public String buildAzureLogoutUrl() {
        String postLogoutRedirectUri = baseUrl + "/?message=logout";
        
        return UriComponentsBuilder
            .fromUri(URI.create("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/logout"))
            .queryParam("post_logout_redirect_uri", postLogoutRedirectUri)
            .build()
            .toUriString();
    }
}
