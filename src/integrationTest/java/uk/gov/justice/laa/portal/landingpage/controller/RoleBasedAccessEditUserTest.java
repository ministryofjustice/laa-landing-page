package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBasedAccessEditUserTest extends RoleBasedAccessIntegrationTest {

    @Test
    public void testGlobalAdminCanOpenGlobalAdminAppsToEdit() throws Exception {
        canOpenEditScreen(globalAdmins.getFirst(), globalAdmins.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenInternalUserManagerAppsToEdit() throws Exception {
        canOpenEditScreen(globalAdmins.getFirst(), internalUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenInternalAndExternalUserManagerAppsToEdit() throws Exception {
        canOpenEditScreen(globalAdmins.getFirst(), internalAndExternalUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenExternalUserAppsToEdit() throws Exception {
        canOpenEditScreen(globalAdmins.getFirst(), externalUsersNoRoles.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenExternalUserManagerAppsToEdit() throws Exception {
        canOpenEditScreen(globalAdmins.getFirst(), externalOnlyUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenExternalUserAdminAppsToEdit() throws Exception {
        canOpenEditScreen(globalAdmins.getFirst(), externalUserAdmins.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenInternalUserWithExternalUserManagerRoleAdminAppsToEdit() throws Exception {
        canOpenEditScreen(globalAdmins.getFirst(), internalWithExternalOnlyUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanOpenInternalUserAdminAppsToEdit() throws Exception {
        canOpenEditScreen(globalAdmins.getFirst(), internalUsersNoRoles.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCanOpenInternalUserManagerAppsToEdit() throws Exception {
        canOpenEditScreen(internalUserManagers.getFirst(), internalUserManagers.getLast(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCanOpenInternalAndExternalUserManagerAppsToEdit() throws Exception {
        canOpenEditScreen(internalUserManagers.getFirst(), internalAndExternalUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCanOpenInternalUserWithExternalUserManagerAppsToEdit() throws Exception {
        canOpenEditScreen(internalUserManagers.getFirst(), internalWithExternalOnlyUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCanOpenInternalUserAppsToEdit() throws Exception {
        canOpenEditScreen(internalUserManagers.getFirst(), internalUsersNoRoles.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCanOpenGlobalAdminAppsToEdit() throws Exception {
        canOpenEditScreen(internalUserManagers.getFirst(), globalAdmins.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCannotOpenExternalUserAppsToEdit() throws Exception {
        canOpenEditScreen(internalUserManagers.getFirst(), externalUsersNoRoles.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testInternalUserWithOnlyExternalUserManagerCanOpenExternalUserAppsToEdit() throws Exception {
        canOpenEditScreen(internalWithExternalOnlyUserManagers.getFirst(), externalUsersNoRoles.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserWithOnlyExternalUserManagerCannotOpenInternalUserAppsToEdit() throws Exception {
        canOpenEditScreen(internalWithExternalOnlyUserManagers.getFirst(), internalUsersNoRoles.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testInternalUserWithOnlyExternalUserManagerCannotOpenGlobalAdminUserAppsToEdit() throws Exception {
        canOpenEditScreen(internalWithExternalOnlyUserManagers.getFirst(), globalAdmins.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testExternalUserManagerCannotOpenInternalUserAppsToEdit() throws Exception {
        canOpenEditScreen(externalOnlyUserManagers.getFirst(), internalUsersNoRoles.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testExternalUserManagerCannotOpenExternalUserInDifferentFirmsAppsToEdit() throws Exception {
        EntraUser externalUserManagerFirm1 = externalOnlyUserManagers.getFirst();
        EntraUser externalUserFirm2 = externalUsersNoRoles.stream()
                        .filter(user -> user.getUserProfiles().stream().findFirst().orElseThrow().getFirm().getId().equals(testFirm2.getId()))
                        .findFirst().orElseThrow();
        canOpenEditScreen(externalUserManagerFirm1, externalUserFirm2, status().is3xxRedirection());
    }

    @Test
    public void testExternalUserManagerCanOpenExternalUserInSameFirmsAppsToEdit() throws Exception {
        EntraUser externalUserManagerFirm1 = externalOnlyUserManagers.getFirst();
        EntraUser externalUserFirm1 = externalUsersNoRoles.stream()
                .filter(user -> user.getUserProfiles().stream().findFirst().orElseThrow().getFirm().getId().equals(testFirm1.getId()))
                .findFirst().orElseThrow();
        canOpenEditScreen(externalUserManagerFirm1, externalUserFirm1, status().isOk());
    }

    @Test
    public void testInternalUserViewerCannotOpenInternalUserAppsToEdit() throws Exception {
        canOpenEditScreen(internalUserViewers.getFirst(), internalUsersNoRoles.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testInternalUserViewerCannotOpenExternalUserAppsToEdit() throws Exception {
        canOpenEditScreen(internalUserViewers.getFirst(), externalUsersNoRoles.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testExternalUserViewerCannotOpenInternalUserAppsToEdit() throws Exception {
        canOpenEditScreen(externalUserViewers.getFirst(), internalUsersNoRoles.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testExternalUserViewerCannotOpenExternalUserAppsToEdit() throws Exception {
        canOpenEditScreen(externalUserViewers.getFirst(), externalUsersNoRoles.getFirst(), status().is3xxRedirection());
    }


    private void canOpenEditScreen(EntraUser loggedInUser, EntraUser editedUser, ResultMatcher expectedResult) throws Exception {
        UserProfile accessedUserProfile = editedUser.getUserProfiles().stream().findFirst().orElseThrow();
        this.mockMvc.perform(get(String.format("/admin/users/edit/%s/apps", accessedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(expectedResult);
    }
}
