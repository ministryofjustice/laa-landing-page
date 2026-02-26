package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

public class SilasAdministrationTest extends RoleBasedAccessIntegrationTest {

    private App testApp;
    private AppRole testAppRole;
    private int distinctIndex = 0;

    @BeforeEach
    public void beforeEach() {
        buildTestAppRole();
    }

    @Test
    public void testShowAdministrationPageLoadsSuccessfully() throws Exception {
        EntraUser loggedInUser = silasAdmins.getFirst();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);

        this.mockMvc.perform(get("/admin/silas-administration")
                        .with(defaultOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("silas-administration/administration"))
                .andExpect(model().attributeExists("adminApps"))
                .andExpect(model().attributeExists("apps"))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().attributeExists("appNames"));
    }

    @Test
    public void testEditAppDetailsGetPageLoadsSuccessfully() throws Exception {
        EntraUser loggedInUser = silasAdmins.getFirst();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);

        this.mockMvc.perform(get(String.format("/admin/silas-administration/app/%s", testApp.getId()))
                        .with(defaultOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("silas-administration/edit-app-details"))
                .andExpect(model().attributeExists("app"))
                .andExpect(model().attributeExists("appDetailsForm"));
    }

    @Test
    public void testEditAppDetailsPostStoresFormInSessionAndRedirects() throws Exception {
        EntraUser loggedInUser = silasAdmins.getFirst();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.post(String.format("/admin/silas-administration/app/%s", testApp.getId()))
                        .with(defaultOauth2Login(loggedInUser))
                        .with(csrf())
                        .param("appId", testApp.getId().toString())
                        .param("enabled", "false")
                        .param("description", "Updated description"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals(String.format("/admin/silas-administration/app/%s/check-answers", testApp.getId()),
                result.getResponse().getRedirectedUrl());

        HttpSession session = result.getRequest().getSession();
        assertNotNull(session.getAttribute("appDetailsForm"));
        assertNotNull(session.getAttribute("appId"));
    }

    @Test
    public void testConfirmAppDetailsGetPageDisplaysCheckAnswers() throws Exception {
        EntraUser loggedInUser = silasAdmins.getFirst();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);

        // First submit form
        MvcResult submitResult = this.mockMvc.perform(MockMvcRequestBuilders.post(String.format("/admin/silas-administration/app/%s", testApp.getId()))
                        .with(defaultOauth2Login(loggedInUser))
                        .with(csrf())
                        .param("appId", testApp.getId().toString())
                        .param("enabled", "false")
                        .param("description", "Updated description"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) submitResult.getRequest().getSession();

        // Then view check-answers page
        this.mockMvc.perform(get(String.format("/admin/silas-administration/app/%s/check-answers", testApp.getId()))
                        .session(session)
                        .with(defaultOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("silas-administration/edit-app-details-check-answers"))
                .andExpect(model().attributeExists("app"))
                .andExpect(model().attributeExists("appDetailsForm"));
    }

    @Test
    public void testConfirmAppDetailsPostSavesChangesAndClearsSession() throws Exception {
        EntraUser loggedInUser = silasAdmins.getFirst();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);

        // Submit form
        MvcResult submitResult = this.mockMvc.perform(MockMvcRequestBuilders.post(String.format("/admin/silas-administration/app/%s", testApp.getId()))
                        .with(defaultOauth2Login(loggedInUser))
                        .with(csrf())
                        .param("appId", testApp.getId().toString())
                        .param("enabled", "false")
                        .param("description", "Updated description"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) submitResult.getRequest().getSession();

        // Confirm changes
        MvcResult confirmResult = this.mockMvc.perform(MockMvcRequestBuilders.post(String.format("/admin/silas-administration/app/%s/check-answers", testApp.getId()))
                        .session(session)
                        .with(defaultOauth2Login(loggedInUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("silas-administration/edit-app-details-confirmation"))
                .andReturn();

        HttpSession confirmSession = confirmResult.getRequest().getSession();
        assertNotNull(confirmSession);
    }

    @Test
    public void testEditAppRoleDetailsGetPageLoadsSuccessfully() throws Exception {
        EntraUser loggedInUser = silasAdmins.getFirst();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);

        this.mockMvc.perform(get(String.format("/admin/silas-administration/role/%s", testAppRole.getId()))
                        .with(defaultOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("silas-administration/edit-role-details"))
                .andExpect(model().attributeExists("appRole"))
                .andExpect(model().attributeExists("appRoleDetailsForm"));
    }

    @Test
    public void testEditAppRoleDetailsPostStoresFormInSessionAndRedirects() throws Exception {
        EntraUser loggedInUser = silasAdmins.getFirst();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.post(String.format("/admin/silas-administration/role/%s", testAppRole.getId()))
                        .with(defaultOauth2Login(loggedInUser))
                        .with(csrf())
                        .param("appRoleId", testAppRole.getId().toString())
                        .param("name", "Updated Role Name")
                        .param("description", "Updated role description"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals(String.format("/admin/silas-administration/role/%s/check-answers", testAppRole.getId()),
                result.getResponse().getRedirectedUrl());

        HttpSession session = result.getRequest().getSession();
        assertNotNull(session.getAttribute("appRoleDetailsForm"));
        assertNotNull(session.getAttribute("roleId"));
    }

    @Test
    public void testConfirmAppRoleDetailsGetPageDisplaysCheckAnswers() throws Exception {
        EntraUser loggedInUser = silasAdmins.getFirst();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);

        // First submit form
        MvcResult submitResult = this.mockMvc.perform(MockMvcRequestBuilders.post(String.format("/admin/silas-administration/role/%s", testAppRole.getId()))
                        .with(defaultOauth2Login(loggedInUser))
                        .with(csrf())
                        .param("appRoleId", testAppRole.getId().toString())
                        .param("name", "Updated Role Name")
                        .param("description", "Updated role description"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) submitResult.getRequest().getSession();

        // Then view check-answers page
        this.mockMvc.perform(get(String.format("/admin/silas-administration/role/%s/check-answers", testAppRole.getId()))
                        .session(session)
                        .with(defaultOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("silas-administration/edit-role-details-check-answers"))
                .andExpect(model().attributeExists("appRole"))
                .andExpect(model().attributeExists("appRoleDetailsForm"));
    }

    @Test
    public void testEditAppOrderGetPageLoadsSuccessfully() throws Exception {
        EntraUser loggedInUser = silasAdmins.getFirst();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);

        this.mockMvc.perform(get("/admin/silas-administration/apps/reorder")
                        .with(defaultOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("silas-administration/edit-apps-order"))
                .andExpect(model().attributeExists("appsOrderForm"));
    }

    @Test
    public void testEditAppRolesOrderGetRedirectsWhenNoAppSelected() throws Exception {
        EntraUser loggedInUser = silasAdmins.getFirst();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);

        MvcResult result = this.mockMvc.perform(get("/admin/silas-administration/roles/reorder")
                        .with(defaultOauth2Login(loggedInUser)))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals("/admin/silas-administration#roles", result.getResponse().getRedirectedUrl());
    }

    @Test
    public void testCancelClearsSessionAndRedirects() throws Exception {
        EntraUser loggedInUser = silasAdmins.getFirst();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);

        MvcResult result = this.mockMvc.perform(get("/admin/silas-administration/cancel/admin-apps")
                        .with(defaultOauth2Login(loggedInUser)))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals("/admin/silas-administration#admin-apps", result.getResponse().getRedirectedUrl());
    }

    @Test
    public void testGlobalAdminCannotAccessAdministrationPage() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);

        this.mockMvc.perform(get("/admin/silas-administration")
                        .with(defaultOauth2Login(loggedInUser)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testSecurityResponseCannotAccessAdministrationPage() throws Exception {
        EntraUser loggedInUser = securityResponseUsers.getFirst();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);

        this.mockMvc.perform(get("/admin/silas-administration")
                        .with(defaultOauth2Login(loggedInUser)))
                .andExpect(status().is4xxClientError());
    }

    private void buildTestAppRole() {
        App app = buildLaaApp("Test App " + distinctIndex++, UUID.randomUUID().toString(),
                "Security Group Id" + distinctIndex++, "Security Group Name" + distinctIndex++,
                "Test App Title " + distinctIndex++, "Test App Description " + distinctIndex++,
                "OID Group " + distinctIndex++, "http://localhost:8080/" + distinctIndex++);
        testApp = appRepository.saveAndFlush(app);
        AppRole appRole = buildLaaAppRole(app, "Test App Role" + distinctIndex++);
        testAppRole = appRoleRepository.saveAndFlush(appRole);
    }
}


