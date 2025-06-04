package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LaaAppRoleTest extends BaseEntityTest {

    @Test
    public void testLaaAppRole() {
        LaaAppRole laaAppRole = buildTestLaaAppRole();

        Set<ConstraintViolation<LaaAppRole>> violations = validator.validate(laaAppRole);

        assertThat(violations).isEmpty();
        assertNotNull(laaAppRole);
        assertEquals("Test Laa App Role", laaAppRole.getName());
    }

    @Test
    public void testLaaAppRoleNullName() {
        LaaAppRole laaAppRole = buildTestLaaAppRole();
        update(laaAppRole, f -> f.setName(null));

        Set<ConstraintViolation<LaaAppRole>> violations = validator.validate(laaAppRole);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application role name must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testLaaAppRoleEmptyName() {
        LaaAppRole laaAppRole = buildTestLaaAppRole();
        update(laaAppRole, f -> f.setName(""));

        Set<ConstraintViolation<LaaAppRole>> violations = validator.validate(laaAppRole);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Application role name must be provided", "Application role name must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testLaaAppRoleNameTooLong() {
        LaaAppRole laaAppRole = buildTestLaaAppRole();
        update(laaAppRole, f -> f.setName("TestLaaAppRoleNameThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<LaaAppRole>> violations = validator.validate(laaAppRole);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application role name must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");

    }
}
