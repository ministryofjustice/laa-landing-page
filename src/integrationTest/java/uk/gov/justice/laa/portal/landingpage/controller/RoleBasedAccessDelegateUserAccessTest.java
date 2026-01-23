package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.MultiFirmUserForm;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBasedAccessDelegateUserAccessTest extends RoleBasedAccessIntegrationTest {

    @Test
    @Transactional
    public void testFirmUserManagerCanDelegateFirmAccessToExternalUser() throws Exception {
        EntraUser editorUser = firmUserManagers.getFirst();
        EntraUser editedUser = multiFirmUsers.getFirst();
        MvcResult result = delegateFirmAccess(editorUser, editedUser);
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/multi-firm/user/add/profile/confirmation");

        // Teardown
        List<UserProfile> optionalCreatedUserProfiles = userProfileRepository.findAll().stream()
                .filter(profile -> profile.getEntraUser().getEmail().equalsIgnoreCase(editedUser.getEmail()))
                .toList();
        assertThat(optionalCreatedUserProfiles).isNotEmpty();
        assertThat(optionalCreatedUserProfiles).hasSize(2);

        // reset
        UserProfile newlyCreatedUserProfile = entraUserRepository.findById(editedUser.getId()).get().getUserProfiles().stream()
                .filter(userProfile -> userProfile.getFirm().getId().equals(testFirm2.getId()))
                .findFirst()
                .orElseThrow();
        userProfileRepository.delete(newlyCreatedUserProfile);
    }

    @Test
    @Transactional
    public void testFirmUserManagerCanDelegateFirmAccessToInternalUser() throws Exception {

        EntraUser editorUser = firmUserManagersInternal.getFirst();
        EntraUser editedUser = multiFirmUsers.getFirst();
        MvcResult result = delegateFirmAccessInternalUser(editorUser, editedUser);
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/multi-firm/user/add/profile/confirmation");

        // Teardown
        List<UserProfile> optionalCreatedUserProfiles = userProfileRepository.findAll().stream()
                .filter(profile -> profile.getEntraUser().getEmail().equalsIgnoreCase(editedUser.getEmail()))
                .toList();
        assertThat(optionalCreatedUserProfiles).isNotEmpty();
        assertThat(optionalCreatedUserProfiles).hasSize(2);

        // reset
        UserProfile newlyCreatedUserProfile = entraUserRepository.findById(editedUser.getId()).get().getUserProfiles().stream()
                .filter(userProfile -> userProfile.getFirm().getId().equals(testFirm2.getId()))
                .findFirst()
                .orElseThrow();
        userProfileRepository.delete(newlyCreatedUserProfile);
    }

    @Test
    @Transactional
    public void testGlobalAdminCannotAccessDelegateFirmAccessToExternalUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        EntraUser editorUser = globalAdmins.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/multi-firm/user/add/profile")
                        .with(userOauth2Login(editorUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getErrorMessage()).isEqualTo("Forbidden");
    }

    @Test
    @Transactional
    public void testGlobalAdminCannotPostToDelegateFirmAccessToExternalUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        EntraUser editorUser = globalAdmins.getFirst();
        MvcResult result = this.mockMvc.perform(post("/admin/multi-firm/user/add/profile")
                        .with(userOauth2Login(editorUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getErrorMessage()).isEqualTo("Forbidden");
    }

    @Test
    @Transactional
    public void testExternalUserManagerCannotAccessDelegateFirmAccessToExternalUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        EntraUser editorUser = internalWithExternalOnlyUserManagers.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/multi-firm/user/add/profile/select/apps")
                        .with(userOauth2Login(editorUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getErrorMessage()).isEqualTo("Forbidden");
    }

    @Test
    @Transactional
    public void testExternalUserManagerCannotPostToDelegateFirmAccessToExternalUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        EntraUser editorUser = internalWithExternalOnlyUserManagers.getFirst();
        MvcResult result = this.mockMvc.perform(post("/admin/multi-firm/user/add/profile/select/apps")
                        .with(userOauth2Login(editorUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getErrorMessage()).isEqualTo("Forbidden");
    }

    @Test
    @Transactional
    public void testExternalUserAdminCannotAccessDelegateFirmAccessToExternalUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        EntraUser editorUser = externalUserAdmins.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/multi-firm/user/add/profile/select/offices")
                        .with(userOauth2Login(editorUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getErrorMessage()).isEqualTo("Forbidden");
    }

    @Test
    @Transactional
    public void testExternalUserAdminCannotPostToDelegateFirmAccessToExternalUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        EntraUser editorUser = externalUserAdmins.getFirst();
        MvcResult result = this.mockMvc.perform(post("/admin/multi-firm/user/add/profile/select/offices")
                        .with(userOauth2Login(editorUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getErrorMessage()).isEqualTo("Forbidden");
    }


    private MvcResult delegateFirmAccess(EntraUser loggedInUser, EntraUser editedUser) throws Exception {
        // Build test app
        App testExternalApp = buildLaaApp("Test External App", generateEntraId(), "TestExternalAppSecurityGroupOid", "TestExternalAppSecurityGroup");

        // Build test role
        AppRole testExternalAppRole = buildLaaAppRole(testExternalApp, "Test External App Role");
        testExternalAppRole.setUserTypeRestriction(new UserType[]{UserType.EXTERNAL});

        // Persist app and role.
        testExternalApp.setAppRoles(Set.of(testExternalAppRole));
        testExternalApp = appRepository.saveAndFlush(testExternalApp);
        testExternalAppRole = testExternalApp.getAppRoles().stream().findFirst().orElseThrow();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream().findFirst().orElseThrow();
        MockHttpSession session = new MockHttpSession();

        MultiFirmUserForm multiFirmUserForm = MultiFirmUserForm.builder().email(editedUserProfile.getEntraUser().getEmail()).build();

        // Select user email using post request.
        MvcResult postEmailResult = this.mockMvc.perform(post("/admin/multi-firm/user/add/profile")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .formField("email", multiFirmUserForm.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(postEmailResult.getResponse()).isNotNull();
        String redirectedUrl = postEmailResult.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isEqualTo("/admin/multi-firm/user/add/profile/select/apps");

        // Select Apps
        MvcResult postSelectApps = this.mockMvc.perform(post("/admin/multi-firm/user/add/profile/select/apps")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("apps", testExternalApp.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(postSelectApps.getResponse()).isNotNull();
        redirectedUrl = postSelectApps.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isEqualTo("/admin/multi-firm/user/add/profile/select/roles");

        // Post Role
        MvcResult getAppRolesResult = this.mockMvc.perform(get("/admin/multi-firm/user/add/profile/select/roles")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Post Role
        MvcResult postAppRolesResult = this.mockMvc.perform(post("/admin/multi-firm/user/add/profile/select/roles")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .param("roles", testExternalAppRole.getId().toString())
                        .param("selectedAppIndex", "0")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(postAppRolesResult.getResponse()).isNotNull();
        redirectedUrl = postAppRolesResult.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isEqualTo("/admin/multi-firm/user/add/profile/select/offices");

        // Access Offices page
        mockMvc.perform(get("/admin/multi-firm/user/add/profile/select/offices")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        Office office = loggedInUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow()
                .getFirm()
                .getOffices()
                .stream()
                .findFirst()
                .orElseThrow();

        this.mockMvc.perform(post("/admin/multi-firm/user/add/profile/select/offices")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("offices", office.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        return this.mockMvc.perform(post("/admin/multi-firm/user/add/profile/check-answers")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
    }

    private MvcResult delegateFirmAccessInternalUser(EntraUser loggedInUser, EntraUser editedUser) throws Exception {
        // Build test app
        App testExternalApp = buildLaaApp("Test Internal App", generateEntraId(), "TestInternalAppSecurityGroupOid", "TestInternalAppSecurityGroup");

        // Build test role
        AppRole testInternalAppRole = buildLaaAppRole(testExternalApp, "Test Internal App Role");
        testInternalAppRole.setPermissions(Set.of(Permission.DELEGATE_EXTERNAL_USER_ACCESS));
        testInternalAppRole.setUserTypeRestriction(new UserType[]{UserType.INTERNAL});

        // Persist app and role.
        testExternalApp.setAppRoles(Set.of(testInternalAppRole));
        testExternalApp = appRepository.saveAndFlush(testExternalApp);
        testInternalAppRole = testExternalApp.getAppRoles().stream().findFirst().orElseThrow();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream().findFirst().orElseThrow();
        MockHttpSession session = new MockHttpSession();

        MultiFirmUserForm multiFirmUserForm = MultiFirmUserForm.builder().email(editedUserProfile.getEntraUser().getEmail()).build();

        // Select user email using post request.
        MvcResult postEmailResult = this.mockMvc.perform(post("/admin/multi-firm/user/add/profile")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .formField("email", multiFirmUserForm.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(postEmailResult.getResponse()).isNotNull();
        String redirectedUrl = postEmailResult.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isEqualTo("/admin/multi-firm/user/add/profile/select/internalUserFirm");

        // add internal firm  post method
        MvcResult postInternalUserFirm = this.mockMvc.perform(post("/admin/multi-firm/user/add/profile/select/internalUserFirm")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("firmSearch", testFirm2.getName()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertThat(postInternalUserFirm.getResponse()).isNotNull();
        redirectedUrl = postInternalUserFirm.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isEqualTo("/admin/multi-firm/user/add/profile/select/apps");

        // Select Apps
        MvcResult postSelectApps = this.mockMvc.perform(post("/admin/multi-firm/user/add/profile/select/apps")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("apps", testExternalApp.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(postSelectApps.getResponse()).isNotNull();
        redirectedUrl = postSelectApps.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isEqualTo("/admin/multi-firm/user/add/profile/select/roles");

        // Post Role
        MvcResult getAppRolesResult = this.mockMvc.perform(get("/admin/multi-firm/user/add/profile/select/roles")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Post Role
        MvcResult postAppRolesResult = this.mockMvc.perform(post("/admin/multi-firm/user/add/profile/select/roles")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .param("roles", testInternalAppRole.getId().toString())
                        .param("selectedAppIndex", "0")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(postAppRolesResult.getResponse()).isNotNull();
        redirectedUrl = postAppRolesResult.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isEqualTo("/admin/multi-firm/user/add/profile/select/offices");

        // Access Offices page
        mockMvc.perform(get("/admin/multi-firm/user/add/profile/select/offices")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        Office office = testFirm2.getOffices().stream()
                .findFirst()
                .orElseThrow();

        this.mockMvc.perform(post("/admin/multi-firm/user/add/profile/select/offices")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("offices", office.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        return this.mockMvc.perform(post("/admin/multi-firm/user/add/profile/check-answers")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
    }
}
