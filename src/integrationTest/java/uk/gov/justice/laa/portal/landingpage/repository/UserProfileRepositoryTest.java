package uk.gov.justice.laa.portal.landingpage.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import jakarta.transaction.Transactional;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;

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
    public void testMultiFirmExternalUsersRequireFirm() {
        // Test that multi-firm users only have entra_user entry, no user_profile
        EntraUser entraUser = buildEntraUser(generateEntraId(), "test10@email.com", "First Name10", "Last Name10");
        entraUser.setMultiFirmUser(true); // Mark as multi-firm user

        // Multi-firm users don't have any user profiles
        EntraUser saved = entraUserRepository.saveAndFlush(entraUser);

        assertThat(saved.isMultiFirmUser()).isTrue();
        assertThat(saved.getUserProfiles()).isEmpty(); // No profiles for multi-firm users
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
    public void testFindBySearchParams_SelectedParentFirmReturnsParentAndChildren() {
        Firm parent = buildFirm("Parent Firm", "PARENT");
        Firm child = buildFirm("Child Firm", "CHILD");
        child.setParentFirm(parent);
        parent.setChildFirms(Set.of(child));
        firmRepository.saveAllAndFlush(List.of(parent, child));

        EntraUser parentUser = buildEntraUser(generateEntraId(), "parent.user@example.com", "Parent", "User");
        entraUserRepository.saveAndFlush(parentUser);
        UserProfile parentProfile = buildLaaUserProfile(parentUser, UserType.EXTERNAL);
        parentProfile.setFirm(parent);
        parentUser.getUserProfiles().add(parentProfile);

        EntraUser childUser = buildEntraUser(generateEntraId(), "child.user@example.com", "Child", "User");
        entraUserRepository.saveAndFlush(childUser);
        UserProfile childProfile = buildLaaUserProfile(childUser, UserType.EXTERNAL);
        childProfile.setFirm(child);
        childUser.getUserProfiles().add(childProfile);

        repository.saveAllAndFlush(List.of(parentProfile, childProfile));

        FirmSearchForm firmSearch = FirmSearchForm.builder().selectedFirmId(parent.getId()).build();
        UserSearchCriteria criteria = new UserSearchCriteria("", firmSearch, UserType.EXTERNAL, false, false);

        Page<UserProfile> page = repository.findBySearchParams(criteria, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(UserProfile::getId)
                .containsExactlyInAnyOrder(parentProfile.getId(), childProfile.getId());
    }

    @Test
    public void testFindBySearchParams_SelectedChildFirmReturnsOnlyChild() {
        Firm parent = buildFirm("Parent F2", "P2");
        Firm child = buildFirm("Child F2", "C2");
        child.setParentFirm(parent);
        parent.setChildFirms(Set.of(child));
        firmRepository.saveAllAndFlush(List.of(parent, child));

        EntraUser parentUser = buildEntraUser(generateEntraId(), "pu2@example.com", "P2", "U");
        entraUserRepository.saveAndFlush(parentUser);
        UserProfile parentProfile = buildLaaUserProfile(parentUser, UserType.EXTERNAL);
        parentProfile.setFirm(parent);
        parentUser.getUserProfiles().add(parentProfile);

        EntraUser childUser = buildEntraUser(generateEntraId(), "cu2@example.com", "C2", "U");
        entraUserRepository.saveAndFlush(childUser);
        UserProfile childProfile = buildLaaUserProfile(childUser, UserType.EXTERNAL);
        childProfile.setFirm(child);
        childUser.getUserProfiles().add(childProfile);

        repository.saveAllAndFlush(List.of(parentProfile, childProfile));

        FirmSearchForm firmSearch = FirmSearchForm.builder().selectedFirmId(child.getId()).build();
        UserSearchCriteria criteria = new UserSearchCriteria("", firmSearch, UserType.EXTERNAL, false, false);

        Page<UserProfile> page = repository.findBySearchParams(criteria, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(UserProfile::getId)
                .containsExactlyInAnyOrder(childProfile.getId());
    }

    @Test
    public void testFindBySearchParams_SelectedParentFirmWithMultipleChildrenReturnsAllWithCorrectCountAndPagination() {
        Firm parent = buildFirm("Parent F3", "P3");
        Firm child1 = buildFirm("Child F3A", "C3A");
        Firm child2 = buildFirm("Child F3B", "C3B");
        child1.setParentFirm(parent);
        child2.setParentFirm(parent);
        parent.setChildFirms(Set.of(child1, child2));
        firmRepository.saveAllAndFlush(List.of(parent, child1, child2));

        EntraUser parentUser = buildEntraUser(generateEntraId(), "p3@example.com", "P3", "User");
        entraUserRepository.saveAndFlush(parentUser);
        UserProfile parentProfile = buildLaaUserProfile(parentUser, UserType.EXTERNAL);
        parentProfile.setFirm(parent);
        parentUser.getUserProfiles().add(parentProfile);

        EntraUser childUser1 = buildEntraUser(generateEntraId(), "c3a@example.com", "C3A", "User");
        entraUserRepository.saveAndFlush(childUser1);
        UserProfile childProfile1 = buildLaaUserProfile(childUser1, UserType.EXTERNAL);
        childProfile1.setFirm(child1);
        childUser1.getUserProfiles().add(childProfile1);

        EntraUser childUser2 = buildEntraUser(generateEntraId(), "c3b@example.com", "C3B", "User");
        entraUserRepository.saveAndFlush(childUser2);
        UserProfile childProfile2 = buildLaaUserProfile(childUser2, UserType.EXTERNAL);
        childProfile2.setFirm(child2);
        childUser2.getUserProfiles().add(childProfile2);

        repository.saveAllAndFlush(List.of(parentProfile, childProfile1, childProfile2));

        FirmSearchForm firmSearch = FirmSearchForm.builder().selectedFirmId(parent.getId()).build();
        UserSearchCriteria criteria = new UserSearchCriteria("", firmSearch, UserType.EXTERNAL, false, false);

        Page<UserProfile> page0 = repository.findBySearchParams(criteria, PageRequest.of(0, 2));
        assertThat(page0.getTotalElements()).isEqualTo(3);
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page0.getContent().size()).isEqualTo(2);
        assertThat(page0.getContent()).extracting(UserProfile::getId)
                .containsAnyOf(parentProfile.getId(), childProfile1.getId(), childProfile2.getId());

        Page<UserProfile> page1 = repository.findBySearchParams(criteria, PageRequest.of(1, 2));
        assertThat(page1.getTotalElements()).isEqualTo(3);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.getContent().size()).isEqualTo(1);
        assertThat(page1.getContent()).extracting(UserProfile::getId)
                .containsAnyOf(parentProfile.getId(), childProfile1.getId(), childProfile2.getId());
    }

    @Test
    public void testFindBySearchParams_SelectedParentFirmWithSearchTermFiltersWithinHierarchy() {
        Firm parent = buildFirm("Parent F4", "P4");
        Firm child1 = buildFirm("Child F4A", "C4A");
        Firm child2 = buildFirm("Child F4B", "C4B");
        child1.setParentFirm(parent);
        child2.setParentFirm(parent);
        parent.setChildFirms(Set.of(child1, child2));
        firmRepository.saveAllAndFlush(List.of(parent, child1, child2));

        EntraUser parentUser = buildEntraUser(generateEntraId(), "parent4@example.com", "Pat", "Rent");
        entraUserRepository.saveAndFlush(parentUser);
        UserProfile parentProfile = buildLaaUserProfile(parentUser, UserType.EXTERNAL);
        parentProfile.setFirm(parent);
        parentUser.getUserProfiles().add(parentProfile);

        EntraUser childUser1 = buildEntraUser(generateEntraId(), "alice.child@example.com", "Alice", "Child");
        entraUserRepository.saveAndFlush(childUser1);
        UserProfile childProfile1 = buildLaaUserProfile(childUser1, UserType.EXTERNAL);
        childProfile1.setFirm(child1);
        childUser1.getUserProfiles().add(childProfile1);

        EntraUser childUser2 = buildEntraUser(generateEntraId(), "charlie.child@example.com", "Charlie", "Child");
        entraUserRepository.saveAndFlush(childUser2);
        UserProfile childProfile2 = buildLaaUserProfile(childUser2, UserType.EXTERNAL);
        childProfile2.setFirm(child2);
        childUser2.getUserProfiles().add(childProfile2);

        repository.saveAllAndFlush(List.of(parentProfile, childProfile1, childProfile2));

        FirmSearchForm firmSearch = FirmSearchForm.builder().selectedFirmId(parent.getId()).build();
        UserSearchCriteria criteria = new UserSearchCriteria("charlie", firmSearch, UserType.EXTERNAL, false, false);

        Page<UserProfile> page = repository.findBySearchParams(criteria, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(UserProfile::getId)
                .containsExactly(childProfile2.getId());
    }

    @Test
    @Transactional
    public void testInternalUserCanOnlyHaveOneProfile() {
        // Setup internal user
        EntraUser internalUser = buildEntraUser(UUID.randomUUID().toString(), "internaluser@test.com", "Internal",
                "User");

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
        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(additionalProfile));
        assertThat(exception.getCause().getMessage()).contains("one_profile_per_internal_user");
    }

    @Test
    public void testFindAllByEntraUserReturnsAssociatedProfiles() {
        EntraUser entraUser = buildEntraUser(generateEntraId(), "user-findall@example.com", "Find", "All");
        entraUser = entraUserRepository.saveAndFlush(entraUser);

        Firm firm1 = buildFirm("Firm One", "FIRM1");
        Firm firm2 = buildFirm("Firm Two", "FIRM2");
        firmRepository.saveAllAndFlush(List.of(firm1, firm2));

        UserProfile profile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        profile1.setFirm(firm1);
        UserProfile profile2 = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        profile2.setFirm(firm2);

        entraUser.getUserProfiles().add(profile1);
        entraUser.getUserProfiles().add(profile2);
        repository.saveAllAndFlush(List.of(profile1, profile2));

        List<UserProfile> profiles = repository.findAllByEntraUser(entraUser);
        UUID expectedId = entraUser.getId();
        assertThat(profiles).isNotNull();
        assertThat(profiles).hasSize(2);
        assertThat(profiles).extracting(UserProfile::getId)
                .containsExactlyInAnyOrder(profile1.getId(), profile2.getId());
        assertThat(profiles).allMatch(p -> p.getEntraUser().getId().equals(expectedId));
    }

    @Test
    public void testFindAllByEntraUserReturnsEmptyWhenNoProfiles() {
        EntraUser entraUserNoProfiles = buildEntraUser(generateEntraId(), "noprof@example.com", "No", "Profiles");
        entraUserNoProfiles = entraUserRepository.saveAndFlush(entraUserNoProfiles);

        EntraUser otherUser = buildEntraUser(generateEntraId(), "other@example.com", "Other", "User");
        otherUser = entraUserRepository.saveAndFlush(otherUser);
        Firm firm = buildFirm("Firm X", "FIRMX");
        firmRepository.saveAndFlush(firm);
        UserProfile otherProfile = buildLaaUserProfile(otherUser, UserType.EXTERNAL);
        otherProfile.setFirm(firm);
        otherUser.getUserProfiles().add(otherProfile);
        repository.saveAndFlush(otherProfile);

        List<UserProfile> result = repository.findAllByEntraUser(entraUserNoProfiles);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    public void testCountByEntraUserId_withMultipleProfiles_returnsCorrectCount() {
        // Setup: Create user with multiple profiles
        EntraUser entraUser = buildEntraUser(generateEntraId(), "multiprofile@example.com", "Multi", "Profile");
        entraUser = entraUserRepository.saveAndFlush(entraUser);

        Firm firm1 = buildFirm("Firm Alpha", "ALPHA");
        Firm firm2 = buildFirm("Firm Beta", "BETA");
        Firm firm3 = buildFirm("Firm Gamma", "GAMMA");
        firmRepository.saveAllAndFlush(List.of(firm1, firm2, firm3));

        UserProfile profile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        profile1.setFirm(firm1);
        UserProfile profile2 = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        profile2.setFirm(firm2);
        UserProfile profile3 = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        profile3.setFirm(firm3);

        entraUser.getUserProfiles().add(profile1);
        entraUser.getUserProfiles().add(profile2);
        entraUser.getUserProfiles().add(profile3);
        repository.saveAllAndFlush(List.of(profile1, profile2, profile3));

        // Execute: Count profiles
        long count = repository.countByEntraUserId(entraUser.getId());

        // Assert: Should return 3
        assertThat(count).isEqualTo(3L);
    }

    @Test
    public void testCountByEntraUserId_withSingleProfile_returnsOne() {
        // Setup: Create user with single profile
        EntraUser entraUser = buildEntraUser(generateEntraId(), "single@example.com", "Single", "Profile");
        entraUser = entraUserRepository.saveAndFlush(entraUser);

        Firm firm = buildFirm("Firm Delta", "DELTA");
        firmRepository.saveAndFlush(firm);

        UserProfile profile = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
        profile.setFirm(firm);
        entraUser.getUserProfiles().add(profile);
        repository.saveAndFlush(profile);

        // Execute: Count profiles
        long count = repository.countByEntraUserId(entraUser.getId());

        // Assert: Should return 1
        assertThat(count).isEqualTo(1L);
    }

    @Test
    public void testCountByEntraUserId_withNoProfiles_returnsZero() {
        // Setup: Create user without any profiles
        EntraUser entraUser = buildEntraUser(generateEntraId(), "noprofiles@example.com", "No", "Profiles");
        entraUser = entraUserRepository.saveAndFlush(entraUser);

        // Execute: Count profiles
        long count = repository.countByEntraUserId(entraUser.getId());

        // Assert: Should return 0
        assertThat(count).isEqualTo(0L);
    }

    @Test
    public void testCountByEntraUserId_withNonExistentUser_returnsZero() {
        // Setup: Use a random UUID that doesn't exist
        UUID nonExistentUserId = UUID.randomUUID();

        // Execute: Count profiles for non-existent user
        long count = repository.countByEntraUserId(nonExistentUserId);

        // Assert: Should return 0
        assertThat(count).isEqualTo(0L);
    }

    @Test
    public void testCountByEntraUserId_onlyCountsProfilesForSpecificUser() {
        // Setup: Create two users with different numbers of profiles
        EntraUser user1 = buildEntraUser(generateEntraId(), "user1@example.com", "User", "One");
        user1 = entraUserRepository.saveAndFlush(user1);

        EntraUser user2 = buildEntraUser(generateEntraId(), "user2@example.com", "User", "Two");
        user2 = entraUserRepository.saveAndFlush(user2);

        Firm firm1 = buildFirm("Firm Epsilon", "EPSILON");
        Firm firm2 = buildFirm("Firm Zeta", "ZETA");
        Firm firm3 = buildFirm("Firm Eta", "ETA");
        firmRepository.saveAllAndFlush(List.of(firm1, firm2, firm3));

        // User 1: 2 profiles
        UserProfile user1Profile1 = buildLaaUserProfile(user1, UserType.EXTERNAL);
        user1Profile1.setFirm(firm1);
        UserProfile user1Profile2 = buildLaaUserProfile(user1, UserType.EXTERNAL);
        user1Profile2.setFirm(firm2);
        user1.getUserProfiles().add(user1Profile1);
        user1.getUserProfiles().add(user1Profile2);
        repository.saveAllAndFlush(List.of(user1Profile1, user1Profile2));

        // User 2: 1 profile
        UserProfile user2Profile1 = buildLaaUserProfile(user2, UserType.EXTERNAL);
        user2Profile1.setFirm(firm3);
        user2.getUserProfiles().add(user2Profile1);
        repository.saveAndFlush(user2Profile1);

        // Execute: Count profiles for each user
        long count1 = repository.countByEntraUserId(user1.getId());
        long count2 = repository.countByEntraUserId(user2.getId());

        // Assert: Each count should be independent
        assertThat(count1).isEqualTo(2L);
        assertThat(count2).isEqualTo(1L);
    }
}
