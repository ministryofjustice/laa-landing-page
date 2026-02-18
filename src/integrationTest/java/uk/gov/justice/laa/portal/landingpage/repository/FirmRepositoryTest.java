package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Tuple;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
public class FirmRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private FirmRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private OfficeRepository officeRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private UserProfileRepository userProfileRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraUserRepository entraUserRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRepository appRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRoleRepository appRoleRepository;

    @BeforeEach
    public void beforeEach() {
        // Clean in FK-safe order
        userProfileRepository.deleteAll();
        entraUserRepository.deleteAll();

        // Keep consistent with other repository tests that avoid deleting authz seed data
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);

        officeRepository.deleteAll();
        repository.deleteAll();
    }


    @Test
    public void testSaveAndRetrieveFirm() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        Firm firm2 = buildFirm("Firm2", "Firm Code 2");
        repository.saveAllAndFlush(Arrays.asList(firm1, firm2));

        Firm result = repository.findById(firm1.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(firm1.getId());
        Assertions.assertThat(result.getName()).isEqualTo("Firm1");
        Assertions.assertThat(result.getCode()).isEqualTo("Firm Code 1");
        Assertions.assertThat(result.getType()).isEqualTo(FirmType.ADVOCATE);

    }

    @Test
    public void testSaveAndRetrieveChildFirm() {
        Firm firm1 = buildParentFirm("Firm1", "Firm Code 1");
        Firm firm2 = buildChildFirm("Firm2", "Firm Code 2", firm1);
        repository.saveAllAndFlush(Arrays.asList(firm1, firm2));

        Firm result = repository.findById(firm2.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(firm2.getId());
        Assertions.assertThat(result.getParentFirm().getId()).isEqualTo(firm1.getId());

    }

    @Test
    public void testSaveSelfParentFirm() {
        Firm firm1 = buildParentFirm("Firm1", "Firm Code 1");
        firm1.setParentFirm(firm1);
        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(firm1), "Exception expected");
        Assertions.assertThat(ex.getMessage()).contains("new row for relation \"firm\" violates check constraint \"self_parent\"");
    }

    @Test
    public void testSaveGrandParentFirm() {
        Firm firm1 = buildParentFirm("Firm1", "Firm Code 1");
        Firm firm2 = buildChildFirm("Firm2", "Firm Code 2", firm1);
        firm2.setType(FirmType.LEGAL_SERVICES_PROVIDER);
        Firm firm3 = buildChildFirm("Firm3", "Firm Code 3", firm2);
        JpaSystemException ex = assertThrows(JpaSystemException.class,
                () -> repository.saveAllAndFlush(Arrays.asList(firm1, firm2, firm3)), "Exception expected");
        Assertions.assertThat(ex.getMessage()).contains("parent firm (" + firm2.getId() + ") already has parent");
    }

    @Test
    public void testFindRoleCountsByFirm() {

        Firm firm = buildFirm("Firm1", "Firm Code 1");
        firm = repository.saveAndFlush(firm);
        EntraUser entraUser = buildEntraUser(generateEntraId(), "role.counts@test.com", "First", "Last");
        entraUser.setUserStatus(UserStatus.ACTIVE);
        entraUser = entraUserRepository.saveAndFlush(entraUser);

        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        userProfile.setFirm(firm);
        userProfile.setUserProfileStatus(UserProfileStatus.COMPLETE);
        App app = appRepository.saveAndFlush(buildTestApp("roleCountsTestApp"));
        AppRole role = appRoleRepository.saveAndFlush(buildExternalRole(app, "roleCountsTestRole"));
        userProfile.setAppRoles(Set.of(role));
        userProfileRepository.saveAndFlush(userProfile);

        List<Tuple> result = repository.findRoleCountsByFirm();

        Assertions.assertThat(result)
                .extracting(t -> tuple(
                        t.get("firmId"),
                        t.get("firmName"),
                        t.get("firmCode"),
                        t.get("roleName"),
                        ((Number) t.get("userCount")).longValue()
                ))
                .contains(tuple(
                        firm.getId(),
                        "Firm1",
                        "Firm Code 1",
                        "roleCountsTestRole",
                        1L
                ));
    }

    @Test
    public void testFindRoleCountsByFirm_filtersOutNonExternalRestrictedRoles() {
        Firm firm = repository.saveAndFlush(buildFirm("Firm1", "Firm Code 1"));
        App app = appRepository.saveAndFlush(buildTestApp("roleCountsTestApp"));

        // Role does NOT include EXTERNAL in user_type_restriction - query should exclude it
        AppRole internalOnlyRole = appRoleRepository.saveAndFlush(
                AppRole.builder()
                        .name("internalOnlyRole")
                        .description("internalOnlyRole")
                        .userTypeRestriction(new UserType[]{UserType.INTERNAL})
                        .app(app)
                        .build()
        );

        createExternalUserProfileInFirmWithRoles(firm, "role.counts.2@test.com", Set.of(internalOnlyRole));

        List<Tuple> result = repository.findRoleCountsByFirm();

        Assertions.assertThat(result)
                .extracting(t -> t.get("roleName"))
                .doesNotContain("internalOnlyRole");
    }

    @Test
    public void testFindRoleCountsByFirm_countsDistinctUserProfilesPerRole() {
        Firm firm = repository.saveAndFlush(buildFirm("Firm1", "Firm Code 1"));
        App app = appRepository.saveAndFlush(buildTestApp("roleCountsTestApp"));
        AppRole role = appRoleRepository.saveAndFlush(buildExternalRole(app, "roleCountsTestRole"));

        createExternalUserProfileInFirmWithRoles(firm, "role.counts.3a@test.com", Set.of(role));
        createExternalUserProfileInFirmWithRoles(firm, "role.counts.3b@test.com", Set.of(role));

        List<Tuple> result = repository.findRoleCountsByFirm();

        Assertions.assertThat(result)
                .extracting(t -> tuple(
                        t.get("firmId"),
                        t.get("roleName"),
                        ((Number) t.get("userCount")).longValue()
                ))
                .contains(tuple(firm.getId(), "roleCountsTestRole", 2L));
    }

    @Test
    public void testFindRoleCountsByFirm_returnsOneRowPerRolePerFirm() {
        Firm firm = repository.saveAndFlush(buildFirm("Firm1", "Firm Code 1"));
        App app = appRepository.saveAndFlush(buildTestApp("roleCountsTestApp"));

        AppRole roleA = appRoleRepository.saveAndFlush(buildExternalRole(app, "roleA"));
        AppRole roleB = appRoleRepository.saveAndFlush(buildExternalRole(app, "roleB"));

        // One user has more than 1 role roles - should produce 2 grouped rows (same firm, different roleName)
        createExternalUserProfileInFirmWithRoles(firm, "role.counts.4@test.com", Set.of(roleA, roleB));

        List<Tuple> result = repository.findRoleCountsByFirm();

        Assertions.assertThat(result)
                .extracting(t -> tuple(
                        t.get("firmId"),
                        t.get("roleName"),
                        ((Number) t.get("userCount")).longValue()
                ))
                .contains(
                        tuple(firm.getId(), "roleA", 1L),
                        tuple(firm.getId(), "roleB", 1L));
    }

    @Test
    public void testFindRoleCountsByFirm_ordersByFirmNameThenRoleName() {

        Firm firmA = repository.saveAndFlush(buildFirm("Alpha Firm", "A1"));
        Firm firmB = repository.saveAndFlush(buildFirm("Beta Firm", "B1"));
        App app = appRepository.saveAndFlush(buildTestApp("roleCountsTestApp"));

        AppRole roleA = appRoleRepository.saveAndFlush(buildExternalRole(app, "A Role"));
        AppRole roleB = appRoleRepository.saveAndFlush(buildExternalRole(app, "B Role"));

        createExternalUserProfileInFirmWithRoles(firmB, "role.counts.5b@test.com", Set.of(roleB));
        createExternalUserProfileInFirmWithRoles(firmA, "role.counts.5a@test.com", Set.of(roleB));
        createExternalUserProfileInFirmWithRoles(firmA, "role.counts.5a2@test.com", Set.of(roleA));

        List<Tuple> result = repository.findRoleCountsByFirm();

        Assertions.assertThat(result)
                .extracting(t -> tuple(
                        t.get("firmName"),
                        t.get("roleName")
                ))
                .containsSubsequence(
                        tuple("Alpha Firm", "A Role"),
                        tuple("Alpha Firm", "B Role"),
                        tuple("Beta Firm", "B Role"));
    }

    @Test
    public void testFindMultiFirmUserCountsByFirm() {

        // Create three multi-firm users
        EntraUser user1 = buildMultifirmEntraUser(generateEntraId(), "user1@example.com", "User", "One", true);
        user1 = entraUserRepository.saveAndFlush(user1);
        EntraUser user2 = buildMultifirmEntraUser(generateEntraId(), "user2@example.com", "User", "Two", true);
        user2 = entraUserRepository.saveAndFlush(user2);
        EntraUser user3 = buildMultifirmEntraUser(generateEntraId(), "user3@example.com", "User", "Three", false);
        user3 = entraUserRepository.saveAndFlush(user3);

        // Create firms
        Firm firm1 = buildFirm("Firm Epsilon", "EPSILON");
        Firm firm2 = buildFirm("Firm Zeta", "ZETA");
        Firm firm3 = buildFirm("Firm Eta", "ETA");
        repository.saveAllAndFlush(List.of(firm1, firm2, firm3));


        UserProfile user1P1 = buildLaaUserProfile(user1, UserType.EXTERNAL);
        user1P1.setFirm(firm1);
        UserProfile user1P2 = buildLaaUserProfile(user1, UserType.EXTERNAL);
        user1P2.setFirm(firm2);
        UserProfile user1P3 = buildLaaUserProfile(user1, UserType.EXTERNAL);
        user1P3.setFirm(firm3);

        user1.getUserProfiles().addAll(List.of(user1P1, user1P2, user1P3));
        userProfileRepository.saveAllAndFlush(List.of(user1P1, user1P2, user1P3));
        UserProfile user2P1 = buildLaaUserProfile(user2, UserType.EXTERNAL);
        user2P1.setFirm(firm3);
        user2.getUserProfiles().add(user2P1);
        userProfileRepository.saveAndFlush(user2P1);
        UserProfile user3P1 = buildLaaUserProfile(user3, UserType.EXTERNAL);
        user3P1.setFirm(firm2);
        user3.getUserProfiles().add(user3P1);
        userProfileRepository.saveAndFlush(user3P1);

        List<Object[]> result = repository.findMultiFirmUserCountsByFirm();

        assertThat(result)
                .hasSize(3)
                .containsExactlyInAnyOrder(
                        new Object[]{"Firm Epsilon", "EPSILON", 1L},
                        new Object[]{"Firm Zeta", "ZETA", 1L},
                        new Object[]{"Firm Eta", "ETA", 2L});
    }

    private App buildTestApp(String name) {
        return App.builder()
                .name(name)
                .title(name + " Title")
                .description(name + " Description")
                .oidGroupName(name + " OID Group")
                .appType(AppType.LAA)
                .url("http://localhost/" + name)
                .enabled(true)
                .securityGroupOid(name + "_sg_oid")
                .securityGroupName(name + "_sg_name")
                .build();
    }

    private AppRole buildExternalRole(App app, String roleName) {
        return AppRole.builder()
                .name(roleName)
                .description(roleName)
                .userTypeRestriction(new UserType[]{UserType.EXTERNAL})
                .app(app)
                .build();
    }

    private void createExternalUserProfileInFirmWithRoles(Firm firm, String email, Set<AppRole> roles) {
        EntraUser entraUser = buildEntraUser(generateEntraId(), email, "First", "Last");
        entraUser.setUserStatus(UserStatus.ACTIVE);
        entraUser = entraUserRepository.saveAndFlush(entraUser);

        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        userProfile.setFirm(firm);
        userProfile.setUserProfileStatus(UserProfileStatus.COMPLETE);
        userProfile.setAppRoles(roles);
        userProfileRepository.saveAndFlush(userProfile);
    }
}
