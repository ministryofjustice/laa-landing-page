package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JpaConfigTest {

    @Test
    public void auditorAware() {
        JpaConfig jpaConfig = new JpaConfig();
        Assertions.assertNotNull(jpaConfig.auditorAware());
    }
}
