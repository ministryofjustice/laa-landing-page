package uk.gov.justice.laa.portal.landingpage.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;

import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

@ExtendWith(MockitoExtension.class)
class FirmServiceTest {

    @InjectMocks
    private FirmService firmService;
    @Mock
    private FirmRepository firmRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void setUp() {
        firmService = new FirmService(
            firmRepository,
            userProfileRepository,
            new MapperConfig().modelMapper(),
                cacheManager
        );
    }

    @Test
    void getFirms() {
        Firm firm1 = Firm.builder().build();
        Firm firm2 = Firm.builder().build();
        List<Firm> dbFirms = List.of(firm1, firm2);
        when(firmRepository.findAll()).thenReturn(dbFirms);
        List<FirmDto> firms = firmService.getFirms();
        assertThat(firms).hasSize(2);
    }

    @Test
    void getFirm() {
        UUID firmId = UUID.randomUUID();
        Firm dbFirm = Firm.builder().id(firmId).build();
        when(firmRepository.getReferenceById(firmId)).thenReturn(dbFirm);
        FirmDto firm = firmService.getFirm(firmId.toString());
        assertThat(firm.getId()).isEqualTo(firmId);
    }

    @Test
    void getUserFirms() {
        UserProfile up1 = UserProfile.builder().activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE).firm(Firm.builder().name("F1").build()).build();
        UserProfile up2 = UserProfile.builder().activeProfile(false).userProfileStatus(UserProfileStatus.COMPLETE).firm(Firm.builder().name("F2").build()).build();
        Set<UserProfile> userProfiles = Set.of(up1, up2);
        EntraUser entraUser = EntraUser.builder().userProfiles(userProfiles).build();
        List<FirmDto> firms = firmService.getUserFirms(entraUser);
        assertThat(firms).hasSize(1);
        assertThat(firms.getFirst().getName()).isEqualTo("F1");
    }

    @Test
    void getUserAllFirms() {
        UserProfile up1 = UserProfile.builder().activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE).firm(Firm.builder().name("F1").build()).build();
        UserProfile up2 = UserProfile.builder().activeProfile(false).userProfileStatus(UserProfileStatus.COMPLETE).firm(Firm.builder().name("F2").build()).build();
        Set<UserProfile> userProfiles = Set.of(up1, up2);
        EntraUser entraUser = EntraUser.builder().userProfiles(userProfiles).build();
        List<FirmDto> firms = firmService.getUserAllFirms(entraUser);
        assertThat(firms).hasSize(2);
    }

    @Test
    void getUserFirmsByUserId() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        Firm firm = Firm.builder().id(firmId).name("Test Firm").build();
        UserProfile userProfile = UserProfile.builder().id(userId).activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE).firm(firm).build();

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));

        // When
        List<FirmDto> firms = firmService.getUserFirmsByUserId(userId.toString());

        // Then
        assertThat(firms).hasSize(1);
        assertThat(firms.getFirst().getId()).isEqualTo(firmId);
        assertThat(firms.getFirst().getName()).isEqualTo("Test Firm");
    }

    @Test
    void getUserFirmsByUserId_userNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        List<FirmDto> firms = firmService.getUserFirmsByUserId(userId.toString());

        // Then
        assertThat(firms).isEmpty();
    }

    @Test
    void getUserFirmsByUserId_userWithNoFirm() {
        // Given
        UUID userId = UUID.randomUUID();
        UserProfile userProfile = UserProfile.builder().id(userId).activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE).firm(null).build();

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));

        // When
        List<FirmDto> firms = firmService.getUserFirmsByUserId(userId.toString());

        // Then
        assertThat(firms).isEmpty();
    }

    @Test
    void searchFirms_withValidSearchTerm() {
        // Given
        String searchTerm = "Smith";
        Firm firm1 = Firm.builder()
                .id(UUID.randomUUID())
                .name("Smith & Associates Law Firm")
                .code("SMITH001")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();
        Firm firm2 = Firm.builder()
                .id(UUID.randomUUID())
                .name("John Smith Legal Services")
                .code("JSMITH002")
                .type(FirmType.PARTNERSHIP)
                .build();
        List<Firm> searchResults = List.of(firm1, firm2);

        when(firmRepository.findByNameOrCodeContaining(searchTerm.trim())).thenReturn(searchResults);

        // When
        List<FirmDto> result = firmService.searchFirms(searchTerm);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Smith & Associates Law Firm");
        assertThat(result.get(0).getCode()).isEqualTo("SMITH001");
        assertThat(result.get(1).getName()).isEqualTo("John Smith Legal Services");
        assertThat(result.get(1).getCode()).isEqualTo("JSMITH002");
        verify(firmRepository).findByNameOrCodeContaining(searchTerm.trim());
        verify(firmRepository, never()).findAll();
    }

    @Nested
    class GetUserAccessibleFirmsTest {
        private static final String CACHE_NAME = CachingConfig.LIST_OF_FIRMS_CACHE;
        private static final String ALL_FIRMS_KEY = "all_firms";
        private Cache cache;
        private EntraUser internalUser;
        private EntraUser externalSingleFirmAdminUser;
        private List<FirmDto> allFirms;

        @BeforeEach
        void setUp() {
            // Setup test data
            internalUser = EntraUser.builder()
                    .id(UUID.randomUUID())
                    .userProfiles(Set.of(
                            UserProfile.builder()
                                    .activeProfile(true)
                                    .userType(UserType.INTERNAL)
                                    .build()
                    ))
                    .build();

            externalSingleFirmAdminUser = EntraUser.builder()
                    .id(UUID.randomUUID())
                    .userProfiles(Set.of(
                            UserProfile.builder()
                                    .activeProfile(true)
                                    .userType(UserType.EXTERNAL)
                                    .firm(Firm.builder()
                                            .id(UUID.randomUUID())
                                            .name("Test Firm 1")
                                            .code("TF1")
                                            .build())
                                    .build()
                    ))
                    .build();

            allFirms = List.of(
                    new FirmDto(UUID.randomUUID(), "Test Firm 1", "TF1", false, false),
                    new FirmDto(UUID.randomUUID(), "Test Firm 2", "TF2", false, false),
                    new FirmDto(UUID.randomUUID(), "Another Firm", "AF1", false, false)
            );

            // Setup cache mock
            cache = mock(Cache.class);
        }

        @Test
        void whenInternalUser_returnsAllFirms() {
            // Given
            when(cacheManager.getCache(CACHE_NAME)).thenReturn(cache);
            Cache.ValueWrapper valueWrapper = mock(ValueWrapper.class);
            when(cache.get(ALL_FIRMS_KEY)).thenReturn(valueWrapper);
            when(valueWrapper.get()).thenReturn(allFirms);

            // When
            List<FirmDto> result = firmService.getUserAccessibleFirms(internalUser, "");

            // Then
            assertThat(result).hasSize(3);
            verify(cache).get(ALL_FIRMS_KEY);
        }

        @Test
        void whenInternalUser_withSearchTerm_filtersFirms() {
            // Given
            List<Firm> searchResults = List.of(
                    Firm.builder().id(UUID.randomUUID()).name("Test Firm 1").code("TF1").build(),
                    Firm.builder().id(UUID.randomUUID()).name("Test Firm 2").code("TF2").build()
            );
            when(firmRepository.findByNameOrCodeContaining("Test")).thenReturn(searchResults);

            // When
            List<FirmDto> result = firmService.getUserAccessibleFirms(internalUser, "Test");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(FirmDto::getName)
                    .containsExactlyInAnyOrder("Test Firm 1", "Test Firm 2");
            verify(firmRepository).findByNameOrCodeContaining("Test");
        }

        @Test
        void whenInternalUser_withSearchTerm_filtersFirms_should_return_in_ranking_order() {
            // Given
            List<Firm> searchResults = List.of(
                    Firm.builder().id(UUID.randomUUID()).name("A Test Firm 1").code("TF1").build(),
                    Firm.builder().id(UUID.randomUUID()).name("Test Firm 2").code("TF2").build()
            );
            when(firmRepository.findByNameOrCodeContaining("Test")).thenReturn(searchResults);

            // When
            List<FirmDto> result = firmService.getUserAccessibleFirms(internalUser, "Test");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(FirmDto::getName)
                    .containsExactlyInAnyOrder("Test Firm 2", "A Test Firm 1");
            verify(firmRepository).findByNameOrCodeContaining("Test");
        }

        @Test
        void whenInternalUser_withSearchTerm_code_filtersFirms_should_return_in_ranking_order() {
            // Given
            List<Firm> searchResults = List.of(
                    Firm.builder().id(UUID.randomUUID()).name("Test Firm 1").code("12345").build(),
                    Firm.builder().id(UUID.randomUUID()).name("Test Firm 2").code("1234").build()
            );
            when(firmRepository.findByNameOrCodeContaining("1234")).thenReturn(searchResults);

            // When
            List<FirmDto> result = firmService.getUserAccessibleFirms(internalUser, "1234");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(FirmDto::getName)
                    .containsExactlyInAnyOrder("Test Firm 2", "Test Firm 1");
            verify(firmRepository).findByNameOrCodeContaining("1234");
        }

        @Test
        void whenInternalUser_withCodeSearch_filtersFirmsByCode() {
            // Given
            List<Firm> codeMatchingFirms = List.of(
                    Firm.builder().id(UUID.randomUUID()).name("Test Firm 1").code("TF1").type(FirmType.SOLE_PRACTITIONER).build(),
                    Firm.builder().id(UUID.randomUUID()).name("Test Firm 2").code("TF2").type(FirmType.SOLE_PRACTITIONER).build()
            );
            when(firmRepository.findByNameOrCodeContaining("TF")).thenReturn(codeMatchingFirms);

            // When
            List<FirmDto> result = firmService.getUserAccessibleFirms(internalUser, "TF");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(FirmDto::getCode)
                    .containsExactlyInAnyOrder("TF1", "TF2");
            verify(firmRepository).findByNameOrCodeContaining("TF");
        }

        @Test
        void whenExternalSingleFirmAdmin_returnsOnlyAssignedFirm() {
            // When
            List<FirmDto> result = firmService.getUserAccessibleFirms(externalSingleFirmAdminUser, "");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getName()).isEqualTo("Test Firm 1");
        }

        @Test
        void whenExternalSingleFirmAdmin_withMatchingSearch_returnsFirm() {
            // When
            List<FirmDto> result = firmService.getUserAccessibleFirms(externalSingleFirmAdminUser, "Firm 1");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getName()).isEqualTo("Test Firm 1");
        }

        @Test
        void whenExternalSingleFirmAdmin_withNonMatchingSearch_returnsEmptyList() {
            // When
            List<FirmDto> result = firmService.getUserAccessibleFirms(externalSingleFirmAdminUser, "NonMatching");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void whenNullSearchTerm_returnsAllFirms() {
            // Given
            when(cacheManager.getCache(CACHE_NAME)).thenReturn(cache);
            Cache.ValueWrapper valueWrapper = mock(ValueWrapper.class);
            when(cache.get(ALL_FIRMS_KEY)).thenReturn(valueWrapper);
            when(valueWrapper.get()).thenReturn(allFirms);

            // When
            List<FirmDto> result = firmService.getUserAccessibleFirms(internalUser, null);

            // Then
            assertThat(result).hasSize(3);
        }

        @Test
        void whenEmptySearchTerm_returnsAllFirms() {
            // Given
            when(cacheManager.getCache(CACHE_NAME)).thenReturn(cache);
            Cache.ValueWrapper valueWrapper = mock(ValueWrapper.class);
            when(cache.get(ALL_FIRMS_KEY)).thenReturn(valueWrapper);
            when(valueWrapper.get()).thenReturn(allFirms);

            // When
            List<FirmDto> result = firmService.getUserAccessibleFirms(internalUser, "   ");

            // Then
            assertThat(result).hasSize(3);
        }

        @Test
        void whenSearchIsCaseInsensitive_returnsMatchingFirms() {
            // Given
            List<Firm> caseInsensitiveMatchingFirms = List.of(
                    Firm.builder().id(UUID.randomUUID()).name("Test Firm 1").code("TF1").type(FirmType.SOLE_PRACTITIONER).build(),
                    Firm.builder().id(UUID.randomUUID()).name("Test Firm 2").code("TF2").type(FirmType.SOLE_PRACTITIONER).build(),
                    Firm.builder().id(UUID.randomUUID()).name("Test Firm 3").code("TF3").type(FirmType.SOLE_PRACTITIONER).build()
            );
            when(firmRepository.findByNameOrCodeContaining("fIrM")).thenReturn(caseInsensitiveMatchingFirms);

            // When
            List<FirmDto> result = firmService.getUserAccessibleFirms(internalUser, "fIrM");

            // Then
            assertThat(result).hasSize(3);
            verify(firmRepository).findByNameOrCodeContaining("fIrM");
        }

        @Test
        void whenCacheMiss_loadsFirmsFromRepository() {
            // Given
            when(cacheManager.getCache(CACHE_NAME)).thenReturn(cache);
            when(cache.get(ALL_FIRMS_KEY)).thenReturn(null);
            when(firmRepository.findAll()).thenReturn(List.of(
                    Firm.builder().id(UUID.randomUUID()).name("Test Firm").build()
            ));

            // When
            List<FirmDto> result = firmService.getUserAccessibleFirms(internalUser, "");

            // Then
            assertThat(result).hasSize(1);
            verify(firmRepository).findAll();
            verify(cache).put(ALL_FIRMS_KEY, result);
        }
    }

    @Test
    void searchFirms_withSearchTermHavingWhitespace() {
        // Given
        String searchTerm = "  Legal Services  ";
        String trimmedSearchTerm = "Legal Services";
        Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .name("ABC Legal Services")
                .code("ABC001")
                .type(FirmType.LIMITED_COMPANY)
                .build();
        List<Firm> searchResults = List.of(firm);

        when(firmRepository.findByNameOrCodeContaining(trimmedSearchTerm)).thenReturn(searchResults);

        // When
        List<FirmDto> result = firmService.searchFirms(searchTerm);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("ABC Legal Services");
        verify(firmRepository).findByNameOrCodeContaining(trimmedSearchTerm);
        verify(firmRepository, never()).findAll();
    }

    @Test
    void searchFirms_withEmptySearchTerm() {
        // Given
        String searchTerm = "";
        Firm firm1 = Firm.builder().id(UUID.randomUUID()).name("Firm 1").build();
        Firm firm2 = Firm.builder().id(UUID.randomUUID()).name("Firm 2").build();
        List<Firm> allFirms = List.of(firm1, firm2);

        when(firmRepository.findAll()).thenReturn(allFirms);

        // When
        List<FirmDto> result = firmService.searchFirms(searchTerm);

        // Then
        assertThat(result).hasSize(2);
        verify(firmRepository).findAll();
    }

    @Test
    void searchFirms_withNullSearchTerm() {
        // Given
        String searchTerm = null;
        Firm firm1 = Firm.builder().id(UUID.randomUUID()).name("Firm 1").build();
        Firm firm2 = Firm.builder().id(UUID.randomUUID()).name("Firm 2").build();
        List<Firm> allFirms = List.of(firm1, firm2);

        when(firmRepository.findAll()).thenReturn(allFirms);

        // When
        List<FirmDto> result = firmService.searchFirms(searchTerm);

        // Then
        assertThat(result).hasSize(2);
        verify(firmRepository).findAll();
    }

    @Test
    void searchFirms_withWhitespaceOnlySearchTerm() {
        // Given
        String searchTerm = "   ";
        Firm firm1 = Firm.builder().id(UUID.randomUUID()).name("Firm 1").build();
        Firm firm2 = Firm.builder().id(UUID.randomUUID()).name("Firm 2").build();
        List<Firm> allFirms = List.of(firm1, firm2);

        when(firmRepository.findAll()).thenReturn(allFirms);

        // When
        List<FirmDto> result = firmService.searchFirms(searchTerm);

        // Then
        assertThat(result).hasSize(2);
        verify(firmRepository).findAll();
    }

    @Test
    void searchFirms_noResultsFound() {
        // Given
        String searchTerm = "NonExistentFirm";
        when(firmRepository.findByNameOrCodeContaining(searchTerm.trim())).thenReturn(List.of());

        // When
        List<FirmDto> result = firmService.searchFirms(searchTerm);

        // Then
        assertThat(result).isEmpty();
        verify(firmRepository).findByNameOrCodeContaining(searchTerm.trim());
    }

    @Test
    void searchFirms_matchingByCode() {
        // Given
        String searchTerm = "ABC001";
        Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .name("ABC Legal Services")
                .code("ABC001")
                .type(FirmType.SOLE_PRACTITIONER)
                .build();
        List<Firm> searchResults = List.of(firm);

        when(firmRepository.findByNameOrCodeContaining(searchTerm.trim())).thenReturn(searchResults);

        // When
        List<FirmDto> result = firmService.searchFirms(searchTerm);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCode()).isEqualTo("ABC001");
        verify(firmRepository).findByNameOrCodeContaining(searchTerm.trim());
    }

    @Test
    void getUserFirms_withEmptyUserProfiles() {
        // Given
        EntraUser entraUser = EntraUser.builder().userProfiles(Set.of()).build();

        // When
        List<FirmDto> result = firmService.getUserFirms(entraUser);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getUserFirms_withOnlyInactiveProfiles() {
        // Given
        UserProfile up1 = UserProfile.builder()
                .activeProfile(false)
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .firm(Firm.builder().name("F1").build())
                .build();
        UserProfile up2 = UserProfile.builder()
                .activeProfile(false)
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .firm(Firm.builder().name("F2").build())
                .build();
        Set<UserProfile> userProfiles = Set.of(up1, up2);
        EntraUser entraUser = EntraUser.builder().userProfiles(userProfiles).build();

        // When
        List<FirmDto> result = firmService.getUserFirms(entraUser);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getUserAllFirms_withEmptyUserProfiles() {
        // Given
        EntraUser entraUser = EntraUser.builder().userProfiles(Set.of()).build();

        // When
        List<FirmDto> result = firmService.getUserAllFirms(entraUser);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldReturnSortedFirmsByRelevance_whenSearchTermIsValid() {
        String searchTerm = "Alpha";

        Firm firm1 = Firm.builder().name("Alpha").code("A001").build();
        Firm firm2 = Firm.builder().name("AlphaTech").code("A002").build();

        when(firmRepository.findByNameOrCodeContaining(searchTerm)).thenReturn(Arrays.asList(firm1, firm2));

        List<FirmDto> result = firmService.searchFirms(searchTerm);

        assertThat(result)
                .hasSize(2)
                .extracting(FirmDto::getName)
                .containsExactly("Alpha", "AlphaTech"); // dto1 should be ranked higher
    }

    @Nested
    class CacheTests {
        private Cache cache;

        @BeforeEach
        void setUp() {
            cache = mock(Cache.class);
            when(cacheManager.getCache(CachingConfig.LIST_OF_FIRMS_CACHE)).thenReturn(cache);
        }

        @Test
        void getAllFirmsFromCache_WhenCacheMiss_ShouldFetchFromRepository() {
            // Arrange
            Firm firm1 = Firm.builder().id(UUID.randomUUID()).name("Firm 1").build();
            Firm firm2 = Firm.builder().id(UUID.randomUUID()).name("Firm 2").build();
            List<Firm> dbFirms = List.of(firm1, firm2);

            when(cache.get("all_firms", List.class)).thenReturn(null);
            when(firmRepository.findAll()).thenReturn(dbFirms);

            // Act
            List<FirmDto> result = firmService.getAllFirmsFromCache();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(FirmDto::getName).containsExactly("Firm 1", "Firm 2");
            verify(firmRepository).findAll();
            verify(cache).put(eq("all_firms"), anyList());
        }

        @Test
        void getAllFirmsFromCache_WhenCacheHit_ShouldReturnCachedValue() {
            // Arrange
            FirmDto cachedFirm1 = new FirmDto();
            cachedFirm1.setId(UUID.randomUUID());
            cachedFirm1.setName("Cached Firm 1");

            List<FirmDto> cachedFirms = List.of(cachedFirm1);
            ValueWrapper valueWrapper = mock(ValueWrapper.class);
            when(valueWrapper.get()).thenReturn(cachedFirms);
            when(cache.get("all_firms")).thenReturn(valueWrapper);

            // Act
            List<FirmDto> result = firmService.getAllFirmsFromCache();

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getName()).isEqualTo("Cached Firm 1");
            verify(firmRepository, never()).findAll();
            verify(cache, never()).put(anyString(), any());
        }

        @Test
        void getAllFirmsFromCache_WhenCacheIsNull_ShouldFetchFromRepository() {
            // Arrange
            when(cacheManager.getCache(CachingConfig.LIST_OF_FIRMS_CACHE)).thenReturn(null);
            Firm firm = Firm.builder().id(UUID.randomUUID()).name("Test Firm").build();
            when(firmRepository.findAll()).thenReturn(List.of(firm));

            // Act
            List<FirmDto> result = firmService.getAllFirmsFromCache();

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getName()).isEqualTo("Test Firm");
        }

        @Test
        void getAllFirmsFromCache_WhenCacheThrowsException_ShouldLogAndReturnEmptyList() {
            // Arrange
            when(cache.get("all_firms", List.class)).thenThrow(new RuntimeException("Cache error"));
            when(firmRepository.findAll()).thenReturn(List.of());

            // Act
            List<FirmDto> result = firmService.getAllFirmsFromCache();

            // Assert
            assertThat(result).isEmpty();
            verify(firmRepository).findAll();
        }

        @Test
        public void shouldReturnAllFirms_whenSearchTermIsNull() {
            List<Firm> cachedFirms = Arrays.asList(
                    Firm.builder().name("Alpha").code("A001").build(),
                    Firm.builder().name("Beta").code("B002").build()
            );
            when(cache.get("all_firms", List.class)).thenReturn(null);
            when(firmRepository.findAll()).thenReturn(cachedFirms);

            List<FirmDto> result = firmService.searchFirms(null);

            assertThat(result)
                    .hasSize(2)
                    .extracting(FirmDto::getName)
                    .containsExactly("Alpha", "Beta");
        }

        @Test
        public void shouldReturnAllFirms_whenSearchTermIsEmpty() {
            List<Firm> cachedFirms = Collections.singletonList(
                    Firm.builder().name("Alpha").code("A001").build()
            );
            when(cache.get("all_firms", List.class)).thenReturn(null);
            when(firmRepository.findAll()).thenReturn(cachedFirms);

            List<FirmDto> result = firmService.searchFirms("   ");

            assertThat(result)
                    .hasSize(1)
                    .extracting(FirmDto::getName)
                    .containsExactly("Alpha");
        }

    }

}