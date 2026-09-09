package uk.gov.justice.laa.portal.landingpage.validation;

import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

/**
 * Custom validator for RoleCreationDto that enforces cross-field metadata rules.
 * - Prevents applying firm type restrictions to roles that are internal-only
 */
@Slf4j
@Component
public class RoleCreationValidator implements ConstraintValidator<ValidRoleCreation, RoleCreationDto> {

    @Override
    public boolean isValid(RoleCreationDto dto, ConstraintValidatorContext ctx) {
        boolean valid = true;
        ctx.disableDefaultConstraintViolation();

        boolean isInternalOnly = dto.getUserTypeRestriction() != null
                && !dto.getUserTypeRestriction().isEmpty()
                && dto.getUserTypeRestriction().stream().allMatch(UserType.INTERNAL::equals);
        boolean hasFirmTypeRestriction = dto.getFirmTypeRestriction() != null
                && !dto.getFirmTypeRestriction().isEmpty();

        if (isInternalOnly && hasFirmTypeRestriction) {
            ctx.buildConstraintViolationWithTemplate(ValidationMessages.FIRM_TYPE_RESTRICTION_INTERNAL_ROLE)
                                       .addPropertyNode("firmTypeRestriction")
                                       .addConstraintViolation();
            valid = false;
            log.warn("Validation failed: Firm type restriction cannot be applied to internal roles");
        }

        return valid;
    }
}
