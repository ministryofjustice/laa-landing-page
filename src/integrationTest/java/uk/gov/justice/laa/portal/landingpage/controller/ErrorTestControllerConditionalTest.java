package uk.gov.justice.laa.portal.landingpage.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "app.test.error-pages.enabled=false",
    "spring.main.allow-bean-definition-overriding=true"
})
class ErrorTestControllerConditionalTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void errorTestController_whenPropertyDisabled_shouldNotBeLoaded() {
        // Verify that ErrorTestController bean is not created when property is false
        boolean beanExists = applicationContext.containsBean("errorTestController");
        assertFalse(beanExists, "ErrorTestController should not be loaded when app.test.error-pages.enabled=false");
    }

    @Test
    void errorTestController_whenPropertyDisabled_shouldNotHaveControllerInContext() {
        // Alternative check using bean type
        String[] beanNames = applicationContext.getBeanNamesForType(ErrorTestController.class);
        assertFalse(beanNames.length > 0, "No ErrorTestController beans should exist when property is disabled");
    }
}
