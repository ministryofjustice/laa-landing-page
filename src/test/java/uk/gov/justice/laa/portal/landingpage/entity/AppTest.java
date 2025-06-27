package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AppTest extends BaseEntityTest {

    @Test
    public void testLaaApp() {
        App app = buildTestLaaApp();

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isEmpty();
        assertNotNull(app);
        assertEquals("Test App", app.getName());
    }

    @Test
    public void testLaaAppNullName() {
        App app = buildTestLaaApp();
        update(app, f -> f.setName(null));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application name must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testLaaAppEmptyName() {
        App app = buildTestLaaApp();
        update(app, f -> f.setName(""));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Application name must be provided", "Application name must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testLaaAppNameTooLong() {
        App app = buildTestLaaApp();
        update(app, f -> f.setName("TestLaaAppNameThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application name must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");

    }

    @Test
    public void testLaaAppNullEntraAppId() {
        App app = buildTestLaaApp();
        update(app, f -> f.setEntraAppId(null));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isEmpty();
    }

    @Test
    public void testLaaAppEmptyEntraAppId() {
        App app = buildTestLaaApp();
        update(app, f -> f.setEntraAppId(""));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isEmpty();
    }

    @Test
    public void testLaaAppEntraAppIdTooLong() {
        App app = buildTestLaaApp();
        update(app, f -> f.setEntraAppId("TestLaaAppEntraAppIDThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Entra App ID must be less than 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("entraAppId");

    }

    @Test
    public void testLaaAppNullSecurityGroupOid() {
        App app = buildTestLaaApp();
        update(app, f -> f.setSecurityGroupOid(null));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isEmpty();
    }

    @Test
    public void testLaaAppEmptySecurityGroupOid() {
        App app = buildTestLaaApp();
        update(app, f -> f.setSecurityGroupOid(""));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isEmpty();
    }

    @Test
    public void testLaaAppSecurityGroupOidTooLong() {
        App app = buildTestLaaApp();
        update(app, f -> f.setSecurityGroupOid("TestLaaAppNameThatIsTooLong".repeat(25)));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Security Group Oid must be less than 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("securityGroupOid");

    }


    @Test
    public void testLaaAppNullSecurityGroupName() {
        App app = buildTestLaaApp();
        update(app, f -> f.setSecurityGroupName(null));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isEmpty();
    }

    @Test
    public void testLaaAppEmptySecurityGroupName() {
        App app = buildTestLaaApp();
        update(app, f -> f.setSecurityGroupName(""));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isEmpty();
    }

    @Test
    public void testLaaAppSecurityGroupNameTooLong() {
        App app = buildTestLaaApp();
        update(app, f -> f.setSecurityGroupName("TestLaaAppNameThatIsTooLong".repeat(25)));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Security Group Name must be less than 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("securityGroupName");

    }
}
