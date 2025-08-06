package uk.gov.justice.laa.portal.landingpage.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class ConditionalPatternValidator implements ConstraintValidator<ConditionalPattern, String> {
    
    private Pattern pattern;
    
    @Override
    public void initialize(ConditionalPattern constraintAnnotation) {
        this.pattern = Pattern.compile(constraintAnnotation.regexp());
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // If value is null or empty, skip pattern validation (let @NotEmpty handle it)
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        
        // Only validate pattern if value is not empty
        return pattern.matcher(value).matches();
    }
}
