package uk.gov.justice.laa.portal.landingpage.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.laa.portal.landingpage.model.CcmsMessage;

/**
 * Client for communicating with the CCMS User Management API
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CcmsApiClient {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${ccms.user.management.api.base-url}")
    private String ccmsApiBaseUrl;
    
    @Value("${ccms.user.management.api.key}")
    private String ccmsApiKey;

    /**
     * Send user role change notification to CCMS API
     *
     * @param message The CCMS message to send
     * @throws CcmsApiException if the API call fails
     */
    public void sendUserRoleChange(CcmsMessage message) {
        try {
            String messageBodyJson = objectMapper.writeValueAsString(message);
            log.info("Request payload: {}", messageBodyJson);

            String apiUrl = ccmsApiBaseUrl + "/api/v1/user";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Authorization", ccmsApiKey);
            
            HttpEntity<String> requestEntity = new HttpEntity<>(messageBodyJson, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.PUT,
                requestEntity,
                String.class
            );
            
            log.info("Successfully sent role change notification. Response status: {}", response.getStatusCode());
            
        } catch (RestClientException e) {
            log.error("Failed to send role change notification to CCMS API: {}", e.getMessage(), e);
            throw new CcmsApiException("Failed to communicate with CCMS API", e);
        } catch (Exception e) {
            log.error("Unexpected error sending role change notification: {}", e.getMessage(), e);
            throw new CcmsApiException("Failed to send role change notification", e);
        }
    }
}
