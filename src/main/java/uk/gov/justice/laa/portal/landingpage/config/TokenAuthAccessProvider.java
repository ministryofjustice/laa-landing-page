package uk.gov.justice.laa.portal.landingpage.config;

import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.authentication.AuthenticationProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Auth provider (using access token) class for creating graph api client.
 */
public class TokenAuthAccessProvider implements AuthenticationProvider {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String OAUTH_BEARER_PREFIX = "Bearer ";
    private final String accessToken;

    public TokenAuthAccessProvider(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public void authenticateRequest(@NotNull RequestInformation request, @Nullable Map<String, Object> additionalAuthenticationContext) {

        if (request.headers.containsKey(AUTHORIZATION_HEADER_NAME)) {
            // Found an existing authorization header so don't add another
            return;
        }

        request.headers.add(AUTHORIZATION_HEADER_NAME, OAUTH_BEARER_PREFIX + accessToken);
    }
}
