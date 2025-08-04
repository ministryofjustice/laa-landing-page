package uk.gov.justice.laa.portal.landingpage.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cache.CacheManager;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
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
        String searchTerm = "Smith & Associates";
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
        assertThat(result.get(0).getName()).isEqualTo("ABC Legal Services");
        verify(firmRepository).findByNameOrCodeContaining(trimmedSearchTerm);
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
        when(firmRepository.findByNameOrCodeContaining(searchTerm)).thenReturn(List.of());

        // When
        List<FirmDto> result = firmService.searchFirms(searchTerm);

        // Then
        assertThat(result).isEmpty();
        verify(firmRepository).findByNameOrCodeContaining(searchTerm);
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

        when(firmRepository.findByNameOrCodeContaining(searchTerm)).thenReturn(searchResults);

        // When
        List<FirmDto> result = firmService.searchFirms(searchTerm);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("ABC001");
        verify(firmRepository).findByNameOrCodeContaining(searchTerm);
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
}