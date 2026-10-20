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
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;

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

    private LocalFirmDirectoryProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LocalFirmDirectoryProvider(firmService, officeService, userAccountStatusService);
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
    void getFirmUnvalidatedDelegatesDirectlyToFirmServiceStringOverload() {
        FirmDto expected = FirmDto.builder().build();
        when(firmService.getFirm("some-id")).thenReturn(expected);

        FirmDto result = provider.getFirmUnvalidated("some-id");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getFirmUnvalidatedPropagatesRawExceptionForInvalidId() {
        when(firmService.getFirm("not-a-uuid")).thenThrow(new IllegalArgumentException("Invalid UUID"));

        assertThatThrownBy(() -> provider.getFirmUnvalidated("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
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
        UUID firmId = UUID.randomUUID();
        UUID disabledByUserId = UUID.randomUUID();

        provider.bulkDeactivateFirmUsers(firmId, () -> disabledByUserId);

        verify(userAccountStatusService).disableUserAllUserByFirmIdWithCyberRisk(eq(firmId.toString()),
                eq(disabledByUserId));
    }

    @Test
    void supportsBulkDisableButtonReturnsTrue() {
        assertThat(provider.supportsBulkDisableButton()).isTrue();
    }
}
