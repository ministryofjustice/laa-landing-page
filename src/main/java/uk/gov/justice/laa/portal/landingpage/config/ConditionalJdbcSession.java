package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.TYPE})
@Import(ConditionalJdbcSessionImportSelector.class)
public @interface ConditionalJdbcSession {
}
