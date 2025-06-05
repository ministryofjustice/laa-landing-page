package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AppRoleTest extends BaseEntityTest {

    @Test
    public void testLaaAppRole() {
        AppRole appRole = buildTestLaaAppRole();

        Set<ConstraintViolation<AppRole>> violations = validator.validate(appRole);

        assertThat(violations).isEmpty();
        assertNotNull(appRole);
        assertEquals("Test App Role", appRole.getName());
    }

    @Test
    public void testLaaAppRoleNullName() {
        AppRole appRole = buildTestLaaAppRole();
        update(appRole, f -> f.setName(null));

        Set<ConstraintViolation<AppRole>> violations = validator.validate(appRole);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application role name must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testLaaAppRoleEmptyName() {
        AppRole appRole = buildTestLaaAppRole();
        update(appRole, f -> f.setName(""));

        Set<ConstraintViolation<AppRole>> violations = validator.validate(appRole);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Application role name must be provided", "Application role name must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testLaaAppRoleNameTooLong() {
        AppRole appRole = buildTestLaaAppRole();
        update(appRole, f -> f.setName("TestAppRoleNameThatIsTooLong".repeat(20)));

        Set<ConstraintViolation<AppRole>> violations = validator.validate(appRole);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application role name must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");

    }
}
