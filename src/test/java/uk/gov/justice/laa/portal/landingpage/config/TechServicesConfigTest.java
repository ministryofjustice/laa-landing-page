package uk.gov.justice.laa.portal.landingpage.config;

import com.azure.identity.ClientSecretCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.service.TechServicesClient;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class TechServicesConfigTest {

    @Mock
    private ClientSecretCredential clientSecretCredential;
    @Mock
    private EntraUserRepository entraUserRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private JwtDecoder jwtDecoder;

    private TechServicesConfig techServicesConfig;

    @BeforeEach
    void setUp() {
        techServicesConfig = new TechServicesConfig();
    }

    @Test
    void techServicesConfig_shouldCreateRestClientInstance() {
        RestClient client = techServicesConfig.restClient("http://localhost");

        assertThat(client).isNotNull();
    }

    @Test
    void techServicesConfig_shouldCreateTechServicesNotifierInstance() {
        RestClient client = techServicesConfig.restClient("http://localhost");
        TechServicesClient techServicesClient = techServicesConfig.techServicesClient(clientSecretCredential, client,
                entraUserRepository, cacheManager, jwtDecoder);

        assertThat(techServicesClient).isNotNull();
    }

    @Test
    void techServicesConfig_shouldCreateTechServicesClientSecretCredential() {
        ClientSecretCredential client = techServicesConfig.techServicesClientSecretCredential("client", "secret", "tenant");

        assertThat(client).isNotNull();
    }

}
