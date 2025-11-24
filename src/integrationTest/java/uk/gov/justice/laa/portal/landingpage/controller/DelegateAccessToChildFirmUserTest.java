package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

public class DelegateAccessToChildFirmUserTest extends RoleBasedAccessIntegrationTest {

    @Test
    @Transactional
    public void parentFirmManagerDelegatesAccessToChildFirmUser_redirectsViaSelectFirm() throws Exception {
        EntraUser loggedInUser = firmUserManagers.getFirst();
        Firm parent = loggedInUser.getUserProfiles().stream().findFirst().orElseThrow().getFirm();
        setParentFirmType(parent);
        Firm child = createChildFirm(parent, "DA Child A", "DA-CH-A");

        MockHttpSession session = new MockHttpSession();

        MvcResult firstGet = mockMvc.perform(get("/admin/multi-firm/user/add/profile")
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(firstGet.getResponse().getRedirectedUrl())
                .isEqualTo("/admin/multi-firm/user/add/profile/select/firm");

        MvcResult selectResult = mockMvc.perform(post("/admin/multi-firm/user/add/profile/select/firm")
                        .param("firmId", child.getId().toString())
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(selectResult.getResponse().getRedirectedUrl())
                .isEqualTo("/admin/multi-firm/user/add/profile");
    }

    @Test
    @Transactional
    public void parentFirmManagerDelegatesAccessToParentFirmUser_redirectsViaSelectFirm() throws Exception {
        EntraUser loggedInUser = firmUserManagers.getFirst();
        Firm parent = loggedInUser.getUserProfiles().stream().findFirst().orElseThrow().getFirm();
        setParentFirmType(parent);
        createChildFirm(parent, "DA Child B", "DA-CH-B");

        MockHttpSession session = new MockHttpSession();

        MvcResult firstGet = mockMvc.perform(get("/admin/multi-firm/user/add/profile")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(firstGet.getResponse().getRedirectedUrl())
                .isEqualTo("/admin/multi-firm/user/add/profile/select/firm");

        MvcResult selectResult = mockMvc.perform(post("/admin/multi-firm/user/add/profile/select/firm")
                        .param("firmId", parent.getId().toString())
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(selectResult.getResponse().getRedirectedUrl())
                .isEqualTo("/admin/multi-firm/user/add/profile");
    }

    @Test
    @Transactional
    public void childFirmManagerDelegatesAccessToChildFirmUser_noSelectFirmPageShown() throws Exception {
        Firm parent = testFirm2;
        setParentFirmType(parent);
        Firm child = createChildFirm(parent, "DA Child C", "DA-CH-C");

        EntraUser loggedInUser = createExternalFirmUserManagerAtFirm("child-fum@example.com", child);

        MockHttpSession session = new MockHttpSession();
        MvcResult result = mockMvc.perform(get("/admin/multi-firm/user/add/profile")
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
    }
}
