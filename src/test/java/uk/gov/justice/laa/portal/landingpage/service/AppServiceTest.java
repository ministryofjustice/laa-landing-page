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
}
