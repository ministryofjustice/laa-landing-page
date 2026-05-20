package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.laa.portal.landingpage.dto.CcmsUserDetailsResponse;
import uk.gov.justice.laa.portal.landingpage.registry.CcmsUdaRegistry;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CcmsUserDetailsService {

    private static final String CCMS_UDA_GET_USER_DETAILS_ENDPOINT = "%s/api/v1/user-details/silas/%s";

    private final CcmsUdaRegistry ccmsUdaRegistry;

    public CcmsUserDetailsResponse getUserDetailsByLegacyUserId(String appEntraOid, String legacyUserId) {
        Optional<String> udaBaseUrlOpt = ccmsUdaRegistry.getUdaBaseUrl(appEntraOid);
        Optional<String> udaApiKeyOpt = ccmsUdaRegistry.getUdaApiKey(appEntraOid);
        if (udaBaseUrlOpt.isEmpty() || ("NONE").equalsIgnoreCase(udaBaseUrlOpt.get())
                || udaApiKeyOpt.isEmpty() || ("NONE").equalsIgnoreCase(udaApiKeyOpt.get())) {
            return null;
        }

        String udaBaseUrl = udaBaseUrlOpt.get();
        String udaApiKey = udaApiKeyOpt.get();

        String url = String.format(CCMS_UDA_GET_USER_DETAILS_ENDPOINT, udaBaseUrl, legacyUserId);

        try {
            RestTemplate restTemplate = createRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Authorization", udaApiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<CcmsUserDetailsResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    CcmsUserDetailsResponse.class
            );

            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.info("CCMS UDA details not found for legacyUserId: {} 404", legacyUserId);
                return null;
            }

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }

            log.warn("Unexpected response from UDA for legacyUserId {}: status={} ", legacyUserId,
                    response.getStatusCode());
        } catch (Exception e) {
            log.warn("Error calling CCMS UDA for legacyUserId {}: {}", legacyUserId, e.getMessage());
        }

        return null;
    }

    RestTemplate createRestTemplate() {
        return new RestTemplate();
    }
}
