package uk.gov.justice.laa.portal.landingpage.controller;

import com.microsoft.graph.models.AppRoleAssignment;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.service.LaaAppDetailsStore;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to exercise loading of applications metadata.
 */
public class LaaAppDetailsStoreTest extends BaseIntegrationTest {

    @Test
    public void noUserAssignedApps() {
        assertThat(LaaAppDetailsStore.getUserAssignedApps(Collections.emptyList())).isEmpty();
    }

    @Test
    public void userAssignedApps() {
        AppRoleAssignment ara1 = new AppRoleAssignment();
        ara1.setResourceId(UUID.fromString("4a191f71-9c7a-4c50-a588-2621890d6dc0"));
        AppRoleAssignment ara2 = new AppRoleAssignment();
        ara2.setResourceId(UUID.fromString("1a126a83-004a-4226-9ca5-ac75be57cd4c"));

        List<AppRoleAssignment> appRoleAssignments = List.of(ara1, ara2);
        Set<LaaApplication> userAssignedApps = LaaAppDetailsStore.getUserAssignedApps(appRoleAssignments);
        assertThat(userAssignedApps).isNotEmpty();
        assertThat(userAssignedApps).hasSize(2);
    }


}
