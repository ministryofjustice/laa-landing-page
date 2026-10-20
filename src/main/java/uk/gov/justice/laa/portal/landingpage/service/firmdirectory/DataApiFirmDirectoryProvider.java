package uk.gov.justice.laa.portal.landingpage.service.firmdirectory;

import org.springframework.stereotype.Component;
import uk.gov.justice.laa.datauserapi.contracts.domain.DeactivateUserReason;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.service.FirmDirectoryApiService;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * {@link FirmDirectoryProvider} backed by laa-data-user-api, used when the firm
 * directory data API feature flag is on.
 */
@Component
public class DataApiFirmDirectoryProvider implements FirmDirectoryProvider {

    private final FirmDirectoryApiService firmDirectoryApiService;

    public DataApiFirmDirectoryProvider(FirmDirectoryApiService firmDirectoryApiService) {
        this.firmDirectoryApiService = firmDirectoryApiService;
    }

    @Override
    public PaginatedFirmDirectory getFirmsPage(String firmSearch, String firmType, int page, int pageSize,
                                               String sort, String direction) {
        return firmDirectoryApiService.getFirmsPage(firmSearch, firmType, page, pageSize, sort, direction);
    }

    @Override
    public FirmDto getFirm(String firmId) {
        return firmDirectoryApiService.getFirm(firmId);
    }

    @Override
    public FirmDto getFirmUnvalidated(String firmId) {
        return firmDirectoryApiService.getFirm(firmId);
    }

    @Override
    public PaginatedOffices getOfficesPage(String firmId, int page, int pageSize, String sort, String direction) {
        return firmDirectoryApiService.getOfficesPage(firmId, page, pageSize);
    }

    @Override
    public void bulkDeactivateFirmUsers(UUID firmId, Supplier<UUID> disabledByUserIdSupplier) {
        firmDirectoryApiService.bulkDeactivateFirmUsers(firmId, DeactivateUserReason.CyberRisk);
    }

    @Override
    public boolean supportsBulkDisableButton() {
        // Bulk disable is not available on the API path: the API identifies firms by code, not UUID
        return false;
    }
}
