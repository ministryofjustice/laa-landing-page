package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class EntraUserTest extends BaseEntityTest {

    @Test
    public void testEntraUser() {
        EntraUser entraUser = buildTestEntraUser();

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isEmpty();
        assertThat(entraUser.getEmail()).isEqualTo("test@email.com");
        assertThat(entraUser.getFirstName()).isEqualTo("FirstName");
        assertThat(entraUser.getLastName()).isEqualTo("LastName");
        assertThat(entraUser.isActive()).isTrue();
        assertThat(entraUser.getStartDate()).isNotNull();
        assertThat(entraUser.getEndDate()).isNotNull();
        assertThat(entraUser.getEmail()).isEqualTo("test@email.com");
        assertThat(entraUser.getCreatedBy()).isEqualTo("test");
        assertThat(entraUser.getCreatedDate()).isNotNull();
    }

    @Test
    public void testEntraUserNullUserName() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setUserName(null));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Entra username must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("userName");
    }

    @Test
    public void testEntraUserEmptyUserName() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setUserName(""));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(java.util.stream.Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Entra username must be provided",
                "Entra username must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("userName");
    }

    @Test
    public void testEntraUserUserNameTooLong() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setUserName("UserNameThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Entra username must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("userName");
    }

    @Test
    public void testEntraUserNullEmail() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setEmail(null));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Entra user email must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("email");
    }

    @Test
    public void testEntraUserEmptyEmail() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setEmail(""));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Entra user email must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("email");
    }

    @Test
    public void testEntraUserInvalidEmail() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setEmail("test"));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Entra user email must be a valid email address");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("email");
    }

    @Test
    public void testEntraUserEmailTooLong() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setEmail("testemail".repeat(15) + "@email.com"));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Entra user email must be a valid email address");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("email");
    }

    @Test
    public void testEntraUserNullFirstName() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setFirstName(null));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Entra user first name must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("firstName");
    }

    @Test
    public void testEntraUserEmptyFirstName() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setFirstName(""));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(java.util.stream.Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Entra user first name must be provided",
                "Entra user first name must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("firstName");
    }

    @Test
    public void testEntraUserFirstNameTooLong() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setFirstName("FirstNameThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Entra user first name must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("firstName");
    }

    @Test
    public void testEntraUserNullLastName() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setLastName(null));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Entra user last name must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("lastName");
    }

    @Test
    public void testEntraUserEmptyLastName() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setLastName(""));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(java.util.stream.Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Entra user last name must be provided", "Entra user last name must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("lastName");
    }

    @Test
    public void testEntraUserLastNameTooLong() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setLastName("LastNameThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Entra user last name must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("lastName");
    }

    @Test
    public void testEntraUserNullStartDate() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setStartDate(null));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isEmpty();
    }

    @Test
    public void testEntraUserNullEndDate() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setEndDate(null));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isEmpty();
    }

    @Test
    public void testEntraUserStartDateAfterEndDate() {
        EntraUser entraUser = EntraUser.builder().firstName("FirstName").lastName("LastName")
                .active(true).startDate(LocalDateTime.now()).endDate(LocalDateTime.now().minusYears(1))
                .email("test@email.com")
                .createdBy("test").createdDate(LocalDateTime.now()).build();
        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("End date must be after start date");
    }

}
