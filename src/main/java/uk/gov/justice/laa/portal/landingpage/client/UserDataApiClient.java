package uk.gov.justice.laa.portal.landingpage.client;

import org.springframework.security.core.Authentication;

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
     * Calls {@code GET /api/v1/hello} on laa-data-user-api.
     *
     * @param authentication  the current user's Spring Security authentication
     * @param correlationId   value to pass as {@code X-Correlation-ID}; generate one if null
     * @return response body map
     * @throws UserDataApiClientException on 4xx or 5xx response
     */
    Map<String, String> hello(Authentication authentication, String correlationId);

    /**
     * Calls {@code GET /api/v1/me} on laa-data-user-api, which echoes the caller's
     * OID as derived from the validated JWT — confirming OBO token round-trip.
     *
     * @param authentication  the current user's Spring Security authentication
     * @param correlationId   value to pass as {@code X-Correlation-ID}; generate one if null
     * @return map containing {@code oid} and {@code sub} fields
     * @throws UserDataApiClientException on 4xx or 5xx response
     */
    Map<String, String> me(Authentication authentication, String correlationId);
}
