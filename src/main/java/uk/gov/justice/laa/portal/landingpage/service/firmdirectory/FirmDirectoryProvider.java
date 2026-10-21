package uk.gov.justice.laa.portal.landingpage.service.firmdirectory;

import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Strategy for Firm Directory data access. Implementations provide the firm/office
 * data from a particular source (e.g. the local database or laa-data-user-api),
 * selected at runtime by {@link FirmDirectoryProviderResolver}.
 */
public interface FirmDirectoryProvider {

    /**
     * @param page 1-based page number as used by the UI
     * @param firmType the firm type to filter by, or null for no filtering
     */
    PaginatedFirmDirectory getFirmsPage(String firmSearch, String firmType, int page, int pageSize,
                                         String sort, String direction);

    /**
     * Strict lookup used by the firm details page: an unknown/invalid id results in a
     * 404 rather than propagating a raw exception.
     */
    FirmDto getFirm(String firmId);

    /**
     * Lookup used by the bulk-disable confirmation page. Preserved separately from
     * {@link #getFirm(String)} because it mirrors pre-existing behaviour that does not
     * translate an invalid id into a 404.
     */
    FirmDto getFirmUnvalidated(String firmId);

    /**
     * @param page 1-based page number as used by the UI
     */
    PaginatedOffices getOfficesPage(String firmId, int page, int pageSize, String sort, String direction);

    /**
     * @param disabledByUserIdSupplier supplies the id of the user performing the deactivation;
     *                                 evaluated lazily so implementations that don't need it
     *                                 (e.g. the data API) never invoke it
     */
    void bulkDeactivateFirmUsers(UUID firmId, Supplier<UUID> disabledByUserIdSupplier);

    /**
     * @param firmId the firm identifier used by this provider (UUID locally, firm code on the data API)
     * @return the local firm UUID, as required by the user search filters, if one exists
     */
    Optional<UUID> findLocalFirmId(String firmId);

    /**
     * @return whether the "remove access for all users" button is available for this provider
     */
    boolean supportsBulkDisableButton();
}
