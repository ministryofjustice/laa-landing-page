package uk.gov.justice.laa.portal.landingpage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.dto.CreateUserAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.DeleteUserAttemptAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.DeleteUserSuccessAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateUserAuditEvent;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;
import uk.gov.justice.laa.portal.landingpage.model.DeletedUser;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring.addListAppenderToLogger;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @InjectMocks
    private EventService eventService;

    @Test
    void auditUserCreate() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("admin");
        UUID adminUuid = UUID.randomUUID();
        currentUserDto.setUserId(adminUuid);
        UUID userId = UUID.randomUUID();
        EntraUser entraUser = EntraUser.builder().firstName("new").lastName("User").id(userId).build();
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(EventService.class);
        String selectedFirm = "Firm";
        boolean isUserManager = true;
        CreateUserAuditEvent createUserAuditEvent = new CreateUserAuditEvent(currentUserDto, entraUser, selectedFirm, isUserManager);
        eventService.logEvent(createUserAuditEvent);
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());
        assertThat(infoLogs.get(0).getFormattedMessage()).contains("Audit event CREATE_USER, by User with user id " + adminUuid
                + ", New user created, user id " + userId + ", with firm Firm and user type External User Manager");
    }

    @Test
    void auditDeleteUserAttempt() {
        // Given
        UUID adminUuid = UUID.randomUUID();
        UUID deletedUserId = UUID.randomUUID();
        String reason = "duplicate user";
        String error = "Tech Services unavailable";

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("admin");
        currentUserDto.setUserId(adminUuid);

        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(EventService.class);

        DeleteUserAttemptAuditEvent event = new DeleteUserAttemptAuditEvent(
                deletedUserId.toString(), reason, adminUuid, error);

        // When
        eventService.logEvent(event);

        // Then
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());
        String message = infoLogs.get(0).getFormattedMessage();
        assertThat(message).contains("Audit event USER_DELETE_ATTEMPT, by User with user id " + adminUuid);
        assertThat(message).contains("User delete attempted, user id " + deletedUserId + " for reason " + reason + ", error: " + error);
    }

    @Test
    void auditDeleteUserSuccess() {
        // Given
        UUID adminUuid = UUID.randomUUID();
        UUID deletedUserId = UUID.randomUUID();
        String reason = "left organisation";

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("admin");
        currentUserDto.setUserId(adminUuid);

        DeletedUser deletedUser = DeletedUser.builder()
                .deletedUserId(deletedUserId)
                .removedRolesCount(3)
                .detachedOfficesCount(2)
                .build();

        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(EventService.class);

        DeleteUserSuccessAuditEvent event = new DeleteUserSuccessAuditEvent(
                reason, adminUuid, deletedUser);

        // When
        eventService.logEvent(event);

        // Then
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());
        String message = infoLogs.get(0).getFormattedMessage();
        assertThat(message).contains("Audit event USER_DELETE_EXECUTED, by User with user id " + adminUuid);
        assertThat(message).contains("User deleted successfully, deleted user id " + deletedUserId
                + " for reason " + reason + ". 3 roles removed. 2 offices detached");
    }

    @Test
    void auditUpdateRole() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("admin");
        UUID adminUuid = UUID.randomUUID();
        currentUserDto.setUserId(adminUuid);
        UUID userId = UUID.randomUUID();
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setFullName("oldUser");
        entraUser.setId(userId.toString());
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(EventService.class);
        String updatedRoles = "Removed: Old Role, Added: New Role";
        UUID profileId = UUID.randomUUID();
        UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(profileId, currentUserDto, entraUser, updatedRoles, "role");
        eventService.logEvent(updateUserAuditEvent);
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());
        assertThat(infoLogs.get(0).getFormattedMessage()).contains("Audit event UPDATE_USER, by User with user id " + adminUuid
                + ", Existing user id " + entraUser.getId() + " updated, profile id " + profileId + ", with role Removed: Old Role, Added: New Role\n"
                + "\n");
    }

    @Test
    void auditUpdateOffices() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("admin");
        UUID adminUuid = UUID.randomUUID();
        currentUserDto.setUserId(adminUuid);
        UUID userId = UUID.randomUUID();
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setFullName("oldUser");
        entraUser.setId(userId.toString());
        ListAppender<ILoggingEvent> listAppender = addListAppenderToLogger(EventService.class);
        String updatedOffices = "Removed : Office1, Added : Office2";
        UUID profileId = UUID.randomUUID();
        UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(profileId, currentUserDto, entraUser, updatedOffices, "office");
        eventService.logEvent(updateUserAuditEvent);
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());
        assertThat(infoLogs.get(0).getFormattedMessage()).contains("Audit event UPDATE_USER, by User with user id " + adminUuid
                + ", Existing user id " + entraUser.getId() + " updated, profile id " + profileId + ", with office Removed : Office1, Added : Office2");
    }
}