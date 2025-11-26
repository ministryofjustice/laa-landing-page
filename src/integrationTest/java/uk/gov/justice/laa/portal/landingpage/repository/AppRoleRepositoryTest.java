package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.BaseEntity;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@DataJpaTest
public class AppRoleRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRepository appRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRoleRepository repository;

    @BeforeEach
    public void beforeEach() {
        deleteNonAuthzAppRoles(repository);
        deleteNonAuthzApps(appRepository);
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
        Assertions.assertThat(result.getUserTypeRestriction()[0]).isEqualTo(UserType.INTERNAL);

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
        appRole1.setUserTypeRestriction(new UserType[]{UserType.INTERNAL, UserType.EXTERNAL});
        repository.saveAndFlush(appRole1);


        AppRole result = repository.findById(appRole1.getId()).orElseThrow();

        Assertions.assertThat(result.getId()).isEqualTo(appRole1.getId());
        Assertions.assertThat(result.getName()).isEqualTo("App Role 1");
        Assertions.assertThat(result.getUserTypeRestriction()).isNotNull();
        Assertions.assertThat(result.getUserTypeRestriction()).hasSize(2);
        Assertions.assertThat(result.getUserTypeRestriction())
                .containsAll(Set.of(UserType.INTERNAL, UserType.EXTERNAL));

    }

    @Test
    public void findAllByIdInAndAuthzRoleIs() {
        App lassie = buildLaaApp("lassie", "Entra App 1", "Security Group Id",
                "Security Group Name", "Lassie App Title", "Lassie App Description",
                "Lassie OID Group", "http://localhost:8080/lassie");
        App crime = buildLaaApp("crime", "Entra App 2", "Security Group Id 2",
                "Security Group Name 2", "Crime App Title", "Crime App Description",
                "Crime OID Group", "http://localhost:8080/crime");
        appRepository.saveAllAndFlush(Arrays.asList(lassie, crime));

        AppRole lassieExMan = buildLaaAppRole(lassie, "App Role 1");
        lassieExMan.setAuthzRole(true);
        AppRole lassieInMan = buildLaaAppRole(lassie, "App Role 2");
        lassieInMan.setAuthzRole(true);

        AppRole crimeViewer = buildLaaAppRole(crime, "App Role 3");
        crimeViewer.setAuthzRole(false);

        repository.saveAllAndFlush(Arrays.asList(lassieExMan, lassieInMan, crimeViewer));

        List<UUID> ids = List.of(lassieExMan.getId(), lassieInMan.getId(), crimeViewer.getId());
        List<AppRole> authzRoles = repository.findAllByIdInAndAuthzRoleIs(ids, true);
        Assertions.assertThat(authzRoles).hasSize(2);
        List<AppRole> nonAuthzRoles = repository.findAllByIdInAndAuthzRoleIs(ids, false);
        Assertions.assertThat(nonAuthzRoles).hasSize(1);
        List<AppRole> allRoles = repository.findAllByIdIn(ids);
        Assertions.assertThat(allRoles).hasSize(3);
    }

}
