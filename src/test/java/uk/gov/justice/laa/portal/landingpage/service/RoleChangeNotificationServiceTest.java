package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.model.CcmsMessage;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleChangeNotificationServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RoleChangeNotificationService roleChangeNotificationService;

    private UserProfile userProfile;
    private EntraUser entraUser;
    private Firm firm;
    private AppRole puiRole1;
    private AppRole puiRole2;
    private AppRole nonPuiRole;
    private Set<AppRole> oldPuiRoles;
    private Set<AppRole> newPuiRoles;
    private Set<AppRole> emptyRoles;

    @BeforeEach
    void setUp() {
        entraUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .entraOid("test-entra-oid")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("FIRM001")
                .name("Test Firm")
                .build();

        puiRole1 = AppRole.builder()
                .id(UUID.randomUUID())
                .name("PUI_ROLE_1")
                .ccmsCode("CCMS_PUI_001")
                .legacySync(true)
                .build();

        puiRole2 = AppRole.builder()
                .id(UUID.randomUUID())
                .name("PUI_ROLE_2")
                .ccmsCode("CCMS_PUI_002")
                .legacySync(true)
                .build();

        nonPuiRole = AppRole.builder()
                .id(UUID.randomUUID())
                .name("NON_PUI_ROLE")
                .ccmsCode("NON_CCMS_001")
                .build();

        userProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .legacyUserId(UUID.randomUUID())
                .userType(UserType.EXTERNAL_SINGLE_FIRM)
                .entraUser(entraUser)
                .firm(firm)
                .appRoles(Set.of(puiRole1, puiRole2, nonPuiRole))
                .build();

        oldPuiRoles = Set.of(puiRole1);
        newPuiRoles = Set.of(puiRole1, puiRole2);
        emptyRoles = Set.of();
    }

    @Test
    void shouldNotSendMessage_whenPuiRolesUnchangedForExternalUser() throws JsonProcessingException {
        Set<AppRole> unchangedRoles = Set.of(puiRole1);

        roleChangeNotificationService.sendMessage(userProfile, unchangedRoles, unchangedRoles);

        verify(objectMapper, never()).writeValueAsString(any(CcmsMessage.class));
    }

    @Test
    void shouldNotSendMessage_whenUserIsInternal() throws JsonProcessingException {
        userProfile.setUserType(UserType.INTERNAL);

        roleChangeNotificationService.sendMessage(userProfile, newPuiRoles, oldPuiRoles);

        verify(objectMapper, never()).writeValueAsString(any(CcmsMessage.class));
    }

    @Test
    void shouldCreateCorrectCcmsMessage_whenPuiRolesChanged() throws JsonProcessingException {
        String expectedJson = "{\"test\":\"message\"}";
        when(objectMapper.writeValueAsString(any(CcmsMessage.class))).thenReturn(expectedJson);

        roleChangeNotificationService.sendMessage(userProfile, newPuiRoles, oldPuiRoles);

        verify(objectMapper).writeValueAsString(argThat(message -> {
            CcmsMessage ccmsMessage = (CcmsMessage) message;
            return ccmsMessage.getUserName().equals(userProfile.getLegacyUserId().toString())
                    && ccmsMessage.getVendorNumber().equals(firm.getCode())
                    && ccmsMessage.getFirstName().equals(entraUser.getFirstName())
                    && ccmsMessage.getLastName().equals(entraUser.getLastName())
                    && ccmsMessage.getEmail().equals(entraUser.getEmail())
                    && ccmsMessage.getTimestamp() != null
                    && ccmsMessage.getResponsibilityKey().contains("CCMS_PUI_001")
                    && ccmsMessage.getResponsibilityKey().contains("CCMS_PUI_002");
        }));
    }

    @Test
    void shouldThrowRuntimeException_whenJsonProcessingFails() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any(CcmsMessage.class)))
                .thenThrow(new JsonProcessingException("JSON processing failed") {});

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                roleChangeNotificationService.sendMessage(userProfile, newPuiRoles, oldPuiRoles));

        assertThat(exception.getMessage()).isEqualTo("Failed to send message");
        assertThat(exception.getCause()).isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void shouldThrowRuntimeException_whenGeneralExceptionOccurs() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any(CcmsMessage.class)))
                .thenThrow(new RuntimeException("General error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                roleChangeNotificationService.sendMessage(userProfile, newPuiRoles, oldPuiRoles));

        assertThat(exception.getMessage()).isEqualTo("Failed to send message");
        assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
        assertThat(exception.getCause().getMessage()).isEqualTo("General error");
    }

    @Test
    void shouldHandleEmptyOldRoles() throws JsonProcessingException {
        String expectedJson = "{\"test\":\"message\"}";
        when(objectMapper.writeValueAsString(any(CcmsMessage.class))).thenReturn(expectedJson);

        roleChangeNotificationService.sendMessage(userProfile, newPuiRoles, emptyRoles);

        verify(objectMapper).writeValueAsString(any(CcmsMessage.class));
    }

    @Test
    void shouldHandleEmptyNewRoles() throws JsonProcessingException {
        String expectedJson = "{\"test\":\"message\"}";
        when(objectMapper.writeValueAsString(any(CcmsMessage.class))).thenReturn(expectedJson);

        roleChangeNotificationService.sendMessage(userProfile, emptyRoles, oldPuiRoles);

        verify(objectMapper).writeValueAsString(any(CcmsMessage.class));
    }

    @Test
    void shouldNotSendMessage_whenBothRoleSetsAreEmpty() throws JsonProcessingException {
        roleChangeNotificationService.sendMessage(userProfile, emptyRoles, emptyRoles);

        verify(objectMapper, never()).writeValueAsString(any(CcmsMessage.class));
    }
}
