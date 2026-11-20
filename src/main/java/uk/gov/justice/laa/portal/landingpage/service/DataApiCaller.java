package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;

import java.time.Instant;
import java.util.UUID;

/**
 * Runs laa-data-user-api calls on behalf of the signed-in user, supplying the user's access token,
 * Entra OID and a correlation id. A 401 from the API (or a rejected OBO exchange) is translated
 * to {@link ClientAuthorizationRequiredException}, which the global handler turns into a re-authentication redirect.
 */
@Component
public class DataApiCaller {

    private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 30;

    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public DataApiCaller(OAuth2AuthorizedClientRepository authorizedClientRepository,
                         ObjectProvider<HttpServletRequest> requestProvider) {
        this.authorizedClientRepository = authorizedClientRepository;
        this.requestProvider = requestProvider;
    }

    public <T> T call(ApiCall<T> apiCall) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            throw new UserDataApiClientException("No OAuth2 authentication available for data API call", 401);
        }
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        HttpServletRequest request = requestProvider.getObject();
        OAuth2AuthorizedClient client = authorizedClientRepository.loadAuthorizedClient(
                registrationId, oauthToken, request);
        if (client == null || client.getAccessToken() == null || isExpiringSoon(client.getAccessToken())) {
            throw new ClientAuthorizationRequiredException(registrationId);
        }
        String oid = oauthToken.getPrincipal().getAttribute("oid");
        try {
            return apiCall.apply(client.getAccessToken().getTokenValue(), oid, resolveCorrelationId(request));
        } catch (UserDataApiClientException e) {
            if (e.getStatusCode() == 401) {
                // Handled by GlobalControllerAdvice: redirects the user to re-authenticate
                throw new ClientAuthorizationRequiredException(registrationId);
            }
            throw e;
        } catch (HttpClientErrorException e) {
            // The OBO token exchange rejected the user's access token (e.g. invalid_grant)
            int status = e.getStatusCode().value();
            if (status == 400 || status == 401) {
                throw new ClientAuthorizationRequiredException(registrationId);
            }
            throw e;
        }
    }

    private boolean isExpiringSoon(OAuth2AccessToken accessToken) {
        Instant expiresAt = accessToken.getExpiresAt();
        return expiresAt != null && !expiresAt.isAfter(Instant.now().plusSeconds(TOKEN_EXPIRY_BUFFER_SECONDS));
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String header = request.getHeader("X-Correlation-ID");
        return header != null && !header.isBlank() ? header : UUID.randomUUID().toString();
    }

    @FunctionalInterface
    public interface ApiCall<T> {
        T apply(String token, String oid, String correlationId);
    }
}
