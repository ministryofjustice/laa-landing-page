package uk.gov.justice.laa.portal.landingpage.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.datauserapi.contracts.request.BulkDeactivateFirmUsersRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CommandResult;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmOfficesResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmPageResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.UserProfileDetailResponse;
import uk.gov.justice.laa.portal.landingpage.service.OboTokenService;

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
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OboTokenService oboTokenService;
    private final RestClient userDataApiRestClient;

    public LiveUserDataApiClient(OboTokenService oboTokenService,
                                 RestClient userDataApiRestClient) {
        this.oboTokenService = oboTokenService;
        this.userDataApiRestClient = userDataApiRestClient;
    }

    @Override
    public UserProfileDetailResponse me(String userAccessToken, String userOid, String correlationId) {
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
                    int status = response.getStatusCode().value();
                    logger.info("Data API rejected token: status={}, correlationId={}, uri={}",
                        status, cid, request.getURI());
                    throw new UserDataApiClientException(
                        "Data API rejected token — check OBO scope and audience configuration",
                        status);
                })
            .onStatus(HttpStatusCode::is5xxServerError,
                (request, response) -> {
                    logger.error("Data API server error: correlationId={}, status={}, uri={}",
                        cid, response.getStatusCode(), request.getURI());
                    throw new UserDataApiClientException(
                        "Data API returned server error",
                        response.getStatusCode().value());
                })
            .body(UserProfileDetailResponse.class);
    }

    @Override
    public FirmPageResponse queryFirms(String userAccessToken, String userOid, String correlationId,
                                       String firm, int pageNumber, int pageSize, String sortBy) {
        String cid = resolveCorrelationId(correlationId);
        String oboToken = oboTokenService.acquireOboToken(userAccessToken, userOid);
        logger.debug("Calling data API GET /queries/firms: correlationId={}", cid);

        RestClient.RequestHeadersSpec<?> spec = userDataApiRestClient.get()
            .uri(uriBuilder -> {
                uriBuilder.path("/api/v1/queries/firms")
                    .queryParam("page_number", pageNumber)
                    .queryParam("page_size", pageSize)
                    .queryParam("sort_by", sortBy);
                if (firm != null && !firm.isBlank()) {
                    return uriBuilder.queryParam("firm", "{firm}").build(firm);
                }
                return uriBuilder.build();
            });
        return retrieve(spec, oboToken, cid).body(FirmPageResponse.class);
    }

    @Override
    public FirmResponse queryFirmById(String userAccessToken, String userOid, String correlationId, String firmId) {
        String cid = resolveCorrelationId(correlationId);
        String oboToken = oboTokenService.acquireOboToken(userAccessToken, userOid);
        logger.debug("Calling data API GET /queries/firms/{}: correlationId={}", firmId, cid);

        return retrieve(userDataApiRestClient.get().uri("/api/v1/queries/firms/{firmId}", firmId), oboToken, cid)
            .body(FirmResponse.class);
    }

    @Override
    public FirmOfficesResponse queryFirmOffices(String userAccessToken, String userOid, String correlationId,
                                                String firmId) {
        String cid = resolveCorrelationId(correlationId);
        String oboToken = oboTokenService.acquireOboToken(userAccessToken, userOid);
        logger.debug("Calling data API GET /queries/firms/{}/offices: correlationId={}", firmId, cid);

        return retrieve(userDataApiRestClient.get().uri("/api/v1/queries/firms/{firmId}/offices", firmId),
            oboToken, cid).body(FirmOfficesResponse.class);
    }

    @Override
    public CommandResult bulkDeactivateFirmUsers(String userAccessToken, String userOid, String correlationId,
                                                 BulkDeactivateFirmUsersRequest request) {
        String cid = resolveCorrelationId(correlationId);
        String oboToken = oboTokenService.acquireOboToken(userAccessToken, userOid);
        logger.debug("Calling data API POST /commands/firms/bulk-deactivate-users: correlationId={}", cid);

        return retrieve(userDataApiRestClient.post()
                .uri("/api/v1/commands/firms/bulk-deactivate-users")
                .body(request), oboToken, cid)
            .body(CommandResult.class);
    }

    /**
     * Adds auth headers and maps every 4xx/5xx to a {@link UserDataApiClientException} carrying
     * only the status code; response bodies are logged server-side and never propagated.
     */
    private RestClient.ResponseSpec retrieve(RestClient.RequestHeadersSpec<?> spec, String oboToken, String cid) {
        return spec
            .header("Authorization", "Bearer " + oboToken)
            .header(CORRELATION_ID_HEADER, cid)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (request, response) -> {
                int status = response.getStatusCode().value();
                logger.warn("Data API error: correlationId={}, status={}, method={}, uri={}",
                    cid, status, request.getMethod(), request.getURI());
                throw new UserDataApiClientException("Data API returned status " + status, status);
            });
    }

    private String resolveCorrelationId(String correlationId) {
        return (correlationId != null && !correlationId.isBlank())
            ? correlationId
            : UUID.randomUUID().toString();
    }
}
