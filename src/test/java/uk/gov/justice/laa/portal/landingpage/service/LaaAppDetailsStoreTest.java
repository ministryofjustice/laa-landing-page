package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.models.Application;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LaaAppDetailsStoreTest {

    private LaaAppDetailsStore store;

    @BeforeEach
    void bootstrapCsv() throws IOException {
        store = new LaaAppDetailsStore();
        store.populateLaaApps();      // mimic @PostConstruct
    }

    @AfterEach
    void tearDown() {
        // clear static cache to avoid cross-test leakage
        org.springframework.test.util.ReflectionTestUtils
                .setField(LaaAppDetailsStore.class, "laaApplications", null);
    }

    @Test
    void findsMatchingApp_bySha256() {
        Application a = new Application();
        a.setAppId("698815d2-5760-4fd0-bdef-54c683e91b26"); // present in CSV

        List<?> result = LaaAppDetailsStore.getUserAssignedApps(List.of(a));
        assertThat(result).hasSize(1);
    }
}