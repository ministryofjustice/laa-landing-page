package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.portal.landingpage.auth.DefaultTokenCredentialFactory;
import uk.gov.justice.laa.portal.landingpage.auth.TokenCredentialFactory;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = TokenCredentialFactoryConfig.class)
@TestPropertySource(properties = {"azure.client-id=test-client-id", "azure.client-secret=test-client-secret", "azure.tenant-id=test-tenant-id"})
class TokenCredentialFactoryConfigTest {

    @Autowired
    private TokenCredentialFactory tokenCredentialFactory;

    @Test
    void factory_shouldBeInstanceOfDefaultTokenCredentialFactory() {
        assertThat(tokenCredentialFactory).isInstanceOf(DefaultTokenCredentialFactory.class);
    }
}
