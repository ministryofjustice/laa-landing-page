package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppServiceTest {

    @Mock
    private AppRepository appRepository;

    @InjectMocks
    private AppService appService;

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
}
