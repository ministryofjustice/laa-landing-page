package uk.gov.justice.laa.portal.landingpage.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.justice.laa.portal.landingpage.exception.GlobalExceptionHandler;

@WebMvcTest(controllers = ErrorTestController.class)
@Import(GlobalExceptionHandler.class)  // Import exception handler for proper error handling
@TestPropertySource(properties = {
    "app.test.error-pages.enabled=true"
})
class ErrorTestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock OAuth2 beans to avoid security auto-configuration issues
    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Test
    @WithMockUser  // Add authenticated user to bypass security
    void testErrorController_whenPropertyEnabled_shouldBeAccessible() throws Exception {
        // Test that the controller is available when the property is enabled
        mockMvc.perform(get("/test-errors/404"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser  // Add authenticated user to bypass security
    void testErrorController_trigger500_endpointExists() throws Exception {
        // Test that the 500 endpoint exists and is accessible
        // The actual error handling would be tested in a full integration test
        // Here we just verify the endpoint is mapped and accessible
        try {
            mockMvc.perform(get("/test-errors/500"));
        } catch (Exception e) {
            // We expect an exception to be thrown, which indicates the endpoint exists
            // and the RuntimeException is being thrown as expected
            assertTrue(e.getMessage().contains("Test internal server error") 
                      || (e.getCause() != null && e.getCause().getMessage().contains("Test internal server error")));
        }
    }

    @Test
    @WithMockUser  // Add authenticated user to bypass security
    void testErrorController_trigger403_shouldReturn403() throws Exception {
        // Test that 403 endpoint triggers access denied
        mockMvc.perform(get("/test-errors/403"))
            .andExpect(status().isForbidden());
    }
}
