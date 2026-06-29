package uk.gov.justice.laa.portal.landingpage.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.InvitationStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileSilasStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class EntraUserRepositoryCustomAuditSearchIntegrationTest extends BaseRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntraUserRepositoryCustomAuditSearchImpl repository;

    @Autowired
    private EntraUserRepository entraUserRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private FirmRepository firmRepository;

    @Autowired
    private AppRepository appRepository;

    private Firm testFirm1;
    private Firm testFirm2;
    private AppRole globalAdminRole;
    private AppRole externalUserAdminRole;

    @BeforeEach
    void setUp() {
        // Clean up
        userProfileRepository.deleteAll();
        entraUserRepository.deleteAll();
        deleteNonAuthzAppRoleAssignments();
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);
        firmRepository.deleteAll();

        // Create test firms
        testFirm1 = buildFirm("Test Firm One", "TF001");
        testFirm1 = firmRepository.save(testFirm1);

        testFirm2 = buildFirm("Test Firm Two", "TF002");
        testFirm2 = firmRepository.save(testFirm2);

        // Create test app and roles
        App testApp = App.builder().name("Test Audit App").description("Lassie App Description").ordinal(0).enabled(true)
                .securityGroupOid("test-audit-security-group-oid").appType(AppType.LAA).url("http://localhost:8080/lassie").build();
        testApp = appRepository.save(testApp);

        globalAdminRole = AppRole.builder().name("Test Global Admin").description("Test Global Administrator").ordinal(0)
                .legacySync(false).authzRole(true).app(testApp).build();
        globalAdminRole = appRoleRepository.save(globalAdminRole);

        externalUserAdminRole = AppRole.builder().name("Test External User Admin").description("Test External User Administrator")
                .ordinal(1).legacySync(false).authzRole(true).app(testApp).build();
        externalUserAdminRole = appRoleRepository.save(externalUserAdminRole);
    }

    @Test
    void findAuditUsersWithDynamicProjection_whenNoFilters_returnsAllUsers() {
        // Given
        createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Admin", "User", "admin@justice.gov.uk", null, UserType.INTERNAL, globalAdminRole);

        Pageable pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection(null, null, null,
                null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().stream().map(row -> (String) row[1]).toArray()).containsExactly("admin user", "jane smith", "john doe");
    }

    @Test
    void findAuditUsersWithDynamicProjection_whenSearchByName_returnsMatchingUsers() {
        // Given
        createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Johnny", "Walker", "johnny@example.com", testFirm1, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection(null, "john", null,
                null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().stream().map(row -> (String) row[1]).toArray()).containsExactlyInAnyOrder("john doe", "johnny walker");
    }

    @Test
    void findAuditUsersWithDynamicProjection_whenSearchByEmail_returnsMatchingUsers() {
        // Given
        createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("email"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection(null, "jane.smith", null,
                null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().stream().map(row -> (String) row[1]).toArray()).containsExactlyInAnyOrder("jane smith");
    }

    @Test
    void findAuditUsersWithDynamicProjection_whenFilterByFirm_returnsUsersFromThatFirm() {
        // Given
        createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Bob", "Brown", "bob.brown@example.com", testFirm1, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection(null, null, testFirm1.getId(),
                null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().stream().map(row -> (String) row[1]).toArray()).containsExactlyInAnyOrder("john doe", "bob brown");
    }

    @Test
    void findAuditUsersWithDynamicProjection_whenFilterBySilasRole_returnsUsersWithThatRole() {
        // Given
        createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, externalUserAdminRole);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Admin", "User", "admin@justice.gov.uk", null, UserType.INTERNAL, globalAdminRole);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection(null, null, null,
                "Test Global Admin", null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().stream().map(row -> (String) row[1]).toArray()).containsExactlyInAnyOrder("admin user");
    }

    @Test
    void findAuditUsersWithDynamicProjection_whenMultipleFilters_returnsIntersection() {
        // Given
        createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, externalUserAdminRole);
        createTestUser("Adam", "Twist", "adam.twist@example.com", testFirm1, UserType.EXTERNAL, externalUserAdminRole);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm1, UserType.EXTERNAL, null);
        createTestUser("John", "Read", "john.read@example.com", testFirm2, UserType.EXTERNAL, externalUserAdminRole);
        createTestUser("Admin", "User", "admin@justice.gov.uk", null, UserType.INTERNAL, globalAdminRole);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection(null, "john", testFirm1.getId(),
                "Test External User Admin", null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().stream().map(row -> (String) row[1]).toArray()).containsExactlyInAnyOrder("john doe");
    }

    @Test
    void findAuditUsersWithDynamicProjection_withPagination_returnsCorrectPage() {
        // Given
        for (int i = 1; i <= 15; i++) {
            createTestUser("User" + i, "Test", "user" + i + "@example.com", testFirm1, UserType.EXTERNAL, null);
        }

        PageRequest pageRequest = PageRequest.of(1, 5, Sort.by("firstName"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection(null, null, null,
                null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(15);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getNumber()).isEqualTo(1); // 0-based page number
        assertThat(result.getContent()).hasSize(5);
    }

    @Test
    void findAuditUsersWithDynamicProjection_whenNoMatch_shouldReturnEmpty() {
        // Given
        createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("email"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection(null, "chris", null,
                null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void findAuditUsersWithDynamicProjection_handlesMultiFirmUsers() {
        // Given
        EntraUser multiFirmUser = buildEntraUser("multi-oid-123", "multi@example.com", "Multi", "Firm");
        multiFirmUser.setMultiFirmUser(true);
        multiFirmUser = entraUserRepository.save(multiFirmUser);

        // Create two profiles for different firms
        UserProfile profile1 = UserProfile.builder().entraUser(multiFirmUser).firm(testFirm1).userType(UserType.EXTERNAL)
                .activeProfile(true).appRoles(new HashSet<>()).userProfileStatus(UserProfileStatus.COMPLETE)
                .silasStatus(UserProfileSilasStatus.COMPLETE).createdBy("Test").createdDate(multiFirmUser.getCreatedDate()).build();
        userProfileRepository.save(profile1);

        UserProfile profile2 = UserProfile.builder().entraUser(multiFirmUser).firm(testFirm2).userType(UserType.EXTERNAL)
                .activeProfile(false).appRoles(new HashSet<>()).userProfileStatus(UserProfileStatus.COMPLETE)
                .silasStatus(UserProfileSilasStatus.COMPLETE).createdBy("Test").createdDate(multiFirmUser.getCreatedDate()).build();
        userProfileRepository.save(profile2);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection(null, null, null,
                null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().stream().map(row -> (String) row[1]).toArray()).containsExactlyInAnyOrder("multi firm");
    }

    @Test
    void findAuditUsersWithDynamicProjection_withNeverActivated_returnsOnlyUsersWhoseInvitationStatusIsNotVerified() {
        // Given
        // invitation not yet successful — should be included
        createTestUserWithLoginAndInvitation("Never", "Activated", "never@example.com", InvitationStatus.INVITE_SENT);

        // verified — should be excluded
        createTestUserWithLoginAndInvitation("Verified", "User", "verified@example.com", InvitationStatus.VERIFICATION_SUCCESS);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection(null, null, null,
                null, null, null, null, null, true, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().stream().map(row -> (String) row[1]).toArray()).containsExactlyInAnyOrder("never activated");
    }

    @Test
    void findAuditUsersWithDynamicProjection_includesUsersWithAnyLoginDateOrInvitationStatus() {
        // Given
        createTestUserWithLoginAndInvitation("A", "User", "a@example.com", InvitationStatus.VERIFICATION_SUCCESS);
        createTestUserWithLoginAndInvitation("B", "User", "b@example.com", InvitationStatus.INVITE_SENT);
        createTestUserWithLoginAndInvitation("C", "User", "c@example.com", InvitationStatus.AWAITING_VERIFICATION);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When — no dormant filters
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection(null, null, null,
                null, null, null, null, null, null, pageRequest);

        // Then — all users returned regardless
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void findAuditUsersWithDynamicProjection_sortByName() {
        // Given
        EntraUser testUser = createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createAndAddUserProfile(testUser, testFirm2, UserType.EXTERNAL, null, UserProfileSilasStatus.COMPLETE, false);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Johnny", "Walker", "johnny@example.com", testFirm1, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("NAME"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection("NAME", null,
                null, null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().stream().map(row -> row[1].toString()).toArray()).containsExactly("jane smith", "john doe", "johnny walker");
    }

    @Test
    void findAuditUsersWithDynamicProjection_sortByNameDesc() {
        // Given
        EntraUser testUser = createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createAndAddUserProfile(testUser, testFirm2, UserType.EXTERNAL, null, UserProfileSilasStatus.COMPLETE, false);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Johnny", "Walker", "johnny@example.com", testFirm1, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("NAME").descending());

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection("NAME", null,
                null, null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().stream().map(row -> row[1].toString()).toArray()).containsExactly("johnny walker", "john doe", "jane smith");
    }

    @Test
    void findAuditUsersWithDynamicProjection_sortByProfileCount() {
        // Given
        EntraUser testUser = createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createAndAddUserProfile(testUser, testFirm2, UserType.EXTERNAL, null, UserProfileSilasStatus.COMPLETE, false);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Johnny", "Walker", "johnny@example.com", testFirm1, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("PROFILE_COUNT"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection("PROFILE_COUNT", null,
                null, null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().stream().map(row -> Integer.parseInt(row[1].toString())).toArray()).containsExactly(1, 1, 2);
    }

    @Test
    void findAuditUsersWithDynamicProjection_sortByProfileCountDesc() {
        // Given
        EntraUser testUser = createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createAndAddUserProfile(testUser, testFirm2, UserType.EXTERNAL, null, UserProfileSilasStatus.COMPLETE, false);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Johnny", "Walker", "johnny@example.com", testFirm1, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("PROFILE_COUNT").descending());

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection("PROFILE_COUNT", null,
                null, null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().stream().map(row -> Integer.parseInt(row[1].toString())).toArray()).containsExactly(2, 1, 1);
    }

    @Test
    void findAuditUsersWithDynamicProjection_sortByFirmName() {
        // Given
        EntraUser testUser = createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createAndAddUserProfile(testUser, testFirm2, UserType.EXTERNAL, null, UserProfileSilasStatus.COMPLETE, false);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Johnny", "Walker", "johnny@example.com", testFirm1, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("FIRM_NAME"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection("FIRM_NAME", null,
                null, null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().stream().map(row -> row[1].toString()).toArray()).containsExactly("Test Firm One", "Test Firm One", "Test Firm Two");
    }

    @Test
    void findAuditUsersWithDynamicProjection_sortByFirmNameDesc() {
        // Given
        EntraUser testUser = createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createAndAddUserProfile(testUser, testFirm2, UserType.EXTERNAL, null, UserProfileSilasStatus.COMPLETE, false);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Johnny", "Walker", "johnny@example.com", testFirm1, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("FIRM_NAME").descending());

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection("FIRM_NAME", null,
                null, null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().stream().map(row -> row[1].toString()).toArray()).containsExactly("Test Firm Two", "Test Firm One", "Test Firm One");
    }

    @Test
    void findAuditUsersWithDynamicProjection_sortByMultiFirmFlag() {
        // Given
        EntraUser testUser = createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createAndAddUserProfile(testUser, testFirm2, UserType.EXTERNAL, null, UserProfileSilasStatus.COMPLETE, false);
        testUser = createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        testUser.setMultiFirmUser(false);
        createTestUser("Johnny", "Walker", "johnny@example.com", testFirm1, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("MULTI_FIRM"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection("MULTI_FIRM", null,
                null, null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().stream().map(row -> Integer.valueOf(row[1].toString())).toArray()).containsExactly(3, 3, 3);
    }

    @Test
    void findAuditUsersWithDynamicProjection_sortByMultiFirmFlagDesc() {
        // Given
        EntraUser testUser = createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createAndAddUserProfile(testUser, testFirm2, UserType.EXTERNAL, null, UserProfileSilasStatus.COMPLETE, false);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Johnny", "Walker", "johnny@example.com", testFirm1, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("MULTI_FIRM").descending());

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection("MULTI_FIRM", null,
                null, null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().stream().map(row -> Integer.valueOf(row[1].toString())).toArray()).containsExactly(3, 3, 3);
    }

    @Test
    void findAuditUsersWithDynamicProjection_sortByUserType() {
        // Given
        EntraUser testUser = createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createAndAddUserProfile(testUser, testFirm2, UserType.EXTERNAL, null, UserProfileSilasStatus.COMPLETE, false);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null, true);
        createTestUser("Johnny", "Walker", "johnny@example.com", null, UserType.INTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("USER_TYPE_RANK"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection("USER_TYPE_RANK", null,
                null, null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().stream().map(row -> row[1].toString()).toArray()).containsExactly("1", "2", "3");
    }

    @Test
    void findAuditUsersWithDynamicProjection_sortByUserTypeDesc() {
        // Given
        EntraUser testUser = createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createAndAddUserProfile(testUser, testFirm2, UserType.EXTERNAL, null, UserProfileSilasStatus.COMPLETE, false);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null, true);
        createTestUser("Johnny", "Walker", "johnny@example.com", null, UserType.INTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("USER_TYPE_RANK").descending());

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection("USER_TYPE_RANK", null,
                null, null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().stream().map(row -> row[1].toString()).toArray()).containsExactly("3", "2", "1");
    }

    @Test
    void findAuditUsersWithDynamicProjection_sortBySilasStatus() {
        // Given
        createTestUserWithLoginAndInvitation("No", "Profiles", "no.profile@example.com", InvitationStatus.INVITE_SENT);
        EntraUser testUser = createTestUserWithLoginAndInvitation("User", "Profile_no_roles", "no.roles@example.com", InvitationStatus.VERIFICATION_SUCCESS);
        createAndAddUserProfile(testUser, testFirm1, UserType.EXTERNAL, null, UserProfileSilasStatus.INCOMPLETE, true);
        testUser = createTestUserWithLoginAndInvitation("MutiFirm", "One_Profile_no_roles", "one.profile.roles@example.com", InvitationStatus.VERIFICATION_SUCCESS);
        createAndAddUserProfile(testUser, testFirm1, UserType.EXTERNAL, null, UserProfileSilasStatus.COMPLETE, true);
        createAndAddUserProfile(testUser, testFirm2, UserType.EXTERNAL, externalUserAdminRole, UserProfileSilasStatus.INCOMPLETE, false);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, externalUserAdminRole);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("STATUS_RANK"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection("STATUS_RANK", null,
                null, null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent().stream().map(row -> row[2].toString()).toArray()).containsExactly("1", "1", "1", "5");
    }

    @Test
    void findAuditUsersWithDynamicProjection_sortBySilasStatusDesc() {
        // Given
        createTestUserWithLoginAndInvitation("No", "Profiles", "no.profile@example.com", InvitationStatus.INVITE_SENT);
        EntraUser testUser = createTestUserWithLoginAndInvitation("User", "Profile_no_roles", "no.roles@example.com", InvitationStatus.VERIFICATION_SUCCESS);
        createAndAddUserProfile(testUser, testFirm1, UserType.EXTERNAL, null, UserProfileSilasStatus.INCOMPLETE, true);
        testUser = createTestUserWithLoginAndInvitation("MutiFirm", "One_Profile_no_roles", "one.profile.roles@example.com", InvitationStatus.VERIFICATION_SUCCESS);
        createAndAddUserProfile(testUser, testFirm1, UserType.EXTERNAL, null, UserProfileSilasStatus.COMPLETE, true);
        createAndAddUserProfile(testUser, testFirm2, UserType.EXTERNAL, externalUserAdminRole, UserProfileSilasStatus.INCOMPLETE, false);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, externalUserAdminRole);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("STATUS_RANK").descending());

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection("STATUS_RANK", null,
                null, null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent().stream().map(row -> row[2].toString()).toArray()).containsExactly("5", "1", "1", "1");
    }

    @Test
    void findAuditUsersWithDynamicProjection_sortByUnknown_defaultsToName() {
        // Given
        EntraUser testUser = createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createAndAddUserProfile(testUser, testFirm2, UserType.EXTERNAL, null, UserProfileSilasStatus.COMPLETE, false);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Johnny", "Walker", "johnny@example.com", testFirm1, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("UNKNOWN"));

        // When
        Page<Object[]> result = repository.findAuditUsersWithDynamicProjection("UNKNOWN", null,
                null, null, null, null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().stream().map(row -> row[1].toString()).toArray()).containsExactly("jane smith", "john doe", "johnny walker");
    }

    private EntraUser createTestUserWithLoginAndInvitation(String firstName, String lastName, String email, InvitationStatus invitationStatus) {
        EntraUser user = buildEntraUser(UUID.randomUUID().toString(), email, firstName, lastName);
        user.setInvitationStatus(invitationStatus);
        return entraUserRepository.save(user);
    }

    private EntraUser createTestUser(String firstName, String lastName, String email, Firm firm, UserType userType, AppRole role) {
        return createTestUser(firstName, lastName, email, firm, userType, role, false);
    }

    private EntraUser createTestUser(String firstName, String lastName, String email, Firm firm, UserType userType, AppRole role, boolean multiFirmUser) {
        EntraUser user = buildEntraUser(UUID.randomUUID().toString(), email, firstName, lastName, multiFirmUser);
        user = entraUserRepository.save(user);

        Set<AppRole> roles = new HashSet<>();
        if (role != null) {
            roles.add(role);
        }

        createAndAddUserProfile(user, firm, userType, role, UserProfileSilasStatus.COMPLETE, true);

        return user;
    }

    private void createAndAddUserProfile(EntraUser entraUser, Firm firm, UserType userType, AppRole role, UserProfileSilasStatus silasStatus, boolean activeProfile) {
        Set<AppRole> roles = new HashSet<>();
        if (role != null) {
            roles.add(role);
        }

        UserProfile profile = UserProfile.builder().entraUser(entraUser).firm(firm).userType(userType)
                .activeProfile(activeProfile).appRoles(roles).userProfileStatus(UserProfileStatus.COMPLETE).silasStatus(silasStatus)
                .createdBy("Test").createdDate(entraUser.getCreatedDate()).build();
        profile = userProfileRepository.save(profile);

        // Maintain bidirectional relationship
        entraUser.getUserProfiles().add(profile);
    }


}
