package uk.gov.justice.laa.portal.landingpage.service.firmdirectory;

import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;

/**
 * Resolves the {@link FirmDirectoryProvider} using the configured feature flag.
 */
@Component
public class FirmDirectoryProviderResolver {

    private final LocalFirmDirectoryProvider localProvider;
    private final DataApiFirmDirectoryProvider dataApiProvider;
    private final DataApiFeatureFlagService dataApiFeatureFlagService;

    public FirmDirectoryProviderResolver(LocalFirmDirectoryProvider localProvider,
                                         DataApiFirmDirectoryProvider dataApiProvider,
                                         DataApiFeatureFlagService dataApiFeatureFlagService) {
        this.localProvider = localProvider;
        this.dataApiProvider = dataApiProvider;
        this.dataApiFeatureFlagService = dataApiFeatureFlagService;
    }

    public FirmDirectoryProvider resolve() {
        return dataApiFeatureFlagService.isFirmDirectoryDataApiEnabled() ? dataApiProvider : localProvider;
    }
}
