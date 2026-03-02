package uk.gov.justice.laa.portal.landingpage.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeleteAppRoleEventTest {

    @Test
    @DisplayName("Constructor correctly assigns all fields including inherited userId")
    void constructorAssignsFieldsCorrectly() {
        UUID modifierUserId = UUID.randomUUID();
        UUID entraUserId = UUID.randomUUID();
        String appName = "MyApp";
        String appRole = "Admin";
        String reason = "Cleanup";

        DeleteAppRoleEvent event = new DeleteAppRoleEvent(modifierUserId, entraUserId, appName, appRole, reason);

        // Check inherited userId
        assertThat(event.getUserId()).isEqualTo(modifierUserId);

        // Check own fields
        assertThat(event.getEntraUserId()).isEqualTo(entraUserId);
        assertThat(event.getAppName()).isEqualTo(appName);
        assertThat(event.getAppRoleName()).isEqualTo(appRole);
        assertThat(event.getReason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("getEventType returns DELETE_LAA_APP_ROLE")
    void eventTypeIsCorrect() {
        DeleteAppRoleEvent event = new DeleteAppRoleEvent(UUID.randomUUID(), UUID.randomUUID(), "A", "B", "C");

        assertThat(event.getEventType()).isEqualTo(EventType.DELETE_LAA_APP_ROLE);
    }

    @Test
    @DisplayName("getDescription returns correct formatted string")
    void descriptionIsCorrect() {
        UUID modifierUserId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        UUID entraUserId = UUID.fromString("ffffffff-1111-2222-3333-444444444444");

        DeleteAppRoleEvent event = new DeleteAppRoleEvent(modifierUserId, entraUserId, "CaseApp", "Manager", "Role deprecated");

        assertThat(event.getDescription()).isEqualTo("User 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee' "
                + "(Entra OID: ffffffff-1111-2222-3333-444444444444) " + "has deleted app role Manager from app CaseApp. Reason: Role deprecated");
    }

    @Test
    @DisplayName("Allows null entraUserId in description")
    void descriptionAllowsNullEntraUserId() {
        UUID modifierUserId = UUID.randomUUID();

        DeleteAppRoleEvent event = new DeleteAppRoleEvent(modifierUserId, null, "MyApp", "Admin", "Test reason");

        assertThat(event.getDescription()).isEqualTo(String.format("User '%s' (Entra OID: null) has deleted app role Admin from app MyApp. Reason: Test reason", modifierUserId));
    }

    @Test
    @DisplayName("Allows null appRoleName in description")
    void descriptionAllowsNullAppRole() {
        DeleteAppRoleEvent event = new DeleteAppRoleEvent(UUID.randomUUID(), UUID.randomUUID(), "MyApp", null, "Because");

        assertThat(event.getDescription()).contains("has deleted app role null from app MyApp. Reason: Because");
    }

    @Test
    @DisplayName("Allows null appName in description")
    void descriptionAllowsNullAppName() {
        DeleteAppRoleEvent event = new DeleteAppRoleEvent(UUID.randomUUID(), UUID.randomUUID(), null, "Admin", "Cleanup");

        assertThat(event.getDescription()).contains("Admin from app null. Reason: Cleanup");
    }

    @Test
    @DisplayName("Allows null reason in description")
    void descriptionAllowsNullReason() {
        DeleteAppRoleEvent event = new DeleteAppRoleEvent(UUID.randomUUID(), UUID.randomUUID(), "MyApp", "Admin", null);

        assertThat(event.getDescription()).endsWith("Reason: null");
    }

    @Test
    @DisplayName("Allows null userId (modifierUserId) in description")
    void descriptionAllowsNullModifierUserId() {
        DeleteAppRoleEvent event = new DeleteAppRoleEvent(null, UUID.randomUUID(), "AppX", "Viewer", "Testing");

        assertThat(event.getDescription()).startsWith("User 'null' (Entra OID:");
    }
}
