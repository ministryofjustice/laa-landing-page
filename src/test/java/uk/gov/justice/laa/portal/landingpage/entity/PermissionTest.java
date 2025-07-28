package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PermissionTest extends BaseEntityTest {

    @Test
    public void testPermission() {
        AppRole appRole = buildTestLaaAppRole();
        Permission permission = buildTestPermission(Set.of(appRole));

        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        assertThat(violations).isEmpty();
        assertNotNull(permission);
        assertThat(permission.getName()).isEqualTo("Test Permission");
        assertThat(permission.getDescription()).isEqualTo("description");
        assertThat(permission.getFunction()).isEqualTo("function");
    }

    @Test
    public void testPermissionNullName() {
        AppRole appRole = buildTestLaaAppRole();
        Permission permission = buildTestPermission(Set.of(appRole));
        update(permission, f -> f.setName(null));

        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Permission name must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testPermissionEmptyName() {
        AppRole appRole = buildTestLaaAppRole();
        Permission permission = buildTestPermission(Set.of(appRole));
        update(permission, f -> f.setName(""));

        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Permission name must be provided", "Permission name must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }


    @Test
    public void testPermissionNameTooLong() {
        AppRole appRole = buildTestLaaAppRole();
        Permission permission = buildTestPermission(Set.of(appRole));
        update(permission, f -> f.setName("TestAppRoleNameThatIsTooLong".repeat(20)));

        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Permission name must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");

    }

    @Test
    public void testPermissionDescriptionEmpty() {
        AppRole appRole = buildTestLaaAppRole();
        Permission permission = buildTestPermission(Set.of(appRole));
        update(permission, f -> f.setDescription(""));

        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Permission description must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("description");

    }

    @Test
    public void testPermissionDescriptionTooLong() {
        AppRole appRole = buildTestLaaAppRole();
        Permission permission = buildTestPermission(Set.of(appRole));
        update(permission, f -> f.setDescription("TestAppRoleNameThatIsTooLong".repeat(20)));

        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Permission description must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("description");

    }

    @Test
    public void testPermissionNullFunction() {
        AppRole appRole = buildTestLaaAppRole();
        Permission permission = buildTestPermission(Set.of(appRole));
        update(permission, f -> f.setFunction(null));

        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Permission function must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("function");
    }

    @Test
    public void testPermissionFunctionEmpty() {
        AppRole appRole = buildTestLaaAppRole();
        Permission permission = buildTestPermission(Set.of(appRole));
        update(permission, f -> f.setFunction(""));

        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Permission function must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("function");

    }

    @Test
    public void testPermissionFunctionTooLong() {
        AppRole appRole = buildTestLaaAppRole();
        Permission permission = buildTestPermission(Set.of(appRole));
        update(permission, f -> f.setFunction("TestAppRoleNameThatIsTooLong".repeat(20)));

        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Permission function must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("function");

    }

}
