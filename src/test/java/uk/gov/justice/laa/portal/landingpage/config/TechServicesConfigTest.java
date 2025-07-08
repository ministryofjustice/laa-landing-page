package uk.gov.justice.laa.portal.landingpage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.service.TechServicesClient;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class TechServicesConfigTest {

    @Mock
    private EntraUserRepository entraUserRepository;
    @Mock
    private ObjectMapper objectMapper;

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
        TechServicesClient techServicesClient = techServicesConfig.techServicesNotifier(client, entraUserRepository, objectMapper);

        assertThat(techServicesClient).isNotNull();
    }

}
