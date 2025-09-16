package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


public class UserCreationTest extends BaseIntegrationTest {

    @Test
    public void testUserIsRedirectedWhenGoingStraightToCheckAnswersScreen() throws Exception {
        final String path = "/admin/user/create/check-answers";
        MvcResult result = mockMvc.perform(get(path)
                .with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals("/error", result.getResponse().getRedirectedUrl());
    }


}
