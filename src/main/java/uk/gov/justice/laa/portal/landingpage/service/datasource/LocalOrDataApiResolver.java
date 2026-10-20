package uk.gov.justice.laa.portal.landingpage.service.datasource;

import java.util.function.BooleanSupplier;

/**
 * Generic per-call resolver between a "local" data source and a "data API" data source, for
 * any feature that exposes the same functionality through two interchangeable providers (e.g.
 * the local database vs laa-data-user-api). This is the reusable building block behind
 * feature-specific resolvers such as
 * {@code uk.gov.justice.laa.portal.landingpage.service.firmdirectory.FirmDirectoryProviderResolver};
 * any future feature that migrates to the data API can adopt the same local/data-api Strategy
 * by wrapping its own provider pair in an instance of this class.
 *
 * <p>The flag supplier is evaluated on every {@link #resolve()} call (not once at startup) so
 * that runtime flag changes (e.g. via Unleash) take effect immediately.
 *
 * @param <T> the provider interface shared by both the local and data-api implementations
 */
public class LocalOrDataApiResolver<T> {

    private final T localProvider;
    private final T dataApiProvider;
    private final BooleanSupplier useDataApi;

    public LocalOrDataApiResolver(T localProvider, T dataApiProvider, BooleanSupplier useDataApi) {
        this.localProvider = localProvider;
        this.dataApiProvider = dataApiProvider;
        this.useDataApi = useDataApi;
    }

    public T resolve() {
        return useDataApi.getAsBoolean() ? dataApiProvider : localProvider;
    }
}
