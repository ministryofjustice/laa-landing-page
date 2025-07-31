package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.RoleType;

import java.util.List;
import java.util.Set;

@DataJpaTest
public class PermissionRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRepository appRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRoleRepository appRoleRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private PermissionRepository repository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
        appRoleRepository.deleteAll();
        appRepository.deleteAll();
    }

    @Test
    public void roleAssignmentAndRetrieval() {
        // Arrange
        App app = App.builder().name("App with permissions").securityGroupName("sec_group")
                .securityGroupOid("sec_group_oid").build();
        appRepository.save(app);

        AppRole appRole1 = AppRole.builder().name("appRole1").description("appRole1").roleType(RoleType.EXTERNAL)
                .app(app).build();
        AppRole appRole2 = AppRole.builder().name("appRole2").description("appRole2").roleType(RoleType.EXTERNAL)
                .app(app).build();
        appRoleRepository.saveAll(List.of(appRole1, appRole2));

        // Act
        Permission rolePermission = Permission.builder().appRoles(Set.of(appRole1, appRole2))
                .name("app name").description("permission description").function("assign-all").build();
        repository.save(rolePermission);


        // Assert
        List<Permission> assignments = repository.findAll();

        Assertions.assertThat(assignments).isNotNull();
        Assertions.assertThat(assignments).hasSize(1);

        Permission result = assignments.stream().findFirst().get();
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getName()).isEqualTo("app name");
        Assertions.assertThat(result.getDescription()).isEqualTo("permission description");
        Assertions.assertThat(result.getFunction()).isEqualTo("assign-all");
        Assertions.assertThat(result.getAppRoles()).isNotNull();
        Assertions.assertThat(result.getAppRoles()).hasSize(2);
        Assertions.assertThat(result.getAppRoles()).containsAll(List.of(appRole1, appRole2));
    }

}
