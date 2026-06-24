package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.dto.CcmsUserDetailsResponse;
import uk.gov.justice.laa.portal.landingpage.registry.CcmsUdaRegistry;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CcmsUserDetailsService {

    private static final String CCMS_UDA_GET_USER_DETAILS_ENDPOINT = "/api/v1/user-details/silas/%s";

    private final CcmsUdaRegistry ccmsUdaRegistry;

    private final RestClient.Builder restClientBuilder;

    public CcmsUserDetailsResponse getUserDetailsByLegacyUserId(String appEntraOid, String legacyUserId) {
        Optional<String> udaBaseUrlOpt = ccmsUdaRegistry.getUdaBaseUrl(appEntraOid);
        Optional<String> udaApiKeyOpt = ccmsUdaRegistry.getUdaApiKey(appEntraOid);
        if (udaBaseUrlOpt.isEmpty() || ("NONE").equalsIgnoreCase(udaBaseUrlOpt.get())
                || udaApiKeyOpt.isEmpty() || ("NONE").equalsIgnoreCase(udaApiKeyOpt.get())) {
            return null;
        }

        String udaBaseUrl = udaBaseUrlOpt.get();
        String udaApiKey = udaApiKeyOpt.get();

        try {
            RestClient restClient = restClientBuilder.baseUrl(udaBaseUrl).build();

            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(String.format(CCMS_UDA_GET_USER_DETAILS_ENDPOINT, legacyUserId)).build())
                    .header("X-Authorization", udaApiKey)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (request, response) -> {
                        log.info("CCMS UDA details not found for legacyUserId: {} 404", legacyUserId);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.warn("Unexpected response from UDA for legacyUserId {}: status={} ",
                                legacyUserId, response.getStatusCode());
                    })
                    .body(CcmsUserDetailsResponse.class);

        } catch (Exception e) {
            log.warn("Error calling CCMS UDA for legacyUserId {}: {}", legacyUserId, e.getMessage());
        }

        return null;
    }
}
