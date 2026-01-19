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
        String modifierUserName = "Admin User";
        String targetUserId = UUID.randomUUID().toString();
        String targetUserName = "Test User";
        String previousFirmName = "Old Firm Ltd";
        String newFirmName = "New Firm Ltd";
        String reason = "Firm merger";

        // When
        UserFirmReassignmentEvent event = new UserFirmReassignmentEvent(
                modifierUserId, 
                modifierUserName, 
                targetUserId, 
                targetUserName,
                previousFirmName, 
                newFirmName, 
                reason
        );

        // Then
        assertThat(event.userId).isEqualTo(modifierUserId);
        assertThat(event.userName).isEqualTo(modifierUserName);
    }

    @Test
    void testGetEventType() {
        // Given
        UUID modifierUserId = UUID.randomUUID();
        String modifierUserName = "Admin User";
        String targetUserId = UUID.randomUUID().toString();
        String targetUserName = "Test User";
        String previousFirmName = "Old Firm Ltd";
        String newFirmName = "New Firm Ltd";
        String reason = "Correction of assignment error";

        UserFirmReassignmentEvent event = new UserFirmReassignmentEvent(
                modifierUserId, 
                modifierUserName, 
                targetUserId, 
                targetUserName,
                previousFirmName, 
                newFirmName, 
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
        String modifierUserName = "Admin User";
        String targetUserId = UUID.randomUUID().toString();
        String targetUserName = "John Doe";
        String previousFirmName = "ABC Law Firm";
        String newFirmName = "XYZ Legal Services";
        String reason = "User requested transfer";

        UserFirmReassignmentEvent event = new UserFirmReassignmentEvent(
                modifierUserId, 
                modifierUserName, 
                targetUserId, 
                targetUserName,
                previousFirmName, 
                newFirmName, 
                reason
        );

        // When
        String description = event.getDescription();

        // Then
        assertThat(description).isEqualTo(
            String.format("User '%s' (ID: %s) was reassigned from firm '%s' to firm '%s'. Reason: %s", 
                targetUserName, targetUserId, previousFirmName, newFirmName, reason)
        );
        assertThat(description).contains("John Doe");
        assertThat(description).contains(targetUserId);
        assertThat(description).contains("ABC Law Firm");
        assertThat(description).contains("XYZ Legal Services");
        assertThat(description).contains("User requested transfer");
    }

    @Test
    void testGetDescriptionWithSpecialCharacters() {
        // Given
        UUID modifierUserId = UUID.randomUUID();
        String modifierUserName = "Admin User";
        String targetUserId = UUID.randomUUID().toString();
        String targetUserName = "O'Brien, Mary";
        String previousFirmName = "Smith & Jones LLP";
        String newFirmName = "Brown, White & Associates";
        String reason = "Client conflict - re-assignment required";

        UserFirmReassignmentEvent event = new UserFirmReassignmentEvent(
                modifierUserId, 
                modifierUserName, 
                targetUserId, 
                targetUserName,
                previousFirmName, 
                newFirmName, 
                reason
        );

        // When
        String description = event.getDescription();

        // Then
        assertThat(description).contains("O'Brien, Mary");
        assertThat(description).contains("Smith & Jones LLP");
        assertThat(description).contains("Brown, White & Associates");
        assertThat(description).contains("Client conflict - re-assignment required");
    }

    @Test
    void testGetDescriptionFormat() {
        // Given
        UUID modifierUserId = UUID.randomUUID();
        String modifierUserName = "System Admin";
        String targetUserId = "12345";
        String targetUserName = "Test User";
        String previousFirmName = "Old Firm";
        String newFirmName = "New Firm";
        String reason = "Test reason";

        UserFirmReassignmentEvent event = new UserFirmReassignmentEvent(
                modifierUserId, 
                modifierUserName, 
                targetUserId, 
                targetUserName,
                previousFirmName, 
                newFirmName, 
                reason
        );

        // When
        String description = event.getDescription();

        // Then
        assertThat(description).startsWith("User '");
        assertThat(description).contains(" was reassigned from firm '");
        assertThat(description).contains(" to firm '");
        assertThat(description).contains(". Reason: ");
        assertThat(description).endsWith("Test reason");
    }
}
