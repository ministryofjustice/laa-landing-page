package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaConfigTest {

    @Test
    public void auditorAware() {
        JpaConfig jpaConfig = new JpaConfig();
        assertThat(jpaConfig.auditorAware()).isNotNull();
    }
}
