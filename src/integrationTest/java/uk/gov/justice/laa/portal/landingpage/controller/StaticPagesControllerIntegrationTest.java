package uk.gov.justice.laa.portal.landingpage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;

@ActiveProfiles("test")
public class StaticPagesControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void getCookies_shouldReturnCookiesPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/cookies"))
                .andExpect(status().isOk())
                .andExpect(view().name("footer/cookies"))
                .andExpect(model().attribute(ModelAttributes.PAGE_TITLE, "Cookies"));
    }

    @Test
    void getCookies_shouldBeAccessibleWithoutAuthentication() throws Exception {
        // When & Then - Test that cookies page is publicly accessible
        mockMvc.perform(get("/cookies"))
                .andExpect(status().isOk())
                .andExpect(view().name("footer/cookies"));
    }

    @Test
    void getCookies_shouldSetCorrectModelAttributes() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/cookies"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        assertThat(result.getModelAndView().getModel())
                .containsEntry(ModelAttributes.PAGE_TITLE, "Cookies");
        assertThat(result.getModelAndView().getViewName())
                .isEqualTo("footer/cookies");
    }
}
