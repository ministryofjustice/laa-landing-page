package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBasedAccessEditUserOfficeTest extends RoleBasedAccessIntegrationTest {

    @Test
    @Transactional
    public void testGlobalAdminCanEditUserOfficeSameFirm() throws Exception {
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        Firm editedUserFirm = editedUserProfile.getFirm();

        Office newOffice = editedUserFirm.getOffices()
                .stream()
                .findFirst()
                .orElseThrow();

        MvcResult result = changeUserOffice(globalAdmins.getFirst(), editedUser, newOffice);

        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/users/edit/" + editedUserProfile.getId() + "/confirmation");
        UserProfile savedProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        assertThat(savedProfile.getOffices().iterator().next().getCode()).isEqualTo(newOffice.getCode());

        // Teardown
        savedProfile.setOffices(new HashSet<>());
        userProfileRepository.save(savedProfile);
    }

    @Test
    @Transactional
    public void testGlobalAdminCannotEditUserOfficeDifferentFirm() throws Exception {
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        Firm editedUserFirm = editedUserProfile.getFirm();
        Firm differentFirm = firmRepository.findAll().stream()
                .filter(firm -> !firm.getId().equals(editedUserFirm.getId()))
                .findFirst()
                .orElseThrow();

        List<Office> offices = officeRepository.findOfficeByFirm_IdIn(List.of(differentFirm.getId()));
        
        Office newOffice = offices.stream()
                .findFirst()
                .orElseThrow();

        MvcResult result = changeUserOffice(globalAdmins.getFirst(), editedUser, newOffice);

        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/users/edit/" + editedUserProfile.getId() + "/confirmation");
        UserProfile savedProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        assertThat(savedProfile.getOffices()).isEmpty();
    }

    @Test
    @Transactional
    public void testSecurityResponseCannotEditUserOffice() throws Exception {
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        Firm editedUserFirm = editedUserProfile.getFirm();

        Office newOffice = editedUserFirm.getOffices()
                .stream()
                .findFirst()
                .orElseThrow();

        MvcResult result = changeUserOffice(securityResponseUsers.getFirst(), editedUser, newOffice, false);

        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
        UserProfile savedProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        assertThat(savedProfile.getOffices()).isEmpty();
    }

    @Test
    @Transactional
    public void testInternalUserWithExternalUserManagerCanEditUserOfficeSameFirm() throws Exception {
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        Firm editedUserFirm = editedUserProfile.getFirm();

        Office newOffice = editedUserFirm.getOffices()
                .stream()
                .findFirst()
                .orElseThrow();

        MvcResult result = changeUserOffice(internalAndExternalUserManagers.getFirst(), editedUser, newOffice);

        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/users/edit/" + editedUserProfile.getId() + "/confirmation");
        UserProfile savedProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        assertThat(savedProfile.getOffices().iterator().next().getCode()).isEqualTo(newOffice.getCode());

        // Teardown
        savedProfile.setOffices(new HashSet<>());
        userProfileRepository.save(savedProfile);
    }

    @Test
    @Transactional
    public void testInternalUserWithExternalUserManagerCannotEditUserOfficeDifferentFirm() throws Exception {
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        Firm editedUserFirm = editedUserProfile.getFirm();
        Firm differentFirm = firmRepository.findAll().stream()
                .filter(firm -> !firm.getId().equals(editedUserFirm.getId()))
                .findFirst()
                .orElseThrow();

        List<Office> offices = officeRepository.findOfficeByFirm_IdIn(List.of(differentFirm.getId()));
        
        Office newOffice = offices.stream()
                .findFirst()
                .orElseThrow();

        MvcResult result = changeUserOffice(internalAndExternalUserManagers.getFirst(), editedUser, newOffice);

        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/users/edit/" + editedUserProfile.getId() + "/confirmation");
        UserProfile savedProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        assertThat(savedProfile.getOffices()).isEmpty();
    }

    @Test
    @Transactional
    public void testExternalUserAdminCanEditUserOfficeSameFirm() throws Exception {
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        Firm editedUserFirm = editedUserProfile.getFirm();

        Office newOffice = editedUserFirm.getOffices()
                .stream()
                .findFirst()
                .orElseThrow();

        MvcResult result = changeUserOffice(externalUserAdmins.getFirst(), editedUser, newOffice);

        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/users/edit/" + editedUserProfile.getId() + "/confirmation");
        UserProfile savedProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        assertThat(savedProfile.getOffices().iterator().next().getCode()).isEqualTo(newOffice.getCode());

        // Teardown
        savedProfile.setOffices(new HashSet<>());
        userProfileRepository.save(savedProfile);
    }

    @Test
    @Transactional
    public void testExternalUserAdminCannotEditUserOfficeDifferentFirm() throws Exception {
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        Firm editedUserFirm = editedUserProfile.getFirm();
        Firm differentFirm = firmRepository.findAll().stream()
                .filter(firm -> !firm.getId().equals(editedUserFirm.getId()))
                .findFirst()
                .orElseThrow();

        Office newOffice = differentFirm.getOffices()
                .stream()
                .findFirst()
                .orElseThrow();

        MvcResult result = changeUserOffice(externalUserAdmins.getFirst(), editedUser, newOffice);

        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/users/edit/" + editedUserProfile.getId() + "/confirmation");
        UserProfile savedProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        assertThat(savedProfile.getOffices()).isEmpty();
    }

    @Test
    @Transactional
    public void testInternalUserManagerCannotEditUserOffice() throws Exception {
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        Firm editedUserFirm = editedUserProfile.getFirm();

        Office newOffice = editedUserFirm.getOffices()
                .stream()
                .findFirst()
                .orElseThrow();

        MvcResult result = changeUserOffice(internalUserManagers.getFirst(), editedUser, newOffice, false);

        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
        UserProfile savedProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        assertThat(savedProfile.getOffices()).isEmpty();
    }

    @Test
    @Transactional
    public void testExternalUserWithExternalUserManagerCanEditUserOfficeSameFirm() throws Exception {
        EntraUser loggedInUser = externalOnlyUserManagers.getFirst();
        UserProfile loggedInUserProfile = loggedInUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        Firm loggedInUserFirm = loggedInUserProfile.getFirm();

        // Get an external user from the same firm
        EntraUser editedUser = externalUsersNoRoles.stream()
                .filter(user -> user.getUserProfiles().stream().findFirst().orElseThrow().getFirm().getId().equals(loggedInUserFirm.getId()))
                .findFirst()
                .orElseThrow();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();


        Office newOffice = loggedInUserFirm.getOffices()
                .stream()
                .findFirst()
                .orElseThrow();

        MvcResult result = changeUserOffice(loggedInUser, editedUser, newOffice);

        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/users/edit/" + editedUserProfile.getId() + "/confirmation");
        UserProfile savedProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        assertThat(savedProfile.getOffices().iterator().next().getCode()).isEqualTo(newOffice.getCode());

        // Teardown
        savedProfile.setOffices(new HashSet<>());
        userProfileRepository.save(savedProfile);
    }

    @Test
    @Transactional
    public void testExternalUserWithExternalUserManagerCannotEditUserOfficeDifferentFirm() throws Exception {
        EntraUser loggedInUser = externalOnlyUserManagers.getFirst();
        UserProfile loggedInUserProfile = loggedInUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        Firm loggedInUserFirm = loggedInUserProfile.getFirm();

        // Get an external user from a different firm
        EntraUser editedUser = externalUsersNoRoles.stream()
                .filter(user -> !user.getUserProfiles().stream().findFirst().orElseThrow().getFirm().getId().equals(loggedInUserFirm.getId()))
                .findFirst()
                .orElseThrow();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();


        Office newOffice = loggedInUserFirm.getOffices()
                .stream()
                .findFirst()
                .orElseThrow();

        MvcResult result = changeUserOffice(loggedInUser, editedUser, newOffice, false);

        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
        UserProfile savedProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        assertThat(savedProfile.getOffices()).isEmpty();
    }

    @Test
    @Transactional
    public void testInternalUserNoRolesCannotEditUserOffice() throws Exception {
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        Firm editedUserFirm = editedUserProfile.getFirm();

        Office newOffice = editedUserFirm.getOffices()
                .stream()
                .findFirst()
                .orElseThrow();

        this.mockMvc.perform(post(String.format("/admin/users/edit/%s/offices", editedUserProfile.getId()))
                        .with(userOauth2Login(internalUsersNoRoles.getFirst()))
                        .with(csrf())
                        .param("offices", newOffice.getId().toString()))
                .andExpect(status().isForbidden());

        UserProfile savedProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        assertThat(savedProfile.getOffices()).isEmpty();
    }

    @Test
    @Transactional
    public void testExternalUserNoRolesCannotEditUserOffice() throws Exception {
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        UserProfile editedUserProfile = editedUser.getUserProfiles().stream()
                .findFirst()
                .orElseThrow();

        Firm editedUserFirm = editedUserProfile.getFirm();

        Office newOffice = editedUserFirm.getOffices()
                .stream()
                .findFirst()
                .orElseThrow();

        this.mockMvc.perform(post(String.format("/admin/users/edit/%s/offices", editedUserProfile.getId()))
                        .with(userOauth2Login(externalUsersNoRoles.getLast()))
                        .with(csrf())
                        .param("offices", newOffice.getId().toString()))
                .andExpect(status().isForbidden());

        UserProfile savedProfile = userProfileRepository.findById(editedUserProfile.getId()).orElseThrow();
        assertThat(savedProfile.getOffices()).isEmpty();
    }

    private MvcResult changeUserOffice(EntraUser loggedInUser, EntraUser editedUser, Office newOffice) throws Exception {
        return changeUserOffice(loggedInUser, editedUser, newOffice, true);
    }

    private MvcResult changeUserOffice(EntraUser loggedInUser, EntraUser editedUser, Office newOffice, boolean access) throws Exception {
        UserProfile editedUserProfile = editedUser.getUserProfiles().stream().findFirst().orElseThrow();
        // Show office list
        MockHttpSession session = new MockHttpSession();
        MvcResult result = mockMvc.perform(get(String.format("/admin/users/edit/%s/offices", editedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("id", editedUserProfile.getId().toString()))
                .andExpect(access ? status().isOk() : status().is3xxRedirection())
                .andReturn();
        if (!access) {
            return result;
        }

        this.mockMvc.perform(post(String.format("/admin/users/edit/%s/offices", editedUserProfile.getId()))
                .with(userOauth2Login(loggedInUser))
                .with(csrf())
                .session(session)
                .param("offices", newOffice.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        return this.mockMvc.perform(post(String.format("/admin/users/edit/%s/offices-check-answer", editedUserProfile.getId().toString()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
    }

}
