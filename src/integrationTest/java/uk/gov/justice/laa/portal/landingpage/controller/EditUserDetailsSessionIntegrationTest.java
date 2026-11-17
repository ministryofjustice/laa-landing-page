package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.session.Session;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.service.EmailValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

public class EditUserDetailsSessionIntegrationTest extends RoleBasedAccessIntegrationTest {

    @MockitoBean
    private EmailValidationService emailValidationService;

    // This test reproduces a bug where editing a user details breaks create user.
    @Test
    public void testUserCanAccessCreateUserFlowWhenAfterEditingUserDetails() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();
        MockHttpSession session = new MockHttpSession();
        final String newFirstName = editedUser.getFirstName() + "EDITED";

        // Keep old details in memory for verification/teardown.
        final String oldFirstName = editedUser.getFirstName();
        final String oldLastName = editedUser.getLastName();
        final String oldEmail = editedUser.getEmail();

        // Mock email validation response
        when(emailValidationService.isValidEmailDomain(oldEmail)).thenReturn(true);

        // Update user's first name
        updateUserFirstNamePost(loggedInUser, editedUser, session, newFirstName);

        // Verify details have been updated correctly.
        editedUser = entraUserRepository.findById(editedUser.getId()).orElseThrow();
        assertThat(editedUser.getFirstName()).isEqualTo(newFirstName);
        assertThat(editedUser.getLastName()).isEqualTo(oldLastName);
        assertThat(editedUser.getEmail()).isEqualTo(oldEmail);

        // Verify create user call is successful
        createUserGet(loggedInUser, session);

        // Teardown
        editedUser.setFirstName(oldFirstName);
        entraUserRepository.save(editedUser);
    }

    private void updateUserFirstNamePost(EntraUser loggedInUser, EntraUser editedUser, MockHttpSession session, String newFirstName) throws Exception {
        UserProfile activeProfile = editedUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .findFirst()
                .orElseThrow();
        mockMvc.perform(post(String.format("/admin/users/edit/%s/details", activeProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("firstName", newFirstName)
                        .param("lastName", editedUser.getLastName())
                        .param("email", editedUser.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/manage/" + activeProfile.getId()));
    }

    private void createUserGet(EntraUser loggedInUser, MockHttpSession session) throws Exception {
        mockMvc.perform(get("/admin/user/create/details")
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andExpect(status().is2xxSuccessful());
    }

}
