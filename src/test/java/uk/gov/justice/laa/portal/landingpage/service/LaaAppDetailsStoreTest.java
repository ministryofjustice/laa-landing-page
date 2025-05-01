package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.utils.HashUtil;

import com.microsoft.graph.models.Application;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LaaAppDetailsStoreTest {

    private LaaAppDetailsStore store;

    @BeforeEach
    void setUp() {
        store = new LaaAppDetailsStore();
    }

    @Test
    void testGetUserAssignedAppsFiltersMatchingApplications() {

        // Arrange
        LaaApplication app1 = LaaApplication.builder()
                .id(HashUtil.sha256("id1"))
                .oidGroupName("groupA")
                .title("Title A")
                .description("Desc A")
                .url("http://example.com/A")
                .build();
        LaaApplication app2 = LaaApplication.builder()
                .id(HashUtil.sha256("id2"))
                .oidGroupName("groupB")
                .title("Title B")
                .description("Desc B")
                .url("http://example.com/B")
                .build();

        ReflectionTestUtils.setField(
                LaaAppDetailsStore.class,
                "laaApplications",
                Arrays.asList(app1, app2)
        );

        Application graphApp = new Application();
        graphApp.setAppId("id1");

        // Act
        List<LaaApplication> result = LaaAppDetailsStore.getUserAssignedApps(
                List.of(graphApp)
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(app1.getId(), result.getFirst().getId());
    }

    @Test
    void testPopulateLaaAppsLoadsData() throws IOException {

        // Arrange
        store.populateLaaApps();

        // Act
        @SuppressWarnings("unchecked")
        List<LaaApplication> apps = (List<LaaApplication>) ReflectionTestUtils
                .getField(LaaAppDetailsStore.class, "laaApplications");

        // Assert
        assertNotNull(apps);
        assertFalse(apps.isEmpty());
    }
}
