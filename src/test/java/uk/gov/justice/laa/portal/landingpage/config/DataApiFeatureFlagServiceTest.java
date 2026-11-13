package uk.gov.justice.laa.portal.landingpage.config;

import io.getunleash.Unleash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataApiFeatureFlagServiceTest {

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void journeyFlagIsIndependentOfFirmDirectory(boolean create, boolean directory) {
        DataApiFeatureFlagService flags = new DataApiFeatureFlagService(null, false, false, directory, create);
        assertThat(flags.isCreateUserDataApiEnabled()).isEqualTo(create);
        assertThat(flags.isFirmDirectoryDataApiEnabled()).isEqualTo(directory);
    }

    @Test
    void runtimeFlagChangesTakeEffectWithoutRestart() {
        Unleash unleash = mock(Unleash.class);
        DataApiFeatureFlagService flags = new DataApiFeatureFlagService(unleash, false, false, false, false);
        when(unleash.isEnabled("create-user-data-api-enabled", false)).thenReturn(false, true, false);
        assertThat(flags.isCreateUserDataApiEnabled()).isFalse();
        assertThat(flags.isCreateUserDataApiEnabled()).isTrue();
        assertThat(flags.isCreateUserDataApiEnabled()).isFalse();
    }
}
