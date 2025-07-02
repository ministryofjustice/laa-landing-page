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

class OfficesFormTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validOfficesForm_shouldHaveNoViolations() {
        OfficesForm form = new OfficesForm();
        form.setOffices(List.of("office1", "office2"));
        Set<ConstraintViolation<OfficesForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    void nullOffices_shouldTriggerNotNullViolation() {
        OfficesForm form = new OfficesForm();
        form.setOffices(null);
        Set<ConstraintViolation<OfficesForm>> violations = validator.validate(form);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Office selection is required");
    }

    @Test
    void emptyOffices_shouldNotTriggerNotNullViolation() {
        OfficesForm form = new OfficesForm();
        form.setOffices(List.of());
        Set<ConstraintViolation<OfficesForm>> violations = validator.validate(form);
        // NotNull does not check for empty lists, so this should pass
        assertThat(violations).isEmpty();
    }
}
