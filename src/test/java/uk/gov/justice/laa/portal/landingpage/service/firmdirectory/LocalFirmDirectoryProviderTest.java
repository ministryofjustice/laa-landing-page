package uk.gov.justice.laa.portal.landingpage.service.firmdirectory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalFirmDirectoryProviderTest {

    @Mock
    private FirmService firmService;
    @Mock
    private OfficeService officeService;
    @Mock
    private UserAccountStatusService userAccountStatusService;
    @Mock
    private FirmRepository firmRepository;

    private LocalFirmDirectoryProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LocalFirmDirectoryProvider(firmService, officeService, userAccountStatusService, firmRepository);
    }

    @Test
    void getFirmsPageDelegatesToFirmService() {
        PaginatedFirmDirectory expected = PaginatedFirmDirectory.builder().build();
        when(firmService.getFirmsPage("search", "LEGAL_SERVICES_PROVIDER", 1, 10, "name", "asc"))
                .thenReturn(expected);

        PaginatedFirmDirectory result = provider.getFirmsPage("search", "LEGAL_SERVICES_PROVIDER", 1, 10, "name", "asc");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getFirmParsesUuidAndDelegatesToFirmService() {
        UUID id = UUID.randomUUID();
        FirmDto expected = FirmDto.builder().build();
        when(firmService.getFirm(id)).thenReturn(expected);

        FirmDto result = provider.getFirm(id.toString());

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getFirmThrowsNotFoundForInvalidId() {
        assertThatThrownBy(() -> provider.getFirm("not-a-uuid"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Firm not found");
    }

    @Test
    void getFirmResolvesFirmCodeToUuid() {
        UUID id = UUID.randomUUID();
        FirmDto expected = FirmDto.builder().build();
        when(firmRepository.findByCode("25966")).thenReturn(Firm.builder().id(id).build());
        when(firmService.getFirm(id)).thenReturn(expected);

        assertThat(provider.getFirm("25966")).isSameAs(expected);
    }

    @Test
    void getOfficesPageResolvesFirmCodeToUuid() {
        UUID id = UUID.randomUUID();
        PaginatedOffices expected = PaginatedOffices.builder().build();
        when(firmRepository.findByCode("25966")).thenReturn(Firm.builder().id(id).build());
        when(officeService.getOfficesPage(id, 1, 10, "code", "asc")).thenReturn(expected);

        assertThat(provider.getOfficesPage("25966", 1, 10, "code", "asc")).isSameAs(expected);
    }

    @Test
    void validateFirmIdResolvesFirmCodeToUuidString() {
        UUID id = UUID.randomUUID();
        when(firmRepository.findByCode("25966")).thenReturn(Firm.builder().id(id).build());

        assertThat(provider.validateFirmId("25966")).isEqualTo(id.toString());
    }

    @Test
    void findLocalFirmIdResolvesFirmCode() {
        UUID id = UUID.randomUUID();
        when(firmRepository.findByCode("25966")).thenReturn(Firm.builder().id(id).build());

        assertThat(provider.findLocalFirmId("25966")).contains(id);
    }

    @Test
    void getOfficesPageParsesUuidAndDelegatesToOfficeService() {
        UUID id = UUID.randomUUID();
        PaginatedOffices expected = PaginatedOffices.builder().build();
        when(officeService.getOfficesPage(id, 1, 10, "name", "asc")).thenReturn(expected);

        PaginatedOffices result = provider.getOfficesPage(id.toString(), 1, 10, "name", "asc");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void bulkDeactivateFirmUsersResolvesSupplierAndDelegatesToUserAccountStatusService() {
        String firmId = UUID.randomUUID().toString();
        UUID disabledByUserId = UUID.randomUUID();

        provider.bulkDeactivateFirmUsers(firmId, () -> disabledByUserId);

        verify(userAccountStatusService).disableUserAllUserByFirmIdWithCyberRisk(eq(firmId),
                eq(disabledByUserId));
    }

    @Test
    void validateFirmIdReturnsCanonicalUuid() {
        UUID id = UUID.randomUUID();

        assertThat(provider.validateFirmId(id.toString())).isEqualTo(id.toString());
    }

    @Test
    void validateFirmIdRejectsUnknownFirmCode() {
        assertThatThrownBy(() -> provider.validateFirmId("25966"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void findLocalFirmIdReturnsParsedUuid() {
        UUID id = UUID.randomUUID();

        assertThat(provider.findLocalFirmId(id.toString())).contains(id);
    }

    @Test
    void searchFirmsDelegatesToFirmService() {
        List<FirmDto> expected = List.of(FirmDto.builder().name("Firm A").build());
        when(firmService.searchFirms("abc")).thenReturn(expected);

        assertThat(provider.searchFirms("abc")).isSameAs(expected);
    }

    @Test
    void hasActiveUsersDelegatesWithResolvedFirmUuid() {
        UUID id = UUID.randomUUID();
        when(userAccountStatusService.hasActiveUserByFirmId(id.toString())).thenReturn(true);

        assertThat(provider.hasActiveUsers(id.toString())).isTrue();
    }

    @Test
    void getUserCountsDelegatesWithResolvedFirmUuid() {
        UUID id = UUID.randomUUID();
        Map<String, Long> counts = Map.of("totalOfMultiFirm", 2L);
        when(userAccountStatusService.getUserCountsForFirm(id.toString())).thenReturn(counts);

        assertThat(provider.getUserCounts(id.toString())).isSameAs(counts);
    }
}
