package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FirmSearchFormTest {

    private final Validator validator;

    public FirmSearchFormTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenFirmSearchIsNull_thenValidationFails() {
        FirmSearchForm form = FirmSearchForm.builder().build();
        form.setFirmSearch(null);
        Set<ConstraintViolation<FirmSearchForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Please make a valid firm selection")));
    }

    @Test
    void whenFirmSearchIsEmpty_thenValidationFails() {
        FirmSearchForm form = FirmSearchForm.builder().build();
        form.setFirmSearch("");
        Set<ConstraintViolation<FirmSearchForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Please make a valid firm selection")));
    }

    @Test
    void whenTrimFirmSearchIsEmpty_thenValidationFails() {
        FirmSearchForm form = FirmSearchForm.builder().build();
        form.setFirmSearch("   ");
        Set<ConstraintViolation<FirmSearchForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Please make a valid firm selection")));
    }

    @Test
    void whenTrimFirmSearchIsNotEmpty_thenValidationSuccess() {
        FirmSearchForm form = FirmSearchForm.builder().build();
        form.setFirmSearch("  a ");
        Set<ConstraintViolation<FirmSearchForm>> violations = validator.validate(form);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenTrimFirmSearchIsEmpty_thenValidationSuccessForSkipSelection() {
        FirmSearchForm form = FirmSearchForm.builder().build();
        form.setFirmSearch("");
        form.setSkipFirmSelection(true);
        Set<ConstraintViolation<FirmSearchForm>> violations = validator.validate(form);
        assertTrue(violations.isEmpty());
    }
}
