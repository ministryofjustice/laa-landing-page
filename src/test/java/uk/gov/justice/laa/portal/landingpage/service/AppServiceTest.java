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

}
