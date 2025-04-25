package uk.gov.justice.laa.portal.landingpage.controller;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.junit.jupiter.api.Test;
import com.microsoft.graph.models.Application;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.service.LaaAppDetailsStore;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to exercise loading of applications metadata.
 */
public class LaaAppDetailsStoreTest extends BaseIntegrationTest {

    @MockitoBean
    private GraphServiceClient graphServiceClient;

    @Test
    public void noUserAssignedApps() {
        assertThat(LaaAppDetailsStore.getUserAssignedApps(Collections.emptyList())).isEmpty();
    }

    @Test
    public void userAssignedApps() {
        Application ara1 = new Application();
        ara1.setAppId("4a191f71-9c7a-4c50-a588-2621890d6dc0");
        Application ara2 = new Application();
        ara2.setAppId("1a126a83-004a-4226-9ca5-ac75be57cd4c");
        List<Application> appRoleAssignments = List.of(ara1, ara2);

        List<LaaApplication> userAssignedApps = LaaAppDetailsStore.getUserAssignedApps(appRoleAssignments);

        assertThat(userAssignedApps).isNotEmpty();
        assertThat(userAssignedApps).hasSize(2);
    }


}
