package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.portal.landingpage.entity.EntraAppRegistration;
import uk.gov.justice.laa.portal.landingpage.entity.LaaApp;
import uk.gov.justice.laa.portal.landingpage.entity.LaaAppRole;

import java.util.Arrays;

public class LaaAppRoleRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraAppRegistrationRepository entraAppRegistrationRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private LaaAppRepository laaAppRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private LaaAppRoleRepository repository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
        entraAppRegistrationRepository.deleteAll();
        laaAppRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveLaaAppRole() {
        EntraAppRegistration entraAppRegistration = buildEntraAppRegistration("Entra App");
        entraAppRegistrationRepository.save(entraAppRegistration);

        LaaApp laaApp = buildLaaApp(entraAppRegistration, "LAA App1");
        laaAppRepository.saveAndFlush(laaApp);

        LaaAppRole laaAppRole1 = buildLaaAppRole(laaApp, "LAA App Role 1");
        LaaAppRole laaAppRole2 = buildLaaAppRole(laaApp, "LAA App Role 2");
        laaApp.getAppRoles().add(laaAppRole1);
        laaApp.getAppRoles().add(laaAppRole2);
        repository.saveAllAndFlush(Arrays.asList(laaAppRole1, laaAppRole2));


        LaaAppRole result = repository.findById(laaAppRole1.getId()).orElseThrow();

        Assertions.assertThat(result.getId()).isEqualTo(laaAppRole1.getId());
        Assertions.assertThat(result.getName()).isEqualTo("LAA App Role 1");
        Assertions.assertThat(result.getCreatedBy()).isEqualTo("Test");
        Assertions.assertThat(result.getCreatedDate()).isNotNull();
        Assertions.assertThat(result.getLaaApp()).isNotNull();

        LaaApp resultLaaApp = result.getLaaApp();
        Assertions.assertThat(resultLaaApp).isNotNull();
        Assertions.assertThat(resultLaaApp.getId()).isEqualTo(laaApp.getId());
        Assertions.assertThat(resultLaaApp.getName()).isEqualTo("LAA App1");
        Assertions.assertThat(resultLaaApp.getCreatedBy()).isEqualTo("Test");
        Assertions.assertThat(resultLaaApp.getCreatedDate()).isNotNull();
        Assertions.assertThat(resultLaaApp.getAppRoles()).isNotEmpty();
        Assertions.assertThat(resultLaaApp.getAppRoles()).containsExactlyInAnyOrder(laaAppRole1, laaAppRole2);
        Assertions.assertThat(resultLaaApp.getEntraAppRegistration()).isNotNull();

    }
}
