package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.justice.laa.portal.landingpage.entity.AppRegistration;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;

import java.util.Arrays;

@DataJpaTest
public class AppRoleRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRegistrationRepository appRegistrationRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRepository appRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRoleRepository repository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
        appRegistrationRepository.deleteAll();
        appRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveLaaAppRole() {
        AppRegistration appRegistration = buildEntraAppRegistration("App Registration");
        appRegistrationRepository.save(appRegistration);

        App app = buildLaaApp(appRegistration, "App1");
        appRepository.saveAndFlush(app);

        AppRole appRole1 = buildLaaAppRole(app, "App Role 1");
        AppRole appRole2 = buildLaaAppRole(app, "App Role 2");
        app.getAppRoles().add(appRole1);
        app.getAppRoles().add(appRole2);
        repository.saveAllAndFlush(Arrays.asList(appRole1, appRole2));


        AppRole result = repository.findById(appRole1.getId()).orElseThrow();

        Assertions.assertThat(result.getId()).isEqualTo(appRole1.getId());
        Assertions.assertThat(result.getName()).isEqualTo("App Role 1");
        Assertions.assertThat(result.getApp()).isNotNull();

        App resultApp = result.getApp();
        Assertions.assertThat(resultApp).isNotNull();
        Assertions.assertThat(resultApp.getId()).isEqualTo(app.getId());
        Assertions.assertThat(resultApp.getName()).isEqualTo("App1");
        Assertions.assertThat(resultApp.getAppRoles()).isNotEmpty();
        Assertions.assertThat(resultApp.getAppRoles()).containsExactlyInAnyOrder(appRole1, appRole2);
        Assertions.assertThat(resultApp.getAppRegistration()).isNotNull();

    }
}
