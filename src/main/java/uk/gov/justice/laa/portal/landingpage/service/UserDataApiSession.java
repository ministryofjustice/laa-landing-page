package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;

import java.time.Instant;
import java.util.UUID;

final class UserDataApiSession {

    private UserDataApiSession() {
    }

    static <T> T call(OAuth2AuthorizedClientRepository authorizedClients, HttpServletRequest request, ApiCall<T> apiCall) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken) || !oauthToken.isAuthenticated()) {
            throw new UserDataApiClientException("No OAuth2 authentication available for data API call", 401);
        }
        String registration = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2AuthorizedClient client = authorizedClients.loadAuthorizedClient(registration, oauthToken, request);
        if (client == null || client.getAccessToken() == null) {
            throw new ClientAuthorizationRequiredException(registration);
        }
        Instant expiry = client.getAccessToken().getExpiresAt();
        if (expiry != null && !expiry.isAfter(Instant.now().plusSeconds(30))) {
            throw new ClientAuthorizationRequiredException(registration);
        }
        String oid = oauthToken.getPrincipal().getAttribute("oid");
        String header = request.getHeader("X-Correlation-ID");
        String correlationId = header != null && !header.isBlank() ? header : UUID.randomUUID().toString();
        try {
            return apiCall.apply(client.getAccessToken().getTokenValue(), oid, correlationId);
        } catch (UserDataApiClientException e) {
            if (e.getStatusCode() == 401) {
                throw new ClientAuthorizationRequiredException(registration);
            }
            throw e;
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            if (status == 400 || status == 401) {
                throw new ClientAuthorizationRequiredException(registration);
            }
            throw e;
        }
    }

    @FunctionalInterface
    interface ApiCall<T> {
        T apply(String token, String oid, String correlationId);
    }
}
