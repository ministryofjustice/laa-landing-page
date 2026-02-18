package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppRoleServiceTest {

    private AppRoleRepository appRoleRepository;
    private ModelMapper modelMapper;
    private AppRoleService appRoleService;

    @BeforeEach
    void setUp() {
        appRoleRepository = mock(AppRoleRepository.class);
        modelMapper = mock(ModelMapper.class);
        appRoleService = new AppRoleService(appRoleRepository, modelMapper);
    }

    @Test
    void testGetByIds_allIdsFound_shouldReturnMappedDtos() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<String> ids = List.of(id1.toString(), id2.toString());

        AppRole role1 = AppRole.builder().id(id1).name("role 1").build();
        AppRole role2 = AppRole.builder().id(id2).name("role 2").build();

        AppRoleDto dto1 = new AppRoleDto();
        AppRoleDto dto2 = new AppRoleDto();

        when(appRoleRepository.findAllById(List.of(id1, id2))).thenReturn(List.of(role1, role2));
        when(modelMapper.map(role1, AppRoleDto.class)).thenReturn(dto1);
        when(modelMapper.map(role2, AppRoleDto.class)).thenReturn(dto2);

        // Act
        List<AppRoleDto> result = appRoleService.getByIds(ids);

        // Assert
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).contains(dto1);
        assertThat(result).contains(dto2);
    }

    @Test
    void testGetByIds_someIdsMissing_shouldThrowException() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<String> ids = List.of(id1.toString(), id2.toString());

        AppRole role1 = AppRole.builder().id(id1).name("role 1").build();

        when(appRoleRepository.findAllById(List.of(id1, id2))).thenReturn(List.of(role1));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                appRoleService.getByIds(ids));

        assertThat(exception.getMessage()).contains("Failed to load all app roles");
    }

    @Test
    void testGetAllAppRoles_ReturnsSortedByAppAndOrdinal() {
        // Arrange
        App appA = createApp("App A", 1);
        App appB = createApp("App B", 2);

        List<AppRole> roles = Arrays.asList(
                createAppRole("Role B1", appB, 1),
                createAppRole("Role A2", appA, 2),
                createAppRole("Role A1", appA, 1),
                createAppRole("Role B2", appB, 2)
        );

        when(appRoleRepository.findByApp_AppType(AppType.LAA)).thenReturn(roles);

        // Act
        List<AppRoleAdminDto> result = appRoleService.getAllLaaAppRoles();

        // Assert
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getName()).isEqualTo("Role A1");
        assertThat(result.get(1).getName()).isEqualTo("Role A2");
        assertThat(result.get(2).getName()).isEqualTo("Role B1");
        assertThat(result.get(3).getName()).isEqualTo("Role B2");
    }

    @Test
    void testGetAppRolesByApp_FiltersAndSortsByOrdinal() {
        // Arrange
        App targetApp = createApp("Target App", 1);

        List<AppRole> roles = Arrays.asList(
                createAppRole("Role 3", targetApp, 3),
                createAppRole("Role 1", targetApp, 1),
                createAppRole("Role 2", targetApp, 2)
        );

        when(appRoleRepository.findByApp_NameAndApp_AppType("Target App", AppType.LAA)).thenReturn(roles);

        // Act
        List<AppRoleAdminDto> result = appRoleService.getLaaAppRolesByAppName("Target App");

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("Role 1");
        assertThat(result.get(1).getName()).isEqualTo("Role 2");
        assertThat(result.get(2).getName()).isEqualTo("Role 3");
    }

    @Test
    void testGetAllAppRoles_HandlesUserTypeRestrictions() {
        // Arrange
        App app = createApp("Test App", 1);
        AppRole role = AppRole.builder()
                .id(UUID.randomUUID())
                .name("Restricted Role")
                .description("Role with restrictions")
                .app(app)
                .ordinal(1)
                .authzRole(false)
                .userTypeRestriction(new UserType[]{UserType.INTERNAL, UserType.EXTERNAL})
                .build();

        when(appRoleRepository.findByApp_AppType(AppType.LAA)).thenReturn(Collections.singletonList(role));

        // Act
        List<AppRoleAdminDto> result = appRoleService.getAllLaaAppRoles();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUserTypeRestriction()).contains("INTERNAL", "EXTERNAL");
    }

    @Test
    void testGetAllAppRoles_HandlesNullApp() {
        // Arrange
        AppRole role = AppRole.builder()
                .id(UUID.randomUUID())
                .name("Orphan Role")
                .description("Role without app")
                .app(null)
                .ordinal(1)
                .authzRole(false)
                .build();

        when(appRoleRepository.findByApp_AppType(AppType.LAA)).thenReturn(Collections.singletonList(role));

        // Act
        List<AppRoleAdminDto> result = appRoleService.getAllLaaAppRoles();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getParentApp()).isEmpty();
        assertThat(result.getFirst().getParentAppId()).isEmpty();
    }

    @Test
    void testGetAppRolesByApp_HandlesEmptyResults() {
        // Arrange
        when(appRoleRepository.findAll()).thenReturn(List.of());

        // Act
        List<AppRoleAdminDto> result = appRoleService.getLaaAppRolesByAppName("Non-existent App");

        // Assert
        assertThat(result).isEmpty();
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

    private AppRole createAppRole(String name, App app, int ordinal) {
        return AppRole.builder()
                .id(UUID.randomUUID())
                .name(name)
                .description("Description for " + name)
                .app(app)
                .ordinal(ordinal)
                .authzRole(false)
                .build();
    }
}
