package uk.gov.justice.laa.portal.landingpage.validation;

import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

/**
 * Custom validator for RoleCreationDto that enforces interdependent validation rules
 * between Legacy Sync and CCMS Code fields.
 */
@Slf4j
@Component
public class RoleCreationValidator implements ConstraintValidator<ValidRoleCreation, RoleCreationDto> {

    @Override
    public boolean isValid(RoleCreationDto dto, ConstraintValidatorContext ctx) {
        boolean valid = true;
        ctx.disableDefaultConstraintViolation();

        boolean legacySyncEnabled = dto.getLegacySync() != null && dto.getLegacySync();
        String ccmsCode = dto.getCcmsCode();
        boolean ccmsCodeProvided = ccmsCode != null && !ccmsCode.trim().isEmpty();


        if (legacySyncEnabled && !ccmsCodeProvided) {
            ctx.buildConstraintViolationWithTemplate("Enter a CCMS code for roles that sync with CCMS.")
                                       .addPropertyNode("ccmsCode")
                                        .addConstraintViolation();
            valid = false;
            log.warn("Validation failed: Legacy sync is enabled but CCMS code is missing");
        }


        if (ccmsCodeProvided && !legacySyncEnabled) {
            ctx.buildConstraintViolationWithTemplate("This role must have legacy sync enabled when a CCMS code is provided.")
                                        .addPropertyNode("legacySync")
                                      .addConstraintViolation();
            valid = false;
            log.warn("Validation failed: CCMS code is provided but legacy sync is not enabled");
        }

        boolean isInternalOnly = dto.getUserTypeRestriction() != null
                && dto.getUserTypeRestriction().size() == 1
                && dto.getUserTypeRestriction().contains(UserType.INTERNAL);
        boolean hasFirmTypeRestriction = dto.getFirmTypeRestriction() != null
                && !dto.getFirmTypeRestriction().isEmpty();

        if (isInternalOnly && hasFirmTypeRestriction) {
            ctx.buildConstraintViolationWithTemplate("Firm type restriction cannot be applied to internal roles.")
                                        .addPropertyNode("firmTypeRestriction")
                                        .addConstraintViolation();
            valid = false;
            log.warn("Validation failed: Firm type restriction cannot be applied to internal roles");
        }

        return valid;
    }
}
