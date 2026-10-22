package uk.gov.justice.laa.portal.landingpage.service.firmdirectory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmDirectoryProviderResolverTest {

    @Mock
    private LocalFirmDirectoryProvider localFirmDirectoryProvider;
    @Mock
    private DataApiFirmDirectoryProvider dataApiFirmDirectoryProvider;
    @Mock
    private DataApiFeatureFlagService dataApiFeatureFlagService;

    private FirmDirectoryProviderResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new FirmDirectoryProviderResolver(localFirmDirectoryProvider, dataApiFirmDirectoryProvider,
                dataApiFeatureFlagService);
    }

    @Test
    void resolvesToDataApiProviderWhenFlagEnabled() {
        when(dataApiFeatureFlagService.isFirmDirectoryDataApiEnabled()).thenReturn(true);

        assertThat(resolver.resolve()).isSameAs(dataApiFirmDirectoryProvider);
    }

    @Test
    void resolvesToLocalProviderWhenFlagDisabled() {
        when(dataApiFeatureFlagService.isFirmDirectoryDataApiEnabled()).thenReturn(false);

        assertThat(resolver.resolve()).isSameAs(localFirmDirectoryProvider);
    }
}
