package uk.gov.justice.laa.portal.landingpage.forms;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;




class ApplicationsFormTest {

    private final Validator validator;

    public ApplicationsFormTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
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
        form.setApps(List.of());

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
}