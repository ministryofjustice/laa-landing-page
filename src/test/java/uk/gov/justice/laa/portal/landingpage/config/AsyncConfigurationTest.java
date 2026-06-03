package uk.gov.justice.laa.portal.landingpage.config;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Async config test
 */
public class AsyncConfigurationTest {

    @Test
    public void asyncConfiguration() {
        try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AsyncConfig.class)) {
            Map<String, Object> beans = applicationContext.getBeansWithAnnotation(EnableAsync.class);
            assertThat(beans).hasSize(1);
            assertThat(beans).containsKey("asyncConfig");
        }
    }
}
