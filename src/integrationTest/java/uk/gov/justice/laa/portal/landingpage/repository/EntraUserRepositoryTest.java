package uk.gov.justice.laa.portal.landingpage.repository;

import jakarta.validation.ConstraintViolationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
public class EntraUserRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraUserRepository repository;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private FirmRepository firmRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
        firmRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveEntraUser() {
        String entraUserId = generateEntraId();
        EntraUser entraUser = buildEntraUser(entraUserId, "test@email.com", "FirstName", "LastName");
        repository.saveAndFlush(entraUser);

        EntraUser result = repository.findById(entraUser.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getEntraUserId()).isEqualTo(entraUserId);
        Assertions.assertThat(result.getFirstName()).isEqualTo("FirstName");
        Assertions.assertThat(result.getLastName()).isEqualTo("LastName");
        Assertions.assertThat(result.getEmail()).isEqualTo("test@email.com");
        Assertions.assertThat(result.getCreatedBy()).isEqualTo("Test");
        Assertions.assertThat(result.getCreatedDate()).isNotNull();

    }

    @Test
    public void testSaveEntraUserWithInvalidEmail() {
        EntraUser entraUser = buildEntraUser("test", "testemail.com", "FirstName", "LastName");

        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> repository.saveAndFlush(entraUser), "Exception expected");
        Assertions.assertThat(ex.getMessage()).contains("User email must be a valid email address");
    }

    @Test
    public void testSaveEntraUserStartDateAfterEndDate() {
        EntraUser entraUser = buildEntraUser("test", "test@email.com", "FirstName", "LastName");
        entraUser.setStartDate(LocalDateTime.now());
        entraUser.setEndDate(LocalDateTime.now().minusMinutes(1));

        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> repository.saveAndFlush(entraUser), "Exception expected");
        Assertions.assertThat(ex.getMessage()).contains("End date must be after start date");
    }

    @Test
    public void testGetPageUsersByTypeFirms() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        firm1 = firmRepository.save(firm1);
        Firm firm2 = buildFirm("Firm2", "Firm Code 2");
        firm2 = firmRepository.save(firm2);

        String entraUser1Id = generateEntraId();
        EntraUser entraUser1 = buildEntraUser(entraUser1Id, "test1@email.com", "First Name1", "Last Name1");
        repository.save(entraUser1);
        UserProfile userProfile1_1 = buildLaaUserProfile(entraUser1, UserType.EXTERNAL_MULTI_FIRM);
        userProfile1_1.setFirm(firm1);
        UserProfile userProfile1_2 = buildLaaUserProfile(entraUser1, UserType.EXTERNAL_MULTI_FIRM);
        userProfile1_2.setFirm(firm2);
        entraUser1.getUserProfiles().add(userProfile1_1);
        entraUser1.getUserProfiles().add(userProfile1_2);
        userProfileRepository.saveAllAndFlush(Arrays.asList(userProfile1_1, userProfile1_2));

        String entraUser2Id = generateEntraId();
        EntraUser entraUser2 = buildEntraUser(entraUser2Id, "test2@email.com", "First Name2", "Last Name2");
        repository.save(entraUser2);
        UserProfile userProfile2_1 = buildLaaUserProfile(entraUser2, UserType.EXTERNAL_SINGLE_FIRM);
        userProfile2_1.setFirm(firm1);
        entraUser2.getUserProfiles().add(userProfile2_1);
        userProfileRepository.saveAllAndFlush(Arrays.asList(userProfile2_1));

        String entraUser3Id = generateEntraId();
        EntraUser entraUser3 = buildEntraUser(entraUser3Id, "test3@email.com", "First Name3", "Last Name3");
        repository.save(entraUser3);
        UserProfile userProfile3_1 = buildLaaUserProfile(entraUser3, UserType.INTERNAL);
        entraUser3.getUserProfiles().add(userProfile3_1);
        userProfileRepository.saveAllAndFlush(Arrays.asList(userProfile3_1));
        Pageable page1 = PageRequest.of(0, 10);
        Page<EntraUser> internalUsers = repository.findByUserTypes(UserType.INTERNAL_TYPES, page1);
        Assertions.assertThat(internalUsers).hasSize(1);

        Page<EntraUser> externalUsers = repository.findByUserTypes(UserType.EXTERNAL_TYPES, page1);
        Assertions.assertThat(externalUsers).hasSize(2);

        Page<EntraUser> firm1Users = repository.findByUserTypesAndFirms(UserType.EXTERNAL_TYPES, List.of(firm1.getId()), page1);
        Assertions.assertThat(firm1Users).hasSize(2);

        Page<EntraUser> firm1User = repository.findByNameEmailAndUserTypesFirms("TeSt1", "TeSt1", "TeSt1", UserType.EXTERNAL_TYPES, List.of(firm1.getId()), page1);
        Assertions.assertThat(firm1User).hasSize(1);

        Page<EntraUser> firm1Result = repository.findByNameEmailAndUserTypesFirms("l.cOm", "l.cOm", "l.cOm", UserType.EXTERNAL_TYPES, List.of(firm1.getId()), page1);
        Assertions.assertThat(firm1Result).hasSize(2);
    }


}
