package uk.gov.justice.laa.portal.landingpage.service.datasource;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class LocalOrDataApiResolverTest {

    @Test
    void resolvesToDataApiProviderWhenFlagTrue() {
        LocalOrDataApiResolver<String> resolver = new LocalOrDataApiResolver<>("local", "dataApi", () -> true);

        assertThat(resolver.resolve()).isEqualTo("dataApi");
    }

    @Test
    void resolvesToLocalProviderWhenFlagFalse() {
        LocalOrDataApiResolver<String> resolver = new LocalOrDataApiResolver<>("local", "dataApi", () -> false);

        assertThat(resolver.resolve()).isEqualTo("local");
    }

    @Test
    void evaluatesFlagOnEveryCallRatherThanCachingAtConstruction() {
        AtomicBoolean flag = new AtomicBoolean(false);
        LocalOrDataApiResolver<String> resolver = new LocalOrDataApiResolver<>("local", "dataApi", flag::get);

        assertThat(resolver.resolve()).isEqualTo("local");

        flag.set(true);

        assertThat(resolver.resolve()).isEqualTo("dataApi");
    }
}
