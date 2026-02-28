package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

class DeleteFirmProfileAuditEventTest {

    @Test
    void testAuditEventFieldsAndMethods() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        String entraOid = UUID.randomUUID().toString();
        int removedRolesCount = 3;
        int detachedOfficesCount = 2;

        // Act
        DeleteFirmProfileAuditEvent event = new DeleteFirmProfileAuditEvent(
                userId, userProfileId, entraOid, firmId,
                removedRolesCount, detachedOfficesCount);

        // Assert
        assertThat(event.getEventType()).isEqualTo(EventType.USER_DELETE_ATTEMPT);

        String description = event.getDescription();
        assertThat(description).contains("Firm profile deleted for multi-firm user");
        assertThat(description).contains("User Profile ID: " + userProfileId);
        assertThat(description).contains(removedRolesCount + " roles removed");
        assertThat(description).contains(detachedOfficesCount + " offices detached");
        assertThat(description).contains("EntraOid: " + entraOid);
    }

    @Test
    void testAuditEventWithNullFirmCode() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        String userEmail = "test.user@example.com";
        String firmName = "Test Law Firm";
        UUID firmId = UUID.randomUUID();
        String entraOid = UUID.randomUUID().toString();
        int removedRolesCount = 5;
        int detachedOfficesCount = 1;

        // Act
        DeleteFirmProfileAuditEvent event = new DeleteFirmProfileAuditEvent(
                userId, userProfileId, entraOid, firmId,
                removedRolesCount, detachedOfficesCount);

        // Assert
        String description = event.getDescription();
        assertThat(description).contains("Firm profile deleted for multi-firm user");
        assertThat(description).contains("FirmId: " + firmId);
        assertThat(description).contains(removedRolesCount + " roles removed");
        assertThat(description).contains(detachedOfficesCount + " offices detached");
    }

    @Test
    void testAuditEventWithZeroCounts() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        String entraOid = UUID.randomUUID().toString();
        int removedRolesCount = 0;
        int detachedOfficesCount = 0;

        // Act
        DeleteFirmProfileAuditEvent event = new DeleteFirmProfileAuditEvent(
                userId, userProfileId, entraOid, firmId,
                removedRolesCount, detachedOfficesCount);

        // Assert
        String description = event.getDescription();
        assertThat(description).contains("0 roles removed");
        assertThat(description).contains("0 offices detached");
    }
}
