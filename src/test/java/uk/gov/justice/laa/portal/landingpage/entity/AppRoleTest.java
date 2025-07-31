package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AppRoleTest extends BaseEntityTest {

    @Test
    public void testLaaAppRole() {
        AppRole appRole = buildTestLaaAppRole();

        Set<ConstraintViolation<AppRole>> violations = validator.validate(appRole);

        assertThat(violations).isEmpty();
        assertNotNull(appRole);
        assertThat(appRole.getName()).isEqualTo("Test App Role");
        assertThat(appRole.getRoleType()).isEqualTo(RoleType.INTERNAL);
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

    @Test
    public void testLaaAppRoleNullCcmsCode() {
        AppRole appRole = buildTestLaaAppRole();
        update(appRole, f -> f.setCcmsCode(null));

        Set<ConstraintViolation<AppRole>> violations = validator.validate(appRole);

        assertThat(violations).isEmpty();
    }

    @Test
    public void testLaaAppRoleEmptyCcmsCode() {
        AppRole appRole = buildTestLaaAppRole();
        update(appRole, f -> f.setCcmsCode(""));

        Set<ConstraintViolation<AppRole>> violations = validator.validate(appRole);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Application role CCMS Code must be between 1 and 30 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("ccmsCode");
    }

    @Test
    public void testLaaAppRoleCcmsCodeTooLong() {
        AppRole appRole = buildTestLaaAppRole();
        update(appRole, f -> f.setCcmsCode("TestAppRoleNameThatIsTooLong".repeat(2)));

        Set<ConstraintViolation<AppRole>> violations = validator.validate(appRole);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application role CCMS Code must be between 1 and 30 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("ccmsCode");

    }

    @Test
    public void testLaaAppRoleDescriptionNull() {
        AppRole appRole = buildTestLaaAppRole();
        update(appRole, f -> f.setDescription(null));

        Set<ConstraintViolation<AppRole>> violations = validator.validate(appRole);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application role description must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("description");

    }

    @Test
    public void testLaaAppRoleDescriptionEmpty() {
        AppRole appRole = buildTestLaaAppRole();
        update(appRole, f -> f.setDescription(""));

        Set<ConstraintViolation<AppRole>> violations = validator.validate(appRole);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Application role description must be provided",
                "Application role description must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("description");

    }

    @Test
    public void testLaaAppRoleDescriptionTooLong() {
        AppRole appRole = buildTestLaaAppRole();
        update(appRole, f -> f.setDescription("TestAppRoleNameThatIsTooLong".repeat(20)));

        Set<ConstraintViolation<AppRole>> violations = validator.validate(appRole);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application role description must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("description");

    }

    @Test
    public void testAppRoleTypeExternal() {
        AppRole appRole = buildTestLaaAppRole();
        update(appRole, f -> f.setRoleType(RoleType.EXTERNAL));

        Set<ConstraintViolation<AppRole>> violations = validator.validate(appRole);

        assertThat(violations).isEmpty();
        assertNotNull(appRole);
        assertThat(appRole.getName()).isEqualTo("Test App Role");
        assertThat(appRole.getRoleType()).isEqualTo(RoleType.EXTERNAL);
    }

    @Test
    public void testAppRoleNullRoleType() {
        AppRole appRole = buildTestLaaAppRole();
        update(appRole, f -> f.setRoleType(null));

        Set<ConstraintViolation<AppRole>> violations = validator.validate(appRole);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("App role type must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("roleType");
    }

    @Test
    public void testAppRoleInvalidRoleType() {
        assertThrows(IllegalArgumentException.class, () -> AppRole.builder()
                .roleType(RoleType.valueOf("INVALID")).build());
    }
}
