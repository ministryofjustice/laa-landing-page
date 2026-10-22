package uk.gov.justice.laa.portal.landingpage.service.firmdirectory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.justice.laa.datauserapi.contracts.domain.DeactivateUserReason;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.service.FirmDirectoryApiService;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataApiFirmDirectoryProviderTest {

    private static final String FIRM_CODE = "25966";

    @Mock
    private FirmDirectoryApiService firmDirectoryApiService;

    @Mock
    private FirmRepository firmRepository;

    @Mock
    private UserAccountStatusService userAccountStatusService;

    private DataApiFirmDirectoryProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DataApiFirmDirectoryProvider(firmDirectoryApiService, firmRepository, userAccountStatusService);
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
    void getOfficesPageDelegatesToFirmDirectoryApiServiceWithSortAndDirection() {
        PaginatedOffices expected = PaginatedOffices.builder().build();
        when(firmDirectoryApiService.getOfficesPage("firm-code", 1, 10, "code", "desc")).thenReturn(expected);

        PaginatedOffices result = provider.getOfficesPage("firm-code", 1, 10, "code", "desc");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void bulkDeactivateFirmUsersDelegatesWithCyberRiskReasonAndNeverTouchesSupplier() {
        provider.bulkDeactivateFirmUsers(FIRM_CODE, () -> {
            throw new AssertionError("Supplier should not be invoked by the data API provider");
        });

        verify(firmDirectoryApiService).bulkDeactivateFirmUsers(FIRM_CODE, DeactivateUserReason.CyberRisk);
    }

    @Test
    void validateFirmIdAcceptsNumericFirmCode() {
        assertThat(provider.validateFirmId(FIRM_CODE)).isEqualTo(FIRM_CODE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-code", "../evil", "123 456", "123456-1"})
    void validateFirmIdRejectsNonNumericValues(String firmId) {
        assertThatThrownBy(() -> provider.validateFirmId(firmId)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void validateFirmIdRejectsUuid() {
        assertThatThrownBy(() -> provider.validateFirmId(UUID.randomUUID().toString()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void searchFirmsDelegatesToFirmDirectoryApiService() {
        List<FirmDto> expected = List.of(FirmDto.builder().code("25966").build());
        when(firmDirectoryApiService.searchFirms("abc", 10)).thenReturn(expected);

        assertThat(provider.searchFirms("abc")).isSameAs(expected);
    }

    @Nested
    class WhenFirmIsKnownLocally {

        private final UUID localId = UUID.randomUUID();

        @BeforeEach
        void stubLocalFirm() {
            when(firmRepository.findByCode(FIRM_CODE)).thenReturn(Firm.builder().id(localId).build());
        }

        @Test
        void findLocalFirmIdTranslatesFirmCodeToLocalUuid() {
            assertThat(provider.findLocalFirmId(FIRM_CODE)).contains(localId);
        }

        @Test
        void hasActiveUsersUsesLocalFirmUuid() {
            when(userAccountStatusService.hasActiveUserByFirmId(localId.toString())).thenReturn(true);

            assertThat(provider.hasActiveUsers(FIRM_CODE)).isTrue();
        }

        @Test
        void getUserCountsUsesLocalFirmUuid() {
            Map<String, Long> counts = Map.of("totalOfSingleFirm", 3L);
            when(userAccountStatusService.getUserCountsForFirm(localId.toString())).thenReturn(counts);

            assertThat(provider.getUserCounts(FIRM_CODE)).isSameAs(counts);
        }
    }

    @Nested
    class WhenFirmIsNotKnownLocally {

        @Test
        void findLocalFirmIdIsEmpty() {
            assertThat(provider.findLocalFirmId(FIRM_CODE)).isEmpty();
        }

        @Test
        void hasActiveUsersIsFalse() {
            assertThat(provider.hasActiveUsers(FIRM_CODE)).isFalse();
        }

        @Test
        void getUserCountsIsEmpty() {
            assertThat(provider.getUserCounts(FIRM_CODE)).isEmpty();
        }
    }
}
