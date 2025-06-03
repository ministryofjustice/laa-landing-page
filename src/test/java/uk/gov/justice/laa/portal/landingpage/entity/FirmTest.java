package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FirmTest extends BaseEntityTest {

    @Test
    public void testFirm() {
        Firm firm = buildTestFirm();

        Set<ConstraintViolation<Firm>> violations = validator.validate(firm);

        assertThat(violations).isEmpty();
        assertNotNull(firm);
        assertEquals("TestFirm", firm.getName());
        assertEquals(FirmType.LEGAL_SERVICES_PROVIDER, firm.getType());
    }

    @Test
    public void testFirmNullName() {
        Firm firm = buildTestFirm();
        update(firm, f -> f.setName(null));

        Set<ConstraintViolation<Firm>> violations = validator.validate(firm);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Firm name must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testFirmEmptyName() {
        Firm firm = buildTestFirm();
        update(firm, f -> f.setName(""));

        Set<ConstraintViolation<Firm>> violations = validator.validate(firm);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Firm name must be provided", "Firm name must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testFirmNameTooLong() {
        Firm firm = buildTestFirm();
        update(firm, f -> f.setName("TestFirmNameThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<Firm>> violations = validator.validate(firm);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Firm name must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");

    }

    @Test
    public void testFirmNullType() {
        Firm firm = buildTestFirm();
        update(firm, f -> f.setType(null));

        Set<ConstraintViolation<Firm>> violations = validator.validate(firm);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages.iterator().next()).isEqualTo("Firm type must be provided");
        assertThat(messages.iterator().next()).isEqualTo("type");
    }


    @Test
    public void testFirmInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> {
            Firm firm = buildTestFirm();
            update(firm, f -> f.setType(FirmType.valueOf("test")));
        });
    }

}