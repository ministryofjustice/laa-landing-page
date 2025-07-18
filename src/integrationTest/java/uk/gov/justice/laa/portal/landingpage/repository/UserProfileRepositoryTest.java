package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

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

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(userProfile.getId());
        Assertions.assertThat(result.getEntraUser()).isNotNull();
        Assertions.assertThat(result.getEntraUser().getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getEntraUser().getEntraOid()).isEqualTo(entraUserId);
        Assertions.assertThat(result.getEntraUser().getEmail()).isEqualTo(entraUser.getEmail());
        Assertions.assertThat(result.getEntraUser().getFirstName()).isEqualTo("First Name5");
        Assertions.assertThat(result.getEntraUser().getLastName()).isEqualTo("Last Name5");

    }

    @Test
    public void testSaveAndRetrieveMultipleLaaUserProfilesForEntraUser() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        Firm firm2 = buildFirm("Firm2", "Firm Code 2");
        firmRepository.saveAll(Arrays.asList(firm1, firm2));

        String entraUserId = generateEntraId();
        EntraUser entraUser = buildEntraUser(entraUserId, "test6@email.com", "First Name6", "Last Name6");
        entraUserRepository.save(entraUser);

        UserProfile userProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_MULTI_FIRM);
        UserProfile userProfile2 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_MULTI_FIRM);
        userProfile1.setFirm(firm1);
        userProfile2.setFirm(firm2);
        entraUser.getUserProfiles().add(userProfile1);
        entraUser.getUserProfiles().add(userProfile2);

        repository.saveAllAndFlush(Arrays.asList(userProfile1, userProfile2));

        UserProfile result = repository.findById(userProfile1.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(userProfile1.getId());
        Assertions.assertThat(result.getEntraUser()).isNotNull();
        Assertions.assertThat(result.getEntraUser().getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getEntraUser().getEntraOid()).isEqualTo(entraUserId);
        Assertions.assertThat(result.getEntraUser().getFirstName()).isEqualTo("First Name6");
        Assertions.assertThat(result.getEntraUser().getLastName()).isEqualTo("Last Name6");
        Assertions.assertThat(result.getEntraUser().getUserProfiles()).isNotEmpty();
        Assertions.assertThat(result.getEntraUser().getUserProfiles()).containsExactlyInAnyOrder(userProfile1, userProfile2);

    }

    @Test
    public void testSaveAndRetrieveMultipleLaaUserProfilesOneDefaultProfile() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        Firm firm2 = buildFirm("Firm2", "Firm Code 2");
        firmRepository.saveAll(Arrays.asList(firm1, firm2));

        EntraUser entraUser = buildEntraUser(generateEntraId(), "test7@email.com", "First Name7", "Last Name7");
        entraUserRepository.save(entraUser);

        UserProfile userProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_MULTI_FIRM);
        UserProfile userProfile2 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_MULTI_FIRM);
        userProfile1.setFirm(firm1);
        userProfile2.setFirm(firm2);
        userProfile1.setActiveProfile(true);
        userProfile2.setActiveProfile(true);
        entraUser.getUserProfiles().add(userProfile1);
        entraUser.getUserProfiles().add(userProfile2);

        DataIntegrityViolationException diEx = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAllAndFlush(Arrays.asList(userProfile1, userProfile2)),
                "DataIntegrityViolationException expected");
        Assertions.assertThat(diEx.getCause()).isInstanceOf(ConstraintViolationException.class);
        Assertions.assertThat(diEx.getCause().getMessage()).contains("one_active_profile_per_user");
    }

    @Test
    public void testNoMultipleProfilesForNonMultiFirmUser() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        Firm firm2 = buildFirm("Firm2", "Firm Code 2");
        firmRepository.saveAll(Arrays.asList(firm1, firm2));

        EntraUser entraUser = buildEntraUser(generateEntraId(), "test8@email.com", "First Name8", "Last Name8");
        entraUserRepository.save(entraUser);

        UserProfile userProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_SINGLE_FIRM);
        UserProfile userProfile2 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_SINGLE_FIRM);
        userProfile1.setFirm(firm1);
        userProfile2.setFirm(firm2);
        entraUser.getUserProfiles().add(userProfile1);
        entraUser.getUserProfiles().add(userProfile2);

        DataIntegrityViolationException diEx = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAllAndFlush(Arrays.asList(userProfile1, userProfile2)),
                "DataIntegrityViolationException expected");
        Assertions.assertThat(diEx.getCause()).isInstanceOf(ConstraintViolationException.class);
        Assertions.assertThat(diEx.getCause().getMessage()).contains("one_profile_per_non_multi_firm_user");

    }

    @Test
    public void testOneUserProfilePerFirm() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        firmRepository.save(firm1);

        EntraUser entraUser = buildEntraUser(generateEntraId(), "test9@email.com", "First Name9", "Last Name9");
        entraUserRepository.save(entraUser);

        UserProfile userProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_MULTI_FIRM);
        UserProfile userProfile2 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_MULTI_FIRM);
        userProfile1.setFirm(firm1);
        userProfile2.setFirm(firm1);
        entraUser.getUserProfiles().add(userProfile1);
        entraUser.getUserProfiles().add(userProfile2);

        DataIntegrityViolationException diEx = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAllAndFlush(Arrays.asList(userProfile1, userProfile2)),
                "DataIntegrityViolationException expected");
        Assertions.assertThat(diEx.getCause()).isInstanceOf(ConstraintViolationException.class);
        Assertions.assertThat(diEx.getCause().getMessage()).contains("one_profile_per_firm_for_multi_firm_user");

    }

    @Test
    public void testFirmNotNullForNonInternalUsers() {
        EntraUser entraUser = buildEntraUser(generateEntraId(), "test10@email.com", "First Name10", "Last Name10");
        entraUserRepository.save(entraUser);

        UserProfile userProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_SINGLE_FIRM);
        entraUser.getUserProfiles().add(userProfile1);

        DataIntegrityViolationException diEx = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(userProfile1),
                "DataIntegrityViolationException expected");
        Assertions.assertThat(diEx.getCause()).isInstanceOf(ConstraintViolationException.class);
        Assertions.assertThat(diEx.getCause().getMessage()).contains("firm_not_null_for_non_internal_users_only");

    }

    @Test
    public void testFirmNotNullForInternalUsers() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        firmRepository.save(firm1);

        EntraUser entraUser = buildEntraUser(generateEntraId(), "test11@email.com", "First Name11", "Last Name11");
        entraUserRepository.save(entraUser);

        UserProfile userProfile1 = buildLaaUserProfile(entraUser, UserType.INTERNAL);
        userProfile1.setFirm(firm1);
        entraUser.getUserProfiles().add(userProfile1);

        DataIntegrityViolationException diEx = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(userProfile1),
                "DataIntegrityViolationException expected");
        Assertions.assertThat(diEx.getCause()).isInstanceOf(ConstraintViolationException.class);
        Assertions.assertThat(diEx.getCause().getMessage()).contains("firm_not_null_for_non_internal_users_only");

    }

    @Test
    public void testFirmNullForInternalUsers() {

        EntraUser entraUser = buildEntraUser(generateEntraId(), "test12@email.com", "First Name12", "Last Name12");
        entraUserRepository.save(entraUser);

        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.INTERNAL);
        entraUser.getUserProfiles().add(userProfile);

        repository.saveAndFlush(userProfile);

        UserProfile result = repository.findById(userProfile.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(userProfile.getId());
        Assertions.assertThat(result.getFirm()).isNull();
        Assertions.assertThat(result.getEntraUser()).isNotNull();
        Assertions.assertThat(result.getEntraUser().getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getEntraUser().getFirstName()).isEqualTo("First Name12");
        Assertions.assertThat(result.getEntraUser().getLastName()).isEqualTo("Last Name12");

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
        Assertions.assertThat(result).containsExactly(UUID.fromString(entraUser.getEntraOid()));
    }

    @Test
    void shouldReturnNoUsers_whenSearchingByInternalUserType() {
        UUID expectedOid = UUID.randomUUID();
        Firm firm = buildFirm("Firm1", "Firm Code 1");
        firmRepository.save(firm);
        EntraUser entraUser = buildEntraUser(expectedOid.toString(), "test6@email.com", "First Name6", "Last Name6");
        entraUserRepository.save(entraUser);

        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL_MULTI_FIRM);
        userProfile.setFirm(firm);
        entraUser.getUserProfiles().add(userProfile);
        repository.saveAndFlush(userProfile);

        List<UUID> result = repository.findByUserTypes(UserType.INTERNAL);
        Assertions.assertThat(result).isEmpty();
    }
}
