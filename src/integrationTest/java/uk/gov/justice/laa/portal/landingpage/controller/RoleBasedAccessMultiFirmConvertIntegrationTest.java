package uk.gov.justice.laa.portal.landingpage.controller;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBasedAccessMultiFirmConvertIntegrationTest extends RoleBasedAccessIntegrationTest {

    @Test
    public void testFirmUserManagerCannotSeeConvertMultiFirmOption() throws Exception {
        EntraUser loggedInUser = firmUserManagers.getFirst();
        UserProfile loggedInUserProfile = loggedInUser.getUserProfiles()
                .stream()
                .findFirst()
                .orElseThrow();
        EntraUser accessedUserSameFirm = externalUsersNoRoles.stream().filter(user -> user.getUserProfiles().stream()
                .findFirst()
                .orElseThrow()
                .getFirm()
                .getId()
                .equals(loggedInUserProfile.getFirm().getId())).findFirst().orElseThrow();
        boolean canSeeConvertMultiFirmOption = canSeeConvertMultiFirmOption(loggedInUser, accessedUserSameFirm);
        Assertions.assertFalse(canSeeConvertMultiFirmOption);
    }

    @Test
    public void testSecurityResponseCannotAccessConvertMultiUserPage() throws Exception {
        EntraUser loggedInUser = securityResponseUsers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        boolean canAccessConvertMultiUserPage = canAccessConvertMultiUserPage(loggedInUser, accessedUser);
        Assertions.assertFalse(canAccessConvertMultiUserPage);
    }

    @Test
    public void testSecurityResponseCannotPostConvertMultiUser() throws Exception {
        EntraUser loggedInUser = securityResponseUsers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        boolean canPostMultiFirmUserConversion = canPostMultiFirmUserConversion(loggedInUser, accessedUser);
        Assertions.assertFalse(canPostMultiFirmUserConversion);
    }

    @Test
    public void testFirmUserManagerCannotAccessConvertMultiUserPage() throws Exception {
        EntraUser loggedInUser = firmUserManagers.getFirst();
        UserProfile loggedInUserProfile = loggedInUser.getUserProfiles()
                .stream()
                .findFirst()
                .orElseThrow();
        EntraUser accessedUserSameFirm = externalUsersNoRoles.stream().filter(user -> user.getUserProfiles().stream()
                .findFirst()
                .orElseThrow()
                .getFirm()
                .getId()
                .equals(loggedInUserProfile.getFirm().getId())).findFirst().orElseThrow();
        boolean canAccessConvertMultiUserPage = canAccessConvertMultiUserPage(loggedInUser, accessedUserSameFirm);
        Assertions.assertFalse(canAccessConvertMultiUserPage);
    }

    @Test
    public void testFirmUserManagerCannotPostConvertMultiUser() throws Exception {
        EntraUser loggedInUser = firmUserManagers.getFirst();
        UserProfile loggedInUserProfile = loggedInUser.getUserProfiles()
                .stream()
                .findFirst()
                .orElseThrow();
        EntraUser accessedUserSameFirm = externalUsersNoRoles.stream().filter(user -> user.getUserProfiles().stream()
                .findFirst()
                .orElseThrow()
                .getFirm()
                .getId()
                .equals(loggedInUserProfile.getFirm().getId())).findFirst().orElseThrow();
        boolean canAccessConvertMultiUserPage = canPostMultiFirmUserConversion(loggedInUser, accessedUserSameFirm);
        Assertions.assertFalse(canAccessConvertMultiUserPage);
    }

    @Test
    public void testExternalUserAdminCanSeeConvertMultiFirmOption() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        boolean canSeeMultiFirmConvertOption = canSeeConvertMultiFirmOption(loggedInUser, accessedUser);
        Assertions.assertTrue(canSeeMultiFirmConvertOption);
    }

    @Test
    public void testExternalUserAdminCanAccessConvertMultiUserPage() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        boolean canAccessConvertMultiUserPage = canAccessConvertMultiUserPage(loggedInUser, accessedUser);
        Assertions.assertTrue(canAccessConvertMultiUserPage);
    }

    @Test
    public void testExternalUserAdminCanPostConvertMultiUser() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        boolean canPostMultiFirmUserConversion = canPostMultiFirmUserConversion(loggedInUser, accessedUser);
        Assertions.assertTrue(canPostMultiFirmUserConversion);
    }

    @Test
    public void testGlobalAdminCanSeeConvertMultiFirmOption() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        boolean canSeeMultiFirmConvertOption = canSeeConvertMultiFirmOption(loggedInUser, accessedUser);
        Assertions.assertTrue(canSeeMultiFirmConvertOption);
    }

    @Test
    public void testGlobalAdminCanAccessConvertMultiUserPage() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        boolean canAccessConvertMultiUserPage = canAccessConvertMultiUserPage(loggedInUser, accessedUser);
        Assertions.assertTrue(canAccessConvertMultiUserPage);
    }

    @Test
    public void testGlobalAdminCanPostConvertMultiUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        boolean canPostMultiFirmUserConversion = canPostMultiFirmUserConversion(loggedInUser, accessedUser);
        Assertions.assertTrue(canPostMultiFirmUserConversion);
    }

    private boolean canSeeConvertMultiFirmOption(EntraUser loggedInUser, EntraUser accessedUser) throws Exception {
        UserProfile accessedUserProfile = accessedUser.getUserProfiles().stream().findFirst().get();
        MvcResult result = this.mockMvc.perform(get("/admin/users/manage/" + accessedUserProfile.getId())
                        .with(userOauth2Login(loggedInUser)))
                        .andExpect(status().isOk())
                        .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        Assertions.assertNotNull(modelAndView);
        Map<String, Object> model = modelAndView.getModel();
        Boolean editorInternalUser = (Boolean) model.get("editorInternalUser");
        Assertions.assertNotNull(editorInternalUser);
        return editorInternalUser;
    }

    private boolean canAccessConvertMultiUserPage(EntraUser loggedInUser, EntraUser accessedUser) throws Exception {
        UserProfile accessedUserProfile = accessedUser.getUserProfiles().stream().findFirst().get();
        MvcResult result = this.mockMvc.perform(get(String.format("/admin/users/edit/%s/convert-to-multi-firm", accessedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser)))
                        .andReturn();
        return result.getResponse().getStatus() == HttpStatus.SC_OK;
    }

    private boolean canPostMultiFirmUserConversion(EntraUser loggedInUser, EntraUser accessedUser) throws Exception {
        UserProfile accessedUserProfile = accessedUser.getUserProfiles().stream().findFirst().get();
        MvcResult result = this.mockMvc.perform(post(String.format("/admin/users/edit/%s/convert-to-multi-firm", accessedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .param("convertToMultiFirm", "true"))
                        .andReturn();
        MockHttpServletResponse response = result.getResponse();
        Assertions.assertNotNull(response);
        final String expectedRedirect = String.format("/admin/users/manage/%s", accessedUserProfile.getId());
        boolean success = response.getStatus() == 302 && expectedRedirect.equals(response.getRedirectedUrl());
        if (success) {
            // Teardown
            accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
            accessedUser.setMultiFirmUser(false);
            entraUserRepository.saveAndFlush(accessedUser);
        }
        return success;
    }
}
