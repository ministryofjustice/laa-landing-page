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

public class RoleBasedAccessAppMetadataTest extends RoleBasedAccessIntegrationTest {

    @Test
    public void testSilasAdminCanSeeSilasAdminLinkAndAccess() throws Exception {
        EntraUser silasAdminsFirst = silasAdmins.getFirst();
        testCanSeeSilasAdminLink(silasAdminsFirst, true);
        testCanAccessSilasAdminPage(silasAdminsFirst, status().isOk());
    }

    @Test
    public void testSecurityResponseCannotAccessSilasAdminAndSeeLink() throws Exception {
        EntraUser securityResponseUser = securityResponseUsers.getFirst();
        testCanSeeSilasAdminLink(securityResponseUser, false);
        testCanAccessSilasAdminPage(securityResponseUser, status().is4xxClientError());
    }

    @Test
    public void testGlobalAdminCannotAccessSilasAdminAndSeeLink() throws Exception {
        EntraUser globalAdmin = globalAdmins.getFirst();
        testCanSeeSilasAdminLink(globalAdmin, false);
        testCanAccessSilasAdminPage(globalAdmin, status().is4xxClientError());
    }

    @Test
    public void testInternalUserManagerCannotAccessSilasAdminAndSeeLink() throws Exception {
        EntraUser internalUserManager = internalUserManagers.getFirst();
        testCanSeeSilasAdminLink(internalUserManager, false);
        testCanAccessSilasAdminPage(internalUserManager, status().is4xxClientError());
    }

    @Test
    public void testInternalUserWithExternalUserManagerCannotAccessSilasAdminAndSeeLink() throws Exception {
        EntraUser internalUserWithExternalUserManager = internalWithExternalOnlyUserManagers.getFirst();
        testCanSeeSilasAdminLink(internalUserWithExternalUserManager, false);
        testCanAccessSilasAdminPage(internalUserWithExternalUserManager, status().is4xxClientError());
    }

    @Test
    public void testInternalUserWithInternalAndExternalUserManagerCannotAccessSilasAdminAndSeeLink() throws Exception {
        EntraUser internalUserWithInternalAndExternalUserManager = internalAndExternalUserManagers.getFirst();
        testCanSeeSilasAdminLink(internalUserWithInternalAndExternalUserManager, false);
        testCanAccessSilasAdminPage(internalUserWithInternalAndExternalUserManager, status().is4xxClientError());
    }

    @Test
    public void testInternalUserViewerCannotAccessSilasAdminAndSeeLink() throws Exception {
        EntraUser internalUserViewer = internalUserViewers.getFirst();
        testCanSeeSilasAdminLink(internalUserViewer, false);
        testCanAccessSilasAdminPage(internalUserViewer, status().is4xxClientError());
    }

    @Test
    public void testExternalUserViewerCannotAccessSilasAdminAndSeeLink() throws Exception {
        EntraUser externalUserViewer = externalUserViewers.getFirst();
        testCanSeeSilasAdminLink(externalUserViewer, false);
        testCanAccessSilasAdminPage(externalUserViewer, status().is4xxClientError());
    }

    @Test
    public void testExternalUserAdminCannotAccessSilasAdminAndSeeLink() throws Exception {
        EntraUser externalUserAdmin = externalUserAdmins.getFirst();
        testCanSeeSilasAdminLink(externalUserAdmin, false);
        testCanAccessSilasAdminPage(externalUserAdmin, status().is4xxClientError());
    }

    @Test
    public void testInternalUserNoRolesCannotAccessSilasAdminAndSeeLink() throws Exception {
        EntraUser internalUserNoRoles = internalUsersNoRoles.getFirst();
        testCanSeeSilasAdminLink(internalUserNoRoles, false);
        testCanAccessSilasAdminPage(internalUserNoRoles, status().is4xxClientError());
    }

    @Test
    public void testExternalUserNoRolesCannotAccessSilasAdminAndSeeLink() throws Exception {
        EntraUser externalUserNoRoles = externalUsersNoRoles.getFirst();
        testCanSeeSilasAdminLink(externalUserNoRoles, false);
        testCanAccessSilasAdminPage(externalUserNoRoles, status().is4xxClientError());
    }

    @Test
    public void testFirmUserManagerCannotAccessSilasAdminAndSeeLink() throws Exception {
        EntraUser firmUserManager = externalOnlyUserManagers.getFirst();
        testCanSeeSilasAdminLink(firmUserManager, false);
        testCanAccessSilasAdminPage(firmUserManager, status().is4xxClientError());
    }

    public void testCanAccessSilasAdminPage(EntraUser loggedInUser, ResultMatcher expectedResult) throws Exception {
        this.mockMvc.perform(get("/admin/silas-administration")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(expectedResult);
    }

    public void testCanSeeSilasAdminLink(EntraUser loggedInUser, boolean canSeeLink) throws Exception {
        MvcResult result = this.mockMvc.perform(get("/home")
                .with(userOauth2Login(loggedInUser))).andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        if (modelAndView == null) {
            Assertions.fail();
        }
        Map<String, Object> model = modelAndView.getModel();
        boolean hasSilasAdminRole = (boolean) model.get("hasSilasAdminRole");
        Assertions.assertEquals(canSeeLink, hasSilasAdminRole);
    }
}
