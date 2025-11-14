package uk.gov.justice.laa.portal.landingpage.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchResultsDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UserProfileRepository pagination functionality.
 * Tests the actual database queries to ensure pagination counts are accurate.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserProfileRepositoryPaginationTest extends BaseRepositoryTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private EntraUserRepository entraUserRepository;

    @Autowired
    private FirmRepository firmRepository;

    private List<UserProfile> testUsers;
    private Firm testFirm1;
    private Firm testFirm2;

    @BeforeEach
    void setUp() {
        // Clean up existing data (but keep app roles which are reference data)
        userProfileRepository.deleteAll();
        entraUserRepository.deleteAll();
        firmRepository.deleteAll();

        // Create test firms using helper method
        testFirm1 = buildFirm("Test Firm One", "TF001");
        testFirm1 = firmRepository.save(testFirm1);

        testFirm2 = buildFirm("Test Firm Two", "TF002");
        testFirm2 = firmRepository.save(testFirm2);

        // Create 116 test users to match the real scenario
        testUsers = new ArrayList<>();
        for (int i = 0; i < 116; i++) {
            // Use helper method to build EntraUser with required fields
            EntraUser entraUser = buildEntraUser(
                    UUID.randomUUID().toString(),
                    "user" + i + "@example.com",
                    "User" + i,
                    "Test" + i
            );
            entraUser = entraUserRepository.save(entraUser);

            Firm firm = (i % 2 == 0) ? testFirm1 : testFirm2;

            // Use helper method to build UserProfile with required fields
            UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL);
            userProfile.setFirm(firm);
            testUsers.add(userProfileRepository.save(userProfile));
        }
    }

    @Test
    void findBySearchParams_withNoFilters_returns116TotalElements() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria("", FirmSearchForm.builder().build(), null, false, false);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "entraUser.firstName"));

        // When
        Page<UserSearchResultsDto> result = userProfileRepository.findBySearchParams(criteria, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(116);
        assertThat(result.getTotalPages()).isEqualTo(12); // 116/10 = 11.6, rounds up to 12
        assertThat(result.getContent()).hasSize(10);
    }

    @Test
    void findBySearchParams_lastPage_hasCorrectNumberOfElements() {
        // Given - Request page 12 (0-indexed as 11)
        UserSearchCriteria criteria = new UserSearchCriteria("", FirmSearchForm.builder().build(), null, false, false);
        PageRequest pageRequest = PageRequest.of(11, 10, Sort.by(Sort.Direction.ASC, "entraUser.firstName"));

        // When
        Page<UserSearchResultsDto> result = userProfileRepository.findBySearchParams(criteria, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(116);
        assertThat(result.getTotalPages()).isEqualTo(12);
        assertThat(result.getContent()).hasSize(6); // Last page has 6 users (110 + 6 = 116)
    }

    @Test
    void findBySearchParams_withSortByFirstName_maintainsCorrectCount() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria("", FirmSearchForm.builder().build(), null, false, false);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "entraUser.firstName"));

        // When
        Page<UserSearchResultsDto> result = userProfileRepository.findBySearchParams(criteria, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(116);
        assertThat(result.getTotalPages()).isEqualTo(12);
    }

    @Test
    void findBySearchParams_withSortByFirmName_maintainsCorrectCount() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria("", FirmSearchForm.builder().build(), null, false, false);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "firm.name"));

        // When
        Page<UserSearchResultsDto> result = userProfileRepository.findBySearchParams(criteria, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(116);
        assertThat(result.getTotalPages()).isEqualTo(12);
    }

    @Test
    void findBySearchParams_withSortByEmail_maintainsCorrectCount() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria("", FirmSearchForm.builder().build(), null, false, false);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "entraUser.email"));

        // When
        Page<UserSearchResultsDto> result = userProfileRepository.findBySearchParams(criteria, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(116);
        assertThat(result.getTotalPages()).isEqualTo(12);
    }

    @Test
    void findBySearchParams_withFirmFilter_returnsCorrectCount() {
        // Given - Filter by firm 1 (should have 58 users - half of 116)
        FirmSearchForm firmSearch = FirmSearchForm.builder()
                .selectedFirmId(testFirm1.getId())
                .build();
        UserSearchCriteria criteria = new UserSearchCriteria("", firmSearch, null, false, false);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "entraUser.firstName"));

        // When
        Page<UserSearchResultsDto> result = userProfileRepository.findBySearchParams(criteria, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(58);
        assertThat(result.getTotalPages()).isEqualTo(6); // 58/10 = 5.8, rounds up to 6
    }

    @Test
    void findBySearchParams_withSearchTerm_returnsFilteredResults() {
        // Given - Search for "User10" which should match User10, User100-109
        UserSearchCriteria criteria = new UserSearchCriteria("User10", FirmSearchForm.builder().build(), null, false, false);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "entraUser.firstName"));

        // When
        Page<UserSearchResultsDto> result = userProfileRepository.findBySearchParams(criteria, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isGreaterThan(0);
        assertThat(result.getTotalElements()).isLessThan(116);
        // Verify count matches actual content across all pages
        long expectedTotal = result.getTotalElements();
        int totalPages = result.getTotalPages();
        assertThat(totalPages).isEqualTo((int) Math.ceil((double) expectedTotal / 10));
    }

    @Test
    void findBySearchParams_withUserTypeFilter_returnsCorrectCount() {
        // Given - All test users are EXTERNAL type
        UserSearchCriteria criteria = new UserSearchCriteria("", FirmSearchForm.builder().build(), UserType.EXTERNAL, false, false);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "entraUser.firstName"));

        // When
        Page<UserSearchResultsDto> result = userProfileRepository.findBySearchParams(criteria, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(116);
        assertThat(result.getTotalPages()).isEqualTo(12);
    }

    @Test
    void findBySearchParams_withInternalUserTypeFilter_returnsZero() {
        // Given - No INTERNAL users in test data
        UserSearchCriteria criteria = new UserSearchCriteria("", FirmSearchForm.builder().build(), UserType.INTERNAL, false, false);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "entraUser.firstName"));

        // When
        Page<UserSearchResultsDto> result = userProfileRepository.findBySearchParams(criteria, pageRequest);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    void findBySearchParams_differentPageSizes_maintainsSameTotal() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria("", FirmSearchForm.builder().build(), null, false, false);

        // Test different page sizes
        int[] pageSizes = {5, 10, 20, 50};
        for (int pageSize : pageSizes) {
            PageRequest pageRequest = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "entraUser.firstName"));

            // When
            Page<UserSearchResultsDto> result = userProfileRepository.findBySearchParams(criteria, pageRequest);

            // Then
            assertThat(result.getTotalElements()).isEqualTo(116);
            assertThat(result.getTotalPages()).isEqualTo((int) Math.ceil(116.0 / pageSize));
        }
    }

    @Test
    void findBySearchParams_multipleSorts_allReturnSameCount() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria("", FirmSearchForm.builder().build(), null, false, false);

        // Test different sort fields and directions
        Sort[] sorts = {
                Sort.by(Sort.Direction.ASC, "entraUser.firstName"),
                Sort.by(Sort.Direction.DESC, "entraUser.firstName"),
                Sort.by(Sort.Direction.ASC, "entraUser.lastName"),
                Sort.by(Sort.Direction.DESC, "entraUser.email"),
                Sort.by(Sort.Direction.ASC, "firm.name"),
                Sort.by(Sort.Direction.DESC, "userProfileStatus")
        };

        for (Sort sort : sorts) {
            PageRequest pageRequest = PageRequest.of(0, 10, sort);

            // When
            Page<UserSearchResultsDto> result = userProfileRepository.findBySearchParams(criteria, pageRequest);

            // Then - All sorts should return same total count
            assertThat(result.getTotalElements())
                    .as("Total elements should be 116 regardless of sort field: " + sort)
                    .isEqualTo(116);
            assertThat(result.getTotalPages())
                    .as("Total pages should be 12 regardless of sort field: " + sort)
                    .isEqualTo(12);
        }
    }
}
