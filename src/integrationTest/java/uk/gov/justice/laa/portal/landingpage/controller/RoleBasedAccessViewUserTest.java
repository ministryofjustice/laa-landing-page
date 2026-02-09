package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBasedAccessViewUserTest extends RoleBasedAccessIntegrationTest {

    @Test
    public void testGlobalAdminCanAccessGlobalAdminProfile() throws Exception {
        testCanAccessUser(globalAdmins.getFirst(), globalAdmins.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanAccessInternalUserManagerProfile() throws Exception {
        testCanAccessUser(globalAdmins.getFirst(), internalUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanAccessInternalAndExternalUserManagerProfile() throws Exception {
        testCanAccessUser(globalAdmins.getFirst(), internalAndExternalUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanAccessExternalUserProfile() throws Exception {
        testCanAccessUser(globalAdmins.getFirst(), externalUsersNoRoles.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanAccessExternalUserAdminProfile() throws Exception {
        testCanAccessUser(globalAdmins.getFirst(), externalUserAdmins.getFirst(), status().isOk());
    }

    @Test
    public void testGlobalAdminCanAccessSecurityResponseProfile() throws Exception {
        testCanAccessUser(globalAdmins.getFirst(), securityResponseUsers.getFirst(), status().isOk());
    }

    @Test
    public void testSecurityResponseCanAccessGlobalAdminProfile() throws Exception {
        testCanAccessUser(securityResponseUsers.getFirst(), globalAdmins.getFirst(), status().isOk());
    }

    @Test
    public void testSecurityResponseCanAccessInternalUserManagerProfile() throws Exception {
        testCanAccessUser(securityResponseUsers.getFirst(), internalUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testSecurityResponseCanAccessInternalAndExternalUserManagerProfile() throws Exception {
        testCanAccessUser(securityResponseUsers.getFirst(), internalAndExternalUserManagers.getFirst(), status().isOk());
    }

    @Test
    public void testSecurityResponseCanAccessExternalUserProfile() throws Exception {
        testCanAccessUser(securityResponseUsers.getFirst(), externalUsersNoRoles.getFirst(), status().isOk());
    }

    @Test
    public void testSecurityResponseCanAccessExternalUserAdminProfile() throws Exception {
        testCanAccessUser(securityResponseUsers.getFirst(), externalUserAdmins.getFirst(), status().isOk());
    }

    @Test
    public void testSecurityResponseCanAccessSecurityResponseProfile() throws Exception {
        testCanAccessUser(securityResponseUsers.getFirst(), securityResponseUsers.getFirst(), status().isOk());
    }

    @Test
    public void testInternalAndExternalUserManagerCanAccessGlobalAdminProfile() throws Exception {
        testCanAccessUser(internalAndExternalUserManagers.getFirst(), globalAdmins.getFirst(), status().isOk());

    }

    @Test
    public void testInternalAndExternalUserManagerCanAccessInternalUserManagerProfile() throws Exception {
        testCanAccessUser(internalAndExternalUserManagers.getFirst(), internalUserManagers.getFirst(), status().isOk());

    }

    @Test
    public void testInternalAndExternalUserManagerCanAccessInternalAndExternalUserManagerProfile() throws Exception {
        testCanAccessUser(internalAndExternalUserManagers.getFirst(), internalAndExternalUserManagers.getLast(), status().isOk());

    }

    @Test
    public void testInternalAndExternalUserManagerCanAccessExternalUserProfile() throws Exception {
        testCanAccessUser(internalAndExternalUserManagers.getFirst(), externalUsersNoRoles.getFirst(), status().isOk());

    }

    @Test
    public void testInternalAndExternalUserManagerCanAccessExternalUserAdminProfile() throws Exception {
        testCanAccessUser(internalAndExternalUserManagers.getFirst(), externalUserAdmins.getFirst(), status().isOk());

    }

    @Test
    public void testInternalUserManagerCanAccessGlobalAdminProfile() throws Exception {
        testCanAccessUser(internalUserManagers.getFirst(), globalAdmins.getFirst(), status().isOk());

    }

    @Test
    public void testInternalUserManagerCanAccessInternalUserManagerProfile() throws Exception {
        testCanAccessUser(internalUserManagers.getFirst(), internalUserManagers.getLast(), status().isOk());
    }

    @Test
    public void testInternalUserManagerCannotAccessExternalUserProfile() throws Exception {
        testCanAccessUser(internalUserManagers.getFirst(), externalUsersNoRoles.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testInternalUserManagerCannotAccessExternalUserManagerProfile() throws Exception {
        testCanAccessUser(internalUserManagers.getFirst(), externalOnlyUserManagers.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testInternalUserManagerCanAccessExternalUserAdminProfile() throws Exception {
        testCanAccessUser(internalUserManagers.getFirst(), externalUserAdmins.getFirst(), status().isOk());
    }

    @Test
    public void testExternalUserWithNoRolesCannotAccessInternalUserManagerProfile() throws Exception {
        testCanAccessUser(externalUsersNoRoles.getFirst(), internalUserManagers.getFirst(), status().isForbidden());
    }

    @Test
    public void testExternalUserWithNoRolesCannotAccessInternalAndExternalUserManagerProfile() throws Exception {
        testCanAccessUser(externalUsersNoRoles.getFirst(), internalAndExternalUserManagers.getFirst(), status().isForbidden());
    }

    @Test
    public void testExternalUserWithNoRolesCannotAccessExternalUserProfile() throws Exception {
        testCanAccessUser(externalUsersNoRoles.getFirst(), externalUsersNoRoles.getLast(), status().isForbidden());
    }

    @Test
    public void testExternalUserWithNoRolesCannotAccessExternalUserManagerProfile() throws Exception {
        testCanAccessUser(externalUsersNoRoles.getFirst(), externalOnlyUserManagers.getFirst(), status().isForbidden());
    }

    @Test
    public void testExternalUserWithNoRolesCannotAccessExternalUserAdminProfile() throws Exception {
        testCanAccessUser(externalUsersNoRoles.getFirst(), externalUserAdmins.getFirst(), status().isForbidden());
    }

    @Test
    public void testExternalUserWithNoRolesCannotAccessGlobalAdminProfile() throws Exception {
        testCanAccessUser(externalUsersNoRoles.getFirst(), globalAdmins.getFirst(), status().isForbidden());
    }

    @Test
    public void testExternalUserManagerCannotAccessGlobalAdminProfile() throws Exception {
        testCanAccessUser(externalOnlyUserManagers.getFirst(), globalAdmins.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testExternalUserManagerCannotAccessInternalUserManagerProfile() throws Exception {
        testCanAccessUser(externalOnlyUserManagers.getFirst(), internalUserManagers.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testExternalUserManagerCannotAccessInternalAndExternalUserManagerProfile() throws Exception {
        testCanAccessUser(externalOnlyUserManagers.getFirst(), internalAndExternalUserManagers.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testExternalUserManagerCannotAccessExternalUserInDifferentFirm() throws Exception {
        EntraUser loggedInUser = externalOnlyUserManagers.getFirst();
        Firm loggedInUserFirm = loggedInUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow()
                .getFirm();
        EntraUser accessedUserDifferentFirm = externalUsersNoRoles.stream()
                .filter(user -> !user.getUserProfiles().stream().findFirst().orElseThrow().getFirm().getId().equals(loggedInUserFirm.getId()))
                .findFirst()
                .orElseThrow();
        testCanAccessUser(loggedInUser, accessedUserDifferentFirm, status().is3xxRedirection());
    }

    @Test
    public void testExternalUserManagerCanAccessExternalUserInSameFirm() throws Exception {
        EntraUser loggedInUser = externalOnlyUserManagers.getFirst();
        Firm loggedInUserFirm = loggedInUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow()
                .getFirm();
        EntraUser accessedUserSameFirm = externalUsersNoRoles.stream()
                .filter(user -> !user.getId().equals(loggedInUser.getId()) && user.getUserProfiles().stream().findFirst().orElseThrow().getFirm().getId().equals(loggedInUserFirm.getId()))
                .findFirst()
                .orElseThrow();
        testCanAccessUser(loggedInUser, accessedUserSameFirm, status().isOk());
    }

    @Test
    public void testExternalUserAdminCannotAccessGlobalAdminProfile() throws Exception {
        testCanAccessUser(externalUserAdmins.getFirst(), globalAdmins.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testExternalUserAdminCannotAccessInternalUserManagerProfile() throws Exception {
        testCanAccessUser(externalUserAdmins.getFirst(), internalUserManagers.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testExternalUserAdminCannotAccessInternalAndExternalUserManagerProfile() throws Exception {
        testCanAccessUser(externalUserAdmins.getFirst(), internalAndExternalUserManagers.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testExternalUserAdminCanAccessExternalUserInFirmOne() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        EntraUser accessedUserFirmOne = externalUsersNoRoles.stream()
                .filter(user -> !user.getUserProfiles().stream().findFirst().orElseThrow().getFirm().getId().equals(testFirm1.getId()))
                .findFirst()
                .orElseThrow();
        testCanAccessUser(loggedInUser, accessedUserFirmOne, status().isOk());
    }

    @Test
    public void testExternalUserAdminCanAccessExternalUserInFirmTwo() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        EntraUser accessedUserFirmTwo = externalUsersNoRoles.stream()
                .filter(user -> !user.getUserProfiles().stream().findFirst().orElseThrow().getFirm().getId().equals(testFirm2.getId()))
                .findFirst()
                .orElseThrow();
        testCanAccessUser(loggedInUser, accessedUserFirmTwo, status().isOk());
    }

    @Test
    public void testInternalUserViewerCanAccessInternalUserProfile() throws Exception {
        testCanAccessUser(internalUserViewers.getFirst(), internalUsersNoRoles.getFirst(), status().isOk());
    }

    @Test
    public void testInternalUserViewerCannotAccessExternalUserProfile() throws Exception {
        testCanAccessUser(internalUserViewers.getFirst(), externalUsersNoRoles.getFirst(), status().is3xxRedirection());
    }

    @Test
    public void testExternalUserViewerCanAccessExternalUserProfile() throws Exception {
        testCanAccessUser(externalUserViewers.getFirst(), externalUsersNoRoles.getFirst(), status().isOk());
    }

    @Test
    public void testExternalUserViewerCannotAccessInternalUserProfile() throws Exception {
        testCanAccessUser(externalUserViewers.getFirst(), internalUsersNoRoles.getFirst(), status().is3xxRedirection());
    }



    public void testCanAccessUser(EntraUser loggedInUser, EntraUser accessedUser, ResultMatcher expectedResult) throws Exception {
        UserProfile accessedUserProfile = accessedUser.getUserProfiles().stream().findFirst().get();
        this.mockMvc.perform(get("/admin/users/manage/" + accessedUserProfile.getId())
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(expectedResult);
    }

}
