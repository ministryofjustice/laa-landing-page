package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

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

    @BeforeEach
    public void beforeEach() {
        // Delete offices first to avoid foreign key constraint violations
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
    public void testFindExternalUserCountsByFirm() {

        // Create three users
        EntraUser user1 = buildMultifirmEntraUser(generateEntraId(), "user1@example.com", "User", "One", true);
        user1 = entraUserRepository.saveAndFlush(user1);
        EntraUser user2 = buildMultifirmEntraUser(generateEntraId(), "user2@example.com", "User", "Two", true);
        user2 = entraUserRepository.saveAndFlush(user2);
        EntraUser user3 = buildDeactiveEntraUser(generateEntraId(), "user3@example.com", "User", "Three", false);
        user3 = entraUserRepository.saveAndFlush(user3);

        // Create firms
        Firm firm1 = buildFirm("Firm Epsilon", "EPSILON");
        Firm firm2 = buildFirm("Firm Zeta", "ZETA");
        Firm firm3 = buildFirm("Firm Eta", "ETA");
        repository.saveAllAndFlush(List.of(firm1, firm2, firm3));


        UserProfile user1P1 = buildFirmUserManagerProfile(user1, UserType.EXTERNAL, true);
        user1P1.setFirm(firm1);
        UserProfile user1P2 = buildFirmUserManagerProfile(user1, UserType.EXTERNAL, false);
        user1P2.setFirm(firm2);
        UserProfile user1P3 = buildFirmUserManagerProfile(user1, UserType.EXTERNAL, false);
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

        List<Object[]> result = repository.findAllFirmExternalUserCount();

        assertThat(result)
                .hasSize(3)
                .containsExactlyInAnyOrder(
                        new Object[]{"Firm Epsilon", "EPSILON", "ADVOCATE", null, 1L, 1L, 1L, 0L},
                        new Object[]{"Firm Zeta", "ZETA", "ADVOCATE", null, 2L, 1L, 1L, 1L},
                        new Object[]{"Firm Eta", "ETA", "ADVOCATE", null, 2L, 1L, 2L, 0L});
    }

}
