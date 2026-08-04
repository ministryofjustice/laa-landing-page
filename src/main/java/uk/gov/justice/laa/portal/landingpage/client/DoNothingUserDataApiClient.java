package uk.gov.justice.laa.portal.landingpage.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * No-op implementation of {@link UserDataApiClient}.
 *
 * <p>Active when {@code app.enable.user.data.api.call=false}.
 * Use this in local development environments where laa-data-user-api is not reachable.
 * Set {@code USER_DATA_API_CALLS_ENABLED=false} to activate.
 */
@Service
@ConditionalOnProperty(name = "app.enable.user.data.api.call", havingValue = "false")
public class DoNothingUserDataApiClient implements UserDataApiClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Map<String, String> me(String userAccessToken, String userOid, String correlationId) {
        logger.debug("DoNothing: skipping data API /me call (USER_DATA_API_CALLS_ENABLED=false)");
        return Map.of("oid", "", "sub", "");
    }
}
