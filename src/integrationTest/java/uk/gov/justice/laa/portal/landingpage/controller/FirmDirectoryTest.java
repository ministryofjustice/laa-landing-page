package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

public class FirmDirectoryTest extends BaseIntegrationTest {

    public static final String FIRM_DIRECTORY_PATH = "/admin/firmDirectory";

    @Resource
    private MockMvc mockMvc;

    @Test
    public void accessFirmDirectorySearchScreen() throws Exception {
        mockMvc.perform(get(FIRM_DIRECTORY_PATH)
                .with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("firm-directory/search-page"))
                .andExpect(model().attributeExists(ModelAttributes.PAGE_TITLE))
                .andReturn();
    }
}
