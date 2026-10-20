package uk.gov.justice.laa.portal.landingpage.service.firmdirectory;

import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;
import uk.gov.justice.laa.portal.landingpage.service.datasource.LocalOrDataApiResolver;

/**
 * Resolves the active {@link FirmDirectoryProvider} for the current call, delegating the
 * generic local-vs-data-api selection to {@link LocalOrDataApiResolver}.
 */
@Component
public class FirmDirectoryProviderResolver {

    private final LocalOrDataApiResolver<FirmDirectoryProvider> resolver;

    public FirmDirectoryProviderResolver(LocalFirmDirectoryProvider localFirmDirectoryProvider,
                                         DataApiFirmDirectoryProvider dataApiFirmDirectoryProvider,
                                         DataApiFeatureFlagService dataApiFeatureFlagService) {
        this.resolver = new LocalOrDataApiResolver<>(localFirmDirectoryProvider, dataApiFirmDirectoryProvider,
                dataApiFeatureFlagService::isFirmDirectoryDataApiEnabled);
    }

    public FirmDirectoryProvider resolve() {
        return resolver.resolve();
    }
}
