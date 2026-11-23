package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UnauthenticatedTest extends BaseIntegrationTest {

    @Test
    void shouldRedirectAnonymousUser() throws Exception {
        this.mockMvc
                .perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void shouldRedirectAnonymousUserToStart() throws Exception {
        this.mockMvc
                .perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/start"));
    }

    @Test
    void shouldRedirectAuthenticatedUserToHome() throws Exception {
        this.mockMvc
                .perform(get("/").with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }
}
