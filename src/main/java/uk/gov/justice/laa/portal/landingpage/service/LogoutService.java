package uk.gov.justice.laa.portal.landingpage.service;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class LogoutService {

    @Value("${spring.security.oauth2.client.registration.azure.base-url}")
    private String baseUrl;

    @Value("${spring.security.oauth2.client.registration.azure.tenant-id}")
    private String tenantId;

    /**
     * Builds the Azure AD logout URL with post-logout redirect
     * 
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

    /**
     * Terminates an Azure AD session by calling the logoutsession endpoint
     * This is typically called after the user confirms logout on Azure AD
     * 
     * @param sessionId The session ID to terminate
     * @param canaryToken The CSRF canary token from Azure AD
     * @param cookies The session cookies from the user's browser
     * @return true if session termination was successful
     */
    public boolean terminateAzureSession(String sessionId, String canaryToken, String cookies) {
        try {
            String logoutSessionUrl = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/logoutsession";
            String postLogoutRedirectUri = baseUrl + "/?message=logout";

            RestTemplate restTemplate = new RestTemplate();
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
            headers.set("Accept-Language", "en-GB,en;q=0.9");
            headers.set("Cache-Control", "max-age=0");
            headers.set("Origin", "https://login.microsoftonline.com");
            headers.set("Referer", buildAzureLogoutUrl());
            headers.set("User-Agent", "LAA-Portal-Application/1.0");
            
            if (cookies != null && !cookies.isEmpty()) {
                headers.set("Cookie", cookies);
            }

            // Prepare form data
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("sessionId", sessionId);
            formData.add("canary", canaryToken);
            formData.add("post_logout_redirect_uri", postLogoutRedirectUri);
            formData.add("postLogoutRedirectUriValid", "1");
            formData.add("state", "");
            formData.add("msaSession", "0");
            formData.add("ctx", "");
            formData.add("i19", "");

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
            
            // Make the request
            ResponseEntity<String> response = restTemplate.postForEntity(logoutSessionUrl, requestEntity, String.class);
            
            return response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection();
            
        } catch (Exception e) {
            // Log the error but don't fail the logout process
            System.err.println("Failed to terminate Azure session: " + e.getMessage());
            return false;
        }
    }
}
