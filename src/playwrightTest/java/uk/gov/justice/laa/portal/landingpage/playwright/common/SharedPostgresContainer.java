package uk.gov.justice.laa.portal.landingpage.playwright.common;

import org.testcontainers.containers.PostgreSQLContainer;

public final class SharedPostgresContainer {

    private static final PostgreSQLContainer<?> INSTANCE =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("test_db")
                    .withUsername("postgres")
                    .withPassword("password");

    static {
        INSTANCE.start();
    }

    private SharedPostgresContainer() {}

    public static PostgreSQLContainer<?> getInstance() {
        return INSTANCE;
    }
}
