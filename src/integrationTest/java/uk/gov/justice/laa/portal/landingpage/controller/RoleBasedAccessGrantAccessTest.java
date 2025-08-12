package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBasedAccessGrantAccessTest extends RoleBasedAccessIntegrationTest {

    @Test
    public void testGlobalAdminCanOpenGlobalAdminAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(globalAdmins.getFirst(), globalAdmins.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenInternalUserManagerAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(globalAdmins.getFirst(), internalUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenInternalAndExternalUserManagerAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(globalAdmins.getFirst(), internalAndExternalUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenExternalUserAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(globalAdmins.getFirst(), externalUsersNoRoles.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenExternalUserManagerAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(globalAdmins.getFirst(), externalOnlyUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenExternalUserAdminAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(globalAdmins.getFirst(), externalUserAdmins.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenInternalUserWithExternalUserManagerRoleAdminAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(globalAdmins.getFirst(), internalWithExternalOnlyUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenInternalUserAdminAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(globalAdmins.getFirst(), internalUsersNoRoles.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCanOpenInternalUserManagerAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(internalUserManagers.getFirst(), internalUserManagers.getLast(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCanOpenInternalAndExternalUserManagerAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(internalUserManagers.getFirst(), internalAndExternalUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCanOpenInternalUserWithExternalUserManagerAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(internalUserManagers.getFirst(), internalWithExternalOnlyUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCanOpenInternalUserAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(internalUserManagers.getFirst(), internalUsersNoRoles.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCanOpenGlobalAdminAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(internalUserManagers.getFirst(), globalAdmins.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCannotOpenExternalUserAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(internalUserManagers.getFirst(), externalUsersNoRoles.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testInternalUserWithOnlyExternalUserManagerCanOpenExternalUserAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(internalWithExternalOnlyUserManagers.getFirst(), externalUsersNoRoles.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserWithOnlyExternalUserManagerCannotOpenInternalUserAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(internalWithExternalOnlyUserManagers.getFirst(), internalUsersNoRoles.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testInternalUserWithOnlyExternalUserManagerCannotOpenGlobalAdminUserAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(internalWithExternalOnlyUserManagers.getFirst(), globalAdmins.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testExternalUserManagerCannotOpenInternalUserAppsToGrantAccess() throws Exception {
        canOpenGrantAccessScreen(externalOnlyUserManagers.getFirst(), internalUsersNoRoles.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testExternalUserManagerCannotOpenExternalUserInDifferentFirmsAppsToGrantAccess() throws Exception {
        EntraUser externalUserManagerFirm1 = externalOnlyUserManagers.getFirst();
        EntraUser externalUserFirm2 = externalUsersNoRoles.stream()
                        .filter(user -> user.getUserProfiles().stream().findFirst().orElseThrow().getFirm().getId().equals(testFirm2.getId()))
                        .findFirst().orElseThrow();
        canOpenGrantAccessScreen(externalUserManagerFirm1, externalUserFirm2, status().is3xxRedirection());
    }

    @Test
    public void testExternalUserManagerCanOpenExternalUserInSameFirmsAppsToGrantAccess() throws Exception {
        EntraUser externalUserManagerFirm1 = externalOnlyUserManagers.getFirst();
        EntraUser externalUserFirm1 = externalUsersNoRoles.stream()
                .filter(user -> user.getUserProfiles().stream().findFirst().orElseThrow().getFirm().getId().equals(testFirm1.getId()))
                .findFirst().orElseThrow();
        canOpenGrantAccessScreen(externalUserManagerFirm1, externalUserFirm1, status().isOk());
    }


    private void canOpenGrantAccessScreen(EntraUser loggedInUser, EntraUser editedUser, ResultMatcher expectedResult) throws Exception {
        UserProfile accessedUserProfile = editedUser.getUserProfiles().stream().findFirst().orElseThrow();
        this.mockMvc.perform(get(String.format("/admin/users/grant-access/%s/apps", accessedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(expectedResult);
    }
}
