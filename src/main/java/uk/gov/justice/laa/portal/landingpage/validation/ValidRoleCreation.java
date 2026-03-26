package uk.gov.justice.laa.portal.landingpage.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RoleCreationValidator.class)
public @interface ValidRoleCreation {
    String message() default "Invalid registration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
