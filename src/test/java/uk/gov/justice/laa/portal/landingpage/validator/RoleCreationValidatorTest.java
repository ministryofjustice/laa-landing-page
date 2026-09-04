package uk.gov.justice.laa.portal.landingpage.validator;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.validation.ValidationMessages;

/**
 * Test class for RoleCreationValidator
 */
class RoleCreationValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidate_WithLegacySyncTrueAndroleIdentifierProvided_NoErrors() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(true)
                .roleIdentifier("CCMS001")
                .build();
        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void testValidate_WithLegacySyncTrueAndNoroleIdentifier_RejectsWithError() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(true)
                .roleIdentifier(null)
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations.isEmpty()).isFalse();
        assertThat(violations)
                .anyMatch(v -> v.getMessage().equals("Role identifier is required"));
    }

    @Test
    void testValidate_WithLegacySyncTrueAndEmptyroleIdentifier_RejectsWithError() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(true)
                .roleIdentifier("   ")
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations.isEmpty()).isFalse();
        assertThat(violations)
                .anyMatch(v -> v.getMessage().equals("Role identifier is required"));
    }

    @Test
    void testValidate_WithLegacySyncFalseAndNoroleIdentifier_RejectsWithError() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(false)
                .roleIdentifier(null)
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations.isEmpty()).isFalse();
        assertThat(violations)
                .anyMatch(v -> v.getMessage().equals("Role identifier is required"));
    }

    @Test
    void testValidate_WithroleIdentifierProvidedAndLegacySyncTrue_NoErrors() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(true)
                .roleIdentifier("CCMS002")
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void testValidate_WithroleIdentifierProvidedAndLegacySyncFalse_NoErrors() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(false)
                .roleIdentifier("CCMS003")
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void testValidate_WithroleIdentifierProvidedAndLegacySyncNull_RejectsWithError() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(null)
                .roleIdentifier("CCMS004")
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations.isEmpty()).isFalse();
        assertThat(violations)
                .anyMatch(v -> v.getMessage().equals("Legacy sync selection is required"));
    }

    @Test
    void testValidate_WithInternalUserTypeAndFirmTypeRestriction_RejectsWithError() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .firmTypeRestriction(List.of(FirmType.ADVOCATE))
                .legacySync(false)
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations.isEmpty()).isFalse();
        assertThat(violations)
                .anyMatch(v -> v.getMessage()
                        .equals(ValidationMessages.FIRM_TYPE_RESTRICTION_INTERNAL_ROLE));
    }

    @Test
    void testValidate_WithExternalUserTypeAndFirmTypeRestriction_NoErrors() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.EXTERNAL))
                .firmTypeRestriction(List.of(FirmType.ADVOCATE))
                .legacySync(false)
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations)
                .noneMatch(v -> v.getMessage()
                        .equals(ValidationMessages.FIRM_TYPE_RESTRICTION_INTERNAL_ROLE));
    }

    @Test
    void testValidate_WithInternalUserTypeAndNoFirmTypeRestriction_NoErrors() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .firmTypeRestriction(null)
                .legacySync(false)
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations)
                .noneMatch(v -> v.getMessage()
                        .equals(ValidationMessages.FIRM_TYPE_RESTRICTION_INTERNAL_ROLE));
    }
}
