package uk.gov.justice.laa.portal.landingpage.forms;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;




class ApplicationsFormTest {

    private final Validator validator;

    public ApplicationsFormTest() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void whenAppsIsNull_thenValidationFails() {
        ApplicationsForm form = new ApplicationsForm();
        form.setApps(null);

        Set<ConstraintViolation<ApplicationsForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("At least one service must be selected")));
    }

    @Test
    void whenAppsIsEmpty_thenValidationFails() {
        ApplicationsForm form = new ApplicationsForm();

        Set<ConstraintViolation<ApplicationsForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("At least one service must be selected")));
    }

    @Test
    void whenAppsIsNotEmpty_thenValidationSucceeds() {
        ApplicationsForm form = new ApplicationsForm();
        form.setApps(List.of("Service1", "Service2"));

        Set<ConstraintViolation<ApplicationsForm>> violations = validator.validate(form);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testGetterAndSetter() {
        ApplicationsForm form = new ApplicationsForm();
        List<String> apps = List.of("App1", "App2");
        form.setApps(apps);
        assertEquals(apps, form.getApps());
    }

    @Test
    void whenAppsContainsSingleService_thenValidationSucceeds() {
        ApplicationsForm form = new ApplicationsForm();
        form.setApps(List.of("SingleService"));

        Set<ConstraintViolation<ApplicationsForm>> violations = validator.validate(form);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenAppsContainsMultipleServices_thenValidationSucceeds() {
        ApplicationsForm form = new ApplicationsForm();
        form.setApps(List.of("Service1", "Service2", "Service3"));

        Set<ConstraintViolation<ApplicationsForm>> violations = validator.validate(form);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenValidationFails_thenCorrectErrorMessageReturned() {
        ApplicationsForm form = new ApplicationsForm();
        form.setApps(null);

        Set<ConstraintViolation<ApplicationsForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        
        ConstraintViolation<ApplicationsForm> violation = violations.iterator().next();
        assertEquals("At least one service must be selected", violation.getMessage());
        assertEquals("apps", violation.getPropertyPath().toString());
    }

    @Test
    void defaultConstructor_shouldCreateFormWithNullApps() {
        ApplicationsForm form = new ApplicationsForm();
        assertNull(form.getApps());
    }

    @Test
    void testEqualsAndHashCode() {
        ApplicationsForm form1 = new ApplicationsForm();
        ApplicationsForm form2 = new ApplicationsForm();
        
        // Test with null apps
        assertEquals(form1, form2);
        assertEquals(form1.hashCode(), form2.hashCode());
        
        // Test with same apps
        List<String> apps = List.of("App1", "App2");
        form1.setApps(apps);
        form2.setApps(apps);
        assertEquals(form1, form2);
        assertEquals(form1.hashCode(), form2.hashCode());
        
        // Test with different apps
        form2.setApps(List.of("App3"));
        assertNotEquals(form1, form2);
    }

    @Test
    void testToString() {
        ApplicationsForm form = new ApplicationsForm();
        form.setApps(List.of("App1", "App2"));
        
        String result = form.toString();
        assertTrue(result.contains("ApplicationsForm"));
        assertTrue(result.contains("apps"));
    }
}