package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsyncConfigurationTest {

    ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(AsyncConfiguration.class);

    @Test
    public void should_check_presence_of_example_service() {
        try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext( AsyncConfiguration.class )) {
            Map<String,Object> beans = applicationContext.getBeansWithAnnotation(EnableAsync.class);
            assertEquals(1, beans.size());
            assertTrue(beans.containsKey("asyncConfiguration"));
        }
    }
}
