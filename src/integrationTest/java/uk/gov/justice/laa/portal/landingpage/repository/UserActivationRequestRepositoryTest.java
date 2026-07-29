package uk.gov.justice.laa.portal.landingpage.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRoleType;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRequestStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserActivationRequestRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private TestEntityManager entityManager;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private UserActivationRequestRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private FirmRepository firmRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private UserProfileRepository userProfileRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraUserRepository entraUserRepository;

    private UUID requestId1;
    private UUID requestId2;
    private UUID userProfileId1;
    private UUID userProfileId2;

    @BeforeEach
    void setUp() {
        requestId1 = UUID.randomUUID();
        requestId2 = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
        userProfileRepository.deleteAll();
        firmRepository.deleteAll();
        entraUserRepository.deleteAll();
        entityManager.flush();
    }

    private UserActivationRequest createAndPersistRequest(UserProfile userProfile, UUID requestId, int version,
                                                          Instant createdAt, ReactivationRequestStatus status) {
        UserActivationRequest request = new UserActivationRequest();
        request.setRequestId(requestId);
        request.setUserProfileId(userProfile.getId());
        request.setVersion(version);
        request.setCreatedAt(createdAt);
        request.setStatus(status);
        request.setActorEntraOid(UUID.randomUUID().toString());
        request.setActorRoleType(AuthzRoleType.PROVIDER_ADMIN);
        request.setComments("Test comments");

        // Adjust setters below according to your entity fields
        return entityManager.persistAndFlush(request);
    }

    @Nested
    @DisplayName("Tests for Derived Finder Methods")
    class DerivedFinderTests {

        @Test
        @DisplayName("findFirstByRequestIdOrderByVersionDesc should return latest version for given requestId")
        void findFirstByRequestIdOrderByVersionDesc_returnsLatestVersion() {
            // Arrange
            EntraUser entraUser = buildEntraUser("123", "test@email.com", "First", "Last");
            entraUser = entraUserRepository.saveAndFlush(entraUser);
            UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
            Firm firm1 = buildFirm("Firm1", "Firm Code 1");
            firmRepository.saveAndFlush(firm1);
            userProfile.setFirm(firm1);
            userProfileRepository.saveAndFlush(userProfile);
            userProfileId1 = userProfile.getId();
            createAndPersistRequest(userProfile, requestId1, 1, Instant.now().minus(2, ChronoUnit.HOURS), ReactivationRequestStatus.IN_REVIEW);
            UserActivationRequest v2 = createAndPersistRequest(userProfile, requestId1, 2, Instant.now().minus(1, ChronoUnit.HOURS), ReactivationRequestStatus.INFORMATION_REQUIRED);

            // Act
            Optional<UserActivationRequest> result = repository.findFirstByRequestIdOrderByVersionDesc(requestId1);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getVersion()).isEqualTo(2);
            assertThat(result.get().getId()).isEqualTo(v2.getId());
        }

        @Test
        @DisplayName("findFirstByUserProfileIdOrderByVersionDesc should return highest version for user profile")
        void findFirstByUserProfileIdOrderByVersionDesc_returnsHighestVersionForUser() {
            // Arrange
            EntraUser entraUser = buildEntraUser("123", "test@email.com", "First", "Last");
            entraUser = entraUserRepository.saveAndFlush(entraUser);
            UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
            Firm firm1 = buildFirm("Firm1", "Firm Code 1");
            firmRepository.saveAndFlush(firm1);
            userProfile.setFirm(firm1);
            userProfileRepository.saveAndFlush(userProfile);
            userProfileId1 = userProfile.getId();
            createAndPersistRequest(userProfile, requestId1, 1, Instant.now().minus(1, ChronoUnit.DAYS), ReactivationRequestStatus.IN_REVIEW);
            UserActivationRequest latest = createAndPersistRequest(
                    userProfile, requestId2, 3, Instant.now(), ReactivationRequestStatus.INFORMATION_REQUIRED);

            // Act
            Optional<UserActivationRequest> result = repository.findFirstByUserProfileIdOrderByVersionDesc(userProfileId1);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getVersion()).isEqualTo(3);
            assertThat(result.get().getId()).isEqualTo(latest.getId());
        }

        @Test
        @DisplayName("findAllByRequestIdOrderByVersionAsc should return all versions ordered ascending")
        void findAllByRequestIdOrderByVersionAsc_returnsOrderedList() {
            // Arrange
            EntraUser entraUser = buildEntraUser("123", "test@email.com", "First", "Last");
            entraUser = entraUserRepository.saveAndFlush(entraUser);
            UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
            Firm firm1 = buildFirm("Firm1", "Firm Code 1");
            firmRepository.saveAndFlush(firm1);
            userProfile.setFirm(firm1);
            userProfileRepository.saveAndFlush(userProfile);
            userProfileId1 = userProfile.getId();
            createAndPersistRequest(userProfile, requestId1, 2, Instant.now(), ReactivationRequestStatus.IN_REVIEW);
            createAndPersistRequest(userProfile, requestId1, 1, Instant.now().minus(1, ChronoUnit.HOURS), ReactivationRequestStatus.IN_REVIEW);

            // Act
            List<UserActivationRequest> results = repository.findAllByRequestIdOrderByVersionAsc(requestId1);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getVersion()).isEqualTo(1);
            assertThat(results.get(1).getVersion()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Tests for Custom JPQL Queries")
    class CustomQueryTests {

        @Test
        @DisplayName("findMaxVersionByRequestId should return max version if records exist")
        void findMaxVersionByRequestId_returnsMaxVersion() {
            // Arrange
            EntraUser entraUser = buildEntraUser("123", "test@email.com", "First", "Last");
            entraUser = entraUserRepository.saveAndFlush(entraUser);
            UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
            Firm firm1 = buildFirm("Firm1", "Firm Code 1");
            firmRepository.saveAndFlush(firm1);
            userProfile.setFirm(firm1);
            userProfileRepository.saveAndFlush(userProfile);
            userProfileId1 = userProfile.getId();
            createAndPersistRequest(userProfile, requestId1, 1, Instant.now(), ReactivationRequestStatus.IN_REVIEW);
            createAndPersistRequest(userProfile, requestId1, 3, Instant.now(), ReactivationRequestStatus.INFORMATION_REQUIRED);

            // Act
            Integer maxVersion = repository.findMaxVersionByRequestId(requestId1);

            // Assert
            assertThat(maxVersion).isEqualTo(3);
        }

        @Test
        @DisplayName("findMaxVersionByRequestId should return 0 via COALESCE when no records exist")
        void findMaxVersionByRequestId_returnsZeroWhenNoRecords() {
            // Act
            Integer maxVersion = repository.findMaxVersionByRequestId(UUID.randomUUID());

            // Assert
            assertThat(maxVersion).isEqualTo(0);
        }

        @Test
        @DisplayName("findAllLatestRequests (Unpaged) should return only latest version per requestId sorted by createdAt desc")
        void findAllLatestRequests_returnsOnlyLatestVersionsSorted() {
            // Request 1: Latest is v2 (Older creation time)
            EntraUser entraUser1 = buildEntraUser("123", "test@email.com", "First", "Last");
            entraUser1 = entraUserRepository.saveAndFlush(entraUser1);
            UserProfile userProfile1 = buildLaaUserProfile(entraUser1, UserType.EXTERNAL);
            Firm firm1 = buildFirm("Firm1", "Firm Code 1");
            firmRepository.saveAndFlush(firm1);
            userProfile1.setFirm(firm1);
            userProfileRepository.saveAndFlush(userProfile1);
            userProfileId1 = userProfile1.getId();
            Instant now = Instant.now();
            createAndPersistRequest(userProfile1, requestId1, 1, now.minus(4, ChronoUnit.HOURS), ReactivationRequestStatus.IN_REVIEW);
            final UserActivationRequest req1Latest = createAndPersistRequest(userProfile1, requestId1, 2, now.minus(3, ChronoUnit.HOURS), ReactivationRequestStatus.INFORMATION_REQUIRED);

            // Request 2: Latest is v1 (Newer creation time)
            EntraUser entraUser2 = buildEntraUser("12345", "test2@email.com", "First", "Last");
            entraUser2 = entraUserRepository.saveAndFlush(entraUser2);
            UserProfile userProfile2 = buildLaaUserProfile(entraUser2, UserType.EXTERNAL);
            Firm firm2 = buildFirm("Firm2", "Firm Code 2");
            firmRepository.saveAndFlush(firm2);
            userProfile2.setFirm(firm2);
            userProfileRepository.saveAndFlush(userProfile2);
            userProfileId2 = userProfile2.getId();
            UserActivationRequest req2Latest = createAndPersistRequest(userProfile2, requestId2, 1, now.minus(1, ChronoUnit.HOURS), ReactivationRequestStatus.IN_REVIEW);

            // Act
            List<UserActivationRequest> latestRequests = repository.findAllLatestRequests();

            // Assert
            assertThat(latestRequests).hasSize(2);
            assertThat(latestRequests.get(0).getId()).isEqualTo(req2Latest.getId());
            assertThat(latestRequests.get(1).getId()).isEqualTo(req1Latest.getId());
        }

        @Test
        @DisplayName("findAllLatestRequests (Paged) should return paginated latest requests with accurate count")
        void findAllLatestRequests_paged_returnsPagedResultsAndCorrectTotalCount() {
            EntraUser entraUser1 = buildEntraUser("123", "test@email.com", "First", "Last");
            entraUser1 = entraUserRepository.saveAndFlush(entraUser1);
            UserProfile userProfile1 = buildLaaUserProfile(entraUser1, UserType.EXTERNAL);
            Firm firm1 = buildFirm("Firm1", "Firm Code 1");
            firmRepository.saveAndFlush(firm1);
            userProfile1.setFirm(firm1);
            userProfileRepository.saveAndFlush(userProfile1);
            userProfileId1 = userProfile1.getId();
            Instant now = Instant.now();
            final UUID reqIdA = UUID.randomUUID();
            final UUID reqIdB = UUID.randomUUID();
            final UUID reqIdC = UUID.randomUUID();
            createAndPersistRequest(userProfile1, reqIdA, 1, now, ReactivationRequestStatus.IN_REVIEW);
            createAndPersistRequest(userProfile1, reqIdA, 2, now, ReactivationRequestStatus.INFORMATION_REQUIRED);

            createAndPersistRequest(userProfile1, reqIdB, 1, now, ReactivationRequestStatus.INFORMATION_REQUIRED);

            EntraUser entraUser2 = buildEntraUser("123456", "test2@email.com", "First2", "Last2");
            entraUser2 = entraUserRepository.saveAndFlush(entraUser2);
            UserProfile userProfile2 = buildLaaUserProfile(entraUser2, UserType.EXTERNAL);
            Firm firm2 = buildFirm("Firm2", "Firm Code 2");
            firmRepository.saveAndFlush(firm2);
            userProfile2.setFirm(firm2);
            userProfileRepository.saveAndFlush(userProfile2);
            userProfileId2 = userProfile2.getId();
            createAndPersistRequest(userProfile2, reqIdC, 1, now, ReactivationRequestStatus.IN_REVIEW);
            createAndPersistRequest(userProfile2, reqIdC, 2, now, ReactivationRequestStatus.INFORMATION_REQUIRED);

            Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

            // Act
            Page<UserActivationRequest> page = repository.findAllLatestRequests(pageable);

            // Assert
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }
    }
}
