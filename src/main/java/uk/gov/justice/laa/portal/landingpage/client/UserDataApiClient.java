package uk.gov.justice.laa.portal.landingpage.client;

import java.util.Map;

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
     * @return map containing {@code oid} and {@code sub} fields
     * @throws UserDataApiClientException on 4xx or 5xx response
     */
    Map<String, String> me(String userAccessToken, String userOid, String correlationId);
}
