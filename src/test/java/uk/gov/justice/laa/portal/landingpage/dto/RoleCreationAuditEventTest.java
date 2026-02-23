package uk.gov.justice.laa.portal.landingpage.dto;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoleCreationAuditEventTest {

    @Test
    void testAuditEventFieldsAndMethods() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String userName = "Test Admin";
        String roleName = "Test Role";
        String roleId = UUID.randomUUID().toString();
        String parentAppName = "Test Application";
        String description = "Test role description";
        String ccmsCode = "TEST123";
        String userTypeRestriction = "INTERNAL, EXTERNAL";

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(userId);
        currentUserDto.setName(userName);
        
        String userProfileId = UUID.randomUUID().toString();
        String entraOid = "test-entra-oid-123";

        // Act
        RoleCreationAuditEvent event = new RoleCreationAuditEvent(
                roleName, parentAppName, description, ccmsCode, userTypeRestriction, currentUserDto, userProfileId, entraOid
        );

        // Assert
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getUserName()).isEqualTo(userName);
        assertThat(event.getEventType()).isEqualTo(EventType.CREATE_ROLE);

        String auditDescription = event.getDescription();
        assertThat(auditDescription).contains("New app role created by user entra oid: " + entraOid);
        assertThat(auditDescription).contains("user profile id: " + userProfileId);
        assertThat(auditDescription).contains("for app " + parentAppName);
        assertThat(auditDescription).contains("Name: " + roleName);
        assertThat(auditDescription).contains("Description: " + description);
        assertThat(auditDescription).contains("CCMS Code: " + ccmsCode);
        assertThat(auditDescription).contains("User Type Restriction: " + userTypeRestriction);
        assertThat(event.getCreatedDate()).isNotNull();
    }

    @Test
    void testAuditEventWithNullCcmsCode() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String userName = "Test Admin";
        String roleName = "Test Role";
        String roleId = UUID.randomUUID().toString();
        String parentAppName = "Test Application";
        String description = "Test role description";
        String ccmsCode = null;
        String userTypeRestriction = "INTERNAL";

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(userId);
        currentUserDto.setName(userName);
        
        String userProfileId = UUID.randomUUID().toString();
        String entraOid = "test-entra-oid-456";

        // Act
        RoleCreationAuditEvent event = new RoleCreationAuditEvent(
                roleName, parentAppName, description, ccmsCode, userTypeRestriction, currentUserDto, userProfileId, entraOid
        );

        // Assert
        String auditDescription = event.getDescription();
        assertThat(auditDescription).contains("New app role created by user entra oid: " + entraOid);
        assertThat(auditDescription).contains("user profile id: " + userProfileId);
        assertThat(auditDescription).contains("for app " + parentAppName);
        assertThat(auditDescription).contains("Name: " + roleName);
        assertThat(auditDescription).contains("CCMS Code: N/A");
        assertThat(auditDescription).contains("User Type Restriction: " + userTypeRestriction);
    }

    @Test
    void testAuditEventWithEmptyUserTypeRestriction() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String userName = "Test Admin";
        String roleName = "Admin Role";
        String roleId = UUID.randomUUID().toString();
        String parentAppName = "Admin Portal";
        String description = "Administrator role";
        String ccmsCode = "ADMIN001";
        String userTypeRestriction = "";

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(userId);
        currentUserDto.setName(userName);
        
        String userProfileId = UUID.randomUUID().toString();
        String entraOid = "admin-entra-oid-789";

        // Act
        RoleCreationAuditEvent event = new RoleCreationAuditEvent(
                roleName, parentAppName, description, ccmsCode, userTypeRestriction, currentUserDto, userProfileId, entraOid
        );

        // Assert
        assertThat(event.getEventType()).isEqualTo(EventType.CREATE_ROLE);
        String auditDescription = event.getDescription();
        assertThat(auditDescription).contains("New app role created by user entra oid: " + entraOid);
        assertThat(auditDescription).contains("user profile id: " + userProfileId);
        assertThat(auditDescription).contains("Name: " + roleName);
        assertThat(auditDescription).contains("CCMS Code: " + ccmsCode);
        assertThat(auditDescription).contains("User Type Restriction: ");
    }

    @Test
    void testAuditEventWithLongValues() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String userName = "Administrator With Very Long Name";
        String roleName = "Very Long Role Name That Exceeds Normal Length";
        String roleId = UUID.randomUUID().toString();
        String parentAppName = "Very Long Application Name That Might Be Used In Production";
        String description = "This is a very long description that might contain multiple sentences and detailed information about what this role is supposed to do in the system.";
        String ccmsCode = "VERYLONGCCMSCODE123456789";
        String userTypeRestriction = "INTERNAL, EXTERNAL, CONTRACTOR, ADMIN";

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(userId);
        currentUserDto.setName(userName);
        
        String userProfileId = UUID.randomUUID().toString();
        String entraOid = "long-entra-oid-with-many-characters-123456789";

        // Act
        RoleCreationAuditEvent event = new RoleCreationAuditEvent(
                roleName, parentAppName, description, ccmsCode, userTypeRestriction, currentUserDto, userProfileId, entraOid
        );

        // Assert
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getUserName()).isEqualTo(userName);
        assertThat(event.getEventType()).isEqualTo(EventType.CREATE_ROLE);
        
        String auditDescription = event.getDescription();
        assertThat(auditDescription).contains(entraOid);
        assertThat(auditDescription).contains(userProfileId);
        assertThat(auditDescription).contains(roleName);
        assertThat(auditDescription).contains(parentAppName);
        assertThat(auditDescription).contains(description);
        assertThat(auditDescription).contains(ccmsCode);
        assertThat(auditDescription).contains(userTypeRestriction);
    }

    @Test
    void testAuditEventWithSpecialCharacters() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String userName = "Test User";
        String roleName = "Role with Special Characters: @#$%";
        String roleId = UUID.randomUUID().toString();
        String parentAppName = "App & Service (Test)";
        String description = "Description with quotes \"test\" and symbols: !@#$%^&*()";
        String ccmsCode = "TEST-123_ABC";
        String userTypeRestriction = "INTERNAL";

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(userId);
        currentUserDto.setName(userName);
        
        String userProfileId = UUID.randomUUID().toString();
        String entraOid = "special-chars-entra-oid@#$%";

        // Act
        RoleCreationAuditEvent event = new RoleCreationAuditEvent(
                roleName, parentAppName, description, ccmsCode, userTypeRestriction, currentUserDto, userProfileId, entraOid
        );

        // Assert
        assertThat(event.getEventType()).isEqualTo(EventType.CREATE_ROLE);
        String auditDescription = event.getDescription();
        assertThat(auditDescription).contains(roleName);
        assertThat(auditDescription).contains(parentAppName);
        assertThat(auditDescription).contains(description);
        assertThat(auditDescription).contains(ccmsCode);
    }
}
