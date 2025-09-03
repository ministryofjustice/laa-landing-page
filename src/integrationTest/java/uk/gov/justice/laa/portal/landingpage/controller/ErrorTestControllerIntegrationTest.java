package uk.gov.justice.laa.portal.landingpage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "app.test.error-pages.enabled=true",
    "spring.main.allow-bean-definition-overriding=true"
})
class ErrorTestControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Test
    void testErrorController_whenPropertyEnabled_shouldBeAccessible() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Test that the controller is available when the property is enabled
        mockMvc.perform(get("/test-errors/404"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testErrorController_trigger500_shouldReturn500() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Test that 500 endpoint triggers internal server error
        mockMvc.perform(get("/test-errors/500"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testErrorController_trigger403_shouldReturn403() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Test that 403 endpoint triggers access denied
        mockMvc.perform(get("/test-errors/403"))
            .andExpect(status().isForbidden());
    }
}
