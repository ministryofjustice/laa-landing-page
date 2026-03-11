package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateAppRoleAssignRestrictionsAuditEvent;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoleAssignmentServiceTest {

    @InjectMocks
    private RoleAssignmentService roleAssignmentService;

    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;
    @Mock
    private AppRoleRepository appRoleRepository;
    @Mock
    private AppRepository appRepository;
    @Mock
    private EventService eventService;

    private final UUID gbAdminId = UUID.randomUUID();
    private final UUID exAdminId = UUID.randomUUID();
    private final UUID exManId = UUID.randomUUID();
    private final UUID firmManId = UUID.randomUUID();
    private CurrentUserDto currentUserDto;
    App app;
    AppRole gbAdmin;
    AppRole exMan;
    AppRole exAdmin;
    AppRole firmMan;
    ModelMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MapperConfig().modelMapper();
        roleAssignmentService = new RoleAssignmentService(roleAssignmentRepository, appRepository, appRoleRepository, mapper, eventService);
        app = App.builder().id(UUID.randomUUID()).name("app").securityGroupOid("sec_grp_oid").securityGroupName("sec_grp_name").build();
        gbAdmin = AppRole.builder().id(gbAdminId).name("globalAdmin").description("appRole1").userTypeRestriction(new UserType[] {UserType.EXTERNAL}).app(app).authzRole(true).build();
        exAdmin = AppRole.builder().id(exAdminId).name("externalAdmin").description("appRole2").userTypeRestriction(new UserType[] {UserType.EXTERNAL}).app(app).authzRole(true).build();
        exMan = AppRole.builder().id(exManId).name("externalManager").description("appRole3").userTypeRestriction(new UserType[] {UserType.EXTERNAL}).app(app).authzRole(true).build();
        firmMan = AppRole.builder().id(firmManId).name("firmManager").description("appRole4").userTypeRestriction(new UserType[]{UserType.EXTERNAL}).app(app).authzRole(true).build();
        app.setAppRoles(Set.of(gbAdmin, exAdmin, exMan, firmMan));
        currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        currentUserDto.setName("Admin User");
        RoleAssignment roleAssignment1 = RoleAssignment.builder().assigningRole(gbAdmin).assignableRole(exAdmin).build();
        RoleAssignment roleAssignment2 = RoleAssignment.builder().assigningRole(gbAdmin).assignableRole(exMan).build();
        RoleAssignment roleAssignment3 = RoleAssignment.builder().assigningRole(gbAdmin).assignableRole(firmMan).build();

        lenient().when(roleAssignmentRepository.findByAssigningRole_IdIn(List.of(gbAdminId))).thenReturn(List.of(roleAssignment1, roleAssignment2, roleAssignment3));
        lenient().when(roleAssignmentRepository.findByAssigningRole_IdIn(List.of(exAdminId))).thenReturn(List.of());
        lenient().when(roleAssignmentRepository.findByAssigningRole_IdIn(List.of(firmManId))).thenReturn(List.of());

    }

    @Test
    void canAssignRole_ok() {
        Set<AppRole> editorRoles = Set.of(gbAdmin);
        List<String> targetRoles = List.of(exAdminId.toString(), exManId.toString());
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(List.of(exAdminId, exManId), true)).thenReturn(List.of(exAdmin, exMan));
        assertThat(roleAssignmentService.canAssignRole(editorRoles, targetRoles)).isTrue();
    }

    @Test
    void canAssignRoleWithNonAuthzRole_ok() {
        Set<AppRole> editorRoles = Set.of(gbAdmin);
        UUID viewCrimeId = UUID.randomUUID();
        AppRole viewCrime = AppRole.builder().id(viewCrimeId).name("View Crime").authzRole(false).build();
        List<String> targetRoles = List.of(exAdminId.toString(), exManId.toString(), viewCrimeId.toString());
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(List.of(exAdminId, exManId, viewCrimeId), true)).thenReturn(List.of(exAdmin, exMan));
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(List.of(exAdminId, exManId, viewCrimeId), false)).thenReturn(List.of(viewCrime));
        assertThat(roleAssignmentService.canAssignRole(editorRoles, targetRoles)).isTrue();
    }

    @Test
    void canAssignRoleWithRestrictedNonAuthzRoleWhenRoleAssignmentExists() {
        Set<AppRole> editorRoles = Set.of(gbAdmin);
        UUID viewCrimeId = UUID.randomUUID();
        AppRole viewCrime = AppRole.builder().id(viewCrimeId).name("View Crime").authzRole(false).build();
        List<String> targetRoles = List.of(exAdminId.toString(), exManId.toString(), viewCrimeId.toString());
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(List.of(exAdminId, exManId, viewCrimeId), true)).thenReturn(List.of(exAdmin, exMan));
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(List.of(exAdminId, exManId, viewCrimeId), false)).thenReturn(List.of(viewCrime));
        when(roleAssignmentRepository.findByAssignableRole_Id(viewCrimeId)).thenReturn(List.of(RoleAssignment.builder().assigningRole(gbAdmin).assignableRole(viewCrime).build()));
        assertThat(roleAssignmentService.canAssignRole(editorRoles, targetRoles)).isTrue();
    }

    @Test
    void cannotAssignRoleWithRestrictedNonAuthzRoleWhenRoleAssignmentDoesNotExist() {
        Set<AppRole> editorRoles = Set.of(gbAdmin);
        UUID viewCrimeId = UUID.randomUUID();
        AppRole viewCrime = AppRole.builder().id(viewCrimeId).name("View Crime").authzRole(false).build();
        List<String> targetRoles = List.of(exAdminId.toString(), exManId.toString(), viewCrimeId.toString());
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(List.of(exAdminId, exManId, viewCrimeId), true)).thenReturn(List.of(exAdmin, exMan));
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(List.of(exAdminId, exManId, viewCrimeId), false)).thenReturn(List.of(viewCrime));
        when(roleAssignmentRepository.findByAssignableRole_Id(viewCrimeId)).thenReturn(List.of(RoleAssignment.builder().assigningRole(exMan).assignableRole(viewCrime).build()));
        assertThat(roleAssignmentService.canAssignRole(editorRoles, targetRoles)).isFalse();
    }

    @Test
    void canAssignRole_fail() {
        Set<AppRole> editorRoles = Set.of(exAdmin);
        List<String> targetRoles = List.of(gbAdminId.toString(), exManId.toString());
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(List.of(gbAdminId, exManId), true)).thenReturn(List.of(exAdmin, exMan));
        assertThat(roleAssignmentService.canAssignRole(editorRoles, targetRoles)).isFalse();
    }

    @Test
    void filterRoles_globalAdmin() {
        AppRoleDto exAdminDto = new AppRoleDto();
        exAdminDto.setId(exAdminId.toString());
        AppRoleDto exManDto = new AppRoleDto();
        exManDto.setId(exManId.toString());
        List<UUID> targetRoles = List.of(exAdminId, exManId);
        Set<AppRole> editorRoles = Set.of(gbAdmin);
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(targetRoles, true)).thenReturn(List.of(exAdmin, exMan));
        assertThat(roleAssignmentService.filterRoles(editorRoles, targetRoles)).hasSize(2);
    }

    @Test
    void filterRoles_exManager() {
        Set<AppRole> editorRoles = Set.of(exMan);
        AppRoleDto exAdminDto = new AppRoleDto();
        exAdminDto.setId(exAdminId.toString());
        AppRoleDto exManDto = new AppRoleDto();
        exManDto.setId(gbAdminId.toString());
        List<UUID> targetRoles = List.of(exAdminId, exManId);
        assertThat(roleAssignmentService.filterRoles(editorRoles, targetRoles)).hasSize(0);
    }

    @Test
    void filterRoles_exManager_withNoAuthzRole() {
        AppRoleDto exAdminDto = new AppRoleDto();
        exAdminDto.setId(exAdminId.toString());
        AppRoleDto exManDto = new AppRoleDto();
        exManDto.setId(gbAdminId.toString());
        UUID viewCrimeId = UUID.randomUUID();
        App crimeApp = App.builder().name("crime").securityGroupOid("sec_grp_oid").securityGroupName("sec_grp_name").build();
        AppRole viewCrime = AppRole.builder().id(viewCrimeId).name("View Crime Guy").description("appRole3")
                .userTypeRestriction(new UserType[] {UserType.EXTERNAL}).app(crimeApp).authzRole(false).build();
        AppRoleDto viewCrimeDto = new AppRoleDto();
        viewCrimeDto.setId(viewCrimeId.toString());
        List<UUID> targetRoles = List.of(exAdminId, exManId, viewCrimeId);
        Set<AppRole> editorRoles = Set.of(exMan);
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(targetRoles, true)).thenReturn(List.of(exAdmin, exMan));
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(targetRoles, false)).thenReturn(List.of(viewCrime));
        assertThat(roleAssignmentService.filterRoles(editorRoles, targetRoles)).hasSize(1);
    }

    @Test
    void canUserAssignRolesForApp_ok() {
        UserProfile userProfile = UserProfile.builder().id(UUID.randomUUID()).appRoles(Set.of(gbAdmin)).build();
        when(appRepository.findById(any())).thenReturn(java.util.Optional.of(app));
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(anyList(), any(Boolean.class)))
                .thenReturn(List.of(exMan, exAdmin, gbAdmin, firmMan));
        AppDto appDto = mapper.map(app, AppDto.class);
        assertThat(roleAssignmentService.canUserAssignRolesForApp(userProfile, appDto)).isTrue();
    }

    @Test
    void canUserAssignRolesForApp_fail_fum() {
        UserProfile userProfile = UserProfile.builder().id(UUID.randomUUID()).appRoles(Set.of(firmMan)).build();
        when(appRepository.findById(any())).thenReturn(java.util.Optional.of(app));
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(anyList(), eq(true)))
                .thenReturn(List.of(exMan, exAdmin, gbAdmin, firmMan));
        AppDto appDto = mapper.map(app, AppDto.class);
        assertThat(roleAssignmentService.canUserAssignRolesForApp(userProfile, appDto)).isFalse();
    }

    @Test
    void canUserAssignRolesForApp_fail_fum_to_fum() {
        app.setAppRoles(Set.of(firmMan));
        when(appRepository.findById(any())).thenReturn(java.util.Optional.of(app));
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(anyList(), eq(true)))
                .thenReturn(List.of(firmMan));
        AppDto appDto = mapper.map(app, AppDto.class);
        UserProfile userProfile = UserProfile.builder().id(UUID.randomUUID()).appRoles(Set.of(firmMan)).build();

        assertThat(roleAssignmentService.canUserAssignRolesForApp(userProfile, appDto)).isFalse();
    }

    @Test
    void getLaaAppRoleAssignmentRestrictions_shouldBuildMapCorrectly() {
        Object[] row1 = new Object[]{"A1", "Assignable1", "Desc1", "B1", "Assigning1"};
        Object[] row2 = new Object[]{"A1", "Assignable1", "Desc1", "B2", "Assigning2"};
        when(roleAssignmentRepository.findAssignableRolesWithAssigningRoles())
                .thenReturn(List.of(row1, row2));

        var result = roleAssignmentService.getLaaAppRoleAssignmentRestrictions();

        assertEquals(1, result.size());

        var key = result.keySet().iterator().next();
        assertEquals("A1", key.getId());
        assertEquals("Assignable1", key.getName());
        assertEquals("Desc1", key.getDescription());

        var values = result.get(key);
        assertEquals(2, values.size());
        assertEquals("B1", values.get(0).getId());
        assertEquals("B2", values.get(1).getId());
    }

    @Test
    void getLaaAppRoleAssignmentRestrictionsByAppName_shouldBuildMapCorrectly() {
        Object[] row = new Object[] { "A2", "AssignableX", "DescX", "C1", "AssigningX" };
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);

        when(roleAssignmentRepository.findAssignableRolesWithAssigningRolesByAppName("MyApp"))
                .thenReturn(rows);

        var result = roleAssignmentService.getLaaAppRoleAssignmentRestrictionsByAppName("MyApp");

        assertEquals(1, result.size());

        var key = result.keySet().iterator().next();
        assertEquals("A2", key.getId());
        assertEquals("AssignableX", key.getName());
    }

    @Test
    void updateRoleAssignmentRestrictions_shouldThrow_whenRoleAssignsItself() {

        UUID targetId = UUID.randomUUID();

        AppRole targetRole = AppRole.builder().id(targetId).name("TARGET_ROLE").build();
        List<String> ids = List.of(targetId.toString());

        when(appRoleRepository.findById(targetId)).thenReturn(Optional.of(targetRole));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> roleAssignmentService.updateRoleAssignmentRestrictions(currentUserDto, targetId.toString(), ids)
        );

        assertThat(ex.getMessage()).contains("cannot assign itself");
    }

    @Test
    void updateRoleAssignmentRestrictions_shouldThrow_whenAssignerMissing() {
        UUID existing = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        AppRole targetRole = AppRole.builder().id(targetId).name("TARGET_ROLE").build();

        when(appRoleRepository.findById(targetId)).thenReturn(Optional.of(targetRole));
        when(appRoleRepository.findAllById(any())).thenReturn(
                List.of(AppRole.builder().id(existing).name("ROLE_EXISTING").build())
        );

        List<String> ids = List.of(existing.toString(), missing.toString());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> roleAssignmentService.updateRoleAssignmentRestrictions(currentUserDto, targetId.toString(), ids)
        );

        assertThat(ex.getMessage()).contains("Assigning roles not found");
        assertThat(ex.getMessage()).contains(missing.toString());
    }

    @Test
    void updateRoleAssignmentRestrictions_shouldDeleteOnly_whenNoAssigners() {
        UUID targetId = UUID.randomUUID();

        AppRole targetRole = AppRole.builder().id(targetId).name("TARGET_ROLE").build();
        when(appRoleRepository.findById(targetId)).thenReturn(Optional.of(targetRole));

        roleAssignmentService.updateRoleAssignmentRestrictions(currentUserDto, targetId.toString(), null);

        verify(roleAssignmentRepository).deleteByAssignableRole(targetRole);
        verify(roleAssignmentRepository, never()).saveAll(any());
    }

    @Test
    void updateRoleAssignmentRestrictions_shouldCreateRoleAssignments() {
        UUID assigner1 = UUID.randomUUID();
        UUID assigner2 = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        AppRole targetRole = AppRole.builder().id(targetId).name("TARGET_ROLE").build();

        AppRole role1 = AppRole.builder().id(assigner1).name("ROLE_1").build();
        AppRole role2 = AppRole.builder().id(assigner2).name("ROLE_2").build();

        when(appRoleRepository.findById(targetId)).thenReturn(Optional.of(targetRole));
        when(appRoleRepository.findAllById(any())).thenReturn(List.of(role1, role2));

        List<String> ids = List.of(assigner1.toString(), assigner2.toString());

        roleAssignmentService.updateRoleAssignmentRestrictions(currentUserDto, targetId.toString(), ids);

        verify(roleAssignmentRepository).deleteByAssignableRole(targetRole);
        verify(roleAssignmentRepository).saveAll(argThat((List<RoleAssignment> list) ->
                list.size() == 2
                        && list.stream().allMatch(ra -> ra.getAssignableRole().equals(targetRole))
        ));
        verify(eventService).logEvent(isA(UpdateAppRoleAssignRestrictionsAuditEvent.class));
    }

}
