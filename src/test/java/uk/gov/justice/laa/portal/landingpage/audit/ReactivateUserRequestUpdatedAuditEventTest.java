package uk.gov.justice.laa.portal.landingpage.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReactivateUserRequestUpdatedAuditEventTest {

    @Mock
    private CurrentUserDto currentUserDto;

    @Test
    @DisplayName("Should correctly map fields from CurrentUserDto and target parameters during initialization")
    void shouldInitializeCorrectly() {
        // Given
        UUID expectedUserId = UUID.randomUUID();
        String expectedUserName = "John Doe";
        String targetUserId = "target-entra-id-456";
        String targetUserProfileId = "target-profile-id-789";
        String activity = "updated details for";

        given(currentUserDto.getUserId()).willReturn(expectedUserId);
        given(currentUserDto.getName()).willReturn(expectedUserName);

        // When
        ReactivateUserRequestUpdatedAuditEvent event =
                new ReactivateUserRequestUpdatedAuditEvent(currentUserDto, targetUserId, targetUserProfileId, activity);

        // Then
        assertThat(event.getUserId()).isEqualTo(expectedUserId);
        assertThat(event.getUserName()).isEqualTo(expectedUserName);
    }

    @Test
    @DisplayName("Should return REACTIVATE_REQ_DELEGATE_ENABLE_USER_UPDATED as event type")
    void shouldReturnCorrectEventType() {
        // Given
        given(currentUserDto.getUserId()).willReturn(UUID.randomUUID());
        given(currentUserDto.getName()).willReturn("Test User");

        ReactivateUserRequestUpdatedAuditEvent event =
                new ReactivateUserRequestUpdatedAuditEvent(currentUserDto, "target-1", "profile-1", "amended");

        // When
        EventType actualEventType = event.getEventType();

        // Then
        assertThat(actualEventType)
                .isEqualTo(EventType.REACTIVATE_REQ_DELEGATE_ENABLE_USER_UPDATED);
    }

    @Test
    @DisplayName("Should format description string correctly with user OID, activity, target user ID, and target profile ID")
    void shouldFormatDescriptionCorrectly() {
        // Given
        UUID updaterOid = UUID.randomUUID();
        String activity = "modified comments on";
        String targetUserId = "target-user-456";
        String targetUserProfileId = "target-profile-789";

        given(currentUserDto.getUserId()).willReturn(updaterOid);
        given(currentUserDto.getName()).willReturn("Jane Smith");

        ReactivateUserRequestUpdatedAuditEvent event =
                new ReactivateUserRequestUpdatedAuditEvent(currentUserDto, targetUserId, targetUserProfileId, activity);

        String expectedDescription = String.format(
                "User (Entra OID: %s) has %s reactivation request for (User Entra ID: %s; User Profile ID: %s)",
                updaterOid, activity, targetUserId, targetUserProfileId
        );

        // When
        String actualDescription = event.getDescription();

        // Then
        assertThat(actualDescription).isEqualTo(expectedDescription);
    }
}
