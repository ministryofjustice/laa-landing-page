package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntraAppRegistrationTest extends BaseEntityTest {

    @Test
    public void testEntraAppRegistration() {
        EntraAppRegistration entraAppRegistration = buildTestEntraAppRegistration();

        Set<ConstraintViolation<EntraAppRegistration>> violations = validator.validate(entraAppRegistration);

        assertThat(violations).isEmpty();
        assertNotNull(entraAppRegistration);
        assertEquals("Test Entra app reg", entraAppRegistration.getName());

    }

    @Test
    public void testEntraAppRegistrationNullName() {
        EntraAppRegistration entraAppRegistration = buildTestEntraAppRegistration();
        update(entraAppRegistration, entra -> entra.setName(null));

        Set<ConstraintViolation<EntraAppRegistration>> violations = validator.validate(entraAppRegistration);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("App registration name must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testEntraAppRegistrationEmptyName() {
        EntraAppRegistration entraAppRegistration = buildTestEntraAppRegistration();
        update(entraAppRegistration, entra -> entra.setName(""));

        Set<ConstraintViolation<EntraAppRegistration>> violations = validator.validate(entraAppRegistration);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("App registration name must be provided",
                "App registration name must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testEntraAppRegistrationNameTooLong() {
        EntraAppRegistration entraAppRegistration = buildTestEntraAppRegistration();
        update(entraAppRegistration, entra -> entra.setName("TestEntraAppRegNameThatIsTooLong".repeat(10)));

        Set<ConstraintViolation<EntraAppRegistration>> violations = validator.validate(entraAppRegistration);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("App registration name must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");

    }

}
