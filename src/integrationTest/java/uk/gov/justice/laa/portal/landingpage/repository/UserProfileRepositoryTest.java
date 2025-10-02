package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.hibernate.exception.ConstraintViolationException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.transaction.Transactional;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

@DataJpaTest
public class UserProfileRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraUserRepository entraUserRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private FirmRepository firmRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private UserProfileRepository repository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
        entraUserRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveLaaUserProfile() {
        String entraUserId = generateEntraId();
        EntraUser entraUser = buildEntraUser(entraUserId, "test@email.com", "First Name5", "Last Name5");
        entraUserRepository.saveAndFlush(entraUser);

        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.INTERNAL);
        repository.saveAndFlush(userProfile);

        UserProfile result = repository.findById(userProfile.getId()).orElseThrow();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userProfile.getId());
        assertThat(result.getEntraUser()).isNotNull();
        assertThat(result.getEntraUser().getId()).isEqualTo(entraUser.getId());
        assertThat(result.getEntraUser().getEntraOid()).isEqualTo(entraUserId);
        assertThat(result.getEntraUser().getEmail()).isEqualTo(entraUser.getEmail());
        assertThat(result.getEntraUser().getFirstName()).isEqualTo("First Name5");
        assertThat(result.getEntraUser().getLastName()).isEqualTo("Last Name5");
        assertThat(result.getLegacyUserId()).isNotNull();
    }

    @Test
    public void testSaveAndRetrieveMultipleLaaUserProfilesForEntraUser() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        Firm firm2 = buildFirm("Firm2", "Firm Code 2");
        firmRepository.saveAll(Arrays.asList(firm1, firm2));

        String entraUserId = generateEntraId();
        EntraUser entraUser = buildEntraUser(entraUserId, "test6@email.com", "First Name6", "Last Name6");
        entraUserRepository.save(entraUser);

        UserProfile userProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        UserProfile userProfile2 = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        userProfile1.setFirm(firm1);
        userProfile2.setFirm(firm2);
        entraUser.getUserProfiles().add(userProfile1);
        entraUser.getUserProfiles().add(userProfile2);

        repository.saveAllAndFlush(Arrays.asList(userProfile1, userProfile2));

        UserProfile result = repository.findById(userProfile1.getId()).orElseThrow();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userProfile1.getId());
        assertThat(result.getEntraUser()).isNotNull();
        assertThat(result.getEntraUser().getId()).isEqualTo(entraUser.getId());
        assertThat(result.getEntraUser().getEntraOid()).isEqualTo(entraUserId);
        assertThat(result.getEntraUser().getFirstName()).isEqualTo("First Name6");
        assertThat(result.getEntraUser().getLastName()).isEqualTo("Last Name6");
        assertThat(result.getEntraUser().getUserProfiles()).isNotEmpty();
        assertThat(result.getEntraUser().getUserProfiles()).containsExactlyInAnyOrder(userProfile1, userProfile2);

    }

    @Test
    public void testSaveAndRetrieveMultipleLaaUserProfilesOneDefaultProfile() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        Firm firm2 = buildFirm("Firm2", "Firm Code 2");
        firmRepository.saveAll(Arrays.asList(firm1, firm2));

        EntraUser entraUser = buildEntraUser(generateEntraId(), "test7@email.com", "First Name7", "Last Name7");
        entraUserRepository.save(entraUser);

        UserProfile userProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        UserProfile userProfile2 = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        userProfile1.setFirm(firm1);
        userProfile2.setFirm(firm2);
        userProfile1.setActiveProfile(true);
        userProfile2.setActiveProfile(true);
        entraUser.getUserProfiles().add(userProfile1);
        entraUser.getUserProfiles().add(userProfile2);

        DataIntegrityViolationException diEx = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAllAndFlush(Arrays.asList(userProfile1, userProfile2)),
                "DataIntegrityViolationException expected");
        assertThat(diEx.getCause()).isInstanceOf(ConstraintViolationException.class);
        assertThat(diEx.getCause().getMessage()).contains("one_active_profile_per_user");
    }

    @Test
    public void testOneUserProfilePerFirm() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        firmRepository.save(firm1);

        EntraUser entraUser = buildEntraUser(generateEntraId(), "test9@email.com", "First Name9", "Last Name9");
        entraUserRepository.save(entraUser);

        UserProfile userProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        UserProfile userProfile2 = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        userProfile1.setFirm(firm1);
        userProfile2.setFirm(firm1);
        entraUser.getUserProfiles().add(userProfile1);
        entraUser.getUserProfiles().add(userProfile2);

        DataIntegrityViolationException diEx = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAllAndFlush(Arrays.asList(userProfile1, userProfile2)),
                "DataIntegrityViolationException expected");
        assertThat(diEx.getCause()).isInstanceOf(ConstraintViolationException.class);
        assertThat(diEx.getCause().getMessage()).contains("one_profile_per_firm_for_external_user");

    }

    @Test
    public void testFirmNullAllowedForMultiFirmExternalUsers() {
        // Test that multi-firm EXTERNAL users can have a null firm (constraint removed for STB-2671)
        EntraUser entraUser = buildEntraUser(generateEntraId(), "test10@email.com", "First Name10", "Last Name10");
        entraUser.setMultiFirmUser(true); // Mark as multi-firm user
        entraUserRepository.save(entraUser);

        UserProfile userProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        entraUser.getUserProfiles().add(userProfile1);

        // Should not throw an exception - multi-firm users can have null firm
        UserProfile saved = repository.saveAndFlush(userProfile1);
        assertThat(saved.getFirm()).isNull();
        assertThat(saved.getUserType()).isEqualTo(UserType.EXTERNAL);
    }

    @Test
    public void testFirmNotNullForSingleFirmExternalUsers() {
        // Test that single-firm EXTERNAL users still need a firm assigned
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        firmRepository.save(firm1);

        EntraUser entraUser = buildEntraUser(generateEntraId(), "test11@email.com", "First Name11", "Last Name11");
        entraUser.setMultiFirmUser(false); // Mark as single-firm user
        entraUserRepository.save(entraUser);

        UserProfile userProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        userProfile1.setFirm(firm1);
        entraUser.getUserProfiles().add(userProfile1);

        // Should save successfully with a firm
        UserProfile saved = repository.saveAndFlush(userProfile1);
        assertThat(saved.getFirm()).isNotNull();
        assertThat(saved.getFirm().getName()).isEqualTo("Firm1");
    }

    @Test
    public void testFirmNullForInternalUsers() {

        EntraUser entraUser = buildEntraUser(generateEntraId(), "test12@email.com", "First Name12", "Last Name12");
        entraUserRepository.save(entraUser);

        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.INTERNAL);
        entraUser.getUserProfiles().add(userProfile);

        repository.saveAndFlush(userProfile);

        UserProfile result = repository.findById(userProfile.getId()).orElseThrow();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userProfile.getId());
        assertThat(result.getFirm()).isNull();
        assertThat(result.getEntraUser()).isNotNull();
        assertThat(result.getEntraUser().getId()).isEqualTo(entraUser.getId());
        assertThat(result.getEntraUser().getFirstName()).isEqualTo("First Name12");
        assertThat(result.getEntraUser().getLastName()).isEqualTo("Last Name12");

    }

    @Test
    void shouldReturnCorrectInternalUser_whenSearchingByInternalUserType() {
        UUID expectedOid = UUID.randomUUID();
        EntraUser entraUser = buildEntraUser(expectedOid.toString(), "test12@email.com", "First Name12", "Last Name12");
        entraUserRepository.save(entraUser);

        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.INTERNAL);
        entraUser.getUserProfiles().add(userProfile);

        repository.saveAndFlush(userProfile);

        List<UUID> result = repository.findByUserTypes(UserType.INTERNAL);
        assertThat(result).containsExactly(UUID.fromString(entraUser.getEntraOid()));
    }

    @Test
    void shouldReturnNoUsers_whenSearchingByInternalUserType() {
        UUID expectedOid = UUID.randomUUID();
        Firm firm = buildFirm("Firm1", "Firm Code 1");
        firmRepository.save(firm);
        EntraUser entraUser = buildEntraUser(expectedOid.toString(), "test6@email.com", "First Name6", "Last Name6");
        entraUserRepository.save(entraUser);

        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        userProfile.setFirm(firm);
        entraUser.getUserProfiles().add(userProfile);
        repository.saveAndFlush(userProfile);

        List<UUID> result = repository.findByUserTypes(UserType.INTERNAL);
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    public void testInternalUserCanOnlyHaveOneProfile() {
        // Setup internal user
        EntraUser internalUser = buildEntraUser(UUID.randomUUID().toString(), "internaluser@test.com", "Internal", "User");

        // Persist user
        internalUser = entraUserRepository.saveAndFlush(internalUser);

        // create new profile and persist
        UserProfile internalUserProfile = buildLaaUserProfile(internalUser, UserType.INTERNAL, true);
        repository.saveAndFlush(internalUserProfile);

        // Link to user.
        Set<UserProfile> internalUserProfiles = new HashSet<>();
        internalUserProfiles.add(internalUserProfile);
        internalUser.setUserProfiles(internalUserProfiles);
        entraUserRepository.saveAndFlush(internalUser);

        // Try to add another profile to user and assert it throws exception
        UserProfile additionalProfile = buildLaaUserProfile(internalUser, UserType.INTERNAL, false);
        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(additionalProfile));
        assertThat(exception.getCause().getMessage()).contains("one_profile_per_internal_user");
    }
}
