package uk.gov.justice.laa.portal.landingpage.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;
import uk.gov.justice.laa.datauserapi.contracts.request.BulkDeactivateFirmUsersRequest;
import uk.gov.justice.laa.datauserapi.contracts.request.CreateUserAccountRequest;
import uk.gov.justice.laa.datauserapi.contracts.request.DeleteUserRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CommandResult;
import uk.gov.justice.laa.datauserapi.contracts.response.CreateUserAccountResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmOfficesResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmPageResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmSearchResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.UserAccountSummaryPage;
import uk.gov.justice.laa.datauserapi.contracts.response.UserProfileDetailView;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserPermissions;
import uk.gov.justice.laa.portal.landingpage.service.OboTokenService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

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
    private static final Pattern PERMISSION_NAME = Pattern.compile("[A-Z][A-Z0-9_]*");
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OboTokenService oboTokenService;
    private final RestClient userDataApiRestClient;

    public LiveUserDataApiClient(OboTokenService oboTokenService,
                                 RestClient userDataApiRestClient) {
        this.oboTokenService = oboTokenService;
        this.userDataApiRestClient = userDataApiRestClient;
    }

    @Override
    public UserProfileDetailView me(String userAccessToken, String userOid, String correlationId) {
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
            .body(UserProfileDetailView.class);
    }

    @Override
    public CurrentUserPermissions currentUserPermissions(String userAccessToken, String userOid, String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        String token = oboTokenService.acquireOboToken(userAccessToken, userOid);
        try {
            ResponseEntity<JsonNode> response = userDataApiRestClient.get()
                    .uri("/current-user/permissions")
                    .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON)
                    .headers(headers -> headers.setBearerAuth(token))
                    .header(CORRELATION_ID_HEADER, cid)
                    .retrieve()
                    .onStatus(status -> status.value() != 200, (request, error) -> {
                        throw new UserDataApiClientException("Unable to load current-user permissions",
                                error.getStatusCode().value());
                    })
                    .toEntity(JsonNode.class);
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType == null || !MediaType.APPLICATION_JSON.equalsTypeAndSubtype(contentType)) {
                throw permissionsUnavailable();
            }
            JsonNode body = response.getBody();
            JsonNode permissions = body == null ? null : body.get("permissions");
            if (permissions == null || !permissions.isArray()) {
                throw permissionsUnavailable();
            }
            Set<String> names = new HashSet<>();
            for (JsonNode permission : permissions) {
                if (!permission.isString()) {
                    throw permissionsUnavailable();
                }
                String name = permission.asString();
                if (!PERMISSION_NAME.matcher(name).matches()) {
                    throw permissionsUnavailable();
                }
                names.add(name);
            }
            return new CurrentUserPermissions(names);
        } catch (RestClientException e) {
            // Response and parser exceptions can contain upstream content.
            throw permissionsUnavailable();
        }
    }

    private UserDataApiClientException permissionsUnavailable() {
        return new UserDataApiClientException("Current-user permissions unavailable", HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @Override
    public UserProfileQueryPage queryUserProfiles(String userAccessToken, String userOid, String correlationId, UserProfileQuery query) {
        String cid = resolveCorrelationId(correlationId);
        String token = oboTokenService.acquireOboToken(userAccessToken, userOid);
        var request = userDataApiRestClient.get().uri(builder -> {
            Map<String, String> filters = new HashMap<>();
            builder.path("/api/v1/queries/user-profiles")
                .queryParam("page_number", query.pageNumber())
                .queryParam("page_size", query.pageSize())
                .queryParam("sort_by", query.sortBy());
            addFilter(builder, filters, "user", query.user());
            addFilter(builder, filters, "user_type", query.userType());
            addFilter(builder, filters, "firm", query.firm());
            addFilter(builder, filters, "firm_id", query.firmId());
            if (query.firmAdmins()) {
                builder.queryParam("firm_admin_filter", true);
            }
            if (query.multiFirmUsers()) {
                builder.queryParam("multi_firm_users_filter", true);
            }
            return builder.build(filters);
        });
        return retrieve(request, token, cid).body(UserProfileQueryPage.class);
    }

    @Override
    public UserProfileDetailView queryUserProfileById(String userAccessToken, String userOid, String correlationId, UUID profileId) {
        String cid = resolveCorrelationId(correlationId);
        String token = oboTokenService.acquireOboToken(userAccessToken, userOid);
        return retrieve(userDataApiRestClient.get().uri("/api/v1/queries/user-profiles/{id}", profileId), token, cid)
                .body(UserProfileDetailView.class);
    }

    private void addFilter(UriBuilder builder, Map<String, String> filters, String name, String value) {
        if (value != null && !value.isBlank()) {
            builder.queryParam(name, "{" + name + "}");
            filters.put(name, value);
        }
    }

    @Override
    public RoleAssignmentContract.Options queryAssignableRoles(String userAccessToken, String userOid, String correlationId, UUID profileId) {
        String cid = resolveCorrelationId(correlationId);
        String token = oboTokenService.acquireOboToken(userAccessToken, userOid);
        return retrieve(userDataApiRestClient.get().uri("/api/v1/queries/user-profiles/{id}/assignable-roles", profileId), token, cid)
                .body(RoleAssignmentContract.Options.class);
    }

    @Override
    public RoleAssignmentContract.Result assignAppRoles(String userAccessToken, String userOid, String correlationId,
                                                         RoleAssignmentContract.Command command) {
        String cid = resolveCorrelationId(correlationId);
        String token = oboTokenService.acquireOboToken(userAccessToken, userOid);
        return retrieve(userDataApiRestClient.post().uri("/api/v1/commands/user-profiles/assign-app-roles")
                .body(command), token, cid).body(RoleAssignmentContract.Result.class);
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
                    uriBuilder.queryParam("firm", "{firm}");
                }
                if (firmType != null && !firmType.isBlank()) {
                    uriBuilder.queryParam("firm_type", firmType);
                }
                // Template variables are fully encoded, unlike literal values where '+' would reach the server as a space
                return uriBuilder.build(firm == null ? "" : firm);
            });
        return retrieve(spec, oboToken, cid).body(FirmPageResponse.class);
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

    @Override
    public UserAccountSummaryPage queryUsersByEmail(String userAccessToken, String userOid, String correlationId,
                                                    String emailQuery, int pageNumber, int pageSize) {
        String cid = resolveCorrelationId(correlationId);
        String oboToken = oboTokenService.acquireOboToken(userAccessToken, userOid);
        logger.debug("Calling data API GET /queries/users: correlationId={}", cid);

        RestClient.RequestHeadersSpec<?> spec = userDataApiRestClient.get()
            .uri(uriBuilder -> uriBuilder.path("/api/v1/queries/users")
                .queryParam("email_query", "{emailQuery}")
                .queryParam("page_number", pageNumber)
                .queryParam("page_size", pageSize)
                .build(emailQuery));
        return retrieve(spec, oboToken, cid).body(UserAccountSummaryPage.class);
    }

    @Override
    public CreateUserAccountResponse createUser(String userAccessToken, String userOid, String correlationId,
                                                CreateUserAccountRequest request) {
        String cid = resolveCorrelationId(correlationId);
        String oboToken = oboTokenService.acquireOboToken(userAccessToken, userOid);
        logger.debug("Calling data API POST /commands/users/create: correlationId={}", cid);

        return retrieve(userDataApiRestClient.post()
                .uri("/api/v1/commands/users/create")
                .body(request), oboToken, cid)
            .body(CreateUserAccountResponse.class);
    }

    @Override
    public CommandResult deleteUser(String userAccessToken, String userOid, String correlationId,
                                    DeleteUserRequest request) {
        String cid = resolveCorrelationId(correlationId);
        String oboToken = oboTokenService.acquireOboToken(userAccessToken, userOid);
        logger.debug("Calling data API POST /commands/users/delete: correlationId={}", cid);

        return retrieve(userDataApiRestClient.post()
                .uri("/api/v1/commands/users/delete")
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
