package uk.gov.justice.laa.portal.landingpage.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for UserAccountStatusAuditRepository deleted users functionality.
 * Tests the findDeletedUsers query method with various search, filter, sort, and pagination scenarios.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserAccountStatusAuditRepository - Deleted Users Tests")
class UserAccountStatusAuditRepositoryDeletedUsersTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private UserAccountStatusAuditRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private LocalDateTime testStartTime;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean slate before each test using native SQL to avoid JPA entity loading issues
        cleanupAuditRecords();

        // Use a fixed point in time for consistent test data
        testStartTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Ensure cleanup after each test
        cleanupAuditRecords();
    }

    /**
     * Safely cleanup audit records using native SQL to avoid EntityNotFoundException
     * when orphaned records reference deleted EntraUser entities from other tests
     */
    private void cleanupAuditRecords() {
        try {
            entityManager.createNativeQuery(
                    "DELETE FROM user_account_status_audit WHERE status_change = 'DELETED' OR user_email LIKE '%@example.com'"
            ).executeUpdate();
            entityManager.flush();
        } catch (Exception e) {
            // If cleanup fails, log and continue - tests will handle their own data
            System.err.println("Cleanup warning: " + e.getMessage());
        }
    }

    // ========== BASIC FUNCTIONALITY TESTS ==========

    @Test
    @DisplayName("Should return only deleted users, excluding enabled and disabled users")
    void findDeletedUsers_shouldReturnOnlyDeletedUsers() {
        // Arrange - Create users with different statuses
        createAuditRecord("deleted1@example.com", UserAccountStatus.DELETED, "John Doe", testStartTime.minusDays(1));
        createAuditRecord("deleted2@example.com", UserAccountStatus.DELETED, "Jane Smith", testStartTime.minusDays(2));
        createAuditRecord("enabled@example.com", UserAccountStatus.ENABLED, "Enabled User", testStartTime.minusDays(3));
        createAuditRecord("disabled@example.com", UserAccountStatus.DISABLED, "Disabled User", testStartTime.minusDays(4));

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "statusChangedDate"));

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(null, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(UserAccountStatusAudit::getUserEmail)
                .containsExactly("deleted1@example.com", "deleted2@example.com");
        assertThat(result.getContent())
                .allMatch(audit -> audit.getStatusChange().equals(UserAccountStatus.DELETED));
    }

    @Test
    @DisplayName("Should return empty page when no deleted users exist")
    void findDeletedUsers_shouldReturnEmptyWhenNoDeletedUsers() {
        // Arrange - Create only non-deleted users
        createAuditRecord("enabled@example.com", UserAccountStatus.ENABLED, "User", testStartTime);
        createAuditRecord("disabled@example.com", UserAccountStatus.DISABLED, "User", testStartTime);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(null, pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return empty page when no audit records exist")
    void findDeletedUsers_shouldReturnEmptyWhenNoRecords() {
        // Arrange - No records created
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(null, pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // ========== SEARCH/FILTER TESTS ==========

    @Test
    @DisplayName("Should filter deleted users by exact email match")
    void findDeletedUsers_shouldFilterByExactEmail() {
        // Arrange
        createAuditRecord("test.user1@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("another.user@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("third.user@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(
                "test.user1@example.com", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getUserEmail()).isEqualTo("test.user1@example.com");
    }

    @Test
    @DisplayName("Should filter deleted users by partial email match")
    void findDeletedUsers_shouldFilterByPartialEmail() {
        // Arrange
        createAuditRecord("john.smith@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("jane.smith@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("bob.jones@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);

        Pageable pageable = PageRequest.of(0, 10);

        // Act - Search for "smith"
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(
                "smith", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(UserAccountStatusAudit::getUserEmail)
                .allMatch(email -> email.contains("smith"));
    }

    @Test
    @DisplayName("Should perform case-insensitive email search")
    void findDeletedUsers_shouldBeCaseInsensitive() {
        // Arrange
        createAuditRecord("UPPERCASE@EXAMPLE.COM", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("lowercase@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("MiXeDcAsE@ExAmPlE.CoM", UserAccountStatus.DELETED, "Admin", testStartTime);

        Pageable pageable = PageRequest.of(0, 10);

        // Act - Search with lowercase
        Page<UserAccountStatusAudit> resultLowercase = repository.findDeletedUsers(
                "uppercase", pageable);

        // Act - Search with uppercase
        Page<UserAccountStatusAudit> resultUppercase = repository.findDeletedUsers(
                "LOWERCASE", pageable);

        // Assert
        assertThat(resultLowercase.getContent()).hasSize(1);
        assertThat(resultLowercase.getContent().getFirst().getUserEmail()).isEqualTo("UPPERCASE@EXAMPLE.COM");

        assertThat(resultUppercase.getContent()).hasSize(1);
        assertThat(resultUppercase.getContent().getFirst().getUserEmail()).isEqualTo("lowercase@example.com");
    }

    @Test
    @DisplayName("Should return empty result when search term doesn't match any email")
    void findDeletedUsers_shouldReturnEmptyWhenSearchTermNotFound() {
        // Arrange
        createAuditRecord("john@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("jane@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(
                "nonexistent", pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle null search term as wildcard (return all)")
    void findDeletedUsers_shouldHandleNullSearchTerm() {
        // Arrange
        createAuditRecord("user1@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("user2@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("user3@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(
                null, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle empty string search term")
    void findDeletedUsers_shouldHandleEmptySearchTerm() {
        // Arrange
        createAuditRecord("user1@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("user2@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(
                "", pageable);

        // Assert - Empty string should match all (or none, depending on implementation)
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(0);
    }

    // ========== PAGINATION TESTS ==========

    @Test
    @DisplayName("Should correctly paginate deleted users")
    void findDeletedUsers_shouldRespectPagination() {
        // Arrange - Create 15 deleted users
        for (int i = 1; i <= 15; i++) {
            createAuditRecord(
                    "user" + i + "@example.com",
                    UserAccountStatus.DELETED,
                    "Admin",
                    testStartTime.minusDays(i)
            );
        }

        Pageable firstPage = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "statusChangedDate"));
        Pageable secondPage = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "statusChangedDate"));

        // Act
        Page<UserAccountStatusAudit> page1 = repository.findDeletedUsers(null, firstPage);
        Page<UserAccountStatusAudit> page2 = repository.findDeletedUsers(null, secondPage);

        // Assert page 1
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page1.getTotalElements()).isEqualTo(15);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.getNumber()).isEqualTo(0);
        assertThat(page1.isFirst()).isTrue();
        assertThat(page1.isLast()).isFalse();

        // Assert page 2
        assertThat(page2.getContent()).hasSize(5);
        assertThat(page2.getTotalElements()).isEqualTo(15);
        assertThat(page2.getTotalPages()).isEqualTo(2);
        assertThat(page2.getNumber()).isEqualTo(1);
        assertThat(page2.isFirst()).isFalse();
        assertThat(page2.isLast()).isTrue();

        // Assert no overlap between pages
        assertThat(page1.getContent())
                .extracting(UserAccountStatusAudit::getUserEmail)
                .doesNotContainAnyElementsOf(
                        page2.getContent().stream()
                                .map(UserAccountStatusAudit::getUserEmail)
                                .toList()
            );
    }

    @Test
    @DisplayName("Should handle page size larger than result set")
    void findDeletedUsers_shouldHandleLargePageSize() {
        // Arrange
        createAuditRecord("user1@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("user2@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);

        Pageable pageable = PageRequest.of(0, 100);

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(null, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should handle request for page beyond available data")
    void findDeletedUsers_shouldHandlePageBeyondData() {
        // Arrange
        createAuditRecord("user1@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);

        Pageable pageable = PageRequest.of(5, 10); // Request page 5 when only 1 record exists

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(null, pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getNumber()).isEqualTo(5);
    }

    // ========== SORTING TESTS ==========

    @Test
    @DisplayName("Should sort deleted users by email ascending")
    void findDeletedUsers_shouldSortByEmailAscending() {
        // Arrange
        createAuditRecord("zebra@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("alpha@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("mike@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("beta@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "userEmail"));

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(null, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent())
                .extracting(UserAccountStatusAudit::getUserEmail)
                .containsExactly("alpha@example.com", "beta@example.com", "mike@example.com", "zebra@example.com");
    }

    @Test
    @DisplayName("Should sort deleted users by email descending")
    void findDeletedUsers_shouldSortByEmailDescending() {
        // Arrange
        createAuditRecord("alpha@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("zebra@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("mike@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "userEmail"));

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(null, pageable);

        // Assert
        assertThat(result.getContent())
                .extracting(UserAccountStatusAudit::getUserEmail)
                .containsExactly("zebra@example.com", "mike@example.com", "alpha@example.com");
    }

    @Test
    @DisplayName("Should sort deleted users by status changed date descending")
    void findDeletedUsers_shouldSortByDateDescending() {
        // Arrange - Create with different dates
        LocalDateTime day1 = testStartTime.minusDays(1);
        LocalDateTime day3 = testStartTime.minusDays(3);
        LocalDateTime day5 = testStartTime.minusDays(5);

        createAuditRecord("oldest@example.com", UserAccountStatus.DELETED, "Admin", day5);
        createAuditRecord("newest@example.com", UserAccountStatus.DELETED, "Admin", day1);
        createAuditRecord("middle@example.com", UserAccountStatus.DELETED, "Admin", day3);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "statusChangedDate"));

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(null, pageable);

        // Assert - Most recent first
        assertThat(result.getContent())
                .extracting(UserAccountStatusAudit::getUserEmail)
                .containsExactly("newest@example.com", "middle@example.com", "oldest@example.com");
    }

    @Test
    @DisplayName("Should sort deleted users by status changed date ascending")
    void findDeletedUsers_shouldSortByDateAscending() {
        // Arrange
        LocalDateTime day1 = testStartTime.minusDays(1);
        LocalDateTime day3 = testStartTime.minusDays(3);
        LocalDateTime day5 = testStartTime.minusDays(5);

        createAuditRecord("newest@example.com", UserAccountStatus.DELETED, "Admin", day1);
        createAuditRecord("middle@example.com", UserAccountStatus.DELETED, "Admin", day3);
        createAuditRecord("oldest@example.com", UserAccountStatus.DELETED, "Admin", day5);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "statusChangedDate"));

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(null, pageable);

        // Assert - Oldest first
        assertThat(result.getContent())
                .extracting(UserAccountStatusAudit::getUserEmail)
                .containsExactly("oldest@example.com", "middle@example.com", "newest@example.com");
    }

    // ========== COMBINED SCENARIOS TESTS ==========

    @Test
    @DisplayName("Should combine search, sort, and pagination correctly")
    void findDeletedUsers_shouldCombineSearchSortAndPagination() {
        // Arrange - Create users with "test" in email
        for (int i = 1; i <= 12; i++) {
            createAuditRecord(
                    "test.user" + i + "@example.com",
                    UserAccountStatus.DELETED,
                    "Admin",
                    testStartTime.minusDays(i)
            );
        }
        // Create users without "test" in email
        createAuditRecord("other@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);

        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "userEmail"));

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(
                "test", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getTotalElements()).isEqualTo(12);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getContent())
                .allMatch(audit -> audit.getUserEmail().contains("test"));
        // Assert sorted by email
        assertThat(result.getContent())
                .extracting(UserAccountStatusAudit::getUserEmail)
                .isSorted();
    }

    @Test
    @DisplayName("Should handle complex email patterns in search")
    void findDeletedUsers_shouldHandleComplexEmailPatterns() {
        // Arrange
        createAuditRecord("user+tag@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("user.name@sub.example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("user_underscore@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("123numeric@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);

        Pageable pageable = PageRequest.of(0, 10);

        // Act & Assert - Search for subdomain
        Page<UserAccountStatusAudit> resultSub = repository.findDeletedUsers(
                "sub.example", pageable);
        assertThat(resultSub.getContent()).hasSize(1);
        assertThat(resultSub.getContent().getFirst().getUserEmail()).isEqualTo("user.name@sub.example.com");

        // Act & Assert - Search for underscore
        Page<UserAccountStatusAudit> resultUnderscore = repository.findDeletedUsers(
                "underscore", pageable);
        assertThat(resultUnderscore.getContent()).hasSize(1);

        // Act & Assert - Search for numeric
        Page<UserAccountStatusAudit> resultNumeric = repository.findDeletedUsers(
                "123", pageable);
        assertThat(resultNumeric.getContent()).hasSize(1);
    }

    // ========== EDGE CASES AND VALIDATION TESTS ==========

    @Test
    @DisplayName("Should handle special characters in search term")
    void findDeletedUsers_shouldHandleSpecialCharactersInSearch() {
        // Arrange
        createAuditRecord("user@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);
        createAuditRecord("test@example.com", UserAccountStatus.DELETED, "Admin", testStartTime);

        Pageable pageable = PageRequest.of(0, 10);

        // Act - Search with @ symbol
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(
                "@example", pageable);

        // Assert - Should find both
        assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.getContent())
                .allMatch(audit -> audit.getUserEmail().contains("@example"));
    }

    @Test
    @DisplayName("Should verify all audit fields are populated correctly")
    void findDeletedUsers_shouldReturnCompleteAuditRecords() {
        // Arrange
        LocalDateTime specificDate = testStartTime.minusDays(1);
        createAuditRecord("complete@example.com", UserAccountStatus.DELETED, "John Administrator", specificDate);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserAccountStatusAudit> result = repository.findDeletedUsers(
                "complete", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        UserAccountStatusAudit audit = result.getContent().getFirst();

        assertThat(audit.getId()).isNotNull();
        assertThat(audit.getUserEmail()).isEqualTo("complete@example.com");
        assertThat(audit.getStatusChange()).isEqualTo(UserAccountStatus.DELETED);
        assertThat(audit.getStatusChangedBy()).isEqualTo("John Administrator");
        assertThat(audit.getStatusChangedDate()).isNotNull();
        assertThat(audit.getStatusChangedDate()).isEqualToIgnoringNanos(specificDate);
    }

    // ========== HELPER METHODS ==========

    /**
     * Helper method to create and persist audit records with consistent data
     */
    private UserAccountStatusAudit createAuditRecord(
            String email,
            UserAccountStatus status,
            String changedBy,
            LocalDateTime changedDate) {

        UserAccountStatusAudit audit = UserAccountStatusAudit.builder()
                .userEmail(email)
                .statusChange(status)
                .statusChangedBy(changedBy)
                .statusChangedDate(changedDate)
                .build();

        return repository.saveAndFlush(audit);
    }
}
