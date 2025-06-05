package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.microsoft.graph.serviceclient.GraphServiceClient;

import uk.gov.justice.laa.portal.landingpage.service.EmailService;

class UserControllerTest extends BaseIntegrationTest {

    @MockitoBean
    private GraphServiceClient graphServiceClient;
    @MockitoBean
    private EmailService emailService;

    @Test
    void shouldRedirectAnonymousUser() throws Exception {
        this.mockMvc
                .perform(get("/users"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Happy Path Test: displaySavedUsers get")
    void displaySavedUsers() throws Exception {
        this.mockMvc.perform(get("/userlist"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"));
    }
}
