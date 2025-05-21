package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.portal.landingpage.entity.EntraAppRegistration;
import uk.gov.justice.laa.portal.landingpage.entity.LaaApp;

public class LaaAppRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraAppRegistrationRepository entraAppRegistrationRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private LaaAppRepository repository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
        entraAppRegistrationRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveLaaApp() {
        EntraAppRegistration entraAppRegistration = buildEntraAppRegistration("Entra App");
        entraAppRegistrationRepository.saveAndFlush(entraAppRegistration);

        LaaApp laaApp = buildLaaApp(entraAppRegistration, "LAA App1");
        repository.saveAndFlush(laaApp);

        LaaApp result = repository.findById(laaApp.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(laaApp.getId());
        Assertions.assertThat(result.getName()).isEqualTo("LAA App1");
        Assertions.assertThat(result.getCreatedBy()).isEqualTo("Test");
        Assertions.assertThat(result.getCreatedDate()).isNotNull();
        Assertions.assertThat(result.getEntraAppRegistration()).isNotNull();

        EntraAppRegistration resultEntraAppRegistration = result.getEntraAppRegistration();
        Assertions.assertThat(resultEntraAppRegistration.getId()).isEqualTo(entraAppRegistration.getId());
        Assertions.assertThat(resultEntraAppRegistration.getName()).isEqualTo("Entra App");
        Assertions.assertThat(resultEntraAppRegistration.getCreatedBy()).isEqualTo("Test");
        Assertions.assertThat(resultEntraAppRegistration.getCreatedDate()).isNotNull();

    }
}
