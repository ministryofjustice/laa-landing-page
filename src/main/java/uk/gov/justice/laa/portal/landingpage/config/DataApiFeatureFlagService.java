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
    private static final String FLAG_FIRM_DIRECTORY_API = "firm-directory-data-api-enabled";
    private static final String FLAG_CREATE_USER_API = "create-user-data-api-enabled";

    private final Optional<Unleash> unleash;
    private final boolean defaultApiCallEnabled;
    private final boolean defaultRequestTokenEnabled;
    private final boolean defaultFirmDirectoryApiEnabled;
    private final boolean defaultCreateUserApiEnabled;

    public DataApiFeatureFlagService(
            @Autowired(required = false) Unleash unleash,
            @Value("${app.enable.user.data.api.call:false}") boolean defaultApiCallEnabled,
            @Value("${app.enable.user.data.api.request.token:false}") boolean defaultRequestTokenEnabled,
            @Value("${feature.flag.firm.directory.data.api:false}") boolean defaultFirmDirectoryApiEnabled,
            @Value("${feature.flag.create.user.data.api:false}") boolean defaultCreateUserApiEnabled) {
        this.unleash = Optional.ofNullable(unleash);
        this.defaultApiCallEnabled = defaultApiCallEnabled;
        this.defaultRequestTokenEnabled = defaultRequestTokenEnabled;
        this.defaultFirmDirectoryApiEnabled = defaultFirmDirectoryApiEnabled;
        this.defaultCreateUserApiEnabled = defaultCreateUserApiEnabled;
    }

    public boolean isUserDataApiCallEnabled() {
        return unleash.map(u -> u.isEnabled(FLAG_API_CALL, defaultApiCallEnabled))
                .orElse(defaultApiCallEnabled);
    }

    public boolean isUserDataApiRequestTokenEnabled() {
        return unleash.map(u -> u.isEnabled(FLAG_REQUEST_TOKEN, defaultRequestTokenEnabled))
                .orElse(defaultRequestTokenEnabled);
    }

    /**
     * When true, the Firm Directory screens use laa-data-user-api; when false they use
     * the existing direct data access.
     */
    public boolean isFirmDirectoryDataApiEnabled() {
        return unleash.map(u -> u.isEnabled(FLAG_FIRM_DIRECTORY_API, defaultFirmDirectoryApiEnabled))
                .orElse(defaultFirmDirectoryApiEnabled);
    }

    public boolean isCreateUserDataApiEnabled() {
        return unleash.map(u -> u.isEnabled(FLAG_CREATE_USER_API, defaultCreateUserApiEnabled))
                .orElse(defaultCreateUserApiEnabled);
    }
}
