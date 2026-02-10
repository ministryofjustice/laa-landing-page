package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppService Unit Tests")
class AppServiceTest {

    @Mock
    private AppRepository appRepository;

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AppService appService;

    private App app;

    private AppDto appDto;

    @BeforeEach
    void setUp() {
        appService = new AppService(appRepository, appRoleRepository, new ModelMapper());
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
    void testGetAll_ForAdmin_ShouldReturnMappedDtos() {
        when(appRepository.findAll()).thenReturn(Collections.singletonList(app));

        List<AppDto> result = appService.getAllAppsForAdmin();

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

    @Nested
    @DisplayName("getById Tests")
    class GetByIdTests {

        @Test
        void returnsApp_whenAppExists() {
            UUID appId = UUID.randomUUID();
            App expectedApp = App.builder().id(appId).name("Test App").build();
            when(appRepository.findById(appId)).thenReturn(Optional.of(expectedApp));

            Optional<App> result = appService.getById(appId);

            assertThat(result).isPresent().contains(expectedApp);
            verify(appRepository).findById(appId);
        }

        @Test
        void returnsEmpty_whenAppNotFound() {
            UUID appId = UUID.randomUUID();
            when(appRepository.findById(appId)).thenReturn(Optional.empty());

            Optional<App> result = appService.getById(appId);

            assertThat(result).isEmpty();
            verify(appRepository).findById(appId);
        }
    }

    @Nested
    @DisplayName("getAllLaaApps Tests")
    class GetAllLaaAppsTests {

        @Test
        void returnsAllLaaApps_sortedByOrdinal() {
            App app1 = App.builder()
                    .id(UUID.randomUUID())
                    .name("App 1")
                    .appType(AppType.LAA)
                    .ordinal(2)
                    .build();
            App app2 = App.builder()
                    .id(UUID.randomUUID())
                    .name("App 2")
                    .appType(AppType.LAA)
                    .ordinal(1)
                    .build();
            App app3 = App.builder()
                    .id(UUID.randomUUID())
                    .name("App 3")
                    .appType(AppType.LAA)
                    .ordinal(3)
                    .build();

            when(appRepository.findAppsByAppType(AppType.LAA))
                    .thenReturn(List.of(app1, app2, app3));

            List<AppDto> result = appService.getAllLaaApps();

            assertThat(result)
                    .hasSize(3)
                    .extracting(AppDto::getOrdinal)
                    .containsExactly(1, 2, 3);
            verify(appRepository).findAppsByAppType(AppType.LAA);
        }

        @Test
        void returnsEmptyList_whenNoLaaAppsExist() {
            when(appRepository.findAppsByAppType(AppType.LAA)).thenReturn(Collections.emptyList());

            List<AppDto> result = appService.getAllLaaApps();

            assertThat(result).isEmpty();
            verify(appRepository).findAppsByAppType(AppType.LAA);
        }
    }

    @Nested
    @DisplayName("getAllActiveLaaApps Tests")
    class GetAllActiveLaaAppsTests {

        @Test
        void returnsAllActiveLaaApps_sortedByOrdinal() {
            App app1 = App.builder()
                    .id(UUID.randomUUID())
                    .name("Active App 1")
                    .appType(AppType.LAA)
                    .enabled(true)
                    .ordinal(2)
                    .build();
            App app2 = App.builder()
                    .id(UUID.randomUUID())
                    .name("Active App 2")
                    .appType(AppType.LAA)
                    .enabled(true)
                    .ordinal(1)
                    .build();

            when(appRepository.findAppsByAppTypeAndEnabled(AppType.LAA, true))
                    .thenReturn(List.of(app1, app2));

            List<AppDto> result = appService.getAllActiveLaaApps();

            assertThat(result)
                    .hasSize(2)
                    .extracting(AppDto::getName)
                    .containsExactly("Active App 2", "Active App 1");
            verify(appRepository).findAppsByAppTypeAndEnabled(AppType.LAA, true);
        }

        @Test
        void returnsEmptyList_whenNoActiveAppsExist() {
            when(appRepository.findAppsByAppTypeAndEnabled(AppType.LAA, true))
                    .thenReturn(Collections.emptyList());

            List<AppDto> result = appService.getAllActiveLaaApps();

            assertThat(result).isEmpty();
            verify(appRepository).findAppsByAppTypeAndEnabled(AppType.LAA, true);
        }
    }

    @Nested
    @DisplayName("getAllAdminApps Tests")
    class GetAllAdminAppsTests {

        @Test
        void returnsAllAuthzApps_sortedByOrdinal() {
            App app1 = App.builder()
                    .id(UUID.randomUUID())
                    .name("Admin App 1")
                    .appType(AppType.AUTHZ)
                    .ordinal(2)
                    .build();
            App app2 = App.builder()
                    .id(UUID.randomUUID())
                    .name("Admin App 2")
                    .appType(AppType.AUTHZ)
                    .ordinal(1)
                    .build();

            when(appRepository.findAppsByAppType(AppType.AUTHZ))
                    .thenReturn(List.of(app1, app2));

            List<AppDto> result = appService.getAllAdminApps();

            assertThat(result)
                    .hasSize(2)
                    .extracting(AppDto::getOrdinal)
                    .containsExactly(1, 2);
            verify(appRepository).findAppsByAppType(AppType.AUTHZ);
        }

        @Test
        void returnsEmptyList_whenNoAdminAppsExist() {
            when(appRepository.findAppsByAppType(AppType.AUTHZ))
                    .thenReturn(Collections.emptyList());

            List<AppDto> result = appService.getAllAdminApps();

            assertThat(result).isEmpty();
            verify(appRepository).findAppsByAppType(AppType.AUTHZ);
        }
    }

    @Nested
    @DisplayName("getAllActiveAdminApps Tests")
    class GetAllActiveAdminAppsTests {

        @Test
        void returnsAllActiveAuthzApps_sortedByOrdinal() {
            App app1 = App.builder()
                    .id(UUID.randomUUID())
                    .name("Active Admin App 1")
                    .appType(AppType.AUTHZ)
                    .enabled(true)
                    .ordinal(2)
                    .build();
            App app2 = App.builder()
                    .id(UUID.randomUUID())
                    .name("Active Admin App 2")
                    .appType(AppType.AUTHZ)
                    .enabled(true)
                    .ordinal(1)
                    .build();

            when(appRepository.findAppsByAppTypeAndEnabled(AppType.AUTHZ, true))
                    .thenReturn(List.of(app1, app2));

            List<AppDto> result = appService.getAllActiveAdminApps();

            assertThat(result)
                    .hasSize(2)
                    .extracting(AppDto::getOrdinal)
                    .containsExactly(1, 2);
            verify(appRepository).findAppsByAppTypeAndEnabled(AppType.AUTHZ, true);
        }

        @Test
        void returnsEmptyList_whenNoActiveAdminAppsExist() {
            when(appRepository.findAppsByAppTypeAndEnabled(AppType.AUTHZ, true))
                    .thenReturn(Collections.emptyList());

            List<AppDto> result = appService.getAllActiveAdminApps();

            assertThat(result).isEmpty();
            verify(appRepository).findAppsByAppTypeAndEnabled(AppType.AUTHZ, true);
        }
    }

    @Nested
    @DisplayName("getAllAdminAppRoles Tests")
    class GetAllAdminAppRolesTests {

        @Test
        void returnsAllAuthzAppRoles_sorted() {
            App authzApp = App.builder()
                    .id(UUID.randomUUID())
                    .name("Admin App")
                    .appType(AppType.AUTHZ)
                    .build();

            AppRole role1 = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("Role B")
                    .app(authzApp)
                    .authzRole(true)
                    .build();
            AppRole role2 = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("Role A")
                    .app(authzApp)
                    .authzRole(true)
                    .build();

            when(appRoleRepository.findByApp_AppType(AppType.AUTHZ))
                    .thenReturn(List.of(role1, role2));

            List<AppRoleDto> result = appService.getAllAdminAppRoles();

            assertThat(result).hasSize(2);
            verify(appRoleRepository).findByApp_AppType(AppType.AUTHZ);
        }

        @Test
        void returnsEmptyList_whenNoAuthzRolesExist() {
            when(appRoleRepository.findByApp_AppType(AppType.AUTHZ))
                    .thenReturn(Collections.emptyList());

            List<AppRoleDto> result = appService.getAllAdminAppRoles();

            assertThat(result).isEmpty();
            verify(appRoleRepository).findByApp_AppType(AppType.AUTHZ);
        }
    }

    @Nested
    @DisplayName("getAllAppRolesForAdmin Tests")
    class GetAllAppRolesForAdminTests {

        @Test
        void returnsAllAppRoles_sorted() {
            App app1 = App.builder().id(UUID.randomUUID()).name("App 1").build();
            AppRole role1 = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("Role Z")
                    .app(app1)
                    .build();
            AppRole role2 = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("Role A")
                    .app(app1)
                    .build();

            when(appRoleRepository.findAll()).thenReturn(List.of(role1, role2));

            List<AppRoleDto> result = appService.getAllAppRolesForAdmin();

            assertThat(result).hasSize(2);
            verify(appRoleRepository).findAll();
        }

        @Test
        void returnsEmptyList_whenNoRolesExist() {
            when(appRoleRepository.findAll()).thenReturn(Collections.emptyList());

            List<AppRoleDto> result = appService.getAllAppRolesForAdmin();

            assertThat(result).isEmpty();
            verify(appRoleRepository).findAll();
        }
    }

    @Nested
    @DisplayName("getAppRolesByApp Tests")
    class GetAppRolesByAppTests {

        @Test
        void returnsAppRoles_forSpecificApp() {
            String appName = "TestApp";
            App app = App.builder().id(UUID.randomUUID()).name(appName).build();
            AppRole role1 = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("Role 1")
                    .app(app)
                    .build();
            AppRole role2 = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("Role 2")
                    .app(app)
                    .build();

            when(appRoleRepository.findByApp_Name(appName))
                    .thenReturn(List.of(role1, role2));

            List<AppRoleDto> result = appService.getAppRolesByApp(appName);

            assertThat(result).hasSize(2);
            verify(appRoleRepository).findByApp_Name(appName);
        }

        @Test
        void returnsEmptyList_whenAppHasNoRoles() {
            String appName = "AppWithNoRoles";
            when(appRoleRepository.findByApp_Name(appName))
                    .thenReturn(Collections.emptyList());

            List<AppRoleDto> result = appService.getAppRolesByApp(appName);

            assertThat(result).isEmpty();
            verify(appRoleRepository).findByApp_Name(appName);
        }
    }

    @Nested
    @DisplayName("UserType Restrictions Tests")
    class UserTypeRestrictionsTests {

        @Test
        void mapToAppRoleAdminDtoForView_setsUserTypeRestrictionStr_withSingleType() {
            App app = App.builder().id(UUID.randomUUID()).name("Test App").build();
            AppRole appRole = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("External Only Role")
                    .app(app)
                    .authzRole(false)
                    .userTypeRestriction(new UserType[]{UserType.EXTERNAL})
                    .build();

            when(appRoleRepository.findAll()).thenReturn(List.of(appRole));

            List<AppRoleDto> result = appService.getAllAppRolesForAdmin();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserTypeRestrictionStr()).isEqualTo("EXTERNAL");
        }

        @Test
        void mapToAppRoleAdminDtoForView_setsUserTypeRestrictionStr_withMultipleTypes() {
            App app = App.builder().id(UUID.randomUUID()).name("Test App").build();
            AppRole appRole = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("Multi Type Role")
                    .app(app)
                    .authzRole(false)
                    .userTypeRestriction(new UserType[]{UserType.EXTERNAL, UserType.INTERNAL})
                    .build();

            when(appRoleRepository.findAll()).thenReturn(List.of(appRole));

            List<AppRoleDto> result = appService.getAllAppRolesForAdmin();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserTypeRestrictionStr())
                    .contains("EXTERNAL", "INTERNAL");
        }

        @Test
        void mapToAppRoleAdminDtoForView_setsEmptyUserTypeRestrictionStr_whenNoRestrictions() {
            App app = App.builder().id(UUID.randomUUID()).name("Test App").build();
            AppRole appRole = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("Unrestricted Role")
                    .app(app)
                    .authzRole(false)
                    .userTypeRestriction(null)
                    .build();

            when(appRoleRepository.findAll()).thenReturn(List.of(appRole));

            List<AppRoleDto> result = appService.getAllAppRolesForAdmin();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserTypeRestrictionStr()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Role Group Tests")
    class RoleGroupTests {

        @Test
        void mapToAppRoleAdminDtoForView_setsAuthorizationGroup_forAuthzRoles() {
            App app = App.builder().id(UUID.randomUUID()).name("Test App").build();
            AppRole appRole = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("Authorization Role")
                    .app(app)
                    .authzRole(true)
                    .build();

            when(appRoleRepository.findAll()).thenReturn(List.of(appRole));

            List<AppRoleDto> result = appService.getAllAppRolesForAdmin();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoleGroup()).isEqualTo("Authorization");
        }

        @Test
        void mapToAppRoleAdminDtoForView_setsCcmsGroup_forCcmsRoles() {
            App app = App.builder().id(UUID.randomUUID()).name("Test App").build();
            AppRole appRole = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("CCMS Role")
                    .app(app)
                    .authzRole(false)
                    .ccmsCode("XXCCMS_PUI_CASEWORKER")
                    .build();

            when(appRoleRepository.findAll()).thenReturn(List.of(appRole));

            List<AppRoleDto> result = appService.getAllAppRolesForAdmin();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoleGroup()).isEqualTo("CCMS");
        }

        @Test
        void mapToAppRoleAdminDtoForView_setsDefaultGroup_forNonAuthzNonCcmsRoles() {
            App app = App.builder().id(UUID.randomUUID()).name("Test App").build();
            AppRole appRole = AppRole.builder()
                    .id(UUID.randomUUID())
                    .name("Default Role")
                    .app(app)
                    .authzRole(false)
                    .ccmsCode("SOME_OTHER_CODE")
                    .build();

            when(appRoleRepository.findAll()).thenReturn(List.of(appRole));

            List<AppRoleDto> result = appService.getAllAppRolesForAdmin();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoleGroup()).isEqualTo("Default");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        void getAllLaaApps_withSingleApp_returnsSingleApp() {
            App app = App.builder()
                    .id(UUID.randomUUID())
                    .name("Single App")
                    .appType(AppType.LAA)
                    .ordinal(1)
                    .build();

            when(appRepository.findAppsByAppType(AppType.LAA))
                    .thenReturn(List.of(app));

            List<AppDto> result = appService.getAllLaaApps();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Single App");
        }

        @Test
        void getAllAppsForAdmin_returnsSortedApps() {
            App app1 = App.builder()
                    .id(UUID.randomUUID())
                    .name("App Z")
                    .ordinal(3)
                    .build();
            App app2 = App.builder()
                    .id(UUID.randomUUID())
                    .name("App A")
                    .ordinal(1)
                    .build();
            App app3 = App.builder()
                    .id(UUID.randomUUID())
                    .name("App M")
                    .ordinal(2)
                    .build();

            when(appRepository.findAll())
                    .thenReturn(List.of(app1, app2, app3));

            List<AppDto> result = appService.getAllAppsForAdmin();

            assertThat(result).hasSize(3);
        }

        @Test
        void getAppRolesByApp_withNullAppName_returnsEmptyList() {
            when(appRoleRepository.findByApp_Name(null))
                    .thenReturn(Collections.emptyList());

            List<AppRoleDto> result = appService.getAppRolesByApp(null);

            assertThat(result).isEmpty();
            verify(appRoleRepository).findByApp_Name(null);
        }

        @Test
        void getAppRolesByApp_withEmptyAppName_returnsEmptyList() {
            when(appRoleRepository.findByApp_Name(""))
                    .thenReturn(Collections.emptyList());

            List<AppRoleDto> result = appService.getAppRolesByApp("");

            assertThat(result).isEmpty();
            verify(appRoleRepository).findByApp_Name("");
        }

        @Test
        void getAllActiveLaaApps_whenNoAppsEnabled_returnsEmptyList() {
            when(appRepository.findAppsByAppTypeAndEnabled(AppType.LAA, true))
                    .thenReturn(new ArrayList<>());

            List<AppDto> result = appService.getAllActiveLaaApps();

            assertThat(result).isEmpty();
            assertThat(result).isNotNull();
        }
    }
}
