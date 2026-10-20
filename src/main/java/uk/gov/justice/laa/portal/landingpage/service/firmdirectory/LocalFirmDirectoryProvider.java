package uk.gov.justice.laa.portal.landingpage.service.firmdirectory;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * {@link FirmDirectoryProvider} backed by the local database, used when the firm
 * directory data API feature flag is off.
 */
@Component
public class LocalFirmDirectoryProvider implements FirmDirectoryProvider {

    private final FirmService firmService;
    private final OfficeService officeService;
    private final UserAccountStatusService userAccountStatusService;

    public LocalFirmDirectoryProvider(FirmService firmService, OfficeService officeService,
                                      UserAccountStatusService userAccountStatusService) {
        this.firmService = firmService;
        this.officeService = officeService;
        this.userAccountStatusService = userAccountStatusService;
    }

    @Override
    public PaginatedFirmDirectory getFirmsPage(String firmSearch, String firmType, int page, int pageSize,
                                               String sort, String direction) {
        return firmService.getFirmsPage(firmSearch, firmType, page, pageSize, sort, direction);
    }

    @Override
    public FirmDto getFirm(String firmId) {
        return firmService.getFirm(parseFirmId(firmId));
    }

    @Override
    public FirmDto getFirmUnvalidated(String firmId) {
        return firmService.getFirm(firmId);
    }

    @Override
    public PaginatedOffices getOfficesPage(String firmId, int page, int pageSize, String sort, String direction) {
        return officeService.getOfficesPage(parseFirmId(firmId), page, pageSize, sort, direction);
    }

    @Override
    public void bulkDeactivateFirmUsers(UUID firmId, Supplier<UUID> disabledByUserIdSupplier) {
        userAccountStatusService.disableUserAllUserByFirmIdWithCyberRisk(firmId.toString(),
                disabledByUserIdSupplier.get());
    }

    @Override
    public boolean supportsBulkDisableButton() {
        return true;
    }

    private UUID parseFirmId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Firm not found", ex);
        }
    }
}
