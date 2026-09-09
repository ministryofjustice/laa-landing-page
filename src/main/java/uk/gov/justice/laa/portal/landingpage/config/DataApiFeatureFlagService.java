package uk.gov.justice.laa.portal.landingpage.config;

import io.getunleash.Unleash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DataApiFeatureFlagService {

    private static final String FLAG_API_CALL = "user-data-api-calls-enabled";
    private static final String FLAG_REQUEST_TOKEN = "user-data-api-request-token-enabled";

    private final Optional<Unleash> unleash;
    private final boolean defaultApiCallEnabled;
    private final boolean defaultRequestTokenEnabled;

    public DataApiFeatureFlagService(
            @Autowired(required = false) Unleash unleash,
            @Value("${app.enable.user.data.api.call:false}") boolean defaultApiCallEnabled,
            @Value("${app.enable.user.data.api.request.token:false}") boolean defaultRequestTokenEnabled) {
        this.unleash = Optional.ofNullable(unleash);
        this.defaultApiCallEnabled = defaultApiCallEnabled;
        this.defaultRequestTokenEnabled = defaultRequestTokenEnabled;
    }

    public boolean isUserDataApiCallEnabled() {
        return unleash.map(u -> u.isEnabled(FLAG_API_CALL, defaultApiCallEnabled))
                .orElse(defaultApiCallEnabled);
    }

    public boolean isUserDataApiRequestTokenEnabled() {
        return unleash.map(u -> u.isEnabled(FLAG_REQUEST_TOKEN, defaultRequestTokenEnabled))
                .orElse(defaultRequestTokenEnabled);
    }
}
