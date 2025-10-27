package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RolesFormTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void testRolesForm_withValidRoles_shouldPassValidation() {
        RolesForm form = new RolesForm();
        form.setRoles(List.of("Admin", "User"));

        Set<ConstraintViolation<RolesForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    void testRolesForm_withNullRoles_shouldFailValidation() {
        RolesForm form = new RolesForm();
        form.setRoles(null);

        Set<ConstraintViolation<RolesForm>> violations = validator.validate(form);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("At least one role must be selected");
    }
}