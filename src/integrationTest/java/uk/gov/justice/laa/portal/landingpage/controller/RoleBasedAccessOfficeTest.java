package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBasedAccessOfficeTest extends RoleBasedAccessIntegrationTest {

    @Test
    @Transactional
    public void testExternalUserManagerCanAssignOfficeFromSameFirmAsUser() throws Exception {
        // Add an office for firm1
        Office.Address officeAddress = Office.Address.builder().addressLine1("Firm 1 Office").city("Firm 1 City").postcode("BT12 3AB").build();
        Office office = Office.builder().address(officeAddress).firm(testFirm1).build();
        officeRepository.saveAndFlush(office);

        // Get an external user manager from firm 1.
        EntraUser loggedInUser = externalOnlyUserManagers.stream()
                .filter(user -> getUserFirm(user).getId().equals(testFirm1.getId()))
                .findFirst()
                .orElseThrow();

        // Get an external user from firm 1.
        EntraUser editedUser = externalUsersNoRoles.stream()
                .filter(user -> getUserFirm(user).getId().equals(testFirm1.getId()))
                .findFirst()
                .orElseThrow();

        // Get their profile.
        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        // Show office list
        MockHttpSession session = new MockHttpSession();
        this.mockMvc.perform(get(String.format("/admin/users/edit/%s/offices", editedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("offices", office.getId().toString())
                        .param("id", editedUserProfile.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        // Send post request to update office
        MvcResult postOfficesResult = this.mockMvc.perform(post(String.format("/admin/users/edit/%s/offices", editedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("offices", office.getId().toString())
                        .param("id", editedUserProfile.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        // Check we're redirected to cya screen
        String cyaUrl = postOfficesResult.getResponse().getRedirectedUrl();
        org.assertj.core.api.Assertions.assertThat(cyaUrl).isNotNull();
        this.mockMvc.perform(get(cyaUrl)
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        //Check your answer
        postOfficesResult = this.mockMvc.perform(post(String.format("/admin/users/edit/%s/offices-check-answer", editedUserProfile.getId().toString()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String confirmUrl = postOfficesResult.getResponse().getRedirectedUrl();
        org.assertj.core.api.Assertions.assertThat(confirmUrl).isNotNull();
        this.mockMvc.perform(get(confirmUrl)
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(confirmUrl).contains("confirmation");

        UserProfile updatedEditedUserProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        Assertions.assertTrue(updatedEditedUserProfile.getOffices().contains(office));
    }

    @Test
    @Transactional
    public void testSecurityResponseCannotAssignOffices() throws Exception {
        // Add an office for firm1
        Office.Address officeAddress = Office.Address.builder().addressLine1("Firm 1 Office").city("Firm 1 City").postcode("BT12 3AB").build();
        Office office = Office.builder().address(officeAddress).firm(testFirm1).build();
        officeRepository.saveAndFlush(office);

        // Get a security response user.
        EntraUser loggedInUser = securityResponseUsers.getFirst();

        // Get an external user from firm 1.
        EntraUser editedUser = externalUsersNoRoles.stream()
                .filter(user -> getUserFirm(user).getId().equals(testFirm1.getId()))
                .findFirst()
                .orElseThrow();

        // Get their profile.
        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        // Show office list
        MockHttpSession session = new MockHttpSession();
        this.mockMvc.perform(get(String.format("/admin/users/edit/%s/offices", editedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("offices", office.getId().toString())
                        .param("id", editedUserProfile.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        // Send post request to update office
        MvcResult postOfficesResult = this.mockMvc.perform(post(String.format("/admin/users/edit/%s/offices", editedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("offices", office.getId().toString())
                        .param("id", editedUserProfile.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        // Check we're redirected to cya screen
        String cyaUrl = postOfficesResult.getResponse().getRedirectedUrl();
        org.assertj.core.api.Assertions.assertThat(cyaUrl).isNotNull();
        this.mockMvc.perform(get(cyaUrl)
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        //Check your answer
        postOfficesResult = this.mockMvc.perform(post(String.format("/admin/users/edit/%s/offices-check-answer", editedUserProfile.getId().toString()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String confirmUrl = postOfficesResult.getResponse().getRedirectedUrl();
        org.assertj.core.api.Assertions.assertThat(confirmUrl).isNotNull();

        // Send post request to update office and receive error
        this.mockMvc.perform(post(String.format("/admin/users/edit/%s/offices", editedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .param("offices", office.getId().toString())
                        .param("id", editedUserProfile.getId().toString()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @Transactional
    public void testExternalUserManagerCannotAssignOfficeFromDifferentFirmToUser() throws Exception {
        // Add an office for firm2.
        Office.Address officeAddress = Office.Address.builder().addressLine1("Firm 2 Office").city("Firm 2 City").postcode("BT12 3AB").build();
        Office office = Office.builder().address(officeAddress).firm(testFirm2).build();
        officeRepository.saveAndFlush(office);

        // Get an external user manager from firm 1.
        EntraUser loggedInUser = externalOnlyUserManagers.stream()
                .filter(user -> getUserFirm(user).getId().equals(testFirm1.getId()))
                .findFirst()
                .orElseThrow();

        // Get an external user from firm 1
        EntraUser editedUser = externalUsersNoRoles.stream()
                .filter(user -> getUserFirm(user).getId().equals(testFirm1.getId()))
                .findFirst()
                .orElseThrow();

        // Get their profile
        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        // Send post request to update office
        this.mockMvc.perform(post(String.format("/admin/users/edit/%s/offices", editedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .param("offices", office.getId().toString())
                        .param("id", editedUserProfile.getId().toString()))
                .andExpect(status().is3xxRedirection());

        UserProfile updatedEditedUserProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        Assertions.assertTrue(updatedEditedUserProfile.getOffices().isEmpty());
    }

    private Firm getUserFirm(EntraUser user) {
        return user.getUserProfiles().stream()
                .findFirst()
                .orElseThrow()
                .getFirm();
    }

}
