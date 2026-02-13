package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.forms.AppsOrderForm;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppServiceTest {

    @Mock
    private AppRepository appRepository;

    private AppService appService;

    private App app;

    private AppDto appDto;

    @BeforeEach
    void setUp() {
        appService = new AppService(appRepository, new ModelMapper());
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

        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> appService.save(appDto));

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
        when(appRepository.saveAll(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(app1, app2, app3));

        AppsOrderForm.AppOrderDetailsForm order1 = new AppsOrderForm.AppOrderDetailsForm(app1.getId().toString(), 3);
        AppsOrderForm.AppOrderDetailsForm order2 = new AppsOrderForm.AppOrderDetailsForm(app2.getId().toString(), 1);
        AppsOrderForm.AppOrderDetailsForm order3 = new AppsOrderForm.AppOrderDetailsForm(app3.getId().toString(), 2);

        List<AppDto> result = appService.updateAppsOrder(List.of(order1, order2, order3));

        assertThat(result).hasSize(3);
        verify(appRepository).findAppsByAppType(AppType.LAA);
        verify(appRepository).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateAppsOrder_ignoresAppsNotInOrderList() {
        App app1 = App.builder().id(UUID.randomUUID()).name("App 1").ordinal(1).build();
        App app2 = App.builder().id(UUID.randomUUID()).name("App 2").ordinal(2).build();

        when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(List.of(app1, app2));
        when(appRepository.saveAll(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(app1, app2));

        AppsOrderForm.AppOrderDetailsForm order1 = new AppsOrderForm.AppOrderDetailsForm(app1.getId().toString(), 5);

        List<AppDto> result = appService.updateAppsOrder(List.of(order1));

        assertThat(result).hasSize(2);
        assertThat(app2.getOrdinal()).isEqualTo(2);
        verify(appRepository).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateAppsOrder_returnsEmptyList_whenNoAppsExist() {
        when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(Collections.emptyList());
        when(appRepository.saveAll(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());

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

        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(
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
}
