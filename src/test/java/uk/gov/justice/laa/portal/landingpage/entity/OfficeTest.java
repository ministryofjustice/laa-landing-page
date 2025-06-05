package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OfficeTest extends BaseEntityTest {

    @Test
    public void testOffice() {
        Office office = buildTestOffice();

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isEmpty();
        assertNotNull(office);
        assertEquals("TestOffice", office.getName());
        assertThat("Address").isEqualTo(office.getAddress());
        assertThat("123456").isEqualTo(office.getPhone());

    }

    @Test
    public void testOfficeNullName() {
        Office office = buildTestOffice();
        update(office, off -> off.setName(null));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Office name must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testOfficeEmptyName() {
        Office office = buildTestOffice();
        update(office, off -> off.setName(""));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Office name must be provided", "Office name must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    public void testOfficeNameTooLong() {
        Office office = buildTestOffice();
        update(office, off -> off.setName("TestOfficeNameThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Office name must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");

    }

    @Test
    public void testOfficeNullAddress() {
        Office office = buildTestOffice();
        update(office, off -> off.setAddress(null));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Office address must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("address");
    }

    @Test
    public void testOfficeEmptyAddress() {
        Office office = buildTestOffice();
        update(office, off -> off.setAddress(""));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Office address must be provided", "Office address must be between 1 and 500 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("address");
    }

    @Test
    public void testOfficeAddressTooLong() {
        Office office = buildTestOffice();
        update(office, off -> off.setAddress("OfficeAddress".repeat(51)));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Office address must be between 1 and 500 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("address");
    }

    @Test
    public void testOfficeNullPhone() {
        Office office = buildTestOffice();
        update(office, off -> off.setPhone(null));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Office phone number must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("phone");
    }

    @Test
    public void testOfficeEmptyPhone() {
        Office office = buildTestOffice();
        update(office, off -> off.setPhone(""));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Office phone number must be provided", "Office phone number must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("phone");
    }

    @Test
    public void testOfficePhoneTooLong() {
        Office office = buildTestOffice();
        update(office, off -> off.setPhone("123456".repeat(51)));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Office phone number must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("phone");
    }

}
