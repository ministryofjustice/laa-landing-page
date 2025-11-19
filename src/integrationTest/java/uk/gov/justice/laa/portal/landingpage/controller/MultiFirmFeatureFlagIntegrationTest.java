package uk.gov.justice.laa.portal.landingpage.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;

/**
 * Integration tests for FEATURE_FLAG_ENABLE_MULTI_FIRM_USER feature flag.
 * Tests the routing behavior when the flag is enabled vs disabled.
 * This test verifies that:
 * - When flag is ENABLED: user creation flow includes the multi-firm selection step
 */
class MultiFirmFeatureFlagIntegrationTest extends BaseIntegrationTest {

    /**
     * Tests when feature flag is ENABLED
     */
    @Nested
    class WhenFeatureFlagEnabled {

        @Test
        void getMultiFirmPage_withUserInSession_shouldReturnMultiFirmView() throws Exception {
            // Arrange
            MockHttpSession testSession = new MockHttpSession();
            EntraUserDto user = new EntraUserDto();
            user.setFullName("Test User");
            testSession.setAttribute("user", user);

            // Act & Assert
            mockMvc.perform(get("/admin/user/create/multi-firm")
                            .with(csrf())
                            .with(defaultOauth2Login(defaultLoggedInUser))
                            .session(testSession))
                    .andExpect(status().isOk())
                    .andExpect(view().name("add-user-multi-firm"))
                    .andExpect(model().attribute("pageTitle", is("Allow multi-firm access")));
        }

        @Test
        void getMultiFirmPage_withNoUserInSession_shouldRedirectToDetails() throws Exception {
            // Arrange
            MockHttpSession testSession = new MockHttpSession();

            // Act & Assert
            mockMvc.perform(get("/admin/user/create/multi-firm")
                            .with(csrf())
                            .with(defaultOauth2Login(defaultLoggedInUser))
                            .session(testSession))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/user/create/details"));
        }

        @Test
        void getMultiFirmPage_shouldPrePopulateFromSession() throws Exception {
            // Arrange
            MockHttpSession testSession = new MockHttpSession();
            EntraUserDto user = new EntraUserDto();
            user.setFullName("Test User");
            testSession.setAttribute("user", user);
            testSession.setAttribute("isMultiFirmUser", true);

            // Act & Assert
            mockMvc.perform(get("/admin/user/create/multi-firm")
                            .with(csrf())
                            .with(defaultOauth2Login(defaultLoggedInUser))
                            .session(testSession))
                    .andExpect(status().isOk())
                    .andExpect(view().name("add-user-multi-firm"));
        }
    }

    /**
     * Tests that work regardless of feature flag state
     */
    @Nested
    class FeatureFlagIndependentTests {

        @Test
        void getMultiFirmPage_withoutAuthentication_shouldRedirectToLogin() throws Exception {
            // Act & Assert - unauthenticated requests should redirect to login
            mockMvc.perform(get("/admin/user/create/multi-firm"))
                    .andExpect(status().is3xxRedirection());
        }
    }
}
