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
        String userEmail = "test.user@example.com";
        String firmName = "Test Law Firm";
        String firmCode = "ABC123";
        int removedRolesCount = 3;
        int detachedOfficesCount = 2;

        // Act
        DeleteFirmProfileAuditEvent event = new DeleteFirmProfileAuditEvent(
                userId, userProfileId, userEmail, firmName, firmCode,
                removedRolesCount, detachedOfficesCount);

        // Assert
        assertThat(event.getUserProfileId()).isEqualTo(userProfileId);
        assertThat(event.getFirmName()).isEqualTo(firmName);
        assertThat(event.getFirmCode()).isEqualTo(firmCode);
        assertThat(event.getUserEmail()).isEqualTo(userEmail);
        assertThat(event.getRemovedRolesCount()).isEqualTo(removedRolesCount);
        assertThat(event.getDetachedOfficesCount()).isEqualTo(detachedOfficesCount);
        assertThat(event.getEventType()).isEqualTo(EventType.USER_DELETE_ATTEMPT);

        String description = event.getDescription();
        assertThat(description).contains("Firm profile deleted for multi-firm user");
        assertThat(description).contains("User: " + userEmail);
        assertThat(description).contains("User Profile ID: " + userProfileId);
        assertThat(description).contains("Firm: " + firmName + " (" + firmCode + ")");
        assertThat(description).contains(removedRolesCount + " roles removed");
        assertThat(description).contains(detachedOfficesCount + " offices detached");
        assertThat(description).contains("Deleted by user ID: " + userId);
    }

    @Test
    void testAuditEventWithNullFirmCode() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        String userEmail = "test.user@example.com";
        String firmName = "Test Law Firm";
        String firmCode = null;
        int removedRolesCount = 5;
        int detachedOfficesCount = 1;

        // Act
        DeleteFirmProfileAuditEvent event = new DeleteFirmProfileAuditEvent(
                userId, userProfileId, userEmail, firmName, firmCode,
                removedRolesCount, detachedOfficesCount);

        // Assert
        assertThat(event.getFirmCode()).isNull();
        String description = event.getDescription();
        assertThat(description).contains("Firm profile deleted for multi-firm user");
        assertThat(description).contains("User: " + userEmail);
        assertThat(description).contains("Firm: " + firmName + " (N/A)");
        assertThat(description).contains(removedRolesCount + " roles removed");
        assertThat(description).contains(detachedOfficesCount + " offices detached");
    }

    @Test
    void testAuditEventWithZeroCounts() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        String userEmail = "another.user@example.com";
        String firmName = "Another Firm";
        String firmCode = "XYZ789";
        int removedRolesCount = 0;
        int detachedOfficesCount = 0;

        // Act
        DeleteFirmProfileAuditEvent event = new DeleteFirmProfileAuditEvent(
                userId, userProfileId, userEmail, firmName, firmCode,
                removedRolesCount, detachedOfficesCount);

        // Assert
        assertThat(event.getRemovedRolesCount()).isZero();
        assertThat(event.getDetachedOfficesCount()).isZero();
        String description = event.getDescription();
        assertThat(description).contains("0 roles removed");
        assertThat(description).contains("0 offices detached");
    }
}
