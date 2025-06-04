package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.justice.laa.portal.landingpage.entity.AppRegistration;
import uk.gov.justice.laa.portal.landingpage.entity.App;

@DataJpaTest
public class AppRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRegistrationRepository appRegistrationRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRepository repository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
        appRegistrationRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveLaaApp() {
        AppRegistration appRegistration = buildEntraAppRegistration("App Registration");
        appRegistrationRepository.saveAndFlush(appRegistration);

        App app = buildLaaApp(appRegistration, "App1");
        repository.saveAndFlush(app);

        App result = repository.findById(app.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(app.getId());
        Assertions.assertThat(result.getName()).isEqualTo("App1");
        Assertions.assertThat(result.getAppRegistration()).isNotNull();

        AppRegistration resultAppRegistration = result.getAppRegistration();
        Assertions.assertThat(resultAppRegistration.getId()).isEqualTo(appRegistration.getId());
        Assertions.assertThat(resultAppRegistration.getName()).isEqualTo("App Registration");

    }
}
