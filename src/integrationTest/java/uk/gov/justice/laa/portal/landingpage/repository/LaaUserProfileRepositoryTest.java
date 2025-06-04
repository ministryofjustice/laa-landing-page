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
import uk.gov.justice.laa.portal.landingpage.entity.LaaUserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
public class LaaUserProfileRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraUserRepository entraUserRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private FirmRepository firmRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private LaaUserProfileRepository repository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
        entraUserRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveLaaUserProfile() {
        EntraUser entraUser = buildEntraUser("test@email.com", "First Name5", "Last Name5");
        entraUserRepository.saveAndFlush(entraUser);

        LaaUserProfile laaUserProfile = buildLaaUserProfile(entraUser, UserType.INTERNAL);
        repository.saveAndFlush(laaUserProfile);

        LaaUserProfile result = repository.findById(laaUserProfile.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(laaUserProfile.getId());
        Assertions.assertThat(result.getEntraUser()).isNotNull();
        Assertions.assertThat(result.getEntraUser().getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getEntraUser().getFirstName()).isEqualTo("First Name5");
        Assertions.assertThat(result.getEntraUser().getLastName()).isEqualTo("Last Name5");

    }

    @Test
    public void testSaveAndRetrieveMultipleLaaUserProfilesForEntraUser() {
        Firm firm1 = buildFirm("Firm1");
        Firm firm2 = buildFirm("Firm2");
        firmRepository.saveAll(Arrays.asList(firm1, firm2));

        EntraUser entraUser = buildEntraUser("test6@email.com", "First Name6", "Last Name6");
        entraUserRepository.save(entraUser);

        LaaUserProfile laaUserProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_MULTI_FIRM);
        LaaUserProfile laaUserProfile2 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_MULTI_FIRM);
        laaUserProfile1.setFirm(firm1);
        laaUserProfile2.setFirm(firm2);
        entraUser.getLaaUserProfiles().add(laaUserProfile1);
        entraUser.getLaaUserProfiles().add(laaUserProfile2);

        repository.saveAllAndFlush(Arrays.asList(laaUserProfile1, laaUserProfile2));

        LaaUserProfile result = repository.findById(laaUserProfile1.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(laaUserProfile1.getId());
        Assertions.assertThat(result.getEntraUser()).isNotNull();
        Assertions.assertThat(result.getEntraUser().getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getEntraUser().getFirstName()).isEqualTo("First Name6");
        Assertions.assertThat(result.getEntraUser().getLastName()).isEqualTo("Last Name6");
        Assertions.assertThat(result.getEntraUser().getLaaUserProfiles()).isNotEmpty();
        Assertions.assertThat(result.getEntraUser().getLaaUserProfiles()).containsExactlyInAnyOrder(laaUserProfile1, laaUserProfile2);

    }

    @Test
    public void testSaveAndRetrieveMultipleLaaUserProfilesOneDefaultProfile() {
        Firm firm1 = buildFirm("Firm1");
        Firm firm2 = buildFirm("Firm2");
        firmRepository.saveAll(Arrays.asList(firm1, firm2));

        EntraUser entraUser = buildEntraUser("test7@email.com", "First Name7", "Last Name7");
        entraUserRepository.save(entraUser);

        LaaUserProfile laaUserProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_MULTI_FIRM);
        LaaUserProfile laaUserProfile2 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_MULTI_FIRM);
        laaUserProfile1.setFirm(firm1);
        laaUserProfile2.setFirm(firm2);
        laaUserProfile1.setDefaultProfile(true);
        laaUserProfile2.setDefaultProfile(true);
        entraUser.getLaaUserProfiles().add(laaUserProfile1);
        entraUser.getLaaUserProfiles().add(laaUserProfile2);

        DataIntegrityViolationException diEx = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAllAndFlush(Arrays.asList(laaUserProfile1, laaUserProfile2)),
                "DataIntegrityViolationException expected");
        Assertions.assertThat(diEx.getCause()).isInstanceOf(ConstraintViolationException.class);
        Assertions.assertThat(diEx.getCause().getMessage()).contains("one_default_profile_per_user");
    }

    @Test
    public void testNoMultipleProfilesForNonMultiFirmUser() {
        Firm firm1 = buildFirm("Firm1");
        Firm firm2 = buildFirm("Firm2");
        firmRepository.saveAll(Arrays.asList(firm1, firm2));

        EntraUser entraUser = buildEntraUser("test8@email.com", "First Name8", "Last Name8");
        entraUserRepository.save(entraUser);

        LaaUserProfile laaUserProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_SINGLE_FIRM);
        LaaUserProfile laaUserProfile2 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_SINGLE_FIRM);
        laaUserProfile1.setFirm(firm1);
        laaUserProfile2.setFirm(firm2);
        entraUser.getLaaUserProfiles().add(laaUserProfile1);
        entraUser.getLaaUserProfiles().add(laaUserProfile2);

        DataIntegrityViolationException diEx = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAllAndFlush(Arrays.asList(laaUserProfile1, laaUserProfile2)),
                "DataIntegrityViolationException expected");
        Assertions.assertThat(diEx.getCause()).isInstanceOf(ConstraintViolationException.class);
        Assertions.assertThat(diEx.getCause().getMessage()).contains("one_profile_per_non_multi_firm_user");

    }

    @Test
    public void testOneUserProfilePerFirm() {
        Firm firm1 = buildFirm("Firm1");
        firmRepository.save(firm1);

        EntraUser entraUser = buildEntraUser("test9@email.com", "First Name9", "Last Name9");
        entraUserRepository.save(entraUser);

        LaaUserProfile laaUserProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_MULTI_FIRM);
        LaaUserProfile laaUserProfile2 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_MULTI_FIRM);
        laaUserProfile1.setFirm(firm1);
        laaUserProfile2.setFirm(firm1);
        entraUser.getLaaUserProfiles().add(laaUserProfile1);
        entraUser.getLaaUserProfiles().add(laaUserProfile2);

        DataIntegrityViolationException diEx = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAllAndFlush(Arrays.asList(laaUserProfile1, laaUserProfile2)),
                "DataIntegrityViolationException expected");
        Assertions.assertThat(diEx.getCause()).isInstanceOf(ConstraintViolationException.class);
        Assertions.assertThat(diEx.getCause().getMessage()).contains("one_profile_per_firm_for_multi_firm_user");

    }

    @Test
    public void testFirmNotNullForNonInternalUsers() {
        EntraUser entraUser = buildEntraUser("test10@email.com", "First Name10", "Last Name10");
        entraUserRepository.save(entraUser);

        LaaUserProfile laaUserProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_SINGLE_FIRM);
        entraUser.getLaaUserProfiles().add(laaUserProfile1);

        DataIntegrityViolationException diEx = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(laaUserProfile1),
                "DataIntegrityViolationException expected");
        Assertions.assertThat(diEx.getCause()).isInstanceOf(ConstraintViolationException.class);
        Assertions.assertThat(diEx.getCause().getMessage()).contains("firm_not_null_for_non_internal_users_only");

    }

    @Test
    public void testFirmNotNullForInternalUsers() {
        Firm firm1 = buildFirm("Firm1");
        firmRepository.save(firm1);

        EntraUser entraUser = buildEntraUser("test11@email.com", "First Name11", "Last Name11");
        entraUserRepository.save(entraUser);

        LaaUserProfile laaUserProfile1 = buildLaaUserProfile(entraUser, UserType.INTERNAL);
        laaUserProfile1.setFirm(firm1);
        entraUser.getLaaUserProfiles().add(laaUserProfile1);

        DataIntegrityViolationException diEx = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(laaUserProfile1),
                "DataIntegrityViolationException expected");
        Assertions.assertThat(diEx.getCause()).isInstanceOf(ConstraintViolationException.class);
        Assertions.assertThat(diEx.getCause().getMessage()).contains("firm_not_null_for_non_internal_users_only");

    }

    @Test
    public void testFirmNullForInternalUsers() {

        EntraUser entraUser = buildEntraUser("test12@email.com", "First Name12", "Last Name12");
        entraUserRepository.save(entraUser);

        LaaUserProfile laaUserProfile = buildLaaUserProfile(entraUser, UserType.INTERNAL);
        entraUser.getLaaUserProfiles().add(laaUserProfile);

        repository.saveAndFlush(laaUserProfile);

        LaaUserProfile result = repository.findById(laaUserProfile.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(laaUserProfile.getId());
        Assertions.assertThat(result.getFirm()).isNull();
        Assertions.assertThat(result.getEntraUser()).isNotNull();
        Assertions.assertThat(result.getEntraUser().getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getEntraUser().getFirstName()).isEqualTo("First Name12");
        Assertions.assertThat(result.getEntraUser().getLastName()).isEqualTo("Last Name12");

    }

}
