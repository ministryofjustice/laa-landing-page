package uk.gov.justice.laa.portal.landingpage.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.jdbc.config.annotation.web.http.JdbcHttpSessionConfiguration;

public class ConditionalJdbcSessionImportSelector implements ImportSelector, EnvironmentAware {

    private Environment environment;

    @NotNull
    @Override
    public String[] selectImports(@NotNull AnnotationMetadata importingClassMetadata) {

        String enabled = environment.getProperty("SPRING_SESSION_JDBC_ENABLED", "false");

        if ("true".equalsIgnoreCase(enabled)) {
            String springJdbcSessionClazz = JdbcHttpSessionConfiguration.class.getName();
            return new String[]{springJdbcSessionClazz};
        }

        return new String[0];
    }

    @Override
    public void setEnvironment(@NotNull Environment environment) {
        this.environment = environment;
    }
}
