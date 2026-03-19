package uk.gov.justice.laa.portal.landingpage.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
    void testValidate_WithLegacySyncTrueAndCcmsCodeProvided_NoErrors() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(true)
                .ccmsCode("CCMS001")
                .build();
        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void testValidate_WithLegacySyncTrueAndNoCcmsCode_RejectsWithError() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(true)
                .ccmsCode(null)
                .build();


        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations.isEmpty()).isFalse();
        assertThat(violations)
                .anyMatch(v -> v.getMessage()
                        .equals("Enter a CCMS code for roles that sync with CCMS."));
    }

    @Test
    void testValidate_WithLegacySyncTrueAndEmptyCcmsCode_RejectsWithError() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(true)
                .ccmsCode("   ")
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations.isEmpty()).isFalse();
        assertThat(violations)
                .anyMatch(v -> v.getMessage()
                        .equals("Enter a CCMS code for roles that sync with CCMS."));
    }

    @Test
    void testValidate_WithLegacySyncFalseAndNoCcmsCode_NoErrors() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(false)
                .ccmsCode(null)
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void testValidate_WithCcmsCodeProvidedAndLegacySyncTrue_NoErrors() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(true)
                .ccmsCode("CCMS002")
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void testValidate_WithCcmsCodeProvidedAndLegacySyncFalse_RejectsWithError() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(false)
                .ccmsCode("CCMS003")
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations.isEmpty()).isFalse();
        assertThat(violations)
                .anyMatch(v -> v.getMessage()
                        .equals("This role must have legacy sync enabled when a CCMS code is provided."));
    }

    @Test
    void testValidate_WithCcmsCodeProvidedAndLegacySyncNull_RejectsWithError() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(null)
                .ccmsCode("CCMS004")
                .build();

        // Act
        Set<ConstraintViolation<RoleCreationDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations.isEmpty()).isFalse();
        assertThat(violations)
                .anyMatch(v -> v.getMessage()
                        .equals("This role must have legacy sync enabled when a CCMS code is provided."));
    }
}





