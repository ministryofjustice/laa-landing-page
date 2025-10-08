package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalJdbcSession
public class JdbcSessionConfig {
    // JDBC session configuration
}

