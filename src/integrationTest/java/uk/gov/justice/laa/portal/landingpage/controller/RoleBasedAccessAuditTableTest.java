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
    public void testInformationAndAssuranceCanAccessAuditTableAndSeeLink() throws Exception {
        EntraUser informationAndAssuranceUser = informationAndAssuranceUsers.getFirst();
        testCanAccessAuditTable(informationAndAssuranceUser, status().isOk());
        testCanSeeAuditLink(informationAndAssuranceUser, true);
    }

    @Test
    public void testInternalUserManagerCannotAccessAuditTableAndCannotSeeLink() throws Exception {
        EntraUser internalUserManager = internalUserManagers.getFirst();
        testCanAccessAuditTable(internalUserManager, status().is4xxClientError());
        testCanSeeAuditLink(internalUserManager, false);
    }

    @Test
    public void testInternalUserWithExternalUserManagerCannotAccessAuditTableAndCannotSeeLink() throws Exception {
        EntraUser internalUserWithExternalUserManager = internalWithExternalOnlyUserManagers.getFirst();
        testCanAccessAuditTable(internalUserWithExternalUserManager, status().is4xxClientError());
        testCanSeeAuditLink(internalUserWithExternalUserManager, false);
    }

    @Test
    public void testInternalUserWithInternalAndExternalUserManagerCannotAccessAuditTableAndCannotSeeLink() throws Exception {
        EntraUser internalUserWithInternalAndExternalUserManager = internalAndExternalUserManagers.getFirst();
        testCanAccessAuditTable(internalUserWithInternalAndExternalUserManager, status().is4xxClientError());
        testCanSeeAuditLink(internalUserWithInternalAndExternalUserManager, false);
    }

    @Test
    public void testInternalUserViewerCannotAccessAuditTableAndCannotSeeLink() throws Exception {
        EntraUser internalUserViewer = internalUserViewers.getFirst();
        testCanAccessAuditTable(internalUserViewer, status().is4xxClientError());
        testCanSeeAuditLink(internalUserViewer, false);
    }

    @Test
    public void testExternalUserViewerCannotAccessAuditTableAndCannotSeeLink() throws Exception {
        EntraUser externalUserViewer = externalUserViewers.getFirst();
        testCanAccessAuditTable(externalUserViewer, status().is4xxClientError());
        testCanSeeAuditLink(externalUserViewer, false);
    }

    @Test
    public void testExternalUserAdminCannotAccessAuditTableAndCannotSeeLink() throws Exception {
        EntraUser externalUserAdmin = externalUserAdmins.getFirst();
        testCanAccessAuditTable(externalUserAdmin, status().is4xxClientError());
        testCanSeeAuditLink(externalUserAdmin, false);
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
    public void testFirmUserManagerCannotAccessAuditTableAndCannotSeeLink() throws Exception {
        EntraUser firmUserManager = externalOnlyUserManagers.getFirst();
        testCanAccessAuditTable(firmUserManager, status().is4xxClientError());
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
