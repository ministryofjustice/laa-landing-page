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
import uk.gov.justice.laa.portal.landingpage.dto.UpdateRoleAuditEvent;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;

import java.time.LocalDateTime;
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
        String selectedRoles = "ROLE_ADMIN, ROLE_USER";
        List<String> selectedOfficesDisplay = List.of("Office 1", "Office 2", "Office 3");
        String selectedFirm = "Firm";
        CreateUserAuditEvent createUserAuditEvent = new CreateUserAuditEvent(currentUserDto, entraUser, selectedRoles, selectedOfficesDisplay, selectedFirm);
        eventService.logEvent(createUserAuditEvent);
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());
        assertThat(infoLogs.get(0).getFormattedMessage()).contains("Audit event CREATE_USER, by User admin with user id " + adminUuid
                + ", New user new User created, user id " + userId + ", with role ROLE_ADMIN, ROLE_USER, office Office 1, Office 2, Office 3, firm Firm\n"
                + "\n");
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
        List<String> selectedRoles = List.of("ROLE_ADMIN", "ROLE_USER");
        UpdateRoleAuditEvent updateRoleAuditEvent = new UpdateRoleAuditEvent(currentUserDto, entraUser, selectedRoles);
        eventService.logEvent(updateRoleAuditEvent);
        List<ILoggingEvent> infoLogs = LogMonitoring.getLogsByLevel(listAppender, Level.INFO);
        assertEquals(1, infoLogs.size());
        assertThat(infoLogs.get(0).getFormattedMessage()).contains("Audit event UPDATE_USER, by User admin with user id " + adminUuid
                + ", Existing user oldUser updated, user id " + userId + ", with new role ROLE_ADMIN, ROLE_USER\n"
                + "\n");
    }
}