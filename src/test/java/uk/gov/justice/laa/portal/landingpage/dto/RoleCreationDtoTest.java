package uk.gov.justice.laa.portal.landingpage.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RoleCreationDto validation
 */
class RoleCreationDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidRoleCreationDto_WithSingleUserType() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.EXTERNAL))
                .legacySync(false)
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void testInvalidRoleCreationDto_WithNoUserType() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(new ArrayList<>())
                .legacySync(false)
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<RoleCreationDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Select whether this role is for internal or external users");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("userTypeRestriction");
    }

    @Test
    void testInvalidRoleCreationDto_WithNullUserType() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(null)
                .legacySync(false)
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<RoleCreationDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Select whether this role is for internal or external users");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("userTypeRestriction");
    }

    @Test
    void testInvalidRoleCreationDto_WithMultipleUserTypes() {
        // Arrange - This should now be invalid since we only allow single selection
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL, UserType.EXTERNAL))
                .legacySync(false)
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<RoleCreationDto> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Select whether this role is for internal or external users");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("userTypeRestriction");
    }

    @Test
    void testInvalidRoleCreationDto_WithBlankName() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.EXTERNAL))
                .legacySync(false)
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).hasSizeGreaterThan(0);
        assertThat(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name")))
                .isTrue();
    }

    @Test
    void testInvalidRoleCreationDto_WithNullLegacySync() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.EXTERNAL))
                .legacySync(null)
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("legacySync")))
                .isTrue();
    }
}

