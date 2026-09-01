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
class ReactivateUserRequestRejectedAuditEventTest {

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

        given(currentUserDto.getUserId()).willReturn(expectedUserId);
        given(currentUserDto.getName()).willReturn(expectedUserName);

        // When
        ReactivateUserRequestRejectedAuditEvent event =
                new ReactivateUserRequestRejectedAuditEvent(currentUserDto, targetUserId, targetUserProfileId, "Comments");

        // Then
        assertThat(event.getUserId()).isEqualTo(expectedUserId);
        assertThat(event.getUserName()).isEqualTo(expectedUserName);
    }

    @Test
    @DisplayName("Should return REACTIVATE_REQ_DELEGATE_ENABLE_USER_REJECTED as event type")
    void shouldReturnCorrectEventType() {
        // Given
        given(currentUserDto.getUserId()).willReturn(UUID.randomUUID());
        given(currentUserDto.getName()).willReturn("Test User");

        ReactivateUserRequestRejectedAuditEvent event =
                new ReactivateUserRequestRejectedAuditEvent(currentUserDto, "target-1", "profile-1", "Comments");

        // When
        EventType actualEventType = event.getEventType();

        // Then
        assertThat(actualEventType)
                .isEqualTo(EventType.REACTIVATE_REQ_DELEGATE_ENABLE_USER_REJECTED);
    }

    @Test
    @DisplayName("Should format description string correctly with user OID, target user ID, and target profile ID")
    void shouldFormatDescriptionCorrectly() {
        // Given
        UUID rejectorOid = UUID.randomUUID();
        String targetUserId = "target-user-456";
        String targetUserProfileId = "target-profile-789";

        given(currentUserDto.getUserId()).willReturn(rejectorOid);
        given(currentUserDto.getName()).willReturn("Jane Smith");

        ReactivateUserRequestRejectedAuditEvent event =
                new ReactivateUserRequestRejectedAuditEvent(currentUserDto, targetUserId, targetUserProfileId, "Comments");

        String expectedDescription = String.format(
                "User (Entra OID: %s) has rejected reactivation request for (User Entra ID: %s; User Profile ID: %s), with comments: Comments",
                rejectorOid, targetUserId, targetUserProfileId
        );

        // When
        String actualDescription = event.getDescription();

        // Then
        assertThat(actualDescription).isEqualTo(expectedDescription);
    }
}
