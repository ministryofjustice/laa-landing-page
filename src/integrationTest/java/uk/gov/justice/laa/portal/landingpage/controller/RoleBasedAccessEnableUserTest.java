package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBasedAccessEnableUserTest extends RoleBasedAccessIntegrationTest {
    @Test
    public void testGlobalAdminCanEnableUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        accessedUser.setEnabled(false);
        entraUserRepository.saveAndFlush(accessedUser);
        sendEnableUserPost(loggedInUser, accessedUser, status().isOk());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isTrue();
    }

    @Test
    public void testExternalUserAdminCanEnableUser() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        accessedUser.setEnabled(false);
        entraUserRepository.saveAndFlush(accessedUser);
        sendEnableUserPost(loggedInUser, accessedUser, status().isOk());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isTrue();
    }

    @Test
    public void testInformationAndAssuranceCanEnableUser() throws Exception {
        EntraUser loggedInUser = securityResponseUsers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        accessedUser.setEnabled(false);
        entraUserRepository.saveAndFlush(accessedUser);
        sendEnableUserPost(loggedInUser, accessedUser, status().isOk());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isTrue();
    }

    @Test
    public void testInternalUserManagerCannotEnableUser() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        accessedUser.setEnabled(false);
        entraUserRepository.saveAndFlush(accessedUser);
        sendEnableUserPost(loggedInUser, accessedUser, status().is3xxRedirection());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isFalse();
        // Teardown
        accessedUser.setEnabled(true);
        entraUserRepository.saveAndFlush(accessedUser);
    }

    @Test
    public void testExternalUserManagerCanEnableUser() throws Exception {
        EntraUser loggedInUser = externalOnlyUserManagers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        accessedUser.setEnabled(false);
        entraUserRepository.saveAndFlush(accessedUser);
        sendEnableUserPost(loggedInUser, accessedUser, status().isOk());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isTrue();
    }

    @Test
    public void testFirmUserManagerCanEnableUser() throws Exception {
        EntraUser loggedInUser = firmUserManagers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getLast();
        accessedUser.setEnabled(false);
        entraUserRepository.saveAndFlush(accessedUser);
        sendEnableUserPost(loggedInUser, accessedUser, status().isOk());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isTrue();
    }

    @Test
    public void testInternalUserViewerCannotEnableUser() throws Exception {
        EntraUser loggedInUser = internalUserViewers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        accessedUser.setEnabled(false);
        entraUserRepository.saveAndFlush(accessedUser);
        sendEnableUserPost(loggedInUser, accessedUser, status().is3xxRedirection());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isFalse();
        // Teardown
        accessedUser.setEnabled(true);
        entraUserRepository.saveAndFlush(accessedUser);
    }

    @Test
    public void testExternalUserViewerCannotEnableUser() throws Exception {
        EntraUser loggedInUser = externalUserViewers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        accessedUser.setEnabled(false);
        entraUserRepository.saveAndFlush(accessedUser);
        sendEnableUserPost(loggedInUser, accessedUser, status().is3xxRedirection());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isFalse();
        // Teardown
        accessedUser.setEnabled(true);
        entraUserRepository.saveAndFlush(accessedUser);
    }

    @Test
    public void testInternalUserNoRolesCannotEnableUser() throws Exception {
        EntraUser loggedInUser = internalUsersNoRoles.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        accessedUser.setEnabled(false);
        entraUserRepository.saveAndFlush(accessedUser);
        sendEnableUserPost(loggedInUser, accessedUser, status().is4xxClientError());
        // Teardown
        accessedUser.setEnabled(true);
        entraUserRepository.saveAndFlush(accessedUser);
    }

    @Test
    public void testExternalUserNoRolesCannotEnableUser() throws Exception {
        EntraUser loggedInUser = externalUsersNoRoles.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        accessedUser.setEnabled(false);
        entraUserRepository.saveAndFlush(accessedUser);
        sendEnableUserPost(loggedInUser, accessedUser, status().is4xxClientError());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isFalse();
        // Teardown
        accessedUser.setEnabled(true);
        entraUserRepository.saveAndFlush(accessedUser);
    }


    public void sendEnableUserPost(EntraUser loggedInUser, EntraUser accessedUser, ResultMatcher expectedResult) throws Exception {
        this.mockMvc.perform(post(String.format("/admin/users/manage/%s/enable", accessedUser.getId()))
                        .with(csrf())
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(expectedResult)
                .andReturn();
    }
}
