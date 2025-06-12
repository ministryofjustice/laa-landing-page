package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UnauthenticatedTest extends BaseIntegrationTest {

    @Test
    void shouldRedirectAnonymousUser() throws Exception {
        this.mockMvc
                .perform(get("/users"))
                .andExpect(status().is3xxRedirection());
    }
}
