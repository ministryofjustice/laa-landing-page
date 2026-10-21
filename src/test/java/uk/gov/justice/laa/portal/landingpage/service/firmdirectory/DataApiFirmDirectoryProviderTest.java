package uk.gov.justice.laa.portal.landingpage.service.firmdirectory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.datauserapi.contracts.domain.DeactivateUserReason;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.service.FirmDirectoryApiService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataApiFirmDirectoryProviderTest {

    @Mock
    private FirmDirectoryApiService firmDirectoryApiService;

    @Mock
    private FirmRepository firmRepository;

    private DataApiFirmDirectoryProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DataApiFirmDirectoryProvider(firmDirectoryApiService, firmRepository);
    }

    @Test
    void getFirmsPageDelegatesToFirmDirectoryApiService() {
        PaginatedFirmDirectory expected = PaginatedFirmDirectory.builder().build();
        when(firmDirectoryApiService.getFirmsPage("search", "LEGAL_SERVICES_PROVIDER", 1, 10, "name", "asc"))
                .thenReturn(expected);

        PaginatedFirmDirectory result = provider.getFirmsPage("search", "LEGAL_SERVICES_PROVIDER", 1, 10, "name", "asc");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getFirmDelegatesToFirmDirectoryApiService() {
        FirmDto expected = FirmDto.builder().build();
        when(firmDirectoryApiService.getFirm("firm-code")).thenReturn(expected);

        assertThat(provider.getFirm("firm-code")).isSameAs(expected);
    }

    @Test
    void getFirmUnvalidatedDelegatesToFirmDirectoryApiService() {
        FirmDto expected = FirmDto.builder().build();
        when(firmDirectoryApiService.getFirm("firm-code")).thenReturn(expected);

        assertThat(provider.getFirmUnvalidated("firm-code")).isSameAs(expected);
    }

    @Test
    void getOfficesPageDelegatesToFirmDirectoryApiServiceIgnoringSortAndDirection() {
        PaginatedOffices expected = PaginatedOffices.builder().build();
        when(firmDirectoryApiService.getOfficesPage("firm-code", 1, 10)).thenReturn(expected);

        PaginatedOffices result = provider.getOfficesPage("firm-code", 1, 10, "name", "asc");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void bulkDeactivateFirmUsersDelegatesWithCyberRiskReasonAndNeverTouchesSupplier() {
        UUID firmId = UUID.randomUUID();

        provider.bulkDeactivateFirmUsers(firmId, () -> {
            throw new AssertionError("Supplier should not be invoked by the data API provider");
        });

        verify(firmDirectoryApiService).bulkDeactivateFirmUsers(firmId, DeactivateUserReason.CyberRisk);
    }

    @Test
    void supportsBulkDisableButtonReturnsFalse() {
        assertThat(provider.supportsBulkDisableButton()).isFalse();
    }

    @Test
    void findLocalFirmIdTranslatesFirmCodeToLocalUuid() {
        UUID id = UUID.randomUUID();
        when(firmRepository.findByCode("25966")).thenReturn(Firm.builder().id(id).build());

        assertThat(provider.findLocalFirmId("25966")).contains(id);
    }

    @Test
    void findLocalFirmIdIsEmptyWhenFirmCodeUnknownLocally() {
        assertThat(provider.findLocalFirmId("unknown")).isEmpty();
    }
}
