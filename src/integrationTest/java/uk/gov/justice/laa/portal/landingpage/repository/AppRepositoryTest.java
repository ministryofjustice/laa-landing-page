package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import uk.gov.justice.laa.portal.landingpage.entity.App;

@DataJpaTest
public class AppRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRoleRepository appRoleRepository;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;

    @BeforeEach
    public void beforeEach() {
        // Delete child tables first to avoid foreign key constraint violations
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(repository);
    }

    @Test
    public void testSaveAndRetrieveLaaApp() {
        App app = buildLaaApp("App1", "Entra App 1", "Security Group Id",
                "Security Group Name");
        repository.saveAndFlush(app);

        App result = repository.findById(app.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(app.getId());
        Assertions.assertThat(result.getName()).isEqualTo("App1");
        Assertions.assertThat(result.getEntraAppId()).isEqualTo("Entra App 1");
        Assertions.assertThat(result.getSecurityGroupOid()).isEqualTo("Security Group Id");
        Assertions.assertThat(result.getSecurityGroupName()).isEqualTo("Security Group Name");
    }
}
