package uk.gov.justice.laa.portal.landingpage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ErrorTestControllerConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ErrorTestController.class);

    @Test
    void errorTestController_whenPropertyDisabled_shouldNotBeLoaded() {
        contextRunner
            .withPropertyValues("app.test.error-pages.enabled=false")
            .run(context -> {
                assertFalse(context.containsBean("errorTestController"), 
                    "ErrorTestController should not be loaded when app.test.error-pages.enabled=false");
            });
    }

    @Test
    void errorTestController_whenPropertyEnabled_shouldBeLoaded() {
        contextRunner
            .withPropertyValues("app.test.error-pages.enabled=true")
            .run(context -> {
                assertTrue(context.containsBean("errorTestController"), 
                    "ErrorTestController should be loaded when app.test.error-pages.enabled=true");
            });
    }

    @Test
    void errorTestController_whenPropertyNotSet_shouldNotBeLoaded() {
        contextRunner
            .run(context -> {
                assertFalse(context.containsBean("errorTestController"), 
                    "ErrorTestController should not be loaded when property is not set");
            });
    }

    @Test
    void shouldHaveCorrectConditionalOnPropertyAnnotation() {
        // Verify the controller has the correct conditional annotation
        ConditionalOnProperty annotation = ErrorTestController.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(annotation).isNotNull();
    }

    @Test
    void shouldHaveCorrectConditionalAnnotationProperties() {
        ConditionalOnProperty annotation = ErrorTestController.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).containsExactly("app.test.error-pages.enabled");
        assertThat(annotation.havingValue()).isEqualTo("true");
    }
}
