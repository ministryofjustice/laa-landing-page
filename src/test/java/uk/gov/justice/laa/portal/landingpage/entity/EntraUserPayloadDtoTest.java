package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntraUserPayloadDtoTest extends BaseEntityTest {

    @Test
    public void testEntraUser() {
        EntraUser entraUser = buildTestEntraUser();

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isEmpty();
        assertThat(entraUser.getEntraOid()).isNotNull();
        assertThat(entraUser.getEntraOid()).isEqualTo("entra_id");
        assertThat(entraUser.getEmail()).isEqualTo("test@email.com");
        assertThat(entraUser.getFirstName()).isEqualTo("FirstName");
        assertThat(entraUser.getLastName()).isEqualTo("LastName");
        assertThat(entraUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(entraUser.getEmail()).isEqualTo("test@email.com");
        assertThat(entraUser.getCreatedBy()).isEqualTo("test");
        assertThat(entraUser.getCreatedDate()).isNotNull();
    }

    @Test
    public void testEntraUserNullEntraId() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setEntraOid(null));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Entra Object ID must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("entraOid");
    }

    @Test
    public void testEntraUserEmptyEntraId() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setEntraOid(""));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(java.util.stream.Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Entra Object ID must be provided",
                "Entra Object ID must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("entraOid");
    }

    @Test
    public void testEntraUserEntraIdTooLong() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setEntraOid("EntraIdThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Entra Object ID must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("entraOid");
    }

    @Test
    public void testEntraUserNullEmail() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setEmail(null));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User email must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("email");
    }

    @Test
    public void testEntraUserEmptyEmail() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setEmail(""));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User email must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("email");
    }

    @Test
    public void testEntraUserInvalidEmail() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setEmail("test"));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User email must be a valid email address");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("email");
    }

    @Test
    public void testEntraUserEmailTooLong() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setEmail("testemail".repeat(15) + "@email.com"));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User email must be a valid email address");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("email");
    }

    @Test
    public void testEntraUserNullFirstName() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setFirstName(null));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User first name must be provided");
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
        assertThat(messages).hasSameElementsAs(Set.of("User first name must be provided",
                "User first name must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("firstName");
    }

    @Test
    public void testEntraUserFirstNameTooLong() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setFirstName("FirstNameThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User first name must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("firstName");
    }

    @Test
    public void testEntraUserNullLastName() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setLastName(null));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User last name must be provided");
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
        assertThat(messages).hasSameElementsAs(Set.of("User last name must be provided", "User last name must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("lastName");
    }

    @Test
    public void testEntraUserLastNameTooLong() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, entra -> entra.setLastName("LastNameThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User last name must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("lastName");
    }


    @Test
    public void testEntraUserFalseInvitationAccepted() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, eu -> eu.setUserStatus(UserStatus.AWAITING_USER_APPROVAL));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isEmpty();
        assertThat(entraUser.getEmail()).isEqualTo("test@email.com");
        assertThat(entraUser.getFirstName()).isEqualTo("FirstName");
        assertThat(entraUser.getLastName()).isEqualTo("LastName");
        assertThat(entraUser.getUserStatus()).isEqualTo(UserStatus.AWAITING_USER_APPROVAL);
        assertThat(entraUser.getEmail()).isEqualTo("test@email.com");
        assertThat(entraUser.getCreatedBy()).isEqualTo("test");
        assertThat(entraUser.getCreatedDate()).isNotNull();
    }

    @Test
    public void testEntraUserNullUserStatus() {
        EntraUser entraUser = buildTestEntraUser();
        update(entraUser, eu -> eu.setUserStatus(null));

        Set<ConstraintViolation<EntraUser>> violations = validator.validate(entraUser);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User status must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("userStatus");
    }

    @Test
    public void testEntraUserInvalidUserStatus() {
        assertThrows(IllegalArgumentException.class, () -> EntraUser.builder()
                .userStatus(UserStatus.valueOf("INVALID"))
                .createdDate(LocalDateTime.now()).createdBy("test").build());
    }

}
