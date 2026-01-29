package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBaseAccessDeleteExternalUserTest extends RoleBasedAccessIntegrationTest {

    @Test
    @Transactional
    public void testGlobalAdminCanDeleteExternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser deletedUser = externalUsersNoRoles.getFirst();
        canAccessDeleteUserScreens(loggedInUser, deletedUser, status().isOk());
    }

    @Test
    @Transactional
    public void testExternalUserAdminCanDeleteExternalUser() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        EntraUser deletedUser = externalUsersNoRoles.getFirst();
        canAccessDeleteUserScreens(loggedInUser, deletedUser, status().isOk());
    }

    @Test
    @Transactional
    public void testFirmUserManagerCanDeleteExternalUser() throws Exception {
        EntraUser loggedInUser = externalOnlyUserManagers.getFirst();
        EntraUser deletedUser = externalUsersNoRoles.getFirst();
        canAccessDeleteUserScreens(loggedInUser, deletedUser, status().isOk());
    }

    @Test
    @Transactional
    public void testExternalUserManagerCannotDeleteExternalUser() throws Exception {
        EntraUser loggedInUser = internalWithExternalOnlyUserManagers.getFirst();
        EntraUser deletedUser = externalUsersNoRoles.getFirst();
        canAccessDeleteUserScreens(loggedInUser, deletedUser, status().is3xxRedirection());
    }

    @Test
    @Transactional
    public void testInternalUserManagerCannotDeleteExternalUser() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        EntraUser deletedUser = externalUsersNoRoles.getFirst();
        canAccessDeleteUserScreens(loggedInUser, deletedUser, status().is3xxRedirection());
    }

    @Test
    @Transactional
    public void testGlobalAdminCannotDeleteInternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser deletedUser = internalUsersNoRoles.getFirst();
        canAccessDeleteUserScreens(loggedInUser, deletedUser, status().is3xxRedirection());
    }

    @Test
    @Transactional
    public void testExternalUserAdminCannotDeleteInternalUser() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        EntraUser deletedUser = internalUsersNoRoles.getFirst();
        canAccessDeleteUserScreens(loggedInUser, deletedUser, status().is3xxRedirection());
    }

    private void canAccessDeleteUserScreens(EntraUser loggedInUser, EntraUser deletedUser, ResultMatcher expected) throws Exception {
        UserProfile accessedUserProfile = deletedUser.getUserProfiles().stream().findFirst().orElseThrow();
        this.mockMvc.perform(get(String.format("/admin/users/manage/%s/delete", accessedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(expected);
    }
}
