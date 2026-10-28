package uk.gov.justice.laa.portal.landingpage.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uk.gov.justice.laa.datauserapi.contracts.request.BulkDeactivateFirmUsersRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CommandResult;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmOfficesResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmPageResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmSearchResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.UserAccountSummaryPage;
import uk.gov.justice.laa.portal.landingpage.service.OboTokenService;

import java.util.UUID;
import java.nio.charset.StandardCharsets;

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
                                       String firm, String firmType, int pageNumber, int pageSize, String sortBy) {
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
                    uriBuilder.queryParam("firm", firm);
                }
                if (firmType != null && !firmType.isBlank()) {
                    uriBuilder.queryParam("firm_type", firmType);
                }
                return uriBuilder.build();
            });
        return retrieve(spec, oboToken, cid).body(FirmPageResponse.class);
    }

    @Override
    public UserAccountSummaryPage queryUsersByEmail(String userAccessToken, String userOid, String correlationId,
                                                   String email, int pageNumber) {
        String cid = resolveCorrelationId(correlationId);
        String oboToken = oboTokenService.acquireOboToken(userAccessToken, userOid);
        return retrieve(userDataApiRestClient.get()
                .uri(builder -> builder.path("/api/v1/queries/users")
                        .queryParam("email_query", "{email}")
                        .queryParam("page_number", pageNumber)
                        .queryParam("page_size", 100)
                        .build(email)), oboToken, cid).body(UserAccountSummaryPage.class);
    }

    @Override
    public CreateExternalUserResult createExternalUser(String userAccessToken, String userOid, String correlationId,
                                                       CreateExternalUserRequest request) {
        String cid = resolveCorrelationId(correlationId);
        String oboToken = oboTokenService.acquireOboToken(userAccessToken, userOid);
        ResponseEntity<CreateExternalUserResult> response;
        try {
            response = retrieve(userDataApiRestClient.post()
                    .uri("/api/v1/commands/users/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request), oboToken, cid).toEntity(CreateExternalUserResult.class);
        } catch (RestClientException e) {
            logger.error("Create-user transport or response failure: correlationId={}", cid, e);
            throw new UserDataApiClientException("Create-user request failed", 502, e);
        }
        CreateExternalUserResult result = response.getBody();
        if (response.getStatusCode() != HttpStatus.CREATED || result == null
                || !result.success() || result.userEntraObjectId() == null) {
            logger.error("Invalid create-user result: correlationId={}, status={}", cid, response.getStatusCode());
            throw new UserDataApiClientException("Invalid create-user result", 502);
        }
        return result;
    }

    @Override
    public FirmSearchResponse queryFirmsSearch(String userAccessToken, String userOid, String correlationId,
                                               String query, int limit) {
        String cid = resolveCorrelationId(correlationId);
        String oboToken = oboTokenService.acquireOboToken(userAccessToken, userOid);
        logger.debug("Calling data API GET /queries/firms/search: correlationId={}", cid);

        RestClient.RequestHeadersSpec<?> spec = userDataApiRestClient.get()
            .uri(uriBuilder -> uriBuilder.path("/api/v1/queries/firms/search")
                .queryParam("query", "{query}")
                .queryParam("limit", limit)
                .build(query));
        return retrieve(spec, oboToken, cid).body(FirmSearchResponse.class);
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
                logger.warn("Data API error: correlationId={}, status={}, method={}, uri={}, response={}",
                    cid, status, request.getMethod(), request.getURI(),
                    new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
                throw new UserDataApiClientException("Data API returned status " + status, status);
            });
    }

    private String resolveCorrelationId(String correlationId) {
        return (correlationId != null && !correlationId.isBlank())
            ? correlationId
            : UUID.randomUUID().toString();
    }
}
