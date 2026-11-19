package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.justice.laa.datauserapi.contracts.request.DeleteUserRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CommandResult;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Deletes a user through laa-data-user-api when the delete migration flag is enabled.
 */
@Service
public class UserDeletionApiService {

    private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 30;
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDeletionApiService.class);

    private final UserDataApiClient userDataApiClient;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final DataApiFeatureFlagService featureFlags;
    private final Clock clock;

    public UserDeletionApiService(UserDataApiClient userDataApiClient,
                                  OAuth2AuthorizedClientRepository authorizedClientRepository,
                                  ObjectProvider<HttpServletRequest> requestProvider,
                                  DataApiFeatureFlagService featureFlags,
                                  Clock clock) {
        this.userDataApiClient = userDataApiClient;
        this.authorizedClientRepository = authorizedClientRepository;
        this.requestProvider = requestProvider;
        this.featureFlags = featureFlags;
        this.clock = clock;
    }

    public boolean isEnabled() {
        return featureFlags.isUserDeleteDataApiEnabled();
    }

    public void deleteUser(Authentication authentication, String userEntraObjectId, String reasonCode) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            throw new ClientAuthorizationRequiredException("azure");
        }

        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        HttpServletRequest request = requestProvider.getObject();
        OAuth2AuthorizedClient authorizedClient = authorizedClientRepository.loadAuthorizedClient(
                registrationId, oauthToken, request);
        if (authorizedClient == null || authorizedClient.getAccessToken() == null
                || isExpiringSoon(authorizedClient.getAccessToken())) {
            throw new ClientAuthorizationRequiredException(registrationId);
        }

        String userOid = oauthToken.getPrincipal().getAttribute("oid");
        String correlationId = resolveCorrelationId(request);
        DeleteUserRequest deleteRequest = new DeleteUserRequest(
                UUID.fromString(userEntraObjectId),
                uk.gov.justice.laa.datauserapi.contracts.domain.DeleteUserReason.valueOf(reasonCode));

        CommandResult result;
        try {
            result = userDataApiClient.deleteUser(
                    authorizedClient.getAccessToken().getTokenValue(), userOid, correlationId, deleteRequest);
        } catch (UserDataApiClientException exception) {
            if (exception.getStatusCode() == 401) {
                throw new ClientAuthorizationRequiredException(registrationId);
            }
            throw exception;
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().value() == 400 || exception.getStatusCode().value() == 401) {
                throw new ClientAuthorizationRequiredException(registrationId);
            }
            throw exception;
        }

        if (result == null || !result.success()) {
            LOGGER.error("Data API did not confirm user deletion: status=502");
            throw new UserDataApiClientException("Data API did not confirm user deletion", 502);
        }
    }

    private boolean isExpiringSoon(OAuth2AccessToken accessToken) {
        Instant expiresAt = accessToken.getExpiresAt();
        return expiresAt != null && !expiresAt.isAfter(clock.instant().plusSeconds(TOKEN_EXPIRY_BUFFER_SECONDS));
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String header = request.getHeader("X-Correlation-ID");
        return header != null && !header.isBlank() ? header : UUID.randomUUID().toString();
    }
}
