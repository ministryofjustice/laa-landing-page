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
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
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

    private final UUID gbAdminId = UUID.randomUUID();
    private final UUID exAdminId = UUID.randomUUID();
    private final UUID exManId = UUID.randomUUID();
    private final UUID firmManId = UUID.randomUUID();
    App app;
    AppRole gbAdmin;
    AppRole exMan;
    AppRole exAdmin;
    AppRole firmMan;
    ModelMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MapperConfig().modelMapper();
        roleAssignmentService = new RoleAssignmentService(roleAssignmentRepository, appRepository, appRoleRepository, mapper);
        app = App.builder().id(UUID.randomUUID()).name("app").securityGroupOid("sec_grp_oid").securityGroupName("sec_grp_name").build();
        gbAdmin = AppRole.builder().id(gbAdminId).name("globalAdmin").description("appRole1").userTypeRestriction(new UserType[] {UserType.EXTERNAL}).app(app).authzRole(true).build();
        exAdmin = AppRole.builder().id(exAdminId).name("externalAdmin").description("appRole2").userTypeRestriction(new UserType[] {UserType.EXTERNAL}).app(app).authzRole(true).build();
        exMan = AppRole.builder().id(exManId).name("externalManager").description("appRole3").userTypeRestriction(new UserType[] {UserType.EXTERNAL}).app(app).authzRole(true).build();
        firmMan = AppRole.builder().id(firmManId).name("firmManager").description("appRole4").userTypeRestriction(new UserType[]{UserType.EXTERNAL}).app(app).authzRole(true).build();
        app.setAppRoles(Set.of(gbAdmin, exAdmin, exMan, firmMan));
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
        List<String> targetRoles = List.of(exAdminId.toString(), exManId.toString(), viewCrimeId.toString());
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(List.of(exAdminId, exManId, viewCrimeId), true)).thenReturn(List.of(exAdmin, exMan));
        assertThat(roleAssignmentService.canAssignRole(editorRoles, targetRoles)).isTrue();
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
        Set<AppRole> editorRoles = Set.of(gbAdmin);
        AppRoleDto exAdminDto = new AppRoleDto();
        exAdminDto.setId(exAdminId.toString());
        AppRoleDto exManDto = new AppRoleDto();
        exManDto.setId(exManId.toString());
        List<AppRoleDto> targetRoles = List.of(exAdminDto, exManDto);
        assertThat(roleAssignmentService.filterRoles(editorRoles, targetRoles)).hasSize(2);
    }

    @Test
    void filterRoles_exManager() {
        Set<AppRole> editorRoles = Set.of(exMan);
        AppRoleDto exAdminDto = new AppRoleDto();
        exAdminDto.setId(exAdminId.toString());
        AppRoleDto exManDto = new AppRoleDto();
        exManDto.setId(gbAdminId.toString());
        List<AppRoleDto> targetRoles = List.of(exAdminDto, exManDto);
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
        List<AppRoleDto> targetRoles = List.of(exAdminDto, exManDto, viewCrimeDto);
        Set<AppRole> editorRoles = Set.of(exMan);
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(List.of(exAdminId, gbAdminId, viewCrimeId), false)).thenReturn(List.of(viewCrime));
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
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(anyList(), any(Boolean.class)))
                .thenReturn(List.of(exMan, exAdmin, gbAdmin, firmMan));
        AppDto appDto = mapper.map(app, AppDto.class);
        assertThat(roleAssignmentService.canUserAssignRolesForApp(userProfile, appDto)).isFalse();
    }

    @Test
    void canUserAssignRolesForNonAuthzApp_ok() {
        UserProfile userProfile = UserProfile.builder().id(UUID.randomUUID()).appRoles(Set.of(firmMan)).build();
        App ccmsApp = App.builder().id(UUID.randomUUID()).name("ccms").securityGroupOid("sec_grp_oid")
                .securityGroupName("sec_grp_name").build();
        AppRole ccmsAppRole = AppRole.builder().id(UUID.randomUUID()).name("ccmsRole").description("ccmsRole")
                .userTypeRestriction(new UserType[]{UserType.EXTERNAL}).app(ccmsApp).authzRole(false).build();
        ccmsApp.setAppRoles(Set.of(ccmsAppRole));
        AppDto appDto = mapper.map(ccmsApp, AppDto.class);
        when(appRepository.findById(any())).thenReturn(java.util.Optional.of(ccmsApp));
        assertThat(roleAssignmentService.canUserAssignRolesForApp(userProfile, appDto)).isTrue();
    }

    @Test
    void canUserAssignRolesForApp_fail_fum_to_fum() {
        app.setAppRoles(Set.of(firmMan));
        when(appRepository.findById(any())).thenReturn(java.util.Optional.of(app));
        when(appRoleRepository.findAllByIdInAndAuthzRoleIs(anyList(), any(Boolean.class)))
                .thenReturn(List.of(firmMan));
        AppDto appDto = mapper.map(app, AppDto.class);
        UserProfile userProfile = UserProfile.builder().id(UUID.randomUUID()).appRoles(Set.of(firmMan)).build();

        assertThat(roleAssignmentService.canUserAssignRolesForApp(userProfile, appDto)).isFalse();
    }
}
