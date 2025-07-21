package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.Arrays;
import java.util.Set;

@DataJpaTest
public class AppRoleRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRepository appRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRoleRepository repository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
        appRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveLaaAppRole() {
        App app = buildLaaApp("App1", "Entra App 1", "Security Group Id",
                "Security Group Name");
        appRepository.saveAndFlush(app);

        AppRole appRole1 = buildLaaAppRole(app, "App Role 1");
        AppRole appRole2 = buildLaaAppRole(app, "App Role 2");
        app.getAppRoles().add(appRole1);
        app.getAppRoles().add(appRole2);
        repository.saveAllAndFlush(Arrays.asList(appRole1, appRole2));


        AppRole result = repository.findById(appRole1.getId()).orElseThrow();

        Assertions.assertThat(result.getId()).isEqualTo(appRole1.getId());
        Assertions.assertThat(result.getName()).isEqualTo("App Role 1");
        Assertions.assertThat(result.getUserTypeRestriction()).isNotNull();
        Assertions.assertThat(result.getUserTypeRestriction()).hasSize(1);
        Assertions.assertThat(result.getUserTypeRestriction().stream().findFirst().get()).isEqualTo(UserType.INTERNAL);

        Assertions.assertThat(result.getApp()).isNotNull();

        App resultApp = result.getApp();
        Assertions.assertThat(resultApp).isNotNull();
        Assertions.assertThat(resultApp.getId()).isEqualTo(app.getId());
        Assertions.assertThat(resultApp.getName()).isEqualTo("App1");
        Assertions.assertThat(resultApp.getAppRoles()).isNotEmpty();
        Assertions.assertThat(resultApp.getAppRoles()).containsExactlyInAnyOrder(appRole1, appRole2);

    }

    @Test
    public void testSaveAndRetrieveLaaAppRoleMultipleUserTypeRestriction() {
        App app = buildLaaApp("App1", "Entra App 1", "Security Group Id",
                "Security Group Name");
        appRepository.saveAndFlush(app);

        AppRole appRole1 = buildLaaAppRole(app, "App Role 1");
        appRole1.getUserTypeRestriction().add(UserType.EXTERNAL_SINGLE_FIRM_ADMIN);
        repository.saveAndFlush(appRole1);


        AppRole result = repository.findById(appRole1.getId()).orElseThrow();

        Assertions.assertThat(result.getId()).isEqualTo(appRole1.getId());
        Assertions.assertThat(result.getName()).isEqualTo("App Role 1");
        Assertions.assertThat(result.getUserTypeRestriction()).isNotNull();
        Assertions.assertThat(result.getUserTypeRestriction()).hasSize(2);
        Assertions.assertThat(result.getUserTypeRestriction())
                .containsAll(Set.of(UserType.INTERNAL, UserType.EXTERNAL_SINGLE_FIRM_ADMIN));

    }

}
