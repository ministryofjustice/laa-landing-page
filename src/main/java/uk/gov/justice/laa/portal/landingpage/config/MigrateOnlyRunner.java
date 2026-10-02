package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Exits the JVM once Liquibase has run during context startup, instead of
 * proceeding to serve requests. Used by the migration Job.
 */
@Component
@ConditionalOnProperty(name = "app.migrate-only", havingValue = "true")
@RequiredArgsConstructor
public class MigrateOnlyRunner implements ApplicationRunner {

    private final ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
