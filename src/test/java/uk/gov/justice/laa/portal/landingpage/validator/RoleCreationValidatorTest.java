package uk.gov.justice.laa.portal.landingpage.validator;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.validation.RoleCreationValidator;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for RoleCreationValidator
 */
class RoleCreationValidatorTest {

    private final RoleCreationValidator validator = new RoleCreationValidator();

    @Test
    void testSupports_WithRoleCreationDto_ReturnsTrue() {
        // Act & Assert
        assertThat(validator.supports(RoleCreationDto.class)).isTrue();
    }

    @Test
    void testSupports_WithOtherClass_ReturnsFalse() {
        // Act & Assert
        assertThat(validator.supports(String.class)).isFalse();
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
        Errors errors = new BeanPropertyBindingResult(dto, "roleCreationDto");

        // Act
        validator.validate(dto, errors);

        // Assert
        assertThat(errors.hasErrors()).isFalse();
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
        Errors errors = new BeanPropertyBindingResult(dto, "roleCreationDto");

        // Act
        validator.validate(dto, errors);

        // Assert
        assertThat(errors.hasErrors()).isTrue();
        assertThat(errors.hasFieldErrors("ccmsCode")).isTrue();
        org.springframework.validation.FieldError fieldError = errors.getFieldError("ccmsCode");
        assertThat(fieldError).isNotNull();
        assertThat(fieldError.getCode()).isEqualTo("role.ccmsCode.required.when.legacy.sync");
        assertThat(fieldError.getDefaultMessage())
                .isEqualTo("Enter a CCMS code for roles that sync with CCMS.");
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
        Errors errors = new BeanPropertyBindingResult(dto, "roleCreationDto");

        // Act
        validator.validate(dto, errors);

        // Assert
        assertThat(errors.hasErrors()).isTrue();
        assertThat(errors.hasFieldErrors("ccmsCode")).isTrue();
        org.springframework.validation.FieldError fieldError = errors.getFieldError("ccmsCode");
        assertThat(fieldError).isNotNull();
        assertThat(fieldError.getCode()).isEqualTo("role.ccmsCode.required.when.legacy.sync");
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
        Errors errors = new BeanPropertyBindingResult(dto, "roleCreationDto");

        // Act
        validator.validate(dto, errors);

        // Assert
        assertThat(errors.hasErrors()).isFalse();
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
        Errors errors = new BeanPropertyBindingResult(dto, "roleCreationDto");

        // Act
        validator.validate(dto, errors);

        // Assert
        assertThat(errors.hasErrors()).isFalse();
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
        Errors errors = new BeanPropertyBindingResult(dto, "roleCreationDto");

        // Act
        validator.validate(dto, errors);

        // Assert
        assertThat(errors.hasErrors()).isTrue();
        assertThat(errors.hasFieldErrors("legacySync")).isTrue();
        org.springframework.validation.FieldError fieldError = errors.getFieldError("legacySync");
        assertThat(fieldError).isNotNull();
        assertThat(fieldError.getCode()).isEqualTo("role.legacy.sync.required.when.ccms.code");
        assertThat(fieldError.getDefaultMessage())
                .isEqualTo("This role must have legacy sync enabled when a CCMS code is provided.");
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
        Errors errors = new BeanPropertyBindingResult(dto, "roleCreationDto");

        // Act
        validator.validate(dto, errors);

        // Assert
        assertThat(errors.hasErrors()).isTrue();
        assertThat(errors.hasFieldErrors("legacySync")).isTrue();
        org.springframework.validation.FieldError fieldError = errors.getFieldError("legacySync");
        assertThat(fieldError).isNotNull();
        assertThat(fieldError.getCode()).isEqualTo("role.legacy.sync.required.when.ccms.code");
    }

    @Test
    void testValidate_WithLegacySyncNullAndNoCcmsCode_NoErrors() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(null)
                .ccmsCode(null)
                .build();
        Errors errors = new BeanPropertyBindingResult(dto, "roleCreationDto");

        // Act
        validator.validate(dto, errors);

        // Assert
        assertThat(errors.hasErrors()).isFalse();
    }

    @Test
    void testValidate_WithNullErrorsObject_Completes() {
        // Arrange
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(UUID.randomUUID())
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(true)
                .ccmsCode(null)
                .build();

        // Act & Assert - should not throw exception
        validator.validate(dto, null);
    }

    @Test
    void testValidate_WithNullTarget_Completes() {
        // Arrange
        Errors errors = new BeanPropertyBindingResult(new Object(), "test");

        // Act & Assert - should not throw exception
        validator.validate(null, errors);
    }
}





