package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppRoleServiceTest {

    @Mock
    private AppRoleRepository appRoleRepository;
    @Mock
    private ModelMapper modelMapper;

    private AppRoleService appRoleService;

    @BeforeEach
    void setUp() {
        appRoleService = new AppRoleService(appRoleRepository, modelMapper);
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
        when(modelMapper.map(ar1, AppRoleDto.class)).thenReturn(AppRoleDto.builder().id(ar1.getId().toString()).name(ar1.getName()).ordinal(ar1.getOrdinal()).description(ar1.getDescription()).ccmsCode(ar1.getCcmsCode()).app(null).userTypeRestriction(null).build());
        when(modelMapper.map(ar2, AppRoleDto.class)).thenReturn(AppRoleDto.builder().id(ar2.getId().toString()).name(ar2.getName()).ordinal(ar2.getOrdinal()).description(ar2.getDescription()).ccmsCode(ar2.getCcmsCode()).app(null).userTypeRestriction(null).build());

        List<AppRoleDto> result = appRoleService.getByIds(List.of(id1.toString(), id2.toString()));

        assertThat(result).hasSize(2).extracting(AppRoleDto::getId).containsExactlyInAnyOrder(id1.toString(), id2.toString());
    }

    @Test
    void getByIds_throwsWhenNotAllFound() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        AppRole ar1 = AppRole.builder().id(id1).name("X").build();

        when(appRoleRepository.findAllById(List.of(id1, id2))).thenReturn(List.of(ar1));

        assertThatThrownBy(() -> appRoleService.getByIds(List.of(id1.toString(), id2.toString())))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to load all app roles");
    }

    @Test
    void findById_mapsToDtoWhenPresent() {
        UUID id = UUID.randomUUID();
        AppRole ar = AppRole.builder().id(id).name("Name").build();
        when(appRoleRepository.findById(id)).thenReturn(Optional.of(ar));
        when(modelMapper.map(ar, uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto.class))
            .thenReturn(AppRoleDto.builder().id(ar.getId().toString()).name(ar.getName()).ordinal(ar.getOrdinal()).description(ar.getDescription()).ccmsCode(ar.getCcmsCode()).app(null).userTypeRestriction(null).build());

        Optional<uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto> result = appRoleService.findById(id);

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

        AppRolesOrderForm.AppRolesOrderDetailsForm form = AppRolesOrderForm.AppRolesOrderDetailsForm.builder()
            .appRoleId(id.toString()).ordinal(2).build();

        appRoleService.updateAppRolesOrder(List.of(form));

        assertThat(entity.getOrdinal()).isEqualTo(2);
    }

    @Test
    void mapToAppRoleAdminDto_handlesUserTypeRestriction_authz_ccms_and_nullParentApp() {
        UUID appId = UUID.randomUUID();
        App app = App.builder().id(appId).name("Parent App").build();

        AppRole authzRole = AppRole.builder()
            .id(UUID.randomUUID())
            .name("Authz")
            .description("Authz role")
            .ordinal(1)
            .authzRole(true)
            .ccmsCode("XXCCMS_TEST")
            .userTypeRestriction(new UserType[]{UserType.INTERNAL, UserType.EXTERNAL})
            .app(app)
            .build();

        AppRole ccmsRole = AppRole.builder()
            .id(UUID.randomUUID())
            .name("CCMS")
            .description("CCMS role")
            .ordinal(2)
            .authzRole(false)
            .ccmsCode("XXCCMS_FIRM_X")
            .userTypeRestriction(new UserType[]{UserType.INTERNAL})
            .app(app)
            .build();

        AppRole defaultRole = AppRole.builder()
            .id(UUID.randomUUID())
            .name("Default")
            .description("Default role")
            .ordinal(3)
            .authzRole(false)
            .ccmsCode(null)
            .userTypeRestriction(null)
            .app(app)
            .build();

        AppRole noAppRole = AppRole.builder()
            .id(UUID.randomUUID())
            .name("NoApp")
            .description("No app role")
            .ordinal(4)
            .authzRole(false)
            .ccmsCode(null)
            .userTypeRestriction(null)
            .app(null)
            .build();

        when(appRoleRepository.findByApp_AppType(AppType.LAA)).thenReturn(List.of(authzRole, ccmsRole, defaultRole, noAppRole));

        List<AppRoleAdminDto> dtos = appRoleService.getAllLaaAppRoles();

        // find DTOs by id
        AppRoleAdminDto authzDto = dtos.stream().filter(d -> d.getId().equals(authzRole.getId().toString())).findFirst().orElseThrow();
        AppRoleAdminDto ccmsDto = dtos.stream().filter(d -> d.getId().equals(ccmsRole.getId().toString())).findFirst().orElseThrow();
        AppRoleAdminDto defaultDto = dtos.stream().filter(d -> d.getId().equals(defaultRole.getId().toString())).findFirst().orElseThrow();
        AppRoleAdminDto noAppDto = dtos.stream().filter(d -> d.getId().equals(noAppRole.getId().toString())).findFirst().orElseThrow();

        // authzRole should have userTypeRestriction joined and roleGroup Authorization
        assertThat(authzDto.getUserTypeRestriction()).isEqualTo("INTERNAL, EXTERNAL");
        assertThat(authzDto.getRoleGroup()).isEqualTo("Authorization");
        assertThat(authzDto.getParentApp()).isEqualTo("Parent App");
        assertThat(authzDto.getParentAppId()).isEqualTo(appId.toString());
        assertThat(authzDto.getCcmsCode()).isEqualTo("XXCCMS_TEST");

        // ccms role should have roleGroup CCMS and single user type
        assertThat(ccmsDto.getRoleGroup()).isEqualTo("CCMS");
        assertThat(ccmsDto.getUserTypeRestriction()).isEqualTo("INTERNAL");
        assertThat(ccmsDto.getCcmsCode()).isEqualTo("XXCCMS_FIRM_X");

        // default role should have empty userTypeRestriction and Default roleGroup
        assertThat(defaultDto.getUserTypeRestriction()).isEqualTo("");
        assertThat(defaultDto.getRoleGroup()).isEqualTo("Default");
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
}
