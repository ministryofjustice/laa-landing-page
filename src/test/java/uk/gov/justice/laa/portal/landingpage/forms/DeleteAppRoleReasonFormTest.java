package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DeleteAppRoleReasonFormTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private static DeleteAppRoleReasonForm newForm(String reason, String appName, String appRoleId) {
        DeleteAppRoleReasonForm form = new DeleteAppRoleReasonForm();
        setField(form, "reason", reason);
        setField(form, "appName", appName);
        setField(form, "appRoleId", appRoleId);
        return form;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Failed to set field '" + fieldName + "': " + e.getMessage(), e);
        }
    }

    private static Set<String> messagesOf(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    // -------------------------
    // Tests
    // -------------------------

    @Test
    void validForm_shouldHaveNoViolations() {
        DeleteAppRoleReasonForm form = newForm("Removing unused role after access review.", "FinanceApp", "role-1234");

        Set<ConstraintViolation<DeleteAppRoleReasonForm>> violations = validator.validate(form);

        assertThat(violations).isEmpty();
    }

    @Test
    void blankReason_shouldFailNotBlank() {
        DeleteAppRoleReasonForm form = newForm("   ",  // blank
                "FinanceApp", "role-1234");

        Set<ConstraintViolation<DeleteAppRoleReasonForm>> violations = validator.validate(form);

        assertThat(violations).hasSize(2);
        assertThat(messagesOf(violations)).containsExactlyInAnyOrder("Please provide a reason for the role deletion",
                "Reason must be between 10 and 1000 characters");
    }

    @Test
    void shortReason_shouldFailSizeMin10() {
        DeleteAppRoleReasonForm form = newForm("Too short",   // length 9
                "FinanceApp", "role-1234");

        Set<ConstraintViolation<DeleteAppRoleReasonForm>> violations = validator.validate(form);

        assertThat(violations).hasSize(1);
        assertThat(messagesOf(violations)).containsExactly("Reason must be between 10 and 1000 characters");
    }

    @Test
    void longReason_shouldFailSizeMax1000() {
        String overlong = "a".repeat(1001);
        DeleteAppRoleReasonForm form = newForm(overlong, "FinanceApp", "role-1234");

        Set<ConstraintViolation<DeleteAppRoleReasonForm>> violations = validator.validate(form);

        assertThat(violations).hasSize(1);
        assertThat(messagesOf(violations)).containsExactly("Reason must be between 10 and 1000 characters");
    }

    @Test
    void blankAppName_shouldFailNotBlank() {
        DeleteAppRoleReasonForm form = newForm("Removing unused role after access review.", "   ",    // blank
                "role-1234");

        Set<ConstraintViolation<DeleteAppRoleReasonForm>> violations = validator.validate(form);

        assertThat(violations).hasSize(1);
        assertThat(messagesOf(violations)).containsExactly("Please provide the app name");
    }

    @Test
    void blankAppRoleId_shouldFailNotBlank() {
        DeleteAppRoleReasonForm form = newForm("Removing unused role after access review.", "FinanceApp", ""         // blank
        );

        Set<ConstraintViolation<DeleteAppRoleReasonForm>> violations = validator.validate(form);

        assertThat(violations).hasSize(1);
        assertThat(messagesOf(violations)).containsExactly("Please provide the app role id");
    }

    @Test
    void allBlank_shouldReturnAllThreeViolations() {
        DeleteAppRoleReasonForm form = newForm("  ", "  ", "  ");

        Set<ConstraintViolation<DeleteAppRoleReasonForm>> violations = validator.validate(form);

        assertThat(violations).hasSize(4);
        assertThat(messagesOf(violations)).containsExactlyInAnyOrder("Please provide a reason for the role deletion",
                "Reason must be between 10 and 1000 characters",
                "Please provide the app name",
                "Please provide the app role id");
    }
}

