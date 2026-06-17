package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class HomeControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    public void testUserWithProfileGetsAccountDetails() throws Exception {
        // Set up user with a user profile.
        EntraUser userWithProfile = buildEntraUser(UUID.randomUUID().toString(), "userWithProfile@test.com", "Test", "User");
        userWithProfile = entraUserRepository.saveAndFlush(userWithProfile);
        UserProfile profile = buildLaaUserProfile(userWithProfile, UserType.INTERNAL, true);
        profile = userProfileRepository.saveAndFlush(profile);

        // Access account details and assert success
        MvcResult result = sendMyAccountDetailsGet(userWithProfile);
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        // Teardown
        userProfileRepository.delete(profile);
        entraUserRepository.delete(userWithProfile);
    }

    @Test
    public void testUserWithoutProfileGetsAccountDetails() throws Exception {
        // Set up user with no user profile
        EntraUser userWithoutProfile = buildEntraUser(UUID.randomUUID().toString(), "userWithProfile@test.com", "Test", "User");
        userWithoutProfile = entraUserRepository.saveAndFlush(userWithoutProfile);

        // Access account details and assert success
        MvcResult result = sendMyAccountDetailsGet(userWithoutProfile);
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        // Teardown
        entraUserRepository.delete(userWithoutProfile);
    }


    private MvcResult sendMyAccountDetailsGet(EntraUser loggedInUser) throws Exception {
        return this.mockMvc.perform(get("/home/my-account-details")
                        .with(userOauth2Login(loggedInUser)))
                        .andReturn();
    }
}
