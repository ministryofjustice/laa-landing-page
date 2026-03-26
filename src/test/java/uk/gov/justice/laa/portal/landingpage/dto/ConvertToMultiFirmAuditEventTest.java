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

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("Admin User");
        currentUserDto.setUserId(adminUserId);

        EntraUserDto convertedUser = new EntraUserDto();
        convertedUser.setId(convertedUserId.toString());
        String entraOid = UUID.randomUUID().toString();
        convertedUser.setEntraOid(entraOid);

        // When
        ConvertToMultiFirmAuditEvent event = new ConvertToMultiFirmAuditEvent(currentUserDto, convertedUser, adminUserId.toString());

        // Then
        assertThat(event.getUserId()).isEqualTo(adminUserId);
        assertThat(event.getConvertedUserId()).isEqualTo(adminUserId.toString());
        assertThat(event.getEventType()).isEqualTo(EventType.UPDATE_USER);
        assertThat(event.getDescription()).contains("converted user entra oid " + entraOid);
        assertThat(event.getDescription()).contains("user id " + adminUserId);
        assertThat(event.getCreatedDate()).isNotNull();
    }
}
