package uk.gov.justice.laa.portal.landingpage.validation;

import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.UUID;

/**
 * Custom validator for RoleCreationDto that enforces basic role identifier and metadata validation.
 *
 * Uniqueness of the Role Identifier is enforced in the AppRoleService to keep repository access out
 * of the constraint validator and to keep validation responsibilities clearly separated.
 */
@Slf4j
@Component
public class RoleCreationValidator implements ConstraintValidator<ValidRoleCreation, RoleCreationDto> {

    @Override
    public boolean isValid(RoleCreationDto dto, ConstraintValidatorContext ctx) {
        boolean valid = true;
        ctx.disableDefaultConstraintViolation();

        String roleIdentifier = dto.getRoleIdentifier();
        if (roleIdentifier == null || roleIdentifier.trim().isEmpty()) {
            ctx.buildConstraintViolationWithTemplate("Role identifier is required.")
                   .addPropertyNode("roleIdentifier")
                   .addConstraintViolation();
            valid = false;
            log.warn("Validation failed: role identifier is missing");
        }

        UUID parentAppId = dto.getParentAppId();
        if (parentAppId == null) {
            ctx.buildConstraintViolationWithTemplate("Parent app is required.")
               .addPropertyNode("parentAppId")
                .addConstraintViolation();
            valid = false;
            log.warn("Validation failed: parent app is missing");
        }

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
