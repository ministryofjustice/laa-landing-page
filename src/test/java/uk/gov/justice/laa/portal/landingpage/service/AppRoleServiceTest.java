package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.DeleteAppRoleEvent;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppRoleServiceTest {

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private AppRepository appRepository;

    @Mock
    private EventService eventService;

    @Mock
    private LoginService loginService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;
    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private AppRoleService appRoleService;

    @BeforeEach
    void setUp() {
        appRoleService = new AppRoleService(appRoleRepository, appRepository, eventService, loginService,
                modelMapper, roleAssignmentRepository, userProfileRepository);
        SecurityContextHolder.setContext(securityContext);
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
    void getAllLaaAppRoles_returnsSortedRolesByParentAppAndOrdinal() {
        App appA = App.builder().id(UUID.randomUUID()).name("App A").appType(AppType.LAA).build();
        App appB = App.builder().id(UUID.randomUUID()).name("App B").appType(AppType.LAA).build();

        AppRole r1 = AppRole.builder().id(UUID.randomUUID()).name("R1").ordinal(2).app(appA).ccmsCode(null).authzRole(false).build();
        AppRole r2 = AppRole.builder().id(UUID.randomUUID()).name("R2").ordinal(1).app(appA).ccmsCode(null).authzRole(false).build();
        AppRole r3 = AppRole.builder().id(UUID.randomUUID()).name("R3").ordinal(0).app(appB).ccmsCode(null).authzRole(false).build();

        when(appRoleRepository.findByApp_AppType(AppType.LAA)).thenReturn(List.of(r1, r2, r3));

        List<AppRoleAdminDto> result = appRoleService.getAllLaaAppRoles();

        // Expect sorted by parent app name then ordinal -> App A (ord 1 then 2), then App B (ord 0)
        assertThat(result).extracting(AppRoleAdminDto::getParentApp).containsExactly("App A", "App A", "App B");
        assertThat(result).extracting(AppRoleAdminDto::getOrdinal).containsExactly(1, 2, 0);
    }

    @Test
    void getLaaAppRolesByAppName_filtersByGivenAppAndSortsByOrdinal() {
        String appName = "My App";
        App app = App.builder().id(UUID.randomUUID()).name(appName).appType(AppType.LAA).build();
        AppRole a = AppRole.builder().id(UUID.randomUUID()).name("A").ordinal(2).app(app).build();
        AppRole b = AppRole.builder().id(UUID.randomUUID()).name("B").ordinal(1).app(app).build();

        when(appRoleRepository.findByApp_NameAndApp_AppType(appName, AppType.LAA)).thenReturn(List.of(a, b));

        List<AppRoleAdminDto> result = appRoleService.getLaaAppRolesByAppName(appName);

        assertThat(result).extracting(AppRoleAdminDto::getOrdinal).containsExactly(1, 2);
    }

    @Test
    void getByIds_returnsMappedDtos_whenAllFound() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        AppRole ar1 = AppRole.builder().id(id1).name("X").build();
        AppRole ar2 = AppRole.builder().id(id2).name("Y").build();

        when(appRoleRepository.findAllById(List.of(id1, id2))).thenReturn(List.of(ar1, ar2));
        when(modelMapper.map(ar1, AppRoleDto.class)).thenReturn(AppRoleDto.builder().id(ar1.getId().toString())
                .name(ar1.getName()).ordinal(ar1.getOrdinal()).description(ar1.getDescription()).ccmsCode(ar1.getCcmsCode()).app(null).userTypeRestriction(null).build());
        when(modelMapper.map(ar2, AppRoleDto.class)).thenReturn(AppRoleDto.builder().id(ar2.getId().toString())
                .name(ar2.getName()).ordinal(ar2.getOrdinal()).description(ar2.getDescription()).ccmsCode(ar2.getCcmsCode()).app(null).userTypeRestriction(null).build());

        List<AppRoleDto> result = appRoleService.getByIds(List.of(id1.toString(), id2.toString()));

        assertThat(result).hasSize(2).extracting(AppRoleDto::getId).containsExactlyInAnyOrder(id1.toString(), id2.toString());
    }

    @Test
    void getByIds_throwsWhenNotAllFound() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        AppRole ar1 = AppRole.builder().id(id1).name("X").build();

        when(appRoleRepository.findAllById(List.of(id1, id2))).thenReturn(List.of(ar1));

        assertThatThrownBy(() -> appRoleService.getByIds(List.of(id1.toString(), id2.toString()))).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to load all app roles");
    }

    @Test
    void findById_mapsToDtoWhenPresent() {
        UUID id = UUID.randomUUID();
        AppRole ar = AppRole.builder().id(id).name("Name").build();
        when(appRoleRepository.findById(id)).thenReturn(Optional.of(ar));
        when(modelMapper.map(ar, AppRoleDto.class)).thenReturn(AppRoleDto.builder().id(ar.getId().toString())
                .name(ar.getName()).ordinal(ar.getOrdinal()).description(ar.getDescription()).ccmsCode(ar.getCcmsCode()).app(null).userTypeRestriction(null).build());

        Optional<AppRoleDto> result = appRoleService.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id.toString());
    }

    @Test
    void save_updatesExistingEntityAndSaves() {
        UUID id = UUID.randomUUID();
        AppRole existing = AppRole.builder().id(id).name("Old").description("Old desc").build();
        AppRoleDto dto = AppRoleDto.builder().id(id.toString()).name("New").ordinal(0).description("New desc").ccmsCode(null).app(null).userTypeRestriction(null).build();

        when(appRoleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(appRoleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppRole saved = appRoleService.save(dto);

        assertThat(saved.getName()).isEqualTo("New");
        assertThat(saved.getDescription()).isEqualTo("New desc");
    }

    @Test
    void save_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        AppRoleDto dto = AppRoleDto.builder().id(id.toString()).name("New").ordinal(0).description("New desc").ccmsCode(null).app(null).userTypeRestriction(null).build();

        when(appRoleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appRoleService.save(dto)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateAppRolesOrder_updatesOrdinalsOfEntities() {
        UUID id = UUID.randomUUID();
        AppRole entity = AppRole.builder().id(id).ordinal(5).build();
        when(appRoleRepository.findById(id)).thenReturn(Optional.of(entity));

        AppRolesOrderForm.AppRolesOrderDetailsForm form = AppRolesOrderForm.AppRolesOrderDetailsForm.builder().appRoleId(id.toString()).ordinal(2).build();

        appRoleService.updateAppRolesOrder(List.of(form));

        assertThat(entity.getOrdinal()).isEqualTo(2);
    }

    @Test
    void mapToAppRoleAdminDto_handlesUserTypeRestriction_authz_ccms_and_nullParentApp() {
        UUID appId = UUID.randomUUID();
        App app = App.builder().id(appId).name("Parent App").build();

        AppRole authzRole = AppRole.builder().id(UUID.randomUUID()).name("Authz").description("Authz role").ordinal(1)
                .authzRole(true).ccmsCode("XXCCMS_TEST").userTypeRestriction(new UserType[]{UserType.INTERNAL, UserType.EXTERNAL}).app(app).build();

        AppRole ccmsRole = AppRole.builder().id(UUID.randomUUID()).name("CCMS").description("CCMS role").ordinal(2).legacySync(true)
                .authzRole(false).ccmsCode("XXCCMS_FIRM_X").userTypeRestriction(new UserType[]{UserType.INTERNAL}).app(app).build();

        AppRole defaultRole = AppRole.builder().id(UUID.randomUUID()).name("Default").description("Default role").ordinal(3)
                .authzRole(false).ccmsCode(null).userTypeRestriction(null).app(app).build();

        AppRole noAppRole = AppRole.builder().id(UUID.randomUUID()).name("NoApp").description("No app role").ordinal(4)
                .authzRole(false).ccmsCode(null).userTypeRestriction(null).app(null).build();

        when(appRoleRepository.findByApp_AppType(AppType.LAA)).thenReturn(List.of(authzRole, ccmsRole, defaultRole, noAppRole));

        List<AppRoleAdminDto> dtos = appRoleService.getAllLaaAppRoles();

        // find DTOs by id
        AppRoleAdminDto authzDto = dtos.stream().filter(d -> d.getId().equals(authzRole.getId().toString())).findFirst().orElseThrow();
        AppRoleAdminDto ccmsDto = dtos.stream().filter(d -> d.getId().equals(ccmsRole.getId().toString())).findFirst().orElseThrow();
        AppRoleAdminDto defaultDto = dtos.stream().filter(d -> d.getId().equals(defaultRole.getId().toString())).findFirst().orElseThrow();
        AppRoleAdminDto noAppDto = dtos.stream().filter(d -> d.getId().equals(noAppRole.getId().toString())).findFirst().orElseThrow();

        // authzRole should have userTypeRestriction joined and roleGroup Authorization
        assertThat(authzDto.getUserTypeRestriction()).isEqualTo("INTERNAL, EXTERNAL");
        assertThat(authzDto.getLegacySync()).isEqualTo("No");
        assertThat(authzDto.getParentApp()).isEqualTo("Parent App");
        assertThat(authzDto.getParentAppId()).isEqualTo(appId.toString());
        assertThat(authzDto.getCcmsCode()).isEqualTo("XXCCMS_TEST");

        // ccms role should have roleGroup CCMS and single user type
        assertThat(ccmsDto.getLegacySync()).isEqualTo("Yes");
        assertThat(ccmsDto.getUserTypeRestriction()).isEqualTo("INTERNAL");
        assertThat(ccmsDto.getCcmsCode()).isEqualTo("XXCCMS_FIRM_X");

        // default role should have empty userTypeRestriction and Default roleGroup
        assertThat(defaultDto.getUserTypeRestriction()).isEqualTo("");
        assertThat(defaultDto.getLegacySync()).isEqualTo("No");
        assertThat(defaultDto.getCcmsCode()).isEqualTo("");

        // noAppRole should have empty parent app fields
        assertThat(noAppDto.getParentApp()).isEqualTo("");
        assertThat(noAppDto.getParentAppId()).isEqualTo("");
    }

    @Test
    void getLaaAppRolesByAppName_sortsByOrdinal_and_mapsCorrectly() {
        String appName = "FilterApp";
        App app = App.builder().id(UUID.randomUUID()).name(appName).build();

        AppRole first = AppRole.builder().id(UUID.randomUUID()).name("First").ordinal(5).app(app).ccmsCode(null).build();
        AppRole second = AppRole.builder().id(UUID.randomUUID()).name("Second").ordinal(1).app(app).ccmsCode(null).build();

        when(appRoleRepository.findByApp_NameAndApp_AppType(appName, AppType.LAA)).thenReturn(List.of(first, second));

        List<AppRoleAdminDto> dtos = appRoleService.getLaaAppRolesByAppName(appName);

        // should be sorted by ordinal ascending (1 then 5)
        assertThat(dtos).extracting(AppRoleAdminDto::getOrdinal).containsExactly(1, 5);
        assertThat(dtos).extracting(AppRoleAdminDto::getParentApp).containsOnly(appName);
    }

    private AppRole mkRole(UUID id, String name) {
        return AppRole.builder().id(id).name(name).build();
    }

    @Nested
    class DeleteAppRoleTests {

        @Test
        @DisplayName("deleteAppRole: happy path deletes dependents in order, deletes role, logs event with trimmed reason")
        void deleteAppRole_happyPath() {
            // Arrange
            UUID userProfileId = UUID.randomUUID();
            UUID entraOid = UUID.randomUUID();
            String appName = "MyApp";
            String reason = "  housekeeping  ";
            UUID roleId = UUID.randomUUID();
            String roleIdStr = roleId.toString();
            AppRole role = mkRole(roleId, "ADMIN");

            when(appRoleRepository.findById(roleId)).thenReturn(Optional.of(role));

            // Act
            appRoleService.deleteAppRole(userProfileId, entraOid, appName, roleIdStr, reason);

            // Assert: verify delete order
            InOrder inOrder = inOrder(
                    userProfileRepository,
                    roleAssignmentRepository,
                    appRoleRepository,
                    appRoleRepository // deleteRolePermissions + delete(appRole)
            );

            inOrder.verify(userProfileRepository).deleteAllByAppRoleId(roleId);
            inOrder.verify(roleAssignmentRepository).deleteByRoleIdInEitherColumn(roleId);
            inOrder.verify(appRoleRepository).deleteRolePermissions(roleId);
            inOrder.verify(appRoleRepository).delete(role);

            // Assert: verify event payload (trimmed reason)
            ArgumentCaptor<DeleteAppRoleEvent> evtCap = ArgumentCaptor.forClass(DeleteAppRoleEvent.class);
            verify(eventService).logEvent(evtCap.capture());

            DeleteAppRoleEvent evt = evtCap.getValue();
            assertThat(evt).isNotNull();
            assertThat(evt.getUserId()).isEqualTo(userProfileId);
            assertThat(evt.getEntraUserId()).isEqualTo(entraOid);
            assertThat(evt.getAppName()).isEqualTo(appName);
            assertThat(evt.getAppRoleName()).isEqualTo("ADMIN");
            assertThat(evt.getReason()).isEqualTo("housekeeping");

            // No more interactions beyond what we expect
            verifyNoMoreInteractions(eventService);
        }

        @Test
        @DisplayName("deleteAppRole: invalid UUID string -> IllegalArgumentException")
        void deleteAppRole_invalidUuid() {
            // Arrange
            String badRoleId = "not-a-uuid";

            // Act + Assert
            assertThatThrownBy(() ->
                    appRoleService.deleteAppRole(UUID.randomUUID(), UUID.randomUUID(), "App", badRoleId, "reason")
            ).isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(userProfileRepository, roleAssignmentRepository, appRoleRepository, eventService);
        }

        @Test
        @DisplayName("deleteAppRole: role not found -> EntityNotFoundException (and no side effects)")
        void deleteAppRole_roleNotFound() {
            // Arrange
            UUID roleId = UUID.randomUUID();
            when(appRoleRepository.findById(roleId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() ->
                    appRoleService.deleteAppRole(UUID.randomUUID(), UUID.randomUUID(), "App", roleId.toString(), "reason")
            ).isInstanceOf(EntityNotFoundException.class);

            verify(appRoleRepository).findById(roleId);
            verifyNoMoreInteractions(appRoleRepository);
            verifyNoInteractions(userProfileRepository, roleAssignmentRepository, eventService);
        }

    }

    @Nested
    class CountMethodsTests {

        @Test
        @DisplayName("countNoOfRoleAssignments: delegates to repository with parsed UUID")
        void countNoOfRoleAssignments_ok() {
            // Arrange
            UUID roleId = UUID.randomUUID();
            when(userProfileRepository.countUserProfilesByAppRoleId(roleId)).thenReturn(42L);

            // Act
            long count = appRoleService.countNoOfRoleAssignments(roleId.toString());

            // Assert
            assertThat(count).isEqualTo(42L);
            verify(userProfileRepository).countUserProfilesByAppRoleId(roleId);
            verifyNoMoreInteractions(userProfileRepository);
        }

        @Test
        @DisplayName("countNoOfRoleAssignments: invalid UUID -> IllegalArgumentException")
        void countNoOfRoleAssignments_invalidUuid() {
            assertThatThrownBy(() ->
                    appRoleService.countNoOfRoleAssignments("bad-uuid")
            ).isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(userProfileRepository);
        }

        @Test
        @DisplayName("countNoOfFirmsWithRoleAssignments: delegates to repository with parsed UUID")
        void countNoOfFirmsWithRoleAssignments_ok() {
            // Arrange
            UUID roleId = UUID.randomUUID();
            when(userProfileRepository.countFirmsWithRole(roleId)).thenReturn(7L);

            // Act
            long count = appRoleService.countNoOfFirmsWithRoleAssignments(roleId.toString());

            // Assert
            assertThat(count).isEqualTo(7L);
            verify(userProfileRepository).countFirmsWithRole(roleId);
            verifyNoMoreInteractions(userProfileRepository);
        }

        @Test
        @DisplayName("countNoOfFirmsWithRoleAssignments: invalid UUID -> IllegalArgumentException")
        void countNoOfFirmsWithRoleAssignments_invalidUuid() {
            assertThatThrownBy(() ->
                    appRoleService.countNoOfFirmsWithRoleAssignments("bad-uuid")
            ).isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(userProfileRepository);
        }
    }


    @Test
    void testEnrichRoleCreationDto_SetsAuthzRoleForManageYourUsers() {
        // Arrange
        UUID appId = UUID.randomUUID();
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .parentAppId(appId)
                .build();

        App app = App.builder()
                .id(appId)
                .name("Manage your users")
                .build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(appRoleRepository.findAll()).thenReturn(List.of());

        // Act
        RoleCreationDto result = appRoleService.enrichRoleCreationDto(dto);

        // Assert
        assertTrue(result.isAuthzRole());
        assertEquals("Manage your users", result.getParentAppName());
    }

    @Test
    void testEnrichRoleCreationDto_DoesNotSetAuthzRoleForOtherApps() {
        // Arrange
        UUID appId = UUID.randomUUID();
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .parentAppId(appId)
                .build();

        App app = App.builder()
                .id(appId)
                .name("Other App")
                .build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(appRoleRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        RoleCreationDto result = appRoleService.enrichRoleCreationDto(dto);

        // Assert
        assertFalse(result.isAuthzRole());
        assertEquals("Other App", result.getParentAppName());
    }

    @Test
    void testCreateRole_WithValidDto_CreatesRoleAndAuditEvent() {
        // Arrange
        UUID appId = UUID.randomUUID();

        App parentApp = App.builder()
                .id(appId)
                .name("Test App")
                .build();

        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Test User");

        when(appRepository.findById(appId)).thenReturn(Optional.of(parentApp));
        when(appRoleRepository.findByName("Test Role")).thenReturn(Optional.empty());

        AppRole savedRole = AppRole.builder()
                .id(UUID.randomUUID())
                .name("Test Role")
                .description("Test Description")
                .userTypeRestriction(new UserType[]{UserType.INTERNAL})
                .app(parentApp)
                .build();
        when(appRoleRepository.save(any(AppRole.class))).thenReturn(savedRole);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUser);

        EntraUser mockEntraUser = EntraUser.builder()
                .entraOid("test-entra-oid")
                .userProfiles(Set.of(
                        UserProfile.builder()
                                .id(UUID.randomUUID())
                                .activeProfile(true)
                                .build()
                ))
                .build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(mockEntraUser);

        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(appId)
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .legacySync(true)
                .build();
        // Act
        appRoleService.createRole(dto);

        // Assert
        verify(appRoleRepository).save(any(AppRole.class));
        verify(eventService).logEvent(any(RoleCreationAuditEvent.class));
    }

    @Test
    void testCreateRole_WithDuplicateName_ThrowsException() {
        // Arrange
        UUID appId = UUID.randomUUID();

        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Existing Role")
                .description("Test Description")
                .parentAppId(appId)
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .build();

        App existingApp = App.builder().id(appId).build();
        AppRole existingRole = AppRole.builder()
                .name("Existing Role")
                .app(existingApp)
                .build();

        when(appRoleRepository.findByName("Existing Role")).thenReturn(Optional.of(existingRole));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> appRoleService.createRole(dto));

        assertEquals("Role name 'Existing Role' already exists in this application", exception.getMessage());
    }

    @Test
    void testEnrichRoleCreationDto_GeneratesSequentialOrdinals() {
        // Arrange
        UUID appId = UUID.randomUUID();
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .parentAppId(appId)
                .build();

        App app = App.builder()
                .id(appId)
                .name("Test App")
                .build();

        AppRole role1 = AppRole.builder().ordinal(5).build();
        AppRole role2 = AppRole.builder().ordinal(10).build();
        AppRole role3 = AppRole.builder().ordinal(3).build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(appRoleRepository.findAll()).thenReturn(Arrays.asList(role1, role2, role3));

        // Act
        RoleCreationDto result = appRoleService.enrichRoleCreationDto(dto);

        // Assert
        assertEquals(11, result.getOrdinal());
    }

    @Test
    void testEnrichRoleCreationDto_WithNoExistingRoles_SetsOrdinalToOne() {
        // Arrange
        UUID appId = UUID.randomUUID();
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .parentAppId(appId)
                .build();

        App app = App.builder()
                .id(appId)
                .name("Test App")
                .build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(appRoleRepository.findAll()).thenReturn(List.of());

        // Act
        RoleCreationDto result = appRoleService.enrichRoleCreationDto(dto);

        // Assert
        assertEquals(1, result.getOrdinal());
    }

    @Test
    void testIsRoleNameExistsInApp_WithExistingRole_ReturnsTrue() {
        // Arrange
        UUID appId = UUID.randomUUID();
        String roleName = "Existing Role";

        App app = App.builder().id(appId).build();
        AppRole existingRole = AppRole.builder()
                .name("existing role")  // Different case
                .app(app)
                .build();

        when(appRoleRepository.findAll()).thenReturn(List.of(existingRole));

        // Act
        boolean result = appRoleService.isRoleNameExistsInApp(roleName, appId);

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsRoleNameExistsInApp_WithNonExistingRole_ReturnsFalse() {
        // Arrange
        UUID appId = UUID.randomUUID();
        String roleName = "Non-existing Role";

        when(appRoleRepository.findAll()).thenReturn(List.of());

        // Act
        boolean result = appRoleService.isRoleNameExistsInApp(roleName, appId);

        // Assert
        assertFalse(result);
    }

    @Test
    void testIsRoleNameExistsInApp_WithSameNameDifferentApp_ReturnsFalse() {
        // Arrange
        UUID appId1 = UUID.randomUUID();
        UUID appId2 = UUID.randomUUID();
        String roleName = "Test Role";

        App app1 = App.builder().id(appId1).build();
        AppRole roleInDifferentApp = AppRole.builder()
                .name(roleName)
                .app(app1)
                .build();

        when(appRoleRepository.findAll()).thenReturn(List.of(roleInDifferentApp));

        // Act
        boolean result = appRoleService.isRoleNameExistsInApp(roleName, appId2);

        // Assert
        assertFalse(result);
    }

    @Test
    void testCreateRole_WithNullLegacySync_DefaultsToFalse() {
        // Arrange
        UUID appId = UUID.randomUUID();

        App parentApp = App.builder()
                .id(appId)
                .name("Test App")
                .build();

        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Test User");

        when(appRepository.findById(appId)).thenReturn(Optional.of(parentApp));
        when(appRoleRepository.findByName("Test Role")).thenReturn(Optional.empty());

        AppRole savedRole = AppRole.builder()
                .id(UUID.randomUUID())
                .name("Test Role")
                .description("Test Description")
                .userTypeRestriction(new UserType[]{UserType.EXTERNAL})
                .legacySync(false)
                .app(parentApp)
                .build();

        when(appRoleRepository.save(any(AppRole.class))).thenReturn(savedRole);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUser);

        EntraUser mockEntraUser = EntraUser.builder()
                .entraOid("test-entra-oid")
                .userProfiles(Set.of(
                        UserProfile.builder()
                                .id(UUID.randomUUID())
                                .activeProfile(true)
                                .build()
                ))
                .build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(mockEntraUser);

        // Act
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(appId)
                .userTypeRestriction(List.of(UserType.EXTERNAL))
                .legacySync(null)
                .build();
        appRoleService.createRole(dto);

        // Assert
        verify(appRoleRepository).save(any(AppRole.class));
        verify(eventService).logEvent(any(RoleCreationAuditEvent.class));
    }

    @Test
    void testCreateRole_WithMultipleUserTypes_CreatesCorrectAuditEvent() {
        // Arrange
        UUID appId = UUID.randomUUID();

        App parentApp = App.builder()
                .id(appId)
                .name("Multi-User App")
                .build();

        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Admin User");

        when(appRepository.findById(appId)).thenReturn(Optional.of(parentApp));
        when(appRoleRepository.findByName("Multi-Type Role")).thenReturn(Optional.empty());

        AppRole savedRole = AppRole.builder()
                .id(UUID.randomUUID())
                .name("Multi-Type Role")
                .description("Role for multiple user types")
                .userTypeRestriction(new UserType[]{UserType.INTERNAL, UserType.EXTERNAL})
                .app(parentApp)
                .build();

        when(appRoleRepository.save(any(AppRole.class))).thenReturn(savedRole);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUser);

        EntraUser mockEntraUser = EntraUser.builder()
                .entraOid("test-entra-oid")
                .userProfiles(Set.of(
                        UserProfile.builder()
                                .id(UUID.randomUUID())
                                .activeProfile(true)
                                .build()
                ))
                .build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(mockEntraUser);

        // Act
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Multi-Type Role")
                .description("Role for multiple user types")
                .parentAppId(appId)
                .userTypeRestriction(List.of(UserType.INTERNAL, UserType.EXTERNAL))
                .legacySync(true)
                .build();
        appRoleService.createRole(dto);

        // Assert
        verify(appRoleRepository).save(any(AppRole.class));
        verify(eventService).logEvent(any(RoleCreationAuditEvent.class));
    }

    @Test
    void testCreateRole_WithEmptyCcmsCode_ConvertsToNull() {
        // Arrange
        UUID appId = UUID.randomUUID();

        App parentApp = App.builder()
                .id(appId)
                .name("Test App")
                .build();

        CurrentUserDto currentUser = new CurrentUserDto();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setName("Test User");

        when(appRepository.findById(appId)).thenReturn(Optional.of(parentApp));
        when(appRoleRepository.findByName("Test Role")).thenReturn(Optional.empty());

        AppRole savedRole = AppRole.builder()
                .id(UUID.randomUUID())
                .name("Test Role")
                .description("Test Description")
                .userTypeRestriction(new UserType[]{UserType.INTERNAL})
                .ccmsCode(null)  // Should be converted to null
                .app(parentApp)
                .build();

        when(appRoleRepository.save(any(AppRole.class))).thenReturn(savedRole);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUser);

        EntraUser mockEntraUser = EntraUser.builder()
                .entraOid("test-entra-oid")
                .userProfiles(Set.of(
                        UserProfile.builder()
                                .id(UUID.randomUUID())
                                .activeProfile(true)
                                .build()
                ))
                .build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(mockEntraUser);

        // Act
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(appId)
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .ccmsCode("")
                .legacySync(false)
                .build();
        appRoleService.createRole(dto);

        // Assert
        verify(appRoleRepository).save(any(AppRole.class));
        verify(eventService).logEvent(any(RoleCreationAuditEvent.class));
    }

    @Test
    void testCreateRole_WithAppNotFound_ThrowsException() {
        // Arrange
        UUID appId = UUID.randomUUID();

        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .description("Test Description")
                .parentAppId(appId)
                .userTypeRestriction(List.of(UserType.INTERNAL))
                .build();

        when(appRepository.findById(appId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> appRoleService.createRole(dto));
    }

    @Test
    void testEnrichRoleCreationDto_SetsRandomId() {
        // Arrange
        UUID appId = UUID.randomUUID();
        RoleCreationDto dto = RoleCreationDto.builder()
                .name("Test Role")
                .parentAppId(appId)
                .build();

        App app = App.builder()
                .id(appId)
                .name("Test App")
                .build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(appRoleRepository.findAll()).thenReturn(List.of());

        // Act
        RoleCreationDto result = appRoleService.enrichRoleCreationDto(dto);

        // Assert
        assertTrue(result.getId() != null);
        assertEquals("Test Role", result.getName());
        assertEquals("Test App", result.getParentAppName());
    }
}
