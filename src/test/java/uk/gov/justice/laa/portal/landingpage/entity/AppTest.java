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
    public void testLaaAppNullTitle() {
        App app = buildTestLaaApp();
        update(app, f -> f.setTitle(null));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application title must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("title");
    }

    @Test
    public void testLaaAppEmptyTitle() {
        App app = buildTestLaaApp();
        update(app, f -> f.setTitle(""));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Application title must be provided", "Application title must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("title");
    }

    @Test
    public void testLaaAppTitleTooLong() {
        App app = buildTestLaaApp();
        update(app, f -> f.setTitle("TestLaaAppTitleThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application title must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("title");

    }

    @Test
    public void testLaaAppNullDescription() {
        App app = buildTestLaaApp();
        update(app, f -> f.setDescription(null));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application description must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("description");
    }

    @Test
    public void testLaaAppEmptyDescription() {
        App app = buildTestLaaApp();
        update(app, f -> f.setDescription(""));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Application description must be provided", "Application description must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("description");
    }

    @Test
    public void testLaaAppDescriptionTooLong() {
        App app = buildTestLaaApp();
        update(app, f -> f.setDescription("TestLaaAppDescriptionThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application description must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("description");

    }

    @Test
    public void testLaaAppNullOidGroupName() {
        App app = buildTestLaaApp();
        update(app, f -> f.setOidGroupName(null));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application OID Group Name must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("oidGroupName");
    }

    @Test
    public void testLaaAppEmptyOidGroupName() {
        App app = buildTestLaaApp();
        update(app, f -> f.setOidGroupName(""));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Application OID Group Name must be provided", "Application OID Group Name must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("oidGroupName");
    }

    @Test
    public void testLaaAppOidGroupNameTooLong() {
        App app = buildTestLaaApp();
        update(app, f -> f.setOidGroupName("TestLaaAppOIDGroupNameThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application OID Group Name must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("oidGroupName");

    }

    @Test
    public void testLaaAppNullUrl() {
        App app = buildTestLaaApp();
        update(app, f -> f.setUrl(null));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application url must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("url");
    }

    @Test
    public void testLaaAppEmptyUrl() {
        App app = buildTestLaaApp();
        update(app, f -> f.setUrl(""));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Application url must be provided", "Application url must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("url");
    }

    @Test
    public void testLaaAppUrlTooLong() {
        App app = buildTestLaaApp();
        update(app, f -> f.setUrl("TestLaaAppUrlThatIsTooLong".repeat(20)));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application url must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("url");

    }

    @Test
    public void testLaaAppEmptyAppGroup() {
        App app = buildTestLaaApp();
        update(app, f -> f.setAppType(null));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Application Type must be provided"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("appType");
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

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Security Group Oid must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("securityGroupOid");
    }

    @Test
    public void testLaaAppEmptySecurityGroupOid() {
        App app = buildTestLaaApp();
        update(app, f -> f.setSecurityGroupOid(""));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Security Group Oid must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("securityGroupOid");
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

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Security Group Name must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("securityGroupName");
    }

    @Test
    public void testLaaAppEmptySecurityGroupName() {
        App app = buildTestLaaApp();
        update(app, f -> f.setSecurityGroupName(""));

        Set<ConstraintViolation<App>> violations = validator.validate(app);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Security Group Name must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("securityGroupName");
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
