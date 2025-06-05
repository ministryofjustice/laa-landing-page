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
}
