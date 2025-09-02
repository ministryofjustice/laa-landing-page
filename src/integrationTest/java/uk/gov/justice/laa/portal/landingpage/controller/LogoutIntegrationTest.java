package uk.gov.justice.laa.portal.landingpage.controller;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import org.springframework.test.context.ActiveProfiles;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for logout functionality
 */
@ActiveProfiles("test")
public class LogoutIntegrationTest extends BaseIntegrationTest {

    @Test
    public void testLogoutSuccessPageAccessibleWithoutAuthentication() throws Exception {
        // Test that the logout-success page can be accessed without authentication
        mockMvc.perform(get("/logout-success"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You're now signed out of your account")));
    }

    @Test
    public void testLogoutRedirectsToLogoutSuccessPage() throws Exception {
        // Test that POST /logout redirects to /logout-success
        mockMvc.perform(post("/logout")
                .with(oauth2Login())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/logout-success"));
    }

    @Test
    public void testLogoutSuccessPageContainsGdsElements() throws Exception {
        // Test that the logout page contains expected GDS components
        mockMvc.perform(get("/logout-success"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("govuk-panel")))
                .andExpect(content().string(containsString("Sign in")));
    }
}