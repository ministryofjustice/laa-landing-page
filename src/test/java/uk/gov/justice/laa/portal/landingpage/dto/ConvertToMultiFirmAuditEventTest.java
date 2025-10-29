package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

class ConvertToMultiFirmAuditEventTest {

    @Test
    void testConvertToMultiFirmAuditEvent() {
        // Given
        UUID adminUserId = UUID.randomUUID();
        UUID convertedUserId = UUID.randomUUID();
        final String convertedUserEmail = "user@example.com";
        final String convertedUserName = "John Doe";

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("Admin User");
        currentUserDto.setUserId(adminUserId);

        EntraUserDto convertedUser = new EntraUserDto();
        convertedUser.setId(convertedUserId.toString());
        convertedUser.setEmail(convertedUserEmail);
        convertedUser.setFullName(convertedUserName);

        // When
        ConvertToMultiFirmAuditEvent event = new ConvertToMultiFirmAuditEvent(currentUserDto, convertedUser);

        // Then
        assertThat(event.getUserId()).isEqualTo(adminUserId);
        assertThat(event.getUserName()).isEqualTo("Admin User");
        assertThat(event.getConvertedUserId()).isEqualTo(convertedUserId);
        assertThat(event.getConvertedUserEmail()).isEqualTo(convertedUserEmail);
        assertThat(event.getConvertedUserName()).isEqualTo(convertedUserName);
        assertThat(event.getEventType()).isEqualTo(EventType.UPDATE_USER);
        assertThat(event.getDescription()).contains("User converted to multi-firm by Admin User");
        assertThat(event.getDescription()).contains("converted user id " + convertedUserId.toString());
        assertThat(event.getDescription()).contains(convertedUserEmail);
        assertThat(event.getDescription()).contains(convertedUserName);
        assertThat(event.getCreatedDate()).isNotNull();
    }
}
