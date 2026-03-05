package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.AppRoleService;
import uk.gov.justice.laa.portal.landingpage.service.RoleAssignmentService;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;


class RoleAssignmentRestrictionsControllerTest extends BaseIntegrationTest {

    @MockitoBean
    private AppRoleService appRoleService;

    @MockitoBean
    private RoleAssignmentService roleAssignmentService;

    @MockitoBean
    private AccessControlService accessControlService;

    @MockitoBean
    private org.modelmapper.ModelMapper mapper;

    private String targetId;
    private AppRoleDto targetDto;

    private AppRoleDto dtoA;
    private AppRoleDto dtoB;
    private AppRoleDto dtoC;

    @BeforeEach
    void init() {
        given(accessControlService.authenticatedUserHasPermission(any())).willReturn(true);

        targetId = UUID.randomUUID().toString();
        targetDto = new AppRoleDto();
        targetDto.setId(targetId);
        targetDto.setName("Target Role");

        dtoA = dto("A", "Alpha");
        dtoB = dto("B", "Beta");
        dtoC = dto("C", "Charlie");

        when(mapper.map(any(), eq(AppRoleViewModel.class))).thenAnswer(inv -> {
            AppRoleDto d = inv.getArgument(0);
            AppRoleViewModel vm = new AppRoleViewModel();
            vm.setId(d.getId());
            vm.setName(d.getName());
            vm.setSelected(false);
            return vm;
        });
    }

    private AppRoleDto dto(String idSuffix, String name) {
        AppRoleDto d = new AppRoleDto();
        d.setId(UUID.nameUUIDFromBytes(("ID-" + idSuffix).getBytes()).toString());
        d.setName(name);
        return d;
    }

    @Test
    void getFirstPage_buildsViewModelsAndMarksSelected() throws Exception {
        given(appRoleService.findById(targetId)).willReturn(Optional.of(targetDto));
        given(appRoleService.getAssigningRolesFor(targetId)).willReturn(List.of(dtoB));
        given(appRoleService.getAllAuthzRoles()).willReturn(List.of(dtoA, dtoB, dtoC));

        mockMvc.perform(get("/admin/silas-administration/role/assignRestrictions/{appRoleId}", targetId)
                        .with(defaultOauth2Login(silasAdminUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("silas-administration/edit-role-assignment-restrictions"))
                .andExpect(model().attributeExists("appRole", "assigningRoleViewModels"))
                .andExpect(model().attribute("assigningRoleViewModels", hasSize(3)))
                .andExpect(model().attribute("assigningRoleViewModels",
                        hasItem(allOf(hasProperty("id", equalTo(dtoB.getId())),
                                hasProperty("selected", equalTo(true))))));
    }

    @Test
    void postFirstPage_storesSessionAndRedirectsToCheckAnswers() throws Exception {
        given(appRoleService.findById(targetId)).willReturn(Optional.of(targetDto));
        given(appRoleService.getAllAuthzRoles()).willReturn(List.of(dtoA, dtoB, dtoC));

        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/admin/silas-administration/role/assignRestrictions/{appRoleId}", targetId)
                        .param("roles", dtoA.getId(), dtoC.getId())
                        .with(csrf())
                        .session(session)
                        .with(defaultOauth2Login(silasAdminUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/silas-administration/role/assignRestrictions/check-answers"));

        @SuppressWarnings("unchecked") List<AppRoleViewModel> stored = (List<AppRoleViewModel>) session.getAttribute("appRoleSelections");
        assertThat(session.getAttribute("assignableAppRoleId")).isEqualTo(targetId);
        assertThat(stored).hasSize(3);
        Map<String, Boolean> sel = new HashMap<>();
        for (AppRoleViewModel vm : stored) {
            sel.put(vm.getId(), vm.isSelected());
        }
        assertThat(sel.get(dtoA.getId())).isTrue();
        assertThat(sel.get(dtoB.getId())).isFalse();
        assertThat(sel.get(dtoC.getId())).isTrue();
    }

    @Test
    void getCheckAnswers_withoutSession_redirectsToLanding() throws Exception {
        mockMvc.perform(get("/admin/silas-administration/role/assignRestrictions/check-answers")
                        .with(defaultOauth2Login(silasAdminUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/silas-administration/role"));
    }

    @Test
    void getCheckAnswers_withSession_populatesModelAndFiltersSelf() throws Exception {
        List<AppRoleViewModel> vms = new ArrayList<>();
        vms.add(vm(dtoA, true));
        vms.add(vm(dtoB, false));
        vms.add(vm(targetDto, true));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("assignableAppRoleId", targetId);
        session.setAttribute("appRoleSelections", vms);

        given(appRoleService.findById(targetId)).willReturn(Optional.of(targetDto));
        given(appRoleService.getAssigningRolesFor(targetId)).willReturn(List.of(dtoB));
        given(appRoleService.getByIds(List.of(dtoA.getId()))).willReturn(List.of(dtoA));

        mockMvc.perform(get("/admin/silas-administration/role/assignRestrictions/check-answers")
                        .with(defaultOauth2Login(silasAdminUser)).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("silas-administration/edit-role-assignment-restrictions-check-answers"))
                .andExpect(model().attribute("appRole",
                        hasProperty("id", equalTo(targetId))))
                .andExpect(model().attribute("originalAssigningRoles", hasSize(1)))
                .andExpect(model().attribute("newAssigningRoles", hasSize(1)))
                .andExpect(model().attribute("newAssigningRoles", hasItem(hasProperty("id", equalTo(dtoA.getId())))));
    }

    @Test
    void postCheckAnswers_withSession_callsService_filtersSelf_clearsSession_andShowsConfirmation() throws Exception {
        List<AppRoleViewModel> vms = new ArrayList<>();
        vms.add(vm(dtoA, true));
        vms.add(vm(dtoA, true));
        vms.add(vm(dtoB, true));
        vms.add(vm(targetDto, true));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("assignableAppRoleId", targetId);
        session.setAttribute("appRoleSelections", vms);

        mockMvc.perform(post("/admin/silas-administration/role/assignRestrictions/check-answers")
                        .with(csrf()).with(defaultOauth2Login(silasAdminUser)).session(session))
                .andExpect(status().isOk()).andExpect(view()
                        .name("silas-administration/edit-role-assignment-restrictions-confirmation"));

        ArgumentCaptor<List<String>> cap = ArgumentCaptor.forClass(List.class);
        verify(roleAssignmentService).updateRoleAssignmentRestrictions(eq(targetId), cap.capture());
        List<String> passed = cap.getValue();
        assertThat(passed).containsExactlyInAnyOrder(dtoA.getId(), dtoB.getId());
        assertThat(passed).doesNotContain(targetId);

        assertThat(session.getAttribute("assignableAppRoleId")).isNull();
        assertThat(session.getAttribute("appRoleSelections")).isNull();
    }

    @Test
    void postCheckAnswers_withoutSession_redirectsToLanding() throws Exception {
        mockMvc.perform(post("/admin/silas-administration/role/assignRestrictions/check-answers")
                        .with(csrf()).with(defaultOauth2Login(silasAdminUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/silas-administration/role"));
    }

    private AppRoleViewModel vm(AppRoleDto dto, boolean selected) {
        AppRoleViewModel vm = new AppRoleViewModel();
        vm.setId(dto.getId());
        vm.setName(dto.getName());
        vm.setSelected(selected);
        return vm;
    }
}
