package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RolesFormTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validRolesForm_shouldHaveNoViolations() {
        RolesForm form = new RolesForm();
        form.setRoles(List.of("role1", "role2"));
        Set<ConstraintViolation<RolesForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    void nullRoles_shouldTriggerNotNullViolation() {
        RolesForm form = new RolesForm();
        form.setRoles(null);
        Set<ConstraintViolation<RolesForm>> violations = validator.validate(form);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
            .contains("At least one role must be selected");
    }

    @Test
    void emptyRoles_shouldNotTriggerNotNullViolation() {
        RolesForm form = new RolesForm();
        form.setRoles(List.of());
        Set<ConstraintViolation<RolesForm>> violations = validator.validate(form);
        // NotNull does not check for empty lists, so this should pass
        assertThat(violations).isEmpty();
    }
}
