package uk.gov.justice.laa.portal.landingpage.client;

import uk.gov.justice.laa.datauserapi.contracts.request.BulkDeactivateFirmUsersRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CommandResult;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmOfficesResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmPageResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmSearchResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.UserProfileDetailResponse;

import java.util.UUID;

/**
 * Client for calling laa-data-user-api on behalf of the signed-in user.
 *
 * <p>Implementations must acquire an OBO access token scoped for laa-data-user-api
 * (grant type {@code urn:ietf:params:oauth2:grant-type:jwt-bearer}) and propagate
 * the {@code X-Correlation-ID} header on every outbound request.
 *
 * <p>The data API identifies the caller solely from the validated JWT {@code oid} claim —
 * no Graph lookup is performed.
 */
public interface UserDataApiClient {

    /**
     * Calls {@code GET /api/v1/me} on laa-data-user-api using an OBO token
     * derived from the supplied {@code userAccessToken}.
     *
     * @param userAccessToken  the signed-in user's OAuth2 access token (used in OBO exchange)
     * @param userOid          the user's Entra OID — used as the OBO token cache key
     * @param correlationId    value to pass as {@code X-Correlation-ID}; generate one if null
     * @return {@link UserProfileDetailResponse} for the signed-in user
     * @throws UserDataApiClientException on 4xx or 5xx response
     */
    UserProfileDetailResponse me(String userAccessToken, String userOid, String correlationId);

    /**
     * Calls {@code GET /api/v1/queries/firms}. {@code pageNumber} is zero-indexed.
     * {@code sortBy} uses the API format {@code field,direction}, e.g. {@code name,asc}.
     *
     * @param firm     optional free-text firm filter; may be null
     * @param firmType optional firm type filter, sent as the {@code firm_type} query param using the
     *                 enum name (e.g. {@code LEGAL_SERVICES_PROVIDER}); may be null
     * @throws UserDataApiClientException on 4xx or 5xx response
     */
    FirmPageResponse queryFirms(String userAccessToken, String userOid, String correlationId,
                                String firm, String firmType, int pageNumber, int pageSize, String sortBy);

    /**
     * Calls {@code GET /api/v1/queries/firms/search} (type-ahead).
     *
     * @throws UserDataApiClientException on 4xx or 5xx response
     */
    FirmSearchResponse queryFirmsSearch(String userAccessToken, String userOid, String correlationId,
                                        String query, int limit);

    /**
     * Calls {@code GET /api/v1/queries/firms/{firmId}}.
     *
     * @throws UserDataApiClientException on 4xx or 5xx response
     */
    FirmResponse queryFirmById(String userAccessToken, String userOid, String correlationId, String firmId);

    /**
     * Calls {@code GET /api/v1/queries/firms/{firmId}/offices}.
     *
     * @throws UserDataApiClientException on 4xx or 5xx response
     */
    FirmOfficesResponse queryFirmOffices(String userAccessToken, String userOid, String correlationId, String firmId);

    /**
     * Calls {@code POST /api/v1/commands/firms/bulk-deactivate-users}.
     *
     * @throws UserDataApiClientException on 4xx or 5xx response
     */
    CommandResult bulkDeactivateFirmUsers(String userAccessToken, String userOid, String correlationId,
                                          BulkDeactivateFirmUsersRequest request);
}
