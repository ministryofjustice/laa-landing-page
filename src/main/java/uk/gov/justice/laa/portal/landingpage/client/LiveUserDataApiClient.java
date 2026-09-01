package uk.gov.justice.laa.portal.landingpage.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.service.OboTokenService;

import java.util.Map;
import java.util.UUID;

/**
 * Live implementation of {@link UserDataApiClient}.
 *
 * <p>Acquires an OBO access token via {@link OboTokenService}, then calls
 * laa-data-user-api with {@code Authorization: Bearer <token>} and
 * {@code X-Correlation-ID} on every request.
 *
 * <p>Enabled when {@code app.enable.user.data.api.call=true} (the default).
 * Set {@code USER_DATA_API_CALLS_ENABLED=false} in local environments where
 * the data API is not reachable.
 */
@Service
@ConditionalOnProperty(name = "app.enable.user.data.api.call", havingValue = "true", matchIfMissing = true)
public class LiveUserDataApiClient implements UserDataApiClient {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final ParameterizedTypeReference<Map<String, String>> STRING_MAP =
        new ParameterizedTypeReference<>() {};

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OboTokenService oboTokenService;
    private final RestClient userDataApiRestClient;

    public LiveUserDataApiClient(OboTokenService oboTokenService,
                                 RestClient userDataApiRestClient) {
        this.oboTokenService = oboTokenService;
        this.userDataApiRestClient = userDataApiRestClient;
    }

    @Override
    public Map<String, String> me(String userAccessToken, String userOid, String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        String oboToken = oboTokenService.acquireOboToken(userAccessToken, userOid);

        logger.debug("Calling data API /me: correlationId={}", cid);

        return userDataApiRestClient
            .get()
            .uri("/api/v1/me")
            .header("Authorization", "Bearer " + oboToken)
            .header(CORRELATION_ID_HEADER, cid)
            .retrieve()
            .onStatus(status -> status.value() == 401 || status.value() == 403,
                (request, response) -> {
                    throw new UserDataApiClientException(
                        "Data API rejected token — check OBO scope and audience configuration",
                        response.getStatusCode().value());
                })
            .onStatus(HttpStatusCode::is5xxServerError,
                (request, response) -> {
                    logger.error("Data API server error: correlationId={}, status={}, uri={}",
                        cid, response.getStatusCode(), request.getURI());
                    throw new UserDataApiClientException(
                        "Data API returned server error",
                        response.getStatusCode().value());
                })
            .body(STRING_MAP);
    }

    private String resolveCorrelationId(String correlationId) {
        return (correlationId != null && !correlationId.isBlank())
            ? correlationId
            : UUID.randomUUID().toString();
    }
}
