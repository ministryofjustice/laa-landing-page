package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MultiFirmUserFormTest {

    private Validator validator;

    @BeforeEach
    void setup() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private Set<ConstraintViolation<MultiFirmUserForm>> validateEmail(String email) {
        MultiFirmUserForm form = new MultiFirmUserForm();
        form.setEmail(email);
        return validator.validate(form);
    }

    @Test
    void shouldPassValidationForValidEmail() {
        assertThat(validateEmail("user@example.com")).isEmpty();
    }

    @Test
    void shouldFailValidationForEmptyEmail() {
        assertThat(validateEmail("")).anyMatch(v -> v.getMessage().contains("must be provided"));
    }

    @Test
    void shouldFailValidationForInvalidEmailFormat() {
        assertThat(validateEmail("invalid-email")).anyMatch(v -> v.getMessage().contains("correct format"));
    }

    @Test
    void shouldFailValidationForEmailExceedingMaxLength() {
        String longEmail = "a".repeat(245) + "@example.com"; // >254 chars
        assertThat(validateEmail(longEmail)).anyMatch(v -> v.getMessage().contains("longer than 254 characters"));
    }

    @Test
    void shouldPassValidationForEmailWithSubdomain() {
        assertThat(validateEmail("user@mail.example.co.uk")).isEmpty();
    }

    @Test
    void shouldPassValidationForEmailWithSpecialCharacters() {
        assertThat(validateEmail("user.name+tag-123@example-domain.com")).anyMatch(v -> v.getMessage().contains("correct format"));
    }

    @Test
    void shouldFailValidationForEmailStartingWithSpecialCharacter() {
        assertThat(validateEmail(".user@example.com")).anyMatch(v -> v.getMessage().contains("correct format"));
    }

    @Test
    void shouldFailValidationForEmailMissingDomain() {
        assertThat(validateEmail("user@")).anyMatch(v -> v.getMessage().contains("correct format"));
    }
}