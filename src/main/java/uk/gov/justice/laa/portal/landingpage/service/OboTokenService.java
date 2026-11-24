package uk.gov.justice.laa.portal.landingpage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;

import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Exchanges the signed-in user's access token for an OBO (On-Behalf-Of) token
 * scoped for laa-data-user-api using RFC 7523 / Entra ID OBO grant.
 *
 * <p>Tokens are cached per user OID. Expiry is checked using the tokenExpiryJwtDecoder
 * (no audience/issuer validation — timestamp only) consistent with LiveTechServicesClient.
 *
 * <p>The caller's identity (actor) is embedded in the OBO token's {@code oid} claim and
 * validated locally by laa-data-user-api — no Graph lookup occurs.
 *
 * <p>For local development only, {@code user.data.api.dev-token.enabled=true} skips Entra and sends the
 * synthetic bearer value {@code oid__<oid>_scope__<scope>} that laa-data-user-api's dev, local and test
 * profiles accept. It is refused at startup unless the {@code local} profile is active.
 */
@Service
public class OboTokenService implements EnvironmentAware {

    private static final String OBO_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    private static final String REQUESTED_TOKEN_USE = "on_behalf_of";
    private static final int TOKEN_EXPIRY_BUFFER_SECONDS = 30;
    private static final Pattern DEV_TOKEN_OID = Pattern.compile("[0-9a-fA-F-]+");
    private static final Pattern DEV_TOKEN_SCOPE = Pattern.compile("[A-Za-z0-9._]+");

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CacheManager cacheManager;
    private final JwtDecoder jwtDecoder;
    private final RestClient oboRestClient;

    @Value("${azure.client-id}")
    private String clientId;

    @Value("${azure.client-secret}")
    private String clientSecret;

    @Value("${user.data.api.scope}")
    private String dataApiScope;

    @Value("${user.data.api.obo.token-endpoint}")
    private String tokenEndpoint;

    @Value("${user.data.api.dev-token.enabled:false}")
    private boolean devTokenEnabled;

    @Value("${user.data.api.dev-token.scope:user_data.admin}")
    private String devTokenScope;

    private Environment environment;

    public OboTokenService(CacheManager cacheManager,
                           @Qualifier("tokenExpiryJwtDecoder") JwtDecoder jwtDecoder,
                           RestClient oboRestClient) {
        this.cacheManager = cacheManager;
        this.jwtDecoder = jwtDecoder;
        this.oboRestClient = oboRestClient;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateDevTokenConfiguration() {
        if (devTokenEnabled && !environment.acceptsProfiles(Profiles.of("local"))) {
            throw new IllegalStateException(
                "user.data.api.dev-token.enabled is only allowed with the 'local' profile");
        }
    }

    /**
     * Returns a valid OBO access token for laa-data-user-api, re-using a cached one
     * when it is not within {@value #TOKEN_EXPIRY_BUFFER_SECONDS} seconds of expiry.
     *
     * @param userAccessToken the user's current access token (from OAuth2AuthorizedClient)
     * @param userOid         the user's Entra OID — used as the cache key
     * @return OBO access token string
     */
    public String acquireOboToken(String userAccessToken, String userOid) {
        if (devTokenEnabled) {
            return devToken(userOid);
        }
        Cache cache = cacheManager.getCache(CachingConfig.USER_DATA_API_OBO_TOKENS_CACHE);

        if (cache != null) {
            String cached = cache.get(userOid, String.class);
            if (cached != null && isTokenStillValid(cached)) {
                logger.debug("OBO cache hit for user OID: {}", userOid);
                return cached;
            }
        }

        logger.debug("Acquiring new OBO token for user OID: {}", userOid);
        String newToken = exchangeToken(userAccessToken);

        if (cache != null) {
            cache.put(userOid, newToken);
        }

        return newToken;
    }

    private String devToken(String userOid) {
        if (userOid == null || !DEV_TOKEN_OID.matcher(userOid).matches()
                || !DEV_TOKEN_SCOPE.matcher(devTokenScope).matches()) {
            throw new IllegalStateException("Cannot build a dev data API token for this user or scope");
        }
        logger.warn("Using a synthetic dev token for the data API (local development only)");
        return "oid__" + userOid + "_scope__" + devTokenScope;
    }

    private boolean isTokenStillValid(String token) {
        try {
            var jwt = jwtDecoder.decode(token);
            Instant expiresAt = jwt.getExpiresAt();
            return expiresAt != null
                && expiresAt.isAfter(Instant.now().plusSeconds(TOKEN_EXPIRY_BUFFER_SECONDS));
        } catch (Exception e) {
            logger.debug("Cached OBO token failed expiry check, will refresh: {}", e.getMessage());
            return false;
        }
    }

    private String exchangeToken(String userAccessToken) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", OBO_GRANT_TYPE);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("assertion", userAccessToken);
        params.add("scope", dataApiScope);
        params.add("requested_token_use", REQUESTED_TOKEN_USE);

        OboTokenResponse response = oboRestClient
            .post()
            .uri(tokenEndpoint)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body(params)
            .retrieve()
            .body(OboTokenResponse.class);

        if (response == null || response.getAccessToken() == null) {
            throw new IllegalStateException("OBO token exchange returned null access_token");
        }

        logger.info("OBO token acquired (expires_in={}s)", response.getExpiresIn());
        return response.getAccessToken();
    }
}
