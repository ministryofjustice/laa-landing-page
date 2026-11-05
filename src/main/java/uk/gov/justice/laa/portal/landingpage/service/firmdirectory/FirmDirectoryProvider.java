package uk.gov.justice.laa.portal.landingpage.service.firmdirectory;

import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;

import java.util.List;
import java.util.Map;
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
     * Type-ahead suggestions for the firm search box.
     */
    List<FirmDto> searchFirms(String query);

    /**
     * An unknown or invalid id results in a 404 rather than propagating a raw exception.
     */
    FirmDto getFirm(String firmId);

    /**
     * @param page 1-based page number as used by the UI
     */
    PaginatedOffices getOfficesPage(String firmId, int page, int pageSize, String sort, String direction);

    /**
     * @param firmId the firm identifier used by this provider, as returned by {@link #validateFirmId(String)}
     * @param disabledByUserIdSupplier supplies the id of the user performing the deactivation;
     *                                 evaluated lazily so implementations that don't need it
     *                                 (e.g. the data API) never invoke it
     */
    void bulkDeactivateFirmUsers(String firmId, Supplier<UUID> disabledByUserIdSupplier);

    /**
     * @param firmId the firm identifier used by this provider (UUID locally, firm code on the data API)
     * @return the local firm UUID, as required by the user search filters, if one exists
     */
    Optional<UUID> findLocalFirmId(String firmId);

    /**
     * @return whether the firm has active users, which decides if "remove access for all" is offered.
     *         Users are not exposed by the data API, so every provider reads this locally.
     */
    boolean hasActiveUsers(String firmId);

    /**
     * @return user counts for the bulk deactivation confirmation, keyed {@code totalOfSingleFirm} and
     *         {@code totalOfMultiFirm}; a key is absent when there are none
     */
    Map<String, Long> getUserCounts(String firmId);

    /**
     * Checks that the identifier is valid for this provider (a firm UUID or code locally, a numeric firm
     * code on the data API) so it is safe to use in redirects and downstream calls.
     *
     * @return the identifier to use from here on (locally, always the firm UUID)
     * @throws org.springframework.web.server.ResponseStatusException with 404 when the format is invalid
     */
    String validateFirmId(String firmId);
}
