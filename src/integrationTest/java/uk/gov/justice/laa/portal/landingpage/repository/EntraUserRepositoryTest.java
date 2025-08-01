package uk.gov.justice.laa.portal.landingpage.repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.validation.ConstraintViolationException;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

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

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private OfficeRepository officeRepository;

    @BeforeEach
    public void beforeEach() {
        userProfileRepository.deleteAll();
        repository.deleteAll();
        officeRepository.deleteAll();
        firmRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveEntraUser() {
        String entraUserId = generateEntraId();
        EntraUser entraUser = buildEntraUser(entraUserId, "test@email.com", "FirstName", "LastName");
        repository.saveAndFlush(entraUser);

        EntraUser result = repository.findById(entraUser.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getEntraOid()).isEqualTo(entraUserId);
        Assertions.assertThat(result.getFirstName()).isEqualTo("FirstName");
        Assertions.assertThat(result.getLastName()).isEqualTo("LastName");
        Assertions.assertThat(result.getEmail()).isEqualTo("test@email.com");
        Assertions.assertThat(result.getCreatedBy()).isEqualTo("System");
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
    public void testSort() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        firm1 = firmRepository.save(firm1);
        Firm firm2 = buildFirm("Firm2", "Firm Code 2");
        firm2 = firmRepository.save(firm2);

        String entraUser1Id = generateEntraId();
        EntraUser entraUser1 = buildEntraUser(entraUser1Id, "aEmail@test.com", "bFirst", "cLast");
        entraUser1.setUserStatus(UserStatus.ACTIVE);
        entraUser1.setCreatedDate(LocalDateTime.now());
        repository.save(entraUser1);
        UserProfile userProfile11 = buildLaaUserProfile(entraUser1, UserType.EXTERNAL_MULTI_FIRM);
        userProfile11.setFirm(firm1);
        UserProfile userProfile12 = buildLaaUserProfile(entraUser1, UserType.EXTERNAL_MULTI_FIRM);
        userProfile12.setFirm(firm2);
        entraUser1.getUserProfiles().add(userProfile11);
        entraUser1.getUserProfiles().add(userProfile12);
        userProfileRepository.saveAllAndFlush(Arrays.asList(userProfile11, userProfile12));

        String entraUser2Id = generateEntraId();
        EntraUser entraUser2 = buildEntraUser(entraUser2Id, "bEmail@test.com", "aFirst", "bLast");
        entraUser2.setUserStatus(UserStatus.AWAITING_USER_APPROVAL);
        entraUser2.setCreatedDate(LocalDateTime.now().minusDays(1));
        repository.save(entraUser2);
        UserProfile userProfile21 = buildLaaUserProfile(entraUser2, UserType.EXTERNAL_SINGLE_FIRM);
        userProfile21.setFirm(firm1);
        entraUser2.getUserProfiles().add(userProfile21);
        userProfileRepository.saveAllAndFlush(Arrays.asList(userProfile21));

        String entraUser3Id = generateEntraId();
        EntraUser entraUser3 = buildEntraUser(entraUser3Id, "cEmail@test.com", "cFirst", "aLast");
        entraUser3.setUserStatus(UserStatus.ACTIVE);
        entraUser3.setCreatedDate(LocalDateTime.now().minusDays(1));
        repository.save(entraUser3);
        UserProfile userProfile31 = buildLaaUserProfile(entraUser3, UserType.INTERNAL);
        entraUser3.getUserProfiles().add(userProfile31);
        userProfileRepository.saveAllAndFlush(Arrays.asList(userProfile31));
        //default
        Pageable defaultOrder = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("userStatus"), Sort.Order.desc("createdDate")));
        Page<EntraUser> defaultPage = repository.findAll(defaultOrder);
        Assertions.assertThat(defaultPage.getNumberOfElements()).isEqualTo(3);
        //group by status
        Assertions.assertThat(defaultPage.getContent().get(0).getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        Assertions.assertThat(defaultPage.getContent().get(1).getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        Assertions.assertThat(defaultPage.getContent().get(2).getUserStatus()).isEqualTo(UserStatus.AWAITING_USER_APPROVAL);
        //order by created date desc
        Assertions.assertThat(defaultPage.getContent().get(0).getCreatedDate()).isAfter(defaultPage.getContent().get(1).getCreatedDate());
        //email
        Pageable emailOrder = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "email"));
        Page<EntraUser> emailPage = repository.findAll(emailOrder);
        Assertions.assertThat(emailPage.getContent().get(0).getEmail()).isEqualTo("aEmail@test.com");
        Assertions.assertThat(emailPage.getContent().get(1).getEmail()).isEqualTo("bEmail@test.com");
        Assertions.assertThat(emailPage.getContent().get(2).getEmail()).isEqualTo("cEmail@test.com");
        //first name
        Pageable firstNameOrder = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "firstName"));
        Page<EntraUser> firstNamePage = repository.findAll(firstNameOrder);
        Assertions.assertThat(firstNamePage.getContent().get(0).getFirstName()).isEqualTo("aFirst");
        Assertions.assertThat(firstNamePage.getContent().get(1).getFirstName()).isEqualTo("bFirst");
        Assertions.assertThat(firstNamePage.getContent().get(2).getFirstName()).isEqualTo("cFirst");
        //Last name
        Pageable lastNameOrder = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "lastName"));
        Page<EntraUser> lastNamePage = repository.findAll(lastNameOrder);
        Assertions.assertThat(lastNamePage.getContent().get(0).getLastName()).isEqualTo("aLast");
        Assertions.assertThat(lastNamePage.getContent().get(1).getLastName()).isEqualTo("bLast");
        Assertions.assertThat(lastNamePage.getContent().get(2).getLastName()).isEqualTo("cLast");
    }

}
