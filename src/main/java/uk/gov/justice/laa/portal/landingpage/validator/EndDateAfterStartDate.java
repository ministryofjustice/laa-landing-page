package uk.gov.justice.laa.portal.landingpage.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = EndDateAfterStartDateValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EndDateAfterStartDate {
    String message() default "End date must be after Start date";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
