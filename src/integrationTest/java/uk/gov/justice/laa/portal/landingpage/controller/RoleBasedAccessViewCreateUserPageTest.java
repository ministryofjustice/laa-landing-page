package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBasedAccessViewCreateUserPageTest extends RoleBasedAccessIntegrationTest {

    @Test
    public void testGlobalAdminCanAccessCreateUserPage() throws Exception {
        accessCreateUserScreen(globalAdmins.getFirst(), status().isOk());
    }

    @Test
    public void testSecurityResponseCannotAccessCreateUserPage() throws Exception {
        MvcResult result = accessCreateUserScreen(securityResponseUsers.getFirst(), status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void testExternalUserAdminCanAccessCreateUserPage() throws Exception {
        accessCreateUserScreen(externalUserAdmins.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCannotAccessCreateUserPage() throws Exception {
        MvcResult result = accessCreateUserScreen(internalUserManagers.getFirst(), status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void testInternalUserWithExternalUserManagerCannotAccessCreateUserPage() throws Exception {
        MvcResult result = accessCreateUserScreen(internalWithExternalOnlyUserManagers.getFirst(), status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void testExternalUserWithExternalUserManagerCannotAccessCreateUserPage() throws Exception {
        MvcResult result = accessCreateUserScreen(externalOnlyUserManagers.getFirst(), status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void testInternalUserWithInternalAndExternalUserManagerCannotAccessCreateUserPage() throws Exception {
        MvcResult result = accessCreateUserScreen(internalAndExternalUserManagers.getFirst(), status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void testInternalUserViewerCannotAccessCreateUserPage() throws Exception {
        MvcResult result = accessCreateUserScreen(internalUserViewers.getFirst(), status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void testExternalUserViewerCannotAccessCreateUserPage() throws Exception {
        MvcResult result = accessCreateUserScreen(externalUserViewers.getFirst(), status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void testInternalUserWithNoRolesCannotAccessCreateUserPage() throws Exception {
        accessCreateUserScreen(internalUsersNoRoles.getFirst(), status().isForbidden());
    }

    @Test
    public void testExternalUserWithNoRolesCannotAccessCreateUserPage() throws Exception {
        accessCreateUserScreen(externalUsersNoRoles.getFirst(), status().isForbidden());
    }

    public MvcResult accessCreateUserScreen(EntraUser loggedInUser, ResultMatcher expectedResult) throws Exception {
        return this.mockMvc.perform(get("/admin/user/create/details")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(expectedResult)
                .andReturn();
    }
}
