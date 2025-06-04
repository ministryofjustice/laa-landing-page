package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AppRegistrationTest extends BaseEntityTest {

    @Test
    public void testEntraAppRegistration() {
        AppRegistration appRegistration = buildTestEntraAppRegistration();

        Set<ConstraintViolation<AppRegistration>> violations = validator.validate(appRegistration);

        assertThat(violations).isEmpty();
        assertNotNull(appRegistration);
        assertEquals("Test app reg", appRegistration.getName());

    }

    @Test
    public void testEntraAppRegistrationNullName() {
        AppRegistration appRegistration = buildTestEntraAppRegistration();
        update(appRegistration, entra -> entra.setName(null));

        Set<ConstraintViolation<AppRegistration>> violations = validator.validate(appRegistration);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("App registration name must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testEntraAppRegistrationEmptyName() {
        AppRegistration appRegistration = buildTestEntraAppRegistration();
        update(appRegistration, entra -> entra.setName(""));

        Set<ConstraintViolation<AppRegistration>> violations = validator.validate(appRegistration);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("App registration name must be provided",
                "App registration name must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testEntraAppRegistrationNameTooLong() {
        AppRegistration appRegistration = buildTestEntraAppRegistration();
        update(appRegistration, entra -> entra.setName("TestAppRegNameThatIsTooLong".repeat(10)));

        Set<ConstraintViolation<AppRegistration>> violations = validator.validate(appRegistration);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("App registration name must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");

    }

}
