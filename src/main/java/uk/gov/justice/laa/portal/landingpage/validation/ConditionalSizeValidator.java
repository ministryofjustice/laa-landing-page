package uk.gov.justice.laa.portal.landingpage.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConditionalSizeValidator implements ConstraintValidator<ConditionalSize, String> {
    
    private int min;
    private int max;
    
    @Override
    public void initialize(ConditionalSize constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // If value is null or empty, skip size validation (let @NotEmpty handle it)
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        
        // Only validate size if value is not empty
        int length = value.length();
        return length >= min && length <= max;
    }
}
