package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

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
        form.setFirstName("John");
        form.setLastName("Doe");
        form.setEmail("john.doe@example.com");
        form.setUserType(UserType.EXTERNAL_SINGLE_FIRM);
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
        form.setFirstName("A".repeat(100));
        form.setLastName("B".repeat(100));
        form.setEmail("test@example.com");
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("First name must be between 2-99 characters",
                        "Last name must be between 2-99 characters");
    }

    @Test
    void tooShortFirstNameOrLastName_shouldTriggerSizeViolation() {
        UserDetailsForm form = new UserDetailsForm();
        form.setFirmId("firm1");
        form.setFirstName("A");
        form.setLastName("B");
        form.setEmail("test@example.com");
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("First name must be between 2-99 characters",
                        "Last name must be between 2-99 characters");
    }

    @Test
    void firstNameOrLastNameWithNumbers_shouldTriggerPatternViolation() {
        UserDetailsForm form = new UserDetailsForm();
        form.setFirmId("firm1");
        form.setFirstName("Jan3");
        form.setLastName("Sm1th");
        form.setEmail("test@example.com");
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("First name must not contain numbers or special characters",
                        "Last name must not contain numbers or special characters");
    }

    @Test
    void firstNameOrLastNameWithSpecialCharacters_shouldTriggerPatternViolation() {
        UserDetailsForm form = new UserDetailsForm();
        form.setFirmId("firm1");
        form.setFirstName("J@ne");
        form.setLastName("Sm!th");
        form.setEmail("test@example.com");
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("First name must not contain numbers or special characters",
                        "Last name must not contain numbers or special characters");
    }

    @Test
    void doubleBarrelledLastName_shouldNotTriggerPatternViolation() {
        UserDetailsForm form = new UserDetailsForm();
        form.setFirmId("firm1");
        form.setFirstName("Jane");
        form.setLastName("Anderson-Smith");
        form.setEmail("test@example.com");
        form.setUserType(UserType.EXTERNAL_SINGLE_FIRM);
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations.size()).isEqualTo(0);
    }

    @Test
    void quoteLastName_shouldNotTriggerPatternViolation() {
        UserDetailsForm form = new UserDetailsForm();
        form.setFirmId("firm1");
        form.setFirstName("Mary-Jane");
        form.setLastName("O'Neil");
        form.setEmail("test@example.com");
        form.setUserType(UserType.EXTERNAL_SINGLE_FIRM);
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations.size()).isEqualTo(0);
    }

    @Test
    void spaceInFirstName_shouldNotTriggerPatternViolation() {
        UserDetailsForm form = new UserDetailsForm();
        form.setFirmId("firm1");
        form.setFirstName("Mary Jane");
        form.setLastName("O'Neil");
        form.setEmail("test@example.com");
        form.setUserType(UserType.EXTERNAL_SINGLE_FIRM);
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations.size()).isEqualTo(0);
    }

    @Test
    void noUserType_shouldTriggerNotNullViolation() {
        UserDetailsForm form = new UserDetailsForm();
        form.setFirmId("firm1");
        form.setFirstName("Jane");
        form.setLastName("Smith");
        form.setEmail("test@example.com");
        Set<ConstraintViolation<UserDetailsForm>> violations = validator.validate(form);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Select a user type");
    }
}
