package uk.gov.justice.laa.portal.landingpage.controller;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBaseAccessEditUserRoleTest extends RoleBasedAccessIntegrationTest {


    @Test
    @Transactional
    public void testGlobalAdminCannotAssignInternalUserManagerRoleToExternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, "Internal User Manager", false);
    }

    @Test
    @Transactional
    public void testGlobalAdminCanAssignInternalUserManagerRoleToInternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, "Internal User Manager", true);
    }

    @Test
    @Transactional
    public void testGlobalAdminCanAssignExternalUserManagerRoleToInternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, "External User Manager", true);
    }

    @Test
    @Transactional
    public void testGlobalAdminCanAssignExternalUserManagerRoleToExternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, "External User Manager", true);
    }

    @Test
    @Transactional
    public void testGlobalAdminCannotAssignGlobalAdminRoleToExternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, "Global Admin", false);
    }

    @Test
    @Transactional
    public void testGlobalAdminCannotAssignExternalUserAdminRoleToExternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, "External User Admin", false);
    }

    @Test
    @Transactional
    public void testInternalUserManagerCanAssignInternalUserManagerRoleToInternalUser() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, "Internal User Manager", true);
    }

    @Test
    @Transactional
    public void testInternalUserManagerCanAssignExternalUserManagerRoleToInternalUser() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, "External User Manager", true);
    }

    @SuppressWarnings("unchecked")
    private void assignAuthzRoleToUser(EntraUser loggedInUser, EntraUser editedUser, String authzRoleName, boolean expectedSuccess) throws Exception {
        UserProfile editedUserProfile = editedUser.getUserProfiles().stream().findFirst().orElseThrow();
        MockHttpSession session = new MockHttpSession();

        // Open App editing screen
        MvcResult selectAppsResult = this.mockMvc.perform(get(String.format("/admin/users/edit/%s/apps", editedUserProfile.getId()))
                .with(userOauth2Login(loggedInUser))
                .session(session))
                .andExpect(status().isOk())
                .andReturn();

        // Fetch AuthZ app from response.
        ModelAndView modelAndView = selectAppsResult.getModelAndView();
        List<AppDto> availableApps = (List<AppDto>) modelAndView.getModel().get("apps");
        AppDto authzApp = availableApps.stream()
                .filter(app -> app.getName().equals("Manage Your Users"))
                .findFirst()
                .orElseThrow();

        // Select AuthZ app using post request.
        MvcResult postAppsResult = this.mockMvc.perform(post(String.format("/admin/users/edit/%s/apps", editedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("apps", authzApp.getId()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Fetch redirected URL from response and request it.
        String redirectedUrl = postAppsResult.getResponse().getRedirectedUrl();
        Assertions.assertThat(redirectedUrl).isNotNull();
        MvcResult getRolesResult = this.mockMvc.perform(get(redirectedUrl)
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        // Fetch returned roles.
        List<AppRoleViewModel> roles = (List<AppRoleViewModel>) getRolesResult.getModelAndView().getModel().get("roles");

        // Get Authz Role from list.
        Optional<AppRoleViewModel> optionalAuthzRole = roles.stream()
                .filter(role -> role.getName().equals(authzRoleName))
                .findFirst();

        if (optionalAuthzRole.isEmpty()) {
            if (!expectedSuccess) {
                return;
            } else {
                Assertions.fail(String.format("Authz role \"%s\" was not available for selection when it should have been", authzRoleName));
            }
        }

        AppRoleViewModel authzRole = optionalAuthzRole.get();

        // Post request to set Internal User Manager role
        MvcResult postRolesResult = this.mockMvc.perform(post(redirectedUrl)
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("roles", authzRole.getId())
                        .param("selectedAppIndex", "0"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Check we're redirected back to manage user screen
        Assertions.assertThat(postRolesResult.getResponse().getRedirectedUrl()).contains("manage");

        Set<String> editedUserRoles = userProfileRepository.findById(editedUserProfile.getId())
                .orElseThrow()
                .getAppRoles().stream()
                .map(role -> role.getId().toString())
                .collect(Collectors.toSet());

        if (expectedSuccess) {
            Assertions.assertThat(editedUserRoles).contains(authzRole.getId());
        } else {
            Assertions.assertThat(editedUserRoles).doesNotContain(authzRole.getId());
        }
    }
}
