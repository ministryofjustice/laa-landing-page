package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.List;
import java.util.Optional;

@DataJpaTest
public class RoleAssignmentRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRepository appRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRoleRepository appRoleRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RoleAssignmentRepository repository;

    @BeforeEach
    public void beforeEach() {
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);
    }

    @Test
    public void roleAssignmentAndRetrieval() {
        // Arrange
        App app = App.builder().name("app").securityGroupOid("sec_grp_oid").securityGroupName("sec_grp_name")
                .title("Lassie App Title").description("Lassie App Description").oidGroupName("Lassie OID Group")
                .appType(AppType.LAA).url("http://localhost:8080/lassie").enabled(true).build();
        appRepository.save(app);

        AppRole appRole1 = AppRole.builder().name("appRole1").description("appRole1").userTypeRestriction(new UserType[] {UserType.EXTERNAL}).app(app).build();
        AppRole appRole2 = AppRole.builder().name("appRole2").description("appRole2").userTypeRestriction(new UserType[] {UserType.EXTERNAL}).app(app).build();
        appRoleRepository.saveAll(List.of(appRole1, appRole2));
        int oldAssignmentsSize = repository.findAll().size();

        // Act
        RoleAssignment roleAssignment = RoleAssignment.builder().assigningRole(appRole1).assignableRole(appRole2).build();
        repository.save(roleAssignment);

        // Assert
        List<RoleAssignment> assignments = repository.findAll();

        Assertions.assertThat(assignments).isNotNull();
        Assertions.assertThat(assignments).hasSize(oldAssignmentsSize + 1);

        Optional<RoleAssignment> result = assignments.stream()
                .filter(assignment -> assignment.getAssigningRole().equals(appRole1) && assignment.getAssignableRole().equals(appRole2))
                .findFirst();
        Assertions.assertThat(result).isNotEmpty();
    }

    @Test
    public void findByAssigningRole() {
        // Arrange
        App app = App.builder().name("app").securityGroupOid("sec_grp_oid").securityGroupName("sec_grp_name")
                .title("Lassie App Title").description("Lassie App Description").oidGroupName("Lassie OID Group")
                .appType(AppType.LAA).url("http://localhost:8080/lassie").enabled(true).build();
        appRepository.save(app);

        AppRole appRole1 = AppRole.builder().name("appRole1").description("appRole1").userTypeRestriction(new UserType[] {UserType.EXTERNAL}).app(app).build();
        AppRole appRole2 = AppRole.builder().name("appRole2").description("appRole2").userTypeRestriction(new UserType[] {UserType.EXTERNAL}).app(app).build();
        AppRole appRole3 = AppRole.builder().name("appRole3").description("appRole3").userTypeRestriction(new UserType[] {UserType.EXTERNAL}).app(app).build();
        appRoleRepository.saveAll(List.of(appRole1, appRole2, appRole3));

        // Act
        RoleAssignment roleAssignment1 = RoleAssignment.builder().assigningRole(appRole1).assignableRole(appRole2).build();
        RoleAssignment roleAssignment2 = RoleAssignment.builder().assigningRole(appRole1).assignableRole(appRole3).build();
        repository.saveAll(List.of(roleAssignment1, roleAssignment2));

        // Assert
        List<RoleAssignment> assignments = repository.findByAssigningRole_IdIn(List.of(appRole1.getId()));

        Assertions.assertThat(assignments).isNotNull();
        Assertions.assertThat(assignments).hasSize(2);

        List<RoleAssignment> empty = repository.findByAssigningRole_IdIn(List.of(appRole2.getId()));
        Assertions.assertThat(empty).hasSize(0);
    }
}
