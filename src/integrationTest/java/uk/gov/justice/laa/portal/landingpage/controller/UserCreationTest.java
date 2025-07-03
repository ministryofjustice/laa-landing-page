package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.justice.laa.portal.landingpage.config.TestSecurityConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


@Import(TestSecurityConfig.class)
public class UserCreationTest extends BaseIntegrationTest {

    @Test
    public void testUserIsRedirectedWhenGoingStraightToCreateAppsScreen() throws Exception {
        final String path = "/admin/user/create/services";
        final String expectedRedirectUrl = "/admin/user/create/details";
        MvcResult result = mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals(expectedRedirectUrl, result.getResponse().getRedirectedUrl());
    }

    @Test
    public void testUserIsRedirectedWhenGoingStraightToCreateRolesScreen() throws Exception {
        final String path = "/admin/user/create/roles";
        final String expectedRedirectUrl = "/admin/user/create/details";
        MvcResult result = mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals(expectedRedirectUrl, result.getResponse().getRedirectedUrl());
    }

    @Test
    public void testUserIsRedirectedWhenGoingStraightToCreateOfficesScreen() throws Exception {
        final String path = "/admin/user/create/offices";
        final String expectedRedirectUrl = "/admin/user/create/details";
        MvcResult result = mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals(expectedRedirectUrl, result.getResponse().getRedirectedUrl());
    }

    @Test
    public void testUserIsRedirectedWhenGoingStraightToCheckAnswersScreen() throws Exception {
        final String path = "/admin/user/create/check-answers";
        final String expectedRedirectUrl = "/admin/user/create/details";
        MvcResult result = mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals(expectedRedirectUrl, result.getResponse().getRedirectedUrl());
    }


}
