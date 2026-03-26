package uk.gov.justice.laa.portal.landingpage.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

class UserFirmReassignmentEventTest {

    @Test
    void testConstructorSetsAllFields() {
        // Given
        UUID modifierUserId = UUID.randomUUID();
        String targetUserId = UUID.randomUUID().toString();
        String targetUserName = "Test User";
        UUID previousFirmId = UUID.randomUUID();
        UUID newFirmId = UUID.randomUUID();
        String reason = "Firm merger";

        // When
        UserFirmReassignmentEvent event = new UserFirmReassignmentEvent(
                modifierUserId, 
                targetUserId,
                targetUserName,
                previousFirmId,
                newFirmId,
                reason
        );

        // Then
        assertThat(event.userId).isEqualTo(modifierUserId);
    }

    @Test
    void testGetEventType() {
        // Given
        UUID modifierUserId = UUID.randomUUID();
        String targetUserId = UUID.randomUUID().toString();
        String targetUserName = "Test User";
        UUID previousFirmId = UUID.randomUUID();
        UUID newFirmId = UUID.randomUUID();
        String reason = "Correction of assignment error";

        UserFirmReassignmentEvent event = new UserFirmReassignmentEvent(
                modifierUserId, 
                targetUserId,
                targetUserName,
                previousFirmId,
                newFirmId,
                reason
        );

        // When
        EventType eventType = event.getEventType();

        // Then
        assertThat(eventType).isEqualTo(EventType.REASSIGN_USER_FIRM);
    }

    @Test
    void testGetDescription() {
        // Given
        UUID modifierUserId = UUID.randomUUID();
        String targetUserId = UUID.randomUUID().toString();
        String entraOid = UUID.randomUUID().toString();
        UUID previousFirmId = UUID.randomUUID();
        UUID newFirmId = UUID.randomUUID();
        String reason = "User requested transfer";

        UserFirmReassignmentEvent event = new UserFirmReassignmentEvent(
                modifierUserId, 
                targetUserId,
                entraOid,
                previousFirmId,
                newFirmId,
                reason
        );

        // When
        String description = event.getDescription();

        // Then
        assertThat(description).isEqualTo(
            String.format("User %s and entraOid %s, was reassigned from firmID '%s' to firmID '%s'. Reason: %s",
                 targetUserId, entraOid, previousFirmId, newFirmId, reason)
        );
        assertThat(description).contains(targetUserId);
        assertThat(description).contains(newFirmId.toString());
        assertThat(description).contains(previousFirmId.toString());
        assertThat(description).contains("User requested transfer");
    }

    @Test
    void testGetDescriptionWithSpecialCharacters() {
        // Given
        UUID modifierUserId = UUID.randomUUID();
        String targetUserId = UUID.randomUUID().toString();
        String targetUserName = "O'Brien, Mary";
        UUID previousFirmId = UUID.randomUUID();
        UUID newFirmId = UUID.randomUUID();
        String reason = "Client conflict - re-assignment required";

        UserFirmReassignmentEvent event = new UserFirmReassignmentEvent(
                modifierUserId, 
                targetUserId,
                targetUserName,
                previousFirmId,
                newFirmId,
                reason
        );

        // When
        String description = event.getDescription();

        // Then
        assertThat(description).contains("O'Brien, Mary");
        assertThat(description).contains("Client conflict - re-assignment required");
    }

    @Test
    void testGetDescriptionFormat() {
        // Given
        UUID modifierUserId = UUID.randomUUID();
        String targetUserId = "12345";
        String targetUserName = "Test User";
        UUID previousFirmId = UUID.randomUUID();
        UUID newFirmId = UUID.randomUUID();
        String reason = "Test reason";

        UserFirmReassignmentEvent event = new UserFirmReassignmentEvent(
                modifierUserId, 
                targetUserId,
                targetUserName,
                previousFirmId,
                newFirmId,
                reason
        );

        // When
        String description = event.getDescription();

        // Then
        assertThat(description).startsWith("User ");
        assertThat(description).contains(" was reassigned from firmID '");
        assertThat(description).contains(" to firmID '");
        assertThat(description).contains(". Reason: ");
        assertThat(description).endsWith("Test reason");
    }
}
