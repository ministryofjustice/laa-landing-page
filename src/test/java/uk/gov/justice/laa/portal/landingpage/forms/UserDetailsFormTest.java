package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserDetailsFormTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validUserDetailsForm_shouldHaveNoViolations() {
        UserDetailsForm form = new UserDetailsForm();
        form.setFirmId("firm1");
        form.setIsFirmAdmin(true);
        form.setFirstName("John");
        form.setLastName("Doe");
        form.setEmail("john.doe@example.com");
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    void emptyFields_shouldTriggerNotEmptyViolations() {
        UserDetailsForm form = new UserDetailsForm();
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Select a firm", "Enter a first name", "Enter a last name", "Enter an email address");
    }

    @Test
    void invalidEmailFormat_shouldTriggerPatternViolation() {
        UserDetailsForm form = new UserDetailsForm();
        form.setFirmId("firm1");
        form.setIsFirmAdmin(false);
        form.setFirstName("Jane");
        form.setLastName("Smith");
        form.setEmail("invalid-email");
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Enter an email address in the correct format");
    }

    @Test
    void invalidEmailStartingCharacterFormat_shouldTriggerPatternViolation() {
        UserDetailsForm form = new UserDetailsForm();
        form.setFirmId("firm1");
        form.setIsFirmAdmin(false);
        form.setFirstName("John");
        form.setLastName("Doe");
        form.setEmail("-john.doe@email.com");
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Enter an email address in the correct format");
    }

    @Test
    void invalidEmailSuffixFormat_shouldTriggerPatternViolation() {
        UserDetailsForm form = new UserDetailsForm();
        form.setFirmId("firm1");
        form.setIsFirmAdmin(false);
        form.setFirstName("John");
        form.setLastName("Doe");
        form.setEmail("john.doe@email");
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Enter an email address in the correct format");
    }

    @Test
    void tooLongEmail_shouldTriggerSizeViolation() {
        UserDetailsForm form = new UserDetailsForm();
        form.setFirmId("firm1");
        form.setIsFirmAdmin(false);
        form.setFirstName("Jane");
        form.setLastName("Smith");
        form.setEmail("a".repeat(250) + "@example.com");
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Email must not be longer than 254 characters");
    }

    @Test
    void tooLongFirstNameOrLastName_shouldTriggerSizeViolation() {
        UserDetailsForm form = new UserDetailsForm();
        form.setFirmId("firm1");
        form.setIsFirmAdmin(false);
        form.setFirstName("A".repeat(100));
        form.setLastName("B".repeat(100));
        form.setEmail("test@example.com");
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("First name must not be longer than 99 characters",
                        "Last name must not be longer than 99 characters");
    }
}
