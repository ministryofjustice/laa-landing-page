package uk.gov.justice.laa.portal.landingpage.config;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class DataApiFeatureFlagServiceTest {

    @Test
    void userCreationAndDeletionDefaultToDisabled() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(DataApiFeatureFlagService.class);
            context.refresh();

            DataApiFeatureFlagService flags = context.getBean(DataApiFeatureFlagService.class);
            assertThat(flags.isCreateUserDataApiEnabled()).isFalse();
            assertThat(flags.isUserDeleteDataApiEnabled()).isFalse();
        }
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void userCreationAndDeletionFlagsAreIndependent(boolean createEnabled, boolean deleteEnabled) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                    "feature.flag.create.user.data.api", Boolean.toString(createEnabled),
                    "feature.flag.user.delete.data.api", Boolean.toString(deleteEnabled))));
            context.register(DataApiFeatureFlagService.class);
            context.refresh();

            DataApiFeatureFlagService flags = context.getBean(DataApiFeatureFlagService.class);
            assertThat(flags.isCreateUserDataApiEnabled()).isEqualTo(createEnabled);
            assertThat(flags.isUserDeleteDataApiEnabled()).isEqualTo(deleteEnabled);
        }
    }
}
