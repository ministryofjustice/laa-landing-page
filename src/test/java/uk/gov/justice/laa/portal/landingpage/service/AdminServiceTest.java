package uk.gov.justice.laa.portal.landingpage.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.laa.portal.landingpage.dto.AdminAppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto;
import uk.gov.justice.laa.portal.landingpage.entity.AdminApp;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.repository.AdminAppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AppRepository appRepository;

    @Mock
    private AdminAppRepository adminAppRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
            appRepository,
            adminAppRepository
        );
    }

    @Test
    void testGetAllAdminApps_ReturnsEnabledAppsSortedByOrdinal() {
        // Arrange
        List<AdminApp> adminApps = Arrays.asList(
            createAdminApp("App 2", 2, true),
            createAdminApp("App 1", 1, true),
            createAdminApp("Disabled App", 0, false)
        );

        when(adminAppRepository.findAll()).thenReturn(adminApps);

        // Act
        List<AdminAppDto> result = adminService.getAllAdminApps();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("App 1");
        assertThat(result.get(0).getOrdinal()).isEqualTo(1);
        assertThat(result.get(1).getName()).isEqualTo("App 2");
        assertThat(result.get(1).getOrdinal()).isEqualTo(2);

        verify(adminAppRepository).findAll();
    }

    @Test
    void testGetAllAdminApps_FiltersDisabledApps() {
        // Arrange
        List<AdminApp> adminApps = Arrays.asList(
            createAdminApp("Enabled App", 1, true),
            createAdminApp("Disabled App", 2, false)
        );

        when(adminAppRepository.findAll()).thenReturn(adminApps);

        // Act
        List<AdminAppDto> result = adminService.getAllAdminApps();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Enabled App");
    }

    @Test
    void testGetAllApps_ReturnsSortedByOrdinal() {
        // Arrange
        List<App> apps = Arrays.asList(
            createApp("App C", 3),
            createApp("App A", 1),
            createApp("App B", 2)
        );

        when(appRepository.findAll()).thenReturn(apps);

        // Act
        List<AppAdminDto> result = adminService.getAllApps();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("App A");
        assertThat(result.get(1).getName()).isEqualTo("App B");
        assertThat(result.get(2).getName()).isEqualTo("App C");

        verify(appRepository).findAll();
    }

    private App createApp(String name, int ordinal) {
        return App.builder()
            .id(UUID.randomUUID())
            .name(name)
            .description("Description for " + name)
            .ordinal(ordinal)
            .url("https://example.com/" + name.toLowerCase())
            .enabled(true)
            .build();
    }

    // Helper methods to create test entities
    private AdminApp createAdminApp(String name, int ordinal, boolean enabled) {
        return AdminApp.builder()
                .id(UUID.randomUUID())
                .name(name)
                .description("Description for " + name)
                .ordinal(ordinal)
                .enabled(enabled)
                .build();
    }

}
