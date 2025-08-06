package uk.gov.justice.laa.portal.landingpage.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConditionalSizeValidator.class)
public @interface ConditionalSize {
    String message() default "Size validation failed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    int min() default 0;
    int max() default Integer.MAX_VALUE;
}
