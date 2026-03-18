package uk.gov.justice.laa.portal.landingpage.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;

/**
 * Custom validator for RoleCreationDto that enforces interdependent validation rules
 * between Legacy Sync and CCMS Code fields.
 */
@Slf4j
@Component
public class RoleCreationValidator implements Validator {

    @Override
    public boolean supports(@Nullable Class<?> type) {
        return type != null && RoleCreationDto.class.isAssignableFrom(type);
    }

    @Override
    public void validate(@Nullable Object target, @Nullable Errors errors) {
        if (target == null || errors == null) {
            return;
        }

        RoleCreationDto dto = (RoleCreationDto) target;

        boolean legacySyncEnabled = dto.getLegacySync() != null && dto.getLegacySync();
        String ccmsCode = dto.getCcmsCode();
        boolean ccmsCodeProvided = ccmsCode != null && !ccmsCode.trim().isEmpty();

        // Rule A: Legacy Sync → CCMS Code required
        if (legacySyncEnabled && !ccmsCodeProvided) {
            errors.rejectValue("ccmsCode", "role.ccmsCode.required.when.legacy.sync",
                    "Enter a CCMS code for roles that sync with CCMS.");
            log.debug("Validation failed: Legacy sync is enabled but CCMS code is missing");
        }

        // Rule B: CCMS Code → Legacy Sync must be true
        if (ccmsCodeProvided && !legacySyncEnabled) {
            errors.rejectValue("legacySync", "role.legacy.sync.required.when.ccms.code",
                    "This role must have legacy sync enabled when a CCMS code is provided.");
            log.debug("Validation failed: CCMS code is provided but legacy sync is not enabled");
        }
    }
}


