package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppSynchronizationAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.forms.AppsOrderForm;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.GetAllApplicationsResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesErrorResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppServiceTest {

    @Mock
    private TechServicesClient techServicesClient;
    @Mock
    private AppRepository appRepository;
    @Mock
    private ModelMapper mapper;
    @Mock
    private EventService eventService;

    private AppService appService;
    private CurrentUserDto currentUser;
    private UserProfileDto userProfileDto;
    private App app;
    private AppDto appDto;

    @BeforeEach
    void setUp() {
        currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("user-1");
        userProfileDto = UserProfileDto.builder().id(UUID.randomUUID()).build();

        mapper = new ModelMapper();
        appService = new AppService(appRepository, techServicesClient, mapper, eventService);
        UUID id = UUID.randomUUID();
        app = App.builder()
                .id(id)
                .name("Test App")
                .description("Sample description")
                .build();

        appDto = AppDto.builder()
                .name("Test App")
                .description("Sample description")
                .build();
    }


    @Test
    void getById_ReturnsApp_WhenFound() {
        // Arrange
        UUID appId = UUID.randomUUID();
        App expectedApp = App.builder().id(appId).name("Test App").build();
        when(appRepository.findById(appId)).thenReturn(Optional.of(expectedApp));

        // Act
        Optional<App> result = appService.getById(appId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedApp);
    }

    @Test
    void getById_ReturnsEmpty_WhenNotFound() {
        // Arrange
        UUID appId = UUID.randomUUID();
        when(appRepository.findById(appId)).thenReturn(Optional.empty());

        // Act
        Optional<App> result = appService.getById(appId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void testFindAll_ShouldReturnMappedDtos() {
        when(appRepository.findAll()).thenReturn(Collections.singletonList(app));

        List<AppDto> result = appService.findAll();

        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .containsExactly(appDto);

        verify(appRepository).findAll();
    }

    @Test
    void testGetAllEnabledApps() {
        when(appRepository.findAppsByAppTypeAndEnabled(AppType.LAA, true)).thenReturn(Collections.singletonList(app));

        List<AppDto> result = appService.getAllActiveLaaApps();

        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .containsExactly(appDto);

        verify(appRepository).findAppsByAppTypeAndEnabled(AppType.LAA, true);
    }

    @Test
    void findById_ReturnsMappedDto_WhenAppExists() {
        UUID appId = UUID.randomUUID();
        when(appRepository.findById(appId)).thenReturn(Optional.of(app));

        Optional<AppDto> result = appService.findById(appId);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(app.getName());
        assertThat(result.get().getDescription()).isEqualTo(app.getDescription());

        verify(appRepository).findById(appId);
    }

    @Test
    void findById_ReturnsEmpty_WhenAppDoesNotExist() {
        UUID appId = UUID.randomUUID();
        when(appRepository.findById(appId)).thenReturn(Optional.empty());

        Optional<AppDto> result = appService.findById(appId);

        assertThat(result).isEmpty();

        verify(appRepository).findById(appId);
    }

    @Test
    void updateAppsOrder_UpdatesAndReturnsSortedDtos() {
        app.setOrdinal(1);
        when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(List.of(app));
        when(appRepository.saveAll(List.of(app))).thenReturn(List.of(app));

        AppsOrderForm.AppOrderDetailsForm appOrderDetails = new AppsOrderForm.AppOrderDetailsForm(app.getId().toString(), 1);
        List<AppsOrderForm.AppOrderDetailsForm> orderDetails = List.of(appOrderDetails);
        List<AppDto> result = appService.updateAppsOrder(orderDetails);

        assertThat(result).hasSize(1);
        assertThat(result.stream().findFirst().get().getName()).isEqualTo(app.getName());
        assertThat(result.stream().findFirst().get().getDescription()).isEqualTo(app.getDescription());

        verify(appRepository).findAppsByAppType(AppType.LAA);
        verify(appRepository).saveAll(List.of(app));
    }

    @Test
    void save_UpdatesAndReturnsUpdatedApp() {
        UUID appId = UUID.randomUUID();
        app.setId(appId);
        appDto.setId(appId.toString());
        appDto.setEnabled(true);
        appDto.setDescription("Updated description");

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(appRepository.save(app)).thenReturn(app);

        App result = appService.save(appDto);

        assertThat(result).isNotNull();
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getDescription()).isEqualTo("Updated description");

        verify(appRepository).findById(appId);
        verify(appRepository).save(app);
    }

    @Test
    void save_ThrowsException_WhenAppNotFound() {
        UUID appId = UUID.randomUUID();
        appDto.setId(appId.toString());

        when(appRepository.findById(appId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> appService.save(appDto));

        assertThat(exception.getMessage()).isEqualTo(String.format("App not found for the give app id: %s", appId));

        verify(appRepository).findById(appId);
    }

    @Test
    void getAllLaaAppEntities_returnsLaaAppsOnly() {
        App laaApp = App.builder().id(UUID.randomUUID()).name("LAA App").appType(AppType.LAA).build();
        App otherApp = App.builder().id(UUID.randomUUID()).name("Other App").appType(AppType.AUTHZ).build();

        when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(List.of(laaApp, otherApp));

        List<App> result = appService.getAllLaaAppEntities();

        assertThat(result).hasSize(2);
        assertThat(result).contains(laaApp, otherApp);
        verify(appRepository).findAppsByAppType(AppType.LAA);
    }

    @Test
    void getAllLaaAppEntities_returnsEmptyList_whenNoLaaAppsExist() {
        when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(Collections.emptyList());

        List<App> result = appService.getAllLaaAppEntities();

        assertThat(result).isEmpty();
        verify(appRepository).findAppsByAppType(AppType.LAA);
    }

    @Test
    void getAllLaaApps_returnsSortedDtos() {
        App app1 = App.builder().id(UUID.randomUUID()).name("Z App").appType(AppType.LAA).ordinal(3).build();
        App app2 = App.builder().id(UUID.randomUUID()).name("A App").appType(AppType.LAA).ordinal(1).build();
        App app3 = App.builder().id(UUID.randomUUID()).name("M App").appType(AppType.LAA).ordinal(2).build();

        when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(List.of(app1, app2, app3));

        List<AppDto> result = appService.getAllLaaApps();

        assertThat(result).hasSize(3);
        assertThat(result.stream().map(AppDto::getName).toList()).contains("Z App", "A App", "M App");
        verify(appRepository).findAppsByAppType(AppType.LAA);
    }

    @Test
    void getAllLaaApps_returnsEmptyList_whenNoAppsExist() {
        when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(Collections.emptyList());

        List<AppDto> result = appService.getAllLaaApps();

        assertThat(result).isEmpty();
        verify(appRepository).findAppsByAppType(AppType.LAA);
    }

    @Test
    void getAllActiveLaaApps_returnsOnlyEnabledApps() {
        App enabledApp = App.builder().id(UUID.randomUUID()).name("Enabled").enabled(true).build();

        when(appRepository.findAppsByAppTypeAndEnabled(AppType.LAA, true)).thenReturn(List.of(enabledApp));

        List<AppDto> result = appService.getAllActiveLaaApps();

        assertThat(result).hasSize(1);
        assertThat(result.stream().map(AppDto::getName).toList()).contains("Enabled");
        verify(appRepository).findAppsByAppTypeAndEnabled(AppType.LAA, true);
    }

    @Test
    void getAllActiveLaaApps_returnsEmptyList_whenNoActiveAppsExist() {
        when(appRepository.findAppsByAppTypeAndEnabled(AppType.LAA, true)).thenReturn(Collections.emptyList());

        List<AppDto> result = appService.getAllActiveLaaApps();

        assertThat(result).isEmpty();
        verify(appRepository).findAppsByAppTypeAndEnabled(AppType.LAA, true);
    }

    @Test
    void updateAppsOrder_updatesMultipleApps() {
        App app1 = App.builder().id(UUID.randomUUID()).name("App 1").ordinal(1).build();
        App app2 = App.builder().id(UUID.randomUUID()).name("App 2").ordinal(2).build();
        App app3 = App.builder().id(UUID.randomUUID()).name("App 3").ordinal(3).build();

        when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(List.of(app1, app2, app3));
        when(appRepository.saveAll(any())).thenReturn(List.of(app1, app2, app3));

        AppsOrderForm.AppOrderDetailsForm order1 = new AppsOrderForm.AppOrderDetailsForm(app1.getId().toString(), 3);
        AppsOrderForm.AppOrderDetailsForm order2 = new AppsOrderForm.AppOrderDetailsForm(app2.getId().toString(), 1);
        AppsOrderForm.AppOrderDetailsForm order3 = new AppsOrderForm.AppOrderDetailsForm(app3.getId().toString(), 2);

        List<AppDto> result = appService.updateAppsOrder(List.of(order1, order2, order3));

        assertThat(result).hasSize(3);
        verify(appRepository).findAppsByAppType(AppType.LAA);
        verify(appRepository).saveAll(any());
    }

    @Test
    void updateAppsOrder_ignoresAppsNotInOrderList() {
        App app1 = App.builder().id(UUID.randomUUID()).name("App 1").ordinal(1).build();
        App app2 = App.builder().id(UUID.randomUUID()).name("App 2").ordinal(2).build();

        when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(List.of(app1, app2));
        when(appRepository.saveAll(any())).thenReturn(List.of(app1, app2));

        AppsOrderForm.AppOrderDetailsForm order1 = new AppsOrderForm.AppOrderDetailsForm(app1.getId().toString(), 5);

        List<AppDto> result = appService.updateAppsOrder(List.of(order1));

        assertThat(result).hasSize(2);
        assertThat(app2.getOrdinal()).isEqualTo(2);
        verify(appRepository).saveAll(any());
    }

    @Test
    void updateAppsOrder_returnsEmptyList_whenNoAppsExist() {
        when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(Collections.emptyList());
        when(appRepository.saveAll(any())).thenReturn(Collections.emptyList());

        List<AppDto> result = appService.updateAppsOrder(List.of(new AppsOrderForm.AppOrderDetailsForm("id", 1)));

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_returnsAllAppsAsDtos() {
        App app1 = App.builder().id(UUID.randomUUID()).name("App 1").description("Desc 1").build();
        App app2 = App.builder().id(UUID.randomUUID()).name("App 2").description("Desc 2").build();

        when(appRepository.findAll()).thenReturn(List.of(app1, app2));

        List<AppDto> result = appService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(AppDto::getName).toList()).contains("App 1", "App 2");
        verify(appRepository).findAll();
    }

    @Test
    void findAll_returnsEmptyList_whenNoAppsExist() {
        when(appRepository.findAll()).thenReturn(Collections.emptyList());

        List<AppDto> result = appService.findAll();

        assertThat(result).isEmpty();
        verify(appRepository).findAll();
    }

    @Test
    void save_modifiesOnlyEnabledAndDescription_notOtherId() {
        UUID appId = UUID.randomUUID();
        appDto.setId(appId.toString());
        appDto.setEnabled(true);
        appDto.setDescription("New Description");

        App existingApp = App.builder()
                .id(appId)
                .name("Original Name")
                .description("Original Description")
                .enabled(false)
                .build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(existingApp));
        when(appRepository.save(existingApp)).thenReturn(existingApp);

        App result = appService.save(appDto);

        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getDescription()).isEqualTo("New Description");
        assertThat(result.getName()).isEqualTo("Original Name");
        verify(appRepository).save(existingApp);
    }

    @Test
    void save_throwsException_withCorrectMessage_whenAppNotFound() {
        UUID appId = UUID.randomUUID();
        appDto.setId(appId.toString());

        when(appRepository.findById(appId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> appService.save(appDto)
        );

        assertThat(exception).hasMessage("App not found for the give app id: " + appId);
    }

    @Test
    void getById_returnsCorrectApp_fromMultipleApps() {
        UUID appId1 = UUID.randomUUID();
        UUID appId2 = UUID.randomUUID();
        App app1 = App.builder().id(appId1).name("App 1").build();

        when(appRepository.findById(appId1)).thenReturn(Optional.of(app1));
        when(appRepository.findById(appId2)).thenReturn(Optional.empty());

        Optional<App> result1 = appService.getById(appId1);
        Optional<App> result2 = appService.getById(appId2);

        assertThat(result1).isPresent().contains(app1);
        assertThat(result2).isEmpty();
    }

    @Test
    void findById_mapsAllAppProperties() {
        UUID appId = UUID.randomUUID();
        App app = App.builder()
                .id(appId)
                .name("Test App")
                .description("Test Description")
                .enabled(true)
                .ordinal(5)
                .build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));

        Optional<AppDto> result = appService.findById(appId);

        assertThat(result).isPresent();
        AppDto dto = result.get();
        assertThat(dto.getName()).isEqualTo("Test App");
        assertThat(dto.getDescription()).isEqualTo("Test Description");
    }


    private TechServicesApiResponse<GetAllApplicationsResponse> successResponseWithApps(List<GetAllApplicationsResponse.TechServicesApplication> apps) {
        GetAllApplicationsResponse data = GetAllApplicationsResponse.builder()
                .success(true).apps(apps).build();
        return TechServicesApiResponse.success(data);
    }

    private TechServicesApiResponse<GetAllApplicationsResponse> failureResponse(String message) {
        TechServicesErrorResponse techServicesErrorResponse = TechServicesErrorResponse.builder().message(message).build();
        return TechServicesApiResponse.error(techServicesErrorResponse);
    }

    private GetAllApplicationsResponse.TechServicesApplication remoteApp(
            String id, String name, String url, String sgId, String sgName
    ) {
        GetAllApplicationsResponse.TechServicesApplication app = mock(GetAllApplicationsResponse.TechServicesApplication.class);
        when(app.getId()).thenReturn(id);
        when(app.getName()).thenReturn(name);
        when(app.getUrl()).thenReturn(url);
        if (sgId == null && sgName == null) {
            when(app.getSecurityGroups()).thenReturn(null); // exercise null path
        } else {
            GetAllApplicationsResponse.TechServicesApplication.AppSecurityGroup sg = mock(GetAllApplicationsResponse.TechServicesApplication.AppSecurityGroup.class);
            when(sg.getId()).thenReturn(sgId);
            when(sg.getName()).thenReturn(sgName);
            when(app.getSecurityGroups()).thenReturn(List.of(sg));
        }
        return app;
    }

    private App localApp(
            String id, String name, String url, String sgId, String sgName, boolean enabled
    ) {
        return App.builder()
                .entraAppId(id)
                .name(name)
                .url(url)
                .securityGroupOid(sgId)
                .securityGroupName(sgName)
                .appType(AppType.LAA)
                .enabled(enabled)
                .build();
    }

    // simulate your existing "getAllLaaAppEntities" called by service
    // We'll spy the service to stub that method without hitting DB
    private void stubLocalApps(List<App> locals) throws Exception {
        AppService spyService = Mockito.spy(appService);
        doReturn(locals).when(spyService).getAllLaaAppEntities();
        // swap the spy into field (since @InjectMocks created original)
        this.appService = spyService;
    }

    @Test
    @DisplayName("ADDED: present only in remote → new local app (disabled), saved and returned with changeType=ADDED")
    void added_whenOnlyRemote() throws Exception {
        GetAllApplicationsResponse.TechServicesApplication r1 = remoteApp("R1", "Remote One", "https://r1", "SG1", "Group 1");
        when(techServicesClient.getAllApplications())
                .thenReturn(successResponseWithApps(List.of(r1)));

        stubLocalApps(List.of());

        List<AppDto> out = appService.synchronizeAndGetApplicationsFromTechServices(currentUser, userProfileDto);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getId()).isEqualTo("R1");
        assertThat(out.get(0).getName()).isEqualTo("Remote One");
        assertThat(out.get(0).getChangeType()).isEqualTo(AppDto.ChangeType.ADDED);

        // saveAll called once with new entity enabled=false
        ArgumentCaptor<Iterable<App>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(appRepository, times(1)).saveAll(captor.capture());
        List<App> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().isEnabled()).isFalse();

        verify(eventService).logEvent(any(AppSynchronizationAuditEvent.class));
    }

    @Test
    @DisplayName("DELETED: present only in local (enabled) → disabled and returned with changeType=DELETED")
    void deleted_whenOnlyLocal_enabledGetsDisabledAndSaved() throws Exception {
        App l1 = localApp("L1", "Local One", "https://l1", "SG1", "Group 1", true);
        when(techServicesClient.getAllApplications())
                .thenReturn(successResponseWithApps(List.of()));

        stubLocalApps(List.of(l1));

        List<AppDto> out = appService.synchronizeAndGetApplicationsFromTechServices(currentUser, userProfileDto);
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getId()).isEqualTo("L1");
        assertThat(out.getFirst().getChangeType()).isEqualTo(AppDto.ChangeType.DELETED);

        ArgumentCaptor<Iterable<App>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(appRepository).saveAll(captor.capture());
        List<App> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).isEnabled()).isFalse();

        verify(eventService).logEvent(any(AppSynchronizationAuditEvent.class));
    }

    @Test
    @DisplayName("DELETED: present only in local (already disabled) → no DB write, still returned as DELETED")
    void deleted_whenOnlyLocal_alreadyDisabled_noSave() throws Exception {

        App l1 = localApp("L1", "Local One", "https://l1", "SG1", "Group 1", false);
        when(techServicesClient.getAllApplications())
                .thenReturn(successResponseWithApps(List.of()));

        stubLocalApps(List.of(l1));

        List<AppDto> out = appService.synchronizeAndGetApplicationsFromTechServices(currentUser, userProfileDto);
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getChangeType()).isEqualTo(AppDto.ChangeType.DELETED);

        verify(appRepository, times(1)).saveAll(any());
        verify(eventService).logEvent(any(AppSynchronizationAuditEvent.class));
    }

    @Test
    @DisplayName("NONE: present in both, enabled, and identical fields (including null/empty SG) → no save")
    void none_whenNoDifferences() throws Exception {

        GetAllApplicationsResponse.TechServicesApplication r1 = remoteApp("ID", "Same", "https://same", null, null); // SG list null
        App l1 = localApp("ID", "Same", "https://same", null, null, true);

        when(techServicesClient.getAllApplications())
                .thenReturn(successResponseWithApps(List.of(r1)));
        stubLocalApps(List.of(l1));

        List<AppDto> out = appService.synchronizeAndGetApplicationsFromTechServices(currentUser, userProfileDto);

        assertThat(out).hasSize(1);
        AppDto dto = out.get(0);
        assertThat(dto.getChangeType()).isEqualTo(AppDto.ChangeType.NONE);

        verify(appRepository, times(1)).saveAll(any());
        verify(eventService).logEvent(any(AppSynchronizationAuditEvent.class));
    }

    @Test
    @DisplayName("REVIEW: present in both but local is disabled → metadata updated, saved; changeType=REVIEW")
    void review_whenLocalDisabled() throws Exception {

        GetAllApplicationsResponse.TechServicesApplication r1 = remoteApp("ID", "RemoteName", "https://remote", "SGX", "RemoteSG");
        App l1 = localApp("ID", "OldName", "https://old", "SG0", "OldSG", false);

        when(techServicesClient.getAllApplications())
                .thenReturn(successResponseWithApps(List.of(r1)));
        stubLocalApps(List.of(l1));

        List<AppDto> out = appService.synchronizeAndGetApplicationsFromTechServices(currentUser, userProfileDto);

        assertThat(out).hasSize(1);
        AppDto dto = out.get(0);
        assertThat(dto.getChangeType()).isEqualTo(AppDto.ChangeType.REVIEW);
        ArgumentCaptor<Iterable<App>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(appRepository).saveAll(captor.capture());
        List<App> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);
        assertThat(saved).hasSize(1);
        App savedApp = saved.get(0);
        assertThat(savedApp.getName()).isEqualTo("RemoteName");
        assertThat(savedApp.getUrl()).isEqualTo("https://remote");
        assertThat(savedApp.getSecurityGroupOid()).isEqualTo("SGX");
        assertThat(savedApp.getSecurityGroupName()).isEqualTo("RemoteSG");

        // Verify event summary mentions Updated=1
        ArgumentCaptor<AppSynchronizationAuditEvent> eventCaptor = ArgumentCaptor.forClass(AppSynchronizationAuditEvent.class);
        verify(eventService).logEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDescription())
                .contains("Updated apps: 1");
    }

    @Test
    @DisplayName("UPDATED: present in both, enabled, field differences → saved and returned as UPDATED")
    void updated_whenDifferencesAndEnabled() throws Exception {

        GetAllApplicationsResponse.TechServicesApplication r1 = remoteApp("ID", "NewName", "https://new", "SG2", "SG Two");
        App l1 = localApp("ID", "OldName", "https://old", "SG1", "SG One", true);
        when(techServicesClient.getAllApplications())
                .thenReturn(successResponseWithApps(List.of(r1)));
        stubLocalApps(List.of(l1));

        List<AppDto> out = appService.synchronizeAndGetApplicationsFromTechServices(currentUser, userProfileDto);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getChangeType()).isEqualTo(AppDto.ChangeType.UPDATED);

        // Must save updated local
        verify(appRepository).saveAll(any());
        verify(eventService).logEvent(any(AppSynchronizationAuditEvent.class));
    }

    @Test
    @DisplayName("Defensive nulls: apps list is null → treat as empty")
    void appsNull_treatedAsEmpty() throws Exception {
        GetAllApplicationsResponse data = GetAllApplicationsResponse.builder().success(true).build();
        TechServicesApiResponse<GetAllApplicationsResponse> resp = TechServicesApiResponse.success(data);
        when(techServicesClient.getAllApplications()).thenReturn(resp);


        App l1 = localApp("L1", "Local One", "https://l1", null, null, true);
        stubLocalApps(List.of(l1));

        List<AppDto> out = appService.synchronizeAndGetApplicationsFromTechServices(currentUser, userProfileDto);
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getChangeType()).isEqualTo(AppDto.ChangeType.DELETED);
    }

    @Test
    @DisplayName("Error path: failure with message → throws RuntimeException with that message")
    void errorFromRemote_withMessage() throws Exception {
        when(techServicesClient.getAllApplications())
                .thenReturn(failureResponse("Bad Gateway"));

        assertThatThrownBy(() ->
                appService.synchronizeAndGetApplicationsFromTechServices(currentUser, userProfileDto)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Bad Gateway");

        verifyNoInteractions(appRepository);
        verify(eventService, never()).logEvent(any());
    }

    @Test
    @DisplayName("Security group comparison: empty list on remote equals null local fields → NONE")
    void sgEmptyEqualsNullLocal() throws Exception {
        GetAllApplicationsResponse.TechServicesApplication r = mock(GetAllApplicationsResponse.TechServicesApplication.class);
        when(r.getId()).thenReturn("ID");
        when(r.getName()).thenReturn("Same");
        when(r.getUrl()).thenReturn("https://same");
        when(r.getSecurityGroups()).thenReturn(Collections.emptyList()); // empty

        App l = localApp("ID", "Same", "https://same", null, null, true);

        when(techServicesClient.getAllApplications())
                .thenReturn(successResponseWithApps(List.of(r)));
        stubLocalApps(List.of(l));

        List<AppDto> out = appService.synchronizeAndGetApplicationsFromTechServices(currentUser, userProfileDto);
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getChangeType()).isEqualTo(AppDto.ChangeType.NONE);
    }
}
