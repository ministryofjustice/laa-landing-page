package uk.gov.justice.laa.portal.landingpage.dto;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AddUserProfileAuditEventTest {

    @Test
    void testAuditEventFieldsAndMethods() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String userName = "Test Admin";
        UUID newUserProfileId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        String field = "role";
        String changeString = "Admin";

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(userId);
        currentUserDto.setName(userName);
        EntraUserDto entraUserDto = EntraUserDto.builder().id(UUID.randomUUID().toString()).email("test.user@example.com").build();

        // Act
        AddUserProfileAuditEvent event = new AddUserProfileAuditEvent(
                currentUserDto, newUserProfileId, entraUserDto, firmId, field, changeString
        );

        // Assert
        assertThat(event.getEventType()).isEqualTo(EventType.UPDATE_USER);
        assertThat(event.getNewUserProfileId()).isEqualTo(newUserProfileId);
        assertThat(event.getUser()).isEqualTo(entraUserDto);
        assertThat(event.getFirmId()).isEqualTo(firmId);
        assertThat(event.getField()).isEqualTo(field);
        assertThat(event.getChangeString()).isEqualTo(changeString);

        String expectedDescription = String.format(
                "New user profile with id %s added to user id %s, for the firm %s, added by %s and added %s (%s)",
                newUserProfileId, entraUserDto.getId(), firmId, userId, field, changeString
        );
        assertThat(event.getDescription()).isEqualTo(expectedDescription);
    }
}
