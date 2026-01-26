package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;


public class FirmDirectoryTest extends BaseIntegrationTest {

    public static final String FIRM_DIRECTORY = "firmDirectory";

    @Test
    public void accessFirmDirectorySearchScreen() throws Exception {
        final String path = "/" + FIRM_DIRECTORY;
        mockMvc.perform(get(path)
                .with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("/firm-directory/search-page"))
                .andExpect(model().attributeExists(ModelAttributes.PAGE_TITLE))
                .andReturn();
    }


}
