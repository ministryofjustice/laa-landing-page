package uk.gov.justice.laa.portal.landingpage.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;

public class EndDateAfterStartDateValidator implements
        ConstraintValidator<EndDateAfterStartDate, EntraUser> {

    @Override
    public boolean isValid(EntraUser entraUser, ConstraintValidatorContext context) {

        if (entraUser == null || entraUser.getStartDate() == null || entraUser.getEndDate() == null) {
            return true;
        }

        return (entraUser.getStartDate()).isBefore(entraUser.getEndDate());
    }
}
