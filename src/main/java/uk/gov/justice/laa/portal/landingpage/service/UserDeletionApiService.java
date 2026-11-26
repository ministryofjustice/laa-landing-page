package uk.gov.justice.laa.portal.landingpage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.datauserapi.contracts.request.DeleteUserRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CommandResult;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;

import java.util.UUID;

/**
 * Deletes a user through laa-data-user-api when the delete migration flag is enabled.
 */
@Service
public class UserDeletionApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDeletionApiService.class);

    private final UserDataApiClient userDataApiClient;
    private final DataApiCaller dataApiCaller;
    private final DataApiFeatureFlagService featureFlags;

    public UserDeletionApiService(UserDataApiClient userDataApiClient,
                                  DataApiCaller dataApiCaller,
                                  DataApiFeatureFlagService featureFlags) {
        this.userDataApiClient = userDataApiClient;
        this.dataApiCaller = dataApiCaller;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        return featureFlags.isUserDeleteDataApiEnabled();
    }

    public void deleteUser(Authentication authentication, String userEntraObjectId, String reasonCode) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            throw new ClientAuthorizationRequiredException("azure");
        }

        CommandResult result = dataApiCaller.call(oauthToken, (token, oid, correlationId) ->
                userDataApiClient.deleteUser(token, oid, correlationId, new DeleteUserRequest(
                        UUID.fromString(userEntraObjectId),
                        uk.gov.justice.laa.datauserapi.contracts.domain.DeleteUserReason.valueOf(reasonCode))));

        if (result == null || !result.success()) {
            LOGGER.error("Data API did not confirm user deletion: status=502");
            throw new UserDataApiClientException("Data API did not confirm user deletion", 502);
        }
    }
}
