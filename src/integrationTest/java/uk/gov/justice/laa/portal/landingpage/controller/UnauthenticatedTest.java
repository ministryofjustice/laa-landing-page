package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@TestPropertySource(properties = {
    "app.external-identity-service-link=https://portal-laa.test.external-identity.service.justice.gov.uk"
})
public class UnauthenticatedTest extends BaseIntegrationTest {

    @Test
    void shouldRenderStartPageForAnonymousUser() throws Exception {
        this.mockMvc.perform(get("/start"))
                .andExpect(status().isOk())
                .andExpect(view().name("start"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(content().string(containsString("Sign in to Legal Aid Services")))
                .andExpect(content().string(containsString(
                        "href=\"https://portal-laa.test.external-identity.service.justice.gov.uk/activate\"")))
                .andExpect(content().string(containsString("href=\"/oauth2/authorization/azure\"")));
    }

    @Test
    void shouldRedirectAnonymousUser() throws Exception {
        this.mockMvc
                .perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection());
    }
}
