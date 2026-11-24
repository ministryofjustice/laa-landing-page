package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminControllerIntegrationTest extends RoleBasedAccessIntegrationTest {


    @Test
    public void testAdminCanCreateMultipleRolesWithSameName() throws Exception {
        EntraUser loggedInUser = silasAdmins.getFirst();
        MockHttpSession session = new MockHttpSession();

        App app = buildLaaApp("RoleCreationTestApp", UUID.randomUUID().toString(), UUID.randomUUID().toString());
        app = appRepository.saveAndFlush(app);

        RoleCreationDto role1 = RoleCreationDto.builder()
                .name("Test Role")
                .description("A Test Role")
                .authzRole(false)
                .roleIdentifier("role-identifier-1")
                .parentAppId(app.getId())
                .userTypeRestriction(List.of(UserType.EXTERNAL))
                .legacySync(false)
                .build();

        MvcResult result1 = createRolePost(loggedInUser, session, role1);
        assertThat(result1.getResponse().getRedirectedUrl()).isEqualTo("/admin/silas-administration/roles/create/confirmation");
        role1 = (RoleCreationDto) session.getAttribute("createdRole");

        RoleCreationDto role2 = RoleCreationDto.builder()
                .name("Test Role")
                .description("Another Test Role")
                .authzRole(false)
                .roleIdentifier("role-identifier-2")
                .parentAppId(app.getId())
                .userTypeRestriction(List.of(UserType.EXTERNAL))
                .legacySync(false)
                .build();

        MvcResult result2 = createRolePost(loggedInUser, session, role2);
        assertThat(result2.getResponse().getRedirectedUrl()).isEqualTo("/admin/silas-administration/roles/create/confirmation");
        role2 = (RoleCreationDto) session.getAttribute("createdRole");


        // Teardown
        AppRole createdRole1 = appRoleRepository.findById(role1.getId()).orElseThrow();
        appRoleRepository.delete(createdRole1);

        AppRole createdRole2 = appRoleRepository.findById(role2.getId()).orElseThrow();
        appRoleRepository.delete(createdRole2);

        appRepository.delete(app);
    }

    @Test
    public void testAdminCannotCreateRoleWithAuthzRoleName() throws Exception {
        EntraUser loggedInUser = silasAdmins.getFirst();
        MockHttpSession session = new MockHttpSession();

        App app = buildLaaApp("RoleCreationTestApp", UUID.randomUUID().toString(), UUID.randomUUID().toString());
        app = appRepository.saveAndFlush(app);

        RoleCreationDto role = RoleCreationDto.builder()
                .name("Global Admin")
                .description("A Test Role")
                .authzRole(false)
                .roleIdentifier("role-identifier-1")
                .parentAppId(app.getId())
                .userTypeRestriction(List.of(UserType.EXTERNAL))
                .legacySync(false)
                .build();

        MvcResult result = createRolePost(loggedInUser, session, role);
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/silas-administration?tab=roles");
        assertThat(session.getAttribute("createdRole")).isNull();
        appRepository.delete(app);
    }

    public MvcResult createRolePost(EntraUser loggedInUser, MockHttpSession session, RoleCreationDto role) throws Exception {
        // /admin/silas-administration/roles/create/check-your-answers
        session.setAttribute("roleCreationDto", role);
        return mockMvc.perform(post("/admin/silas-administration/roles/create/check-your-answers")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
    }

}
