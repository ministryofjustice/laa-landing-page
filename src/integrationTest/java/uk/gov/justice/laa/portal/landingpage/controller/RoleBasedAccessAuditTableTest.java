package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBasedAccessAuditTableTest extends RoleBasedAccessIntegrationTest {

    @Test
    public void testGlobalAdminCanAccessAuditTableAndSeeLink() throws Exception {
        EntraUser globalAdmin = globalAdmins.getFirst();
        testCanAccessAuditTable(globalAdmin, status().isOk());
        testCanSeeAuditLink(globalAdmin, true);
    }

    @Test
    public void testSecurityResponseCanAccessAuditTableAndSeeLink() throws Exception {
        EntraUser globalAdmin = securityResponseUsers.getFirst();
        testCanAccessAuditTable(globalAdmin, status().isOk());
        testCanSeeAuditLink(globalAdmin, true);
    }

    @Test
    public void testInternalUserManagerCanAccessAuditTableAndCanSeeLink() throws Exception {
        EntraUser internalUserManager = internalUserManagers.getFirst();
        testCanAccessAuditTable(internalUserManager, status().isOk());
        testCanSeeAuditLink(internalUserManager, true);
    }

    @Test
    public void testInternalUserWithExternalUserManagerCanAccessAuditTableAndCanSeeLink() throws Exception {
        EntraUser internalUserWithExternalUserManager = internalWithExternalOnlyUserManagers.getFirst();
        testCanAccessAuditTable(internalUserWithExternalUserManager, status().isOk());
        testCanSeeAuditLink(internalUserWithExternalUserManager, true);
    }

    @Test
    public void testInternalUserWithInternalAndExternalUserManagerCanAccessAuditTableAndCanSeeLink() throws Exception {
        EntraUser internalUserWithInternalAndExternalUserManager = internalAndExternalUserManagers.getFirst();
        testCanAccessAuditTable(internalUserWithInternalAndExternalUserManager, status().isOk());
        testCanSeeAuditLink(internalUserWithInternalAndExternalUserManager, true);
    }

    @Test
    public void testInternalUserViewerCanAccessAuditTableAndCanSeeLink() throws Exception {
        EntraUser internalUserViewer = internalUserViewers.getFirst();
        testCanAccessAuditTable(internalUserViewer, status().isOk());
        testCanSeeAuditLink(internalUserViewer, true);
    }

    @Test
    public void testExternalUserViewerCanAccessAuditTableAndCanSeeLink() throws Exception {
        EntraUser externalUserViewer = externalUserViewers.getFirst();
        testCanAccessAuditTable(externalUserViewer, status().isOk());
        testCanSeeAuditLink(externalUserViewer, true);
    }

    @Test
    public void testExternalUserAdminCanAccessAuditTableAndCanSeeLink() throws Exception {
        EntraUser externalUserAdmin = externalUserAdmins.getFirst();
        testCanAccessAuditTable(externalUserAdmin, status().isOk());
        testCanSeeAuditLink(externalUserAdmin, true);
    }

    @Test
    public void testInternalUserNoRolesCannotAccessAuditTableAndCannotSeeLink() throws Exception {
        EntraUser internalUserNoRoles = internalUsersNoRoles.getFirst();
        testCanAccessAuditTable(internalUserNoRoles, status().is4xxClientError());
        testCanSeeAuditLink(internalUserNoRoles, false);
    }

    @Test
    public void testExternalUserNoRolesCannotAccessAuditTableAndCannotSeeLink() throws Exception {
        EntraUser externalUserNoRoles = externalUsersNoRoles.getFirst();
        testCanAccessAuditTable(externalUserNoRoles, status().is4xxClientError());
        testCanSeeAuditLink(externalUserNoRoles, false);
    }

    @Test
    public void testFirmUserManagerCannotAccessAuditTableAndCanSeeLink() throws Exception {
        EntraUser firmUserManager = externalOnlyUserManagers.getFirst();
        testCanAccessAuditTable(firmUserManager,  status().is4xxClientError());
        testCanSeeAuditLink(firmUserManager, false);
    }

    public void testCanAccessAuditTable(EntraUser loggedInUser, ResultMatcher expectedResult) throws Exception {
        this.mockMvc.perform(get("/admin/users/audit")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(expectedResult);
    }

    public void testCanSeeAuditLink(EntraUser loggedInUser, boolean canSeeLink) throws Exception {
        MvcResult result = this.mockMvc.perform(get("/home")
                        .with(userOauth2Login(loggedInUser))).andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        if (modelAndView == null) {
            Assertions.fail();
        }
        Map<String, Object> model = modelAndView.getModel();
        boolean canViewAuditTable = (boolean) model.get("canViewAuditTable");
        Assertions.assertEquals(canSeeLink, canViewAuditTable);
    }
}
