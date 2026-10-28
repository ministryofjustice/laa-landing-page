package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDataApiRequestExecutor {

    private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 30;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public <T> T call(ApiCall<T> apiCall) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            throw new UserDataApiClientException("No OAuth2 authentication available for data API call", 401);
        }
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        HttpServletRequest request = requestProvider.getObject();
        OAuth2AuthorizedClient client = authorizedClientRepository.loadAuthorizedClient(
                registrationId, oauthToken, request);
        if (client == null || client.getAccessToken() == null
                || (client.getAccessToken().getExpiresAt() != null
                && !client.getAccessToken().getExpiresAt().isAfter(Instant.now().plusSeconds(TOKEN_EXPIRY_BUFFER_SECONDS)))) {
            throw new ClientAuthorizationRequiredException(registrationId);
        }
        String oid = oauthToken.getPrincipal().getAttribute("oid");
        String header = request.getHeader("X-Correlation-ID");
        String correlationId = header != null && !header.isBlank() ? header : UUID.randomUUID().toString();
        try {
            return apiCall.apply(client.getAccessToken().getTokenValue(), oid, correlationId);
        } catch (UserDataApiClientException e) {
            log.warn("Data API request failed: correlationId={}, status={}", correlationId, e.getStatusCode(), e);
            if (e.getStatusCode() == 401) {
                throw new ClientAuthorizationRequiredException(registrationId);
            }
            throw e;
        } catch (HttpClientErrorException e) {
            // API responses are mapped by the client; these errors originate in the OBO exchange.
            log.warn("OBO exchange failed: correlationId={}, status={}", correlationId, e.getStatusCode());
            int status = e.getStatusCode().value();
            if (status == 400 || status == 401) {
                throw new ClientAuthorizationRequiredException(registrationId);
            }
            throw e;
        } catch (RestClientException e) {
            log.error("Data API transport or response failure: correlationId={}", correlationId, e);
            throw new UserDataApiClientException("Data API request failed", 502, e);
        }
    }

    @FunctionalInterface
    public interface ApiCall<T> {
        T apply(String token, String oid, String correlationId);
    }
}
