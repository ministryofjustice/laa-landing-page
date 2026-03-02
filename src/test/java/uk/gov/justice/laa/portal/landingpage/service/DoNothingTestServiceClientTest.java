package uk.gov.justice.laa.portal.landingpage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.ChangeAccountEnabledResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.GetAllApplicationsResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.GetUsersResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.SendUserVerificationEmailResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DoNothingTestServiceClientTest {

    private ListAppender<ILoggingEvent> logAppender;

    @Mock
    private AppRepository appRepository;

    private TechServicesClient techServicesClient;

    @BeforeEach
    public void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(DoNothingTechServicesClient.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        techServicesClient = new DoNothingTechServicesClient(appRepository);

    }

    @AfterEach
    public void tearDown() {
        logAppender.stop();
    }

    @Test
    void testUpdateRoleAssignment() {
        UUID userId = UUID.randomUUID();

        techServicesClient.updateRoleAssignment(userId);

        assertLogMessage("Updating role assignment received on Dummy Tech Services Client for user");
    }

    @Test
    void testDeleteUser() {
        UUID userId = UUID.randomUUID();
        techServicesClient.deleteRoleAssignment(userId);
        assertLogMessage("Removing all role assignments on Dummy Tech Services Client for user " + userId);
    }

    @Test
    void testRegisterUser() {
        EntraUserDto user = EntraUserDto.builder().build();

        TechServicesApiResponse<RegisterUserResponse> response = techServicesClient.registerNewUser(user);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getCreatedUser()).isNotNull();
        assertThat(response.getData().getCreatedUser().getId()).isNotNull();
        assertLogMessage("Register new user request received on Dummy Tech Services Client for user");
    }

    @Test
    void testSendEmailVerification() {
        EntraUserDto user = EntraUserDto.builder().build();

        TechServicesApiResponse<SendUserVerificationEmailResponse> response = techServicesClient.sendEmailVerification(user);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getError()).isNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getMessage()).isNotNull();
        assertThat(response.getData().getMessage()).isEqualTo("Activation code has been generated and sent successfully via email.");
        assertLogMessage("Verification email has been resent from Dummy Tech Services Client for user");
    }

    @Test
    public void testDisableUserReturnsSuccessResponse() {
        EntraUserDto user = EntraUserDto.builder().build();
        TechServicesApiResponse<ChangeAccountEnabledResponse> response = techServicesClient.disableUser(user, "Test Reason");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getMessage()).isEqualTo("Successfully disabled user.");
    }

    @Test
    public void testEnableUserReturnsSuccessResponse() {
        EntraUserDto user = EntraUserDto.builder().build();
        TechServicesApiResponse<ChangeAccountEnabledResponse> response = techServicesClient.enableUser(user);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getMessage()).isEqualTo("Successfully enabled user.");
    }

    @Test
    void testGetUsers() {
        String fromDateTime = "2024-01-01T00:00:00Z";
        String toDateTime = "2024-01-15T23:59:59Z";

        TechServicesApiResponse<GetUsersResponse> response = techServicesClient.getUsers(fromDateTime, toDateTime);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getError()).isNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getMessage()).isEqualTo("Users retrieved successfully");
        assertThat(response.getData().getUsers()).isNotNull();
        assertThat(response.getData().getUsers()).isEmpty();
        assertLogMessage("Get users request received on Dummy Tech Services Client");
    }

    @Test
    @DisplayName("getAllApplications: maps repository LAA apps into response with success=true")
    void getAllApplications_mapsFieldsCorrectly() {
        // Given repository returns two apps
        App a1 = app("A-1", "Alpha", "https://alpha.example", "SG-1", "Group One");
        App a2 = app("A-2", "Beta", "https://beta.example", "SG-2", "Group Two");

        when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(List.of(a1, a2));

        // When
        TechServicesApiResponse<GetAllApplicationsResponse> resp = techServicesClient.getAllApplications();

        // Then: repo invoked with LAA
        verify(appRepository, times(1)).findAppsByAppType(AppType.LAA);
        verifyNoMoreInteractions(appRepository);

        // Wrapper and payload success flags
        assertThat(resp).isNotNull();
        assertThat(resp.isSuccess()).isTrue();

        GetAllApplicationsResponse payload = resp.getData();
        assertThat(payload).isNotNull();
        assertThat(payload.isSuccess()).isTrue();

        // Apps list mapped
        assertThat(payload.getApps()).hasSize(2);

        // Validate first app mapping
        var t1 = payload.getApps().getFirst();
        assertThat(t1.getId()).isEqualTo("A-1");
        assertThat(t1.getName()).isEqualTo("Alpha");
        assertThat(t1.getUrl()).isEqualTo("https://alpha.example");
        assertThat(t1.getSecurityGroups()).hasSize(1);
        assertThat(t1.getSecurityGroups().getFirst().getId()).isEqualTo("SG-1");
        assertThat(t1.getSecurityGroups().getFirst().getName()).isEqualTo("Group One");

        // Validate second app mapping
        var t2 = payload.getApps().get(1);
        assertThat(t2.getId()).isEqualTo("A-2");
        assertThat(t2.getName()).isEqualTo("Beta");
        assertThat(t2.getUrl()).isEqualTo("https://beta.example");
        assertThat(t2.getSecurityGroups()).hasSize(1);
        assertThat(t2.getSecurityGroups().getFirst().getId()).isEqualTo("SG-2");
        assertThat(t2.getSecurityGroups().getFirst().getName()).isEqualTo("Group Two");
    }

    @Test
    @DisplayName("getAllApplications: empty repository result → empty apps list and success=true")
    void getAllApplications_emptyList() {
        when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(List.of());

        TechServicesApiResponse<GetAllApplicationsResponse> resp = techServicesClient.getAllApplications();

        verify(appRepository).findAppsByAppType(AppType.LAA);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isNotNull();
        assertThat(resp.getData().isSuccess()).isTrue();
        assertThat(resp.getData().getApps()).isEmpty();
    }

    @Test
    @DisplayName("getAllApplications: mapping tolerates null fields (e.g., null SG id/name)")
    void getAllApplications_nullFields() {
        App a1 = app("A-1", null, null, null, null); // null name/url/security group fields
        when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(List.of(a1));

        TechServicesApiResponse<GetAllApplicationsResponse> resp = techServicesClient.getAllApplications();

        assertThat(resp.isSuccess()).isTrue();
        var apps = resp.getData().getApps();
        assertThat(apps).hasSize(1);

        var t1 = apps.getFirst();
        assertThat(t1.getId()).isEqualTo("A-1");
        assertThat(t1.getName()).isNull();
        assertThat(t1.getUrl()).isNull();
        assertThat(t1.getSecurityGroups()).hasSize(1);
        assertThat(t1.getSecurityGroups().getFirst().getId()).isNull();
        assertThat(t1.getSecurityGroups().getFirst().getName()).isNull();
    }

    // OPTIONAL: If you want explicit coverage for the private mapper method:
    @Test
    @DisplayName("mapAppToTechServicesApp: direct invocation via reflection (optional explicit coverage)")
    void mapAppToTechServicesApp_reflection() throws Exception {
        App a = app("ID-9", "Display", "https://u", "SGX", "SG Name");

        Method mapper = DoNothingTechServicesClient.class
                .getDeclaredMethod("mapAppToTechServicesApp", App.class);
        mapper.setAccessible(true);

        GetAllApplicationsResponse.TechServicesApplication mapped =
                (GetAllApplicationsResponse.TechServicesApplication) mapper.invoke(techServicesClient, a);

        assertThat(mapped.getId()).isEqualTo("ID-9");
        assertThat(mapped.getName()).isEqualTo("Display");
        assertThat(mapped.getUrl()).isEqualTo("https://u");
        assertThat(mapped.getSecurityGroups()).hasSize(1);
        assertThat(mapped.getSecurityGroups().getFirst().getId()).isEqualTo("SGX");
        assertThat(mapped.getSecurityGroups().getFirst().getName()).isEqualTo("SG Name");
    }

    // Helper builder for App entity in tests
    private App app(String id, String name, String url, String sgId, String sgName) {
        return App.builder()
                .entraAppId(id)
                .name(name)
                .url(url)
                .securityGroupOid(sgId)
                .securityGroupName(sgName)
                .appType(AppType.LAA)
                .enabled(true)
                .build();
    }

    private void assertLogMessage(String message) {
        assertTrue(logAppender.list.stream()
                        .anyMatch(logEvent -> logEvent.getLevel() == Level.INFO
                                && logEvent.getFormattedMessage().contains(message)),
                String.format("Log message not found with level %s and message %s. Actual Logs are: %s", Level.INFO, message,
                        logAppender.list.stream().map(e -> String.format("[%s] %s", Level.INFO, e.getFormattedMessage()))
                                .toList()));
    }

}
