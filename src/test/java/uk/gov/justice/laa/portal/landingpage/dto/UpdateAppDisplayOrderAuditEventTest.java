package uk.gov.justice.laa.portal.landingpage.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateAppDisplayOrderAuditEventTest {

    @Test
    @DisplayName("Constructor should populate userId, userName and userProfileId correctly")
    void constructorPopulatesFields() {
        UUID entraOid = UUID.randomUUID();
        CurrentUserDto user = currentUser(entraOid, "John Smith");
        UUID profileId = UUID.randomUUID();

        UpdateAppDisplayOrderAuditEvent event = new UpdateAppDisplayOrderAuditEvent(user, profileId);

        assertThat(event.getUserId()).isEqualTo(entraOid);
        assertThat(event.getUserName()).isEqualTo("John Smith");
        assertThat(event.getUserProfileId()).isEqualTo(profileId);
    }

    @Test
    @DisplayName("getEventType returns UPDATE_LAA_APP_METADATA")
    void getEventTypeReturnsExpectedValue() {
        UpdateAppDisplayOrderAuditEvent event = new UpdateAppDisplayOrderAuditEvent(currentUser(UUID.randomUUID(), "User"), UUID.randomUUID());

        assertThat(event.getEventType()).isEqualTo(EventType.UPDATE_LAA_APP_METADATA);
    }

    @Test
    @DisplayName("getDescription returns correctly formatted string")
    void getDescriptionCorrectFormat() {
        UUID entraOid = UUID.randomUUID();
        CurrentUserDto user = currentUser(entraOid, "Jane Doe");
        UUID profileId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        UpdateAppDisplayOrderAuditEvent event = new UpdateAppDisplayOrderAuditEvent(user, profileId);

        String description = event.getDescription();

        assertThat(description).isEqualTo("User (Profile ID: 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee' " + "Entra OID: '" + entraOid + "') has updated App display order");
    }

    @Test
    @DisplayName("getDescription handles null userProfileId")
    void descriptionHandlesNullProfileId() {
        UUID entraOid = UUID.randomUUID();
        UpdateAppDisplayOrderAuditEvent event = new UpdateAppDisplayOrderAuditEvent(currentUser(entraOid, "Test User"), null);

        assertThat(event.getDescription()).isEqualTo("User (Profile ID: 'null' Entra OID: '" + entraOid + "') has updated App display order");
    }

    @Test
    @DisplayName("getDescription handles null userId")
    void descriptionHandlesNullUserId() {
        CurrentUserDto user = currentUser(null, "Test User");
        UUID profileId = UUID.randomUUID();

        UpdateAppDisplayOrderAuditEvent event = new UpdateAppDisplayOrderAuditEvent(user, profileId);

        assertThat(event.getDescription()).isEqualTo(String.format("User (Profile ID: '%s' Entra OID: 'null') has updated App display order", profileId));
    }

    @Test
    @DisplayName("getDescription handles null userName (even though not used in description)")
    void constructorAllowsNullUserName() {
        CurrentUserDto user = currentUser(UUID.randomUUID(), null);
        UUID profileId = UUID.randomUUID();

        UpdateAppDisplayOrderAuditEvent event = new UpdateAppDisplayOrderAuditEvent(user, profileId);

        // userName is unused in description, but we assert it's stored correctly
        assertThat(event.getUserName()).isNull();
    }

    private CurrentUserDto currentUser(UUID id, String name) {
        CurrentUserDto cu = new CurrentUserDto();
        cu.setUserId(id);
        cu.setName(name);
        return cu;
    }

}
