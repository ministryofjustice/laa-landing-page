package uk.gov.justice.laa.portal.landingpage.service.firmdirectory;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
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
import java.util.Optional;
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
    private final FirmRepository firmRepository;

    public LocalFirmDirectoryProvider(FirmService firmService, OfficeService officeService,
                                      UserAccountStatusService userAccountStatusService,
                                      FirmRepository firmRepository) {
        this.firmService = firmService;
        this.officeService = officeService;
        this.userAccountStatusService = userAccountStatusService;
        this.firmRepository = firmRepository;
    }

    @Override
    public PaginatedFirmDirectory getFirmsPage(String firmSearch, String firmType, int page, int pageSize,
                                               String sort, String direction) {
        return firmService.getFirmsPage(firmSearch, firmType, page, pageSize, sort, direction);
    }

    @Override
    public List<FirmDto> searchFirms(String query) {
        return firmService.searchFirms(query);
    }

    @Override
    public FirmDto getFirm(String firmId) {
        return firmService.getFirm(parseFirmId(firmId));
    }

    @Override
    public PaginatedOffices getOfficesPage(String firmId, int page, int pageSize, String sort, String direction) {
        return officeService.getOfficesPage(parseFirmId(firmId), page, pageSize, sort, direction);
    }

    @Override
    public void bulkDeactivateFirmUsers(String firmId, Supplier<UUID> disabledByUserIdSupplier) {
        userAccountStatusService.disableUserAllUserByFirmIdWithCyberRisk(firmId, disabledByUserIdSupplier.get());
    }

    @Override
    public Optional<UUID> findLocalFirmId(String firmId) {
        return Optional.of(parseFirmId(firmId));
    }

    @Override
    public boolean hasActiveUsers(String firmId) {
        return userAccountStatusService.hasActiveUserByFirmId(parseFirmId(firmId).toString());
    }

    @Override
    public Map<String, Long> getUserCounts(String firmId) {
        return userAccountStatusService.getUserCountsForFirm(parseFirmId(firmId).toString());
    }

    @Override
    public String validateFirmId(String firmId) {
        return parseFirmId(firmId).toString();
    }

    /**
     * Accepts either the firm's UUID or its unique firm code, so URLs work the same as on the data API path.
     */
    private UUID parseFirmId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return Optional.ofNullable(firmRepository.findByCode(id))
                    .map(Firm::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Firm not found"));
        }
    }
}
