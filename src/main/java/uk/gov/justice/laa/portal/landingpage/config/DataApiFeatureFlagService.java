package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DataApiFeatureFlagService {

    private final boolean apiCallEnabled;
    private final boolean requestTokenEnabled;
    private final boolean firmDirectoryApiEnabled;
    private final boolean userDeleteApiEnabled;

    public DataApiFeatureFlagService(
            @Value("${app.enable.user.data.api.call:false}") boolean apiCallEnabled,
            @Value("${app.enable.user.data.api.request.token:false}") boolean requestTokenEnabled,
            @Value("${feature.flag.firm.directory.data.api:false}") boolean firmDirectoryApiEnabled,
            @Value("${feature.flag.user.delete.data.api:false}") boolean userDeleteApiEnabled) {
        this.apiCallEnabled = apiCallEnabled;
        this.requestTokenEnabled = requestTokenEnabled;
        this.firmDirectoryApiEnabled = firmDirectoryApiEnabled;
        this.userDeleteApiEnabled = userDeleteApiEnabled;
    }

    public boolean isUserDataApiCallEnabled() {
        return apiCallEnabled;
    }

    public boolean isUserDataApiRequestTokenEnabled() {
        return requestTokenEnabled;
    }

    /**
     * When true, the Firm Directory screens use laa-data-user-api; when false they use
     * the existing direct data access.
     */
    public boolean isFirmDirectoryDataApiEnabled() {
        return firmDirectoryApiEnabled;
    }

    /**
     * When true, user deletion uses laa-data-user-api instead of the legacy deletion path.
     */
    public boolean isUserDeleteDataApiEnabled() {
        return userDeleteApiEnabled;
    }
}
