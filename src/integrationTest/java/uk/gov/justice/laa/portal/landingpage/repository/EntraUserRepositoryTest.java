package uk.gov.justice.laa.portal.landingpage.repository;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.validation.ConstraintViolationException;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.InvitationStatus;
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

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRepository appRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRoleRepository appRoleRepositoryRepository;

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
        Assertions.assertThat(result.getCreatedDate()).isNotNull();

        Optional<EntraUser> ignoreCaseResult1 = repository.findByEmailIgnoreCase("test@email.com");
        Optional<EntraUser> ignoreCaseResult2 = repository.findByEmailIgnoreCase("Test@emaiL.com");
        Assertions.assertThat(ignoreCaseResult1).isPresent();
        Assertions.assertThat(ignoreCaseResult2).isPresent();
        Assertions.assertThat(ignoreCaseResult1.get().getEntraOid()).isEqualTo(ignoreCaseResult2.get().getEntraOid());
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
        UserProfile userProfile11 = buildLaaUserProfile(entraUser1, UserType.EXTERNAL);
        userProfile11.setFirm(firm1);
        UserProfile userProfile12 = buildLaaUserProfile(entraUser1, UserType.EXTERNAL);
        userProfile12.setFirm(firm2);
        entraUser1.getUserProfiles().add(userProfile11);
        entraUser1.getUserProfiles().add(userProfile12);
        userProfileRepository.saveAllAndFlush(Arrays.asList(userProfile11, userProfile12));

        String entraUser2Id = generateEntraId();
        EntraUser entraUser2 = buildEntraUser(entraUser2Id, "bEmail@test.com", "aFirst", "bLast");
        entraUser2.setUserStatus(UserStatus.AWAITING_USER_APPROVAL);
        entraUser2.setCreatedDate(LocalDateTime.now().minusDays(1));
        repository.save(entraUser2);
        UserProfile userProfile21 = buildLaaUserProfile(entraUser2, UserType.EXTERNAL);
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

    @Test
    public void testFindUnlinkedMultifirmUserCount() {
        EntraUser user1 = buildMultifirmEntraUser(generateEntraId(), "user1@example.com", "User", "One", true);
        user1 = repository.saveAndFlush(user1);
        EntraUser user2 = buildMultifirmEntraUser(generateEntraId(), "user2@example.com", "User", "Two", true);
        user2 = repository.saveAndFlush(user2);
        EntraUser user3 = buildMultifirmEntraUser(generateEntraId(), "user3@example.com", "User", "Three", true);
        user3 = repository.saveAndFlush(user3);
        EntraUser user4 = buildMultifirmEntraUser(generateEntraId(), "user4@example.com", "User", "Four", false);
        user4 = repository.saveAndFlush(user4);

        Firm firm1 = buildFirm("Firm Epsilon", "EPSILON");
        firmRepository.saveAllAndFlush(List.of(firm1));

        UserProfile user1P1 = buildLaaUserProfile(user1, UserType.EXTERNAL);
        user1P1.setFirm(firm1);
        userProfileRepository.saveAllAndFlush(List.of(user1P1));


        List<Object[]> result = repository.findUnlinkedMultifirmUsersCount();

        assertThat(result).hasSize(1);
        assertThat(result.get(0))
                .containsExactly("Unlinked multi-firm users", null, 2L);
    }

    @Test
    public void testFindTotalMultiFirmUsersCount() {

        EntraUser user1 = buildMultifirmEntraUser(generateEntraId(), "user1@example.com", "User", "One", true);
        user1 = repository.saveAndFlush(user1);
        EntraUser user2 = buildMultifirmEntraUser(generateEntraId(), "user2@example.com", "User", "Two", true);
        user2 = repository.saveAndFlush(user2);
        EntraUser user3 = buildMultifirmEntraUser(generateEntraId(), "user3@example.com", "User", "Three", true);
        user3 = repository.saveAndFlush(user3);
        EntraUser user4 = buildMultifirmEntraUser(generateEntraId(), "user4@example.com", "User", "Four", false);
        user4 = repository.saveAndFlush(user4);

        List<Object[]> result = repository.findTotalMultiFirmUsersCount();

        assertThat(result).hasSize(1);
        assertThat(result.get(0))
                .containsExactly("Total multi-firm users", null, 3L);

    }

    @Test
    public void testCountInternalUsers() {
        EntraUser internal = buildEntraUser(generateEntraId(),
                "internal@example.com", "Internal", "User");
        repository.saveAndFlush(internal);

        EntraUser external = buildEntraUser(generateEntraId(),
                "external@example.com", "External", "User");
        repository.saveAndFlush(external);

        Firm firm = buildFirm("Test Firm", "TEST");
        firmRepository.saveAndFlush(firm);

        UserProfile internalProfile =
                buildLaaUserProfile(internal, UserType.INTERNAL);

        UserProfile externalProfile =
                buildLaaUserProfile(external, UserType.EXTERNAL);
        externalProfile.setFirm(firm);

        userProfileRepository.saveAllAndFlush(
                List.of(internalProfile, externalProfile)
        );

        long result = repository.countInternalUsers();

        assertThat(result).isEqualTo(1L);
    }

    @Test
    public void testCountTotalExternalUsers() {
        EntraUser user1 = buildEntraUser(generateEntraId(), "u1@x.com", "U1", "User");
        EntraUser user2 = buildEntraUser(generateEntraId(), "u2@x.com", "U2", "User");
        repository.saveAllAndFlush(List.of(user1, user2));

        Firm firm = buildFirm("Test Firm", "TEST");
        firmRepository.saveAndFlush(firm);

        UserProfile profile1 = buildLaaUserProfile(user1, UserType.EXTERNAL);
        profile1.setFirm(firm);

        UserProfile profile2 = buildLaaUserProfile(user2, UserType.EXTERNAL);
        profile2.setFirm(firm);

        userProfileRepository.saveAllAndFlush(List.of(profile1, profile2));

        long result = repository.countTotalExternalUsers();

        assertThat(result).isEqualTo(2L);
    }

    @Test
    public void testCountTotalExternalMultiFirmUsers() {
        EntraUser single = buildEntraUser(generateEntraId(), "s@x.com", "Single", "Firm");

        EntraUser multi = buildEntraUser(generateEntraId(), "m@x.com", "Multi", "Firm");
        multi.setMultiFirmUser(true);

        repository.saveAllAndFlush(List.of(single, multi));

        Firm firm = buildFirm("Test Firm", "TEST");
        firmRepository.saveAndFlush(firm);

        UserProfile profile1 = buildLaaUserProfile(single, UserType.EXTERNAL);
        profile1.setFirm(firm);

        UserProfile profile2 = buildLaaUserProfile(multi, UserType.EXTERNAL);
        profile2.setFirm(firm);

        userProfileRepository.saveAllAndFlush(List.of(profile1, profile2));

        long result = repository.countTotalExternalMultiFirmUsers();

        assertThat(result).isEqualTo(1L);
    }

    @Test
    public void testCountActiveExternalUsers() {
        EntraUser active = buildEntraUser(generateEntraId(), "a@x.com", "Active", "User");
        active.setEnabled(true);
        active.setInvitationStatus(InvitationStatus.VERIFICATION_SUCCESS);

        EntraUser inactive = buildEntraUser(generateEntraId(), "i@x.com", "Inactive", "User");
        inactive.setEnabled(false);
        inactive.setInvitationStatus(InvitationStatus.VERIFICATION_SUCCESS);

        repository.saveAllAndFlush(List.of(active, inactive));

        Firm firm = buildFirm("Test Firm", "TEST");
        firmRepository.saveAndFlush(firm);

        UserProfile activeProfile = buildLaaUserProfile(active, UserType.EXTERNAL);
        activeProfile.setFirm(firm);

        UserProfile inactiveProfile = buildLaaUserProfile(inactive, UserType.EXTERNAL);
        inactiveProfile.setFirm(firm);

        userProfileRepository.saveAllAndFlush(
                List.of(activeProfile, inactiveProfile)
        );

        long result = repository.countActiveExternalUsers();

        assertThat(result).isEqualTo(1L);
    }

    @Test
    public void testCountDisabledExternalUsers() {
        EntraUser disabled = buildEntraUser(generateEntraId(), "d@x.com", "Disabled", "User");
        disabled.setEnabled(false);
        disabled.setInvitationStatus(InvitationStatus.VERIFICATION_SUCCESS);

        EntraUser enabled = buildEntraUser(generateEntraId(), "e@x.com", "Enabled", "User");
        enabled.setEnabled(true);
        enabled.setInvitationStatus(InvitationStatus.VERIFICATION_SUCCESS);

        repository.saveAllAndFlush(List.of(disabled, enabled));

        Firm firm = buildFirm("Test Firm", "TEST");
        firmRepository.saveAndFlush(firm);

        UserProfile disabledProfile = buildLaaUserProfile(disabled, UserType.EXTERNAL);
        disabledProfile.setFirm(firm);

        UserProfile enabledProfile = buildLaaUserProfile(enabled, UserType.EXTERNAL);
        enabledProfile.setFirm(firm);

        userProfileRepository.saveAllAndFlush(
                List.of(disabledProfile, enabledProfile)
        );

        long result = repository.countDisabledExternalUsers();

        assertThat(result).isEqualTo(1L);
    }

    @Test
    public void testCountCompleteExternalUsers() {
        EntraUser user = buildEntraUser(generateEntraId(),
                "c@x.com", "Complete", "User");
        user.setEnabled(true);
        user.setInvitationStatus(InvitationStatus.VERIFICATION_SUCCESS);
        repository.saveAndFlush(user);

        Firm firm = buildFirm("Test Firm", "TEST");
        firmRepository.saveAndFlush(firm);

        App app = buildLaaApp("TEST APP", "123", "456");
        appRepository.saveAndFlush(app);
        AppRole appRole = buildLaaAppRole(app, "ROLE_USER");
        appRoleRepository.saveAndFlush(appRole);

        UserProfile profile = buildLaaUserProfile(user, UserType.EXTERNAL);
        profile.setFirm(firm);
        profile.getAppRoles().add(appRole);

        userProfileRepository.saveAndFlush(profile);

        long result = repository.countCompleteExternalUsers();

        assertThat(result).isEqualTo(1L);
    }

    @Test
    public void testCountExternalUsersWithNoRoles() {
        EntraUser user = buildEntraUser(generateEntraId(),
                "n@x.com", "No", "Roles");
        user.setEnabled(true);
        user.setInvitationStatus(InvitationStatus.VERIFICATION_SUCCESS);
        repository.saveAndFlush(user);

        Firm firm = buildFirm("Test Firm", "TEST");
        firmRepository.saveAndFlush(firm);

        UserProfile profile = buildLaaUserProfileWithoutRoles(user, UserType.EXTERNAL);
        profile.setFirm(firm); // no roles added

        userProfileRepository.saveAndFlush(profile);

        long result = repository.countExternalUsersWithNoRoles();

        assertThat(result).isEqualTo(1L);
    }

    @Test
    public void testMixedProfilesCountsAsMissingRoles() {
        EntraUser user = buildEntraUser(generateEntraId(),
                "m@x.com", "Mixed", "User");
        user.setEnabled(true);
        user.setInvitationStatus(InvitationStatus.VERIFICATION_SUCCESS);
        repository.saveAndFlush(user);

        Firm firmWithRole = buildFirm("Firm One", "F1");
        Firm firmWithoutRole = buildFirm("Firm Two", "F2");
        firmRepository.saveAllAndFlush(List.of(firmWithRole, firmWithoutRole));

        App app = buildLaaApp("TEST APP", "123", "456");
        appRepository.saveAndFlush(app);

        AppRole appRole = buildLaaAppRole(app, "ROLE_USER");
        appRoleRepository.saveAndFlush(appRole);

        UserProfile withRole =
                buildLaaUserProfile(user, UserType.EXTERNAL);
        withRole.setFirm(firmWithRole);
        withRole.getAppRoles().add(appRole);

        UserProfile withoutRole =
                buildLaaUserProfileWithoutRoles(user, UserType.EXTERNAL);
        withoutRole.setFirm(firmWithoutRole);

        userProfileRepository.saveAllAndFlush(
                List.of(withRole, withoutRole)
        );

        assertThat(repository.countExternalUsersWithNoRoles()).isEqualTo(1L);
        assertThat(repository.countCompleteExternalUsers()).isEqualTo(0L);
    }

    @Test
    public void testCountIncompleteExternalUsers() {
        EntraUser user = buildEntraUser(generateEntraId(),
                "i@x.com", "Incomplete", "User");
        user.setInvitationStatus(InvitationStatus.INVITE_SENT);
        repository.saveAndFlush(user);

        Firm firm = buildFirm("Test Firm", "TEST");
        firmRepository.saveAndFlush(firm);

        UserProfile profile = buildLaaUserProfileWithoutRoles(user, UserType.EXTERNAL);
        profile.setFirm(firm);

        userProfileRepository.saveAndFlush(profile);

        long result = repository.countIncompleteExternalUsers();

        assertThat(result).isEqualTo(1L);
    }

    @Test
    public void testCountActivationPendingExternalUsers() {
        EntraUser user = buildEntraUser(generateEntraId(),
                "p@x.com", "Pending", "User");
        user.setInvitationStatus(InvitationStatus.INVITE_SENT);
        repository.saveAndFlush(user);

        Firm firm = buildFirm("Test Firm", "TEST");
        firmRepository.saveAndFlush(firm);

        App app = buildLaaApp("TEST APP", "123", "456");
        appRepository.saveAndFlush(app);
        AppRole appRole = buildLaaAppRole(app, "ROLE_USER");
        appRoleRepository.saveAndFlush(appRole);

        UserProfile profile = buildLaaUserProfile(user, UserType.EXTERNAL);
        profile.setFirm(firm);
        profile.getAppRoles().add(appRole);

        userProfileRepository.saveAndFlush(profile);

        long result = repository.countActivationPendingExternalUsers();

        assertThat(result).isEqualTo(1L);
    }

}
