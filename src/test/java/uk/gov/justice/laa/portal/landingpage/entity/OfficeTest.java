package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OfficeTest extends BaseEntityTest {

    @Test
    public void testOffice() {
        Office office = buildTestOffice();

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isEmpty();
        assertNotNull(office);
        assertNotNull(office.getAddress());
        assertThat("addressLine1").isEqualTo(office.getAddress().getAddressLine1());
        assertThat("city").isEqualTo(office.getAddress().getCity());
        assertThat("postcode").isEqualTo(office.getAddress().getPostcode());

    }

    @Test
    public void testOfficeNullCode() {
        Office office = buildTestOffice();
        update(office, off -> off.setCode(null));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isEmpty();
    }

    @Test
    public void testOfficeEmptyCode() {
        Office office = buildTestOffice();
        update(office, off -> off.setCode(""));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isEmpty();
    }

    @Test
    public void testOfficeCodeTooLong() {
        Office office = buildTestOffice();
        update(office, off -> off.setCode("TestOfficeNameThatIsTooLong".repeat(25)));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Office code must be less than 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("code");

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
        update(office, off -> off.setAddress(new Office.Address()));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(3);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Office address line 1 must be provided",
                "Office postcode must be provided", "Office city must be provided"));
        Set<String> volitionalPaths = violations.stream().map(ConstraintViolation::getPropertyPath).map(Path::toString).collect(Collectors.toSet());
        assertThat(volitionalPaths).contains("address.city", "address.postcode", "address.addressLine1");
    }

    @Test
    public void testOfficeAddressDetailsTooLong() {
        Office office = buildTestOffice();
        Office.Address address = Office.Address.builder()
                .addressLine1("OfficeAddress1".repeat(51))
                .addressLine2("OfficeAddress2".repeat(51))
                .addressLine3("OfficeAddress3".repeat(51))
                .city("OfficeAddress".repeat(51))
                .postcode("OfficeAddressmorethan20characters")
                .build();
        update(office, off -> off.setAddress(address));

        Set<ConstraintViolation<Office>> violations = validator.validate(office);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(5);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Office address must be between 1 and 255 characters",
                "Office address line 2 must be between 1 and 255 characters", "Office address line 3 must be between 1 and 255 characters",
                "Office city must be between 1 and 255 characters", "Office postcode must be between 2 and 20 characters"));
        Set<String> volitionalPaths = violations.stream().map(ConstraintViolation::getPropertyPath).map(Path::toString).collect(Collectors.toSet());
        assertThat(volitionalPaths).contains("address.city", "address.postcode", "address.addressLine2", "address.addressLine1");
    }

}
