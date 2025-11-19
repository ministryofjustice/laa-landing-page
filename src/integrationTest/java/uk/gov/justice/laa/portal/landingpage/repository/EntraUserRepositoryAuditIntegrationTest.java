package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

/**
 * Integration tests for EntraUserRepository audit queries
 */
@DataJpaTest
class EntraUserRepositoryAuditIntegrationTest extends BaseRepositoryTest {

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
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);
        firmRepository.deleteAll();

        // Create test firms
        testFirm1 = buildFirm("Test Firm One", "TF001");
        testFirm1 = firmRepository.save(testFirm1);

        testFirm2 = buildFirm("Test Firm Two", "TF002");
        testFirm2 = firmRepository.save(testFirm2);

        // Create test app and roles
        App testApp = App.builder()
                .name("Test Audit App")
                .ordinal(0)
                .enabled(true)
                .securityGroupOid("test-audit-security-group-oid")
                .securityGroupName("Test Audit Security Group")
                .build();
        testApp = appRepository.save(testApp);

        globalAdminRole = AppRole.builder()
                .name("Test Global Admin")
                .description("Test Global Administrator")
                .ordinal(0)
                .legacySync(false)
                .authzRole(true)
                .app(testApp)
                .build();
        globalAdminRole = appRoleRepository.save(globalAdminRole);

        externalUserAdminRole = AppRole.builder()
                .name("Test External User Admin")
                .description("Test External User Administrator")
                .ordinal(1)
                .legacySync(false)
                .authzRole(true)
                .app(testApp)
                .build();
        externalUserAdminRole = appRoleRepository.save(externalUserAdminRole);
    }

    @Test
    void findAllUsersForAudit_whenNoFilters_returnsAllUsers() {
        // Given
        createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Admin", "User", "admin@justice.gov.uk", null, UserType.INTERNAL, globalAdminRole);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When
        Page<EntraUser> result = entraUserRepository.findAllUsersForAudit(null, null, null, null, pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    void findAllUsersForAudit_whenSearchByName_returnsMatchingUsers() {
        // Given
        createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Johnny", "Walker", "johnny@example.com", testFirm1, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When
        Page<EntraUser> result = entraUserRepository.findAllUsersForAudit("john", null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting("firstName")
                .containsExactlyInAnyOrder("John", "Johnny");
    }

    @Test
    void findAllUsersForAudit_whenSearchByEmail_returnsMatchingUsers() {
        // Given
        createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("email"));

        // When
        Page<EntraUser> result = entraUserRepository.findAllUsersForAudit("jane.smith", null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("jane.smith@example.com");
    }

    @Test
    void findAllUsersForAudit_whenFilterByFirm_returnsUsersFromThatFirm() {
        // Given
        createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, null);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Bob", "Brown", "bob.brown@example.com", testFirm1, UserType.EXTERNAL, null);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When
        Page<EntraUser> result = entraUserRepository.findAllUsersForAudit(null, testFirm1.getId(), null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting("firstName")
                .containsExactlyInAnyOrder("John", "Bob");
    }

    @Test
    void findAllUsersForAudit_whenFilterBySilasRole_returnsUsersWithThatRole() {
        // Given
        createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, externalUserAdminRole);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm2, UserType.EXTERNAL, null);
        createTestUser("Admin", "User", "admin@justice.gov.uk", null, UserType.INTERNAL, globalAdminRole);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When
        Page<EntraUser> result = entraUserRepository.findAllUsersForAudit(null, null, "Test Global Admin", null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("Admin");
    }

    @Test
    void findAllUsersForAudit_whenMultipleFilters_returnsIntersection() {
        // Given
        createTestUser("John", "Doe", "john.doe@example.com", testFirm1, UserType.EXTERNAL, externalUserAdminRole);
        createTestUser("Jane", "Smith", "jane.smith@example.com", testFirm1, UserType.EXTERNAL, null);
        createTestUser("Admin", "User", "admin@justice.gov.uk", null, UserType.INTERNAL, globalAdminRole);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When
        Page<EntraUser> result = entraUserRepository.findAllUsersForAudit(
                "john", testFirm1.getId(), "Test External User Admin", null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    void findAllUsersForAudit_withPagination_returnsCorrectPage() {
        // Given
        for (int i = 1; i <= 15; i++) {
            createTestUser("User" + i, "Test", "user" + i + "@example.com",
                    testFirm1, UserType.EXTERNAL, null);
        }

        PageRequest pageRequest = PageRequest.of(1, 5, Sort.by("firstName"));

        // When
        Page<EntraUser> result = entraUserRepository.findAllUsersForAudit(null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(15);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getNumber()).isEqualTo(1); // 0-based page number
        assertThat(result.getContent()).hasSize(5);
    }

    @Test
    void findUsersWithProfilesAndRoles_loadsAllRelationships() {
        // Given
        EntraUser user1 = createTestUser("John", "Doe", "john.doe@example.com",
                testFirm1, UserType.EXTERNAL, externalUserAdminRole);
        EntraUser user2 = createTestUser("Jane", "Smith", "jane.smith@example.com",
                testFirm2, UserType.EXTERNAL, globalAdminRole);

        entraUserRepository.flush();
        userProfileRepository.flush();

        Set<UUID> userIds = Set.of(user1.getId(), user2.getId());

        // When
        List<EntraUser> result = entraUserRepository.findUsersWithProfilesAndRoles(userIds);

        // Then
        assertThat(result).hasSize(2);

        EntraUser loadedUser1 = result.stream()
                .filter(u -> u.getId().equals(user1.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(loadedUser1.getUserProfiles()).isNotNull();
        assertThat(loadedUser1.getUserProfiles()).hasSize(1);

        UserProfile profile = loadedUser1.getUserProfiles().iterator().next();
        assertThat(profile.getFirm()).isNotNull();
        assertThat(profile.getFirm().getName()).isEqualTo("Test Firm One");
        assertThat(profile.getAppRoles()).isNotNull();
        assertThat(profile.getAppRoles()).hasSize(1);
        assertThat(profile.getAppRoles().iterator().next().getName()).isEqualTo("Test External User Admin");
    }

    @Test
    void findUsersWithProfilesAndRoles_whenEmptySet_returnsEmptyList() {
        // Given
        Set<UUID> emptyIds = Set.of();

        // When
        List<EntraUser> result = entraUserRepository.findUsersWithProfilesAndRoles(emptyIds);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findAllUsersForAudit_handlesMultiFirmUsers() {
        // Given
        EntraUser multiFirmUser = buildEntraUser("multi-oid-123", "multi@example.com", "Multi", "Firm");
        multiFirmUser.setMultiFirmUser(true);
        multiFirmUser = entraUserRepository.save(multiFirmUser);

        // Create two profiles for different firms
        UserProfile profile1 = UserProfile.builder()
                .entraUser(multiFirmUser)
                .firm(testFirm1)
                .userType(UserType.EXTERNAL)
                .activeProfile(true)
                .appRoles(new HashSet<>())
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .createdBy("Test")
                .createdDate(multiFirmUser.getCreatedDate())
                .build();
        userProfileRepository.save(profile1);

        UserProfile profile2 = UserProfile.builder()
                .entraUser(multiFirmUser)
                .firm(testFirm2)
                .userType(UserType.EXTERNAL)
                .activeProfile(false)
                .appRoles(new HashSet<>())
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .createdBy("Test")
                .createdDate(multiFirmUser.getCreatedDate())
                .build();
        userProfileRepository.save(profile2);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // When
        Page<EntraUser> result = entraUserRepository.findAllUsersForAudit(null, null, null, null, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).isMultiFirmUser()).isTrue();
    }

    private EntraUser createTestUser(String firstName, String lastName, String email,
            Firm firm, UserType userType, AppRole role) {
        EntraUser user = buildEntraUser(UUID.randomUUID().toString(), email, firstName, lastName);
        user = entraUserRepository.save(user);

        Set<AppRole> roles = new HashSet<>();
        if (role != null) {
            roles.add(role);
        }

        UserProfile profile = UserProfile.builder()
                .entraUser(user)
                .firm(firm)
                .userType(userType)
                .activeProfile(true)
                .appRoles(roles)
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .createdBy("Test")
                .createdDate(user.getCreatedDate())
                .build();
        profile = userProfileRepository.save(profile);

        // Maintain bidirectional relationship
        user.getUserProfiles().add(profile);

        return user;
    }
}
