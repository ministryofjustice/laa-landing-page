package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.RoleType;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoleAssignmentServiceTest {

    @InjectMocks
    private RoleAssignmentService roleAssignmentService;

    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;

    private UUID gbAdminId = UUID.randomUUID();
    private UUID exAdminId = UUID.randomUUID();
    private UUID exManId = UUID.randomUUID();
    AppRole gbAdmin;
    AppRole exMan;
    AppRole exAdmin;

    @BeforeEach
    void setUp() {
        roleAssignmentService = new RoleAssignmentService(roleAssignmentRepository, new MapperConfig().modelMapper());
        App app = App.builder().name("app").securityGroupOid("sec_grp_oid").securityGroupName("sec_grp_name").build();
        gbAdmin = AppRole.builder().id(gbAdminId).name("globalAdmin").description("appRole1").roleType(RoleType.EXTERNAL).app(app).build();
        exAdmin = AppRole.builder().id(exAdminId).name("externalAdmin").description("appRole2").roleType(RoleType.EXTERNAL).app(app).build();
        exMan = AppRole.builder().id(exManId).name("externalManager").description("appRole3").roleType(RoleType.EXTERNAL).app(app).build();
        RoleAssignment roleAssignment1 = RoleAssignment.builder().assigningRole(gbAdmin).assignableRole(exAdmin).build();
        RoleAssignment roleAssignment2 = RoleAssignment.builder().assigningRole(gbAdmin).assignableRole(exMan).build();

        lenient().when(roleAssignmentRepository.findByAssigningRole_IdIn(List.of(gbAdminId))).thenReturn(List.of(roleAssignment1, roleAssignment2));
        lenient().when(roleAssignmentRepository.findByAssigningRole_IdIn(List.of(exAdminId))).thenReturn(List.of());
    }

    @Test
    void canAssignRole_ok() {
        Set<AppRole> editorRoles = Set.of(gbAdmin);
        List<String> targetRoles = List.of(exAdminId.toString(), exManId.toString());
        assertThat(roleAssignmentService.canAssignRole(editorRoles, targetRoles)).isTrue();
    }

    @Test
    void canAssignRole_fail() {
        Set<AppRole> editorRoles = Set.of(exAdmin);
        List<String> targetRoles = List.of(gbAdminId.toString(), exManId.toString());
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
}
