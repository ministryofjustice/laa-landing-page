package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DisabledUserAccessTest extends RoleBasedAccessIntegrationTest {

    @Test
    public void testEnabledUserCanAccessHomePage() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        requestHomePage(loggedInUser, status().isOk());
    }

    @Test
    public void testDisabledUserCannotAccessHomePage() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        loggedInUser.setEnabled(false);
        entraUserRepository.saveAndFlush(loggedInUser);
        requestHomePage(loggedInUser, status().isForbidden());

        // Teardown
        loggedInUser.setEnabled(true);
        entraUserRepository.saveAndFlush(loggedInUser);
    }

    public MvcResult requestHomePage(EntraUser loggedInUser, ResultMatcher expectedResult) throws Exception {
        return this.mockMvc.perform(get("/home")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(expectedResult)
                .andReturn();
    }
}
