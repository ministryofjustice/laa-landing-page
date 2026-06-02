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

import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileSilasStatus;
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
                .description("Lassie App Description")
                .ordinal(0)
                .enabled(true)
                .securityGroupOid("test-audit-security-group-oid")
                .appType(AppType.LAA)
                .url("http://localhost:8080/lassie")
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
                .silasStatus(UserProfileSilasStatus.COMPLETE)
                .createdBy("Test")
                .createdDate(user.getCreatedDate())
                .build();
        profile = userProfileRepository.save(profile);

        // Maintain bidirectional relationship
        user.getUserProfiles().add(profile);

        return user;
    }
}
