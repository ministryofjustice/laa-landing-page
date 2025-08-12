package uk.gov.justice.laa.portal.landingpage.controller;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import java.util.List;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

public class RoleBasedAccessUserListTest extends RoleBasedAccessIntegrationTest {


    @Test
    public void testGlobalAdminCanSeeAllUsers() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100")
                .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        int expectedSize = allUsers.size();
        Assertions.assertThat(users).hasSize(expectedSize);
    }

    @Test
    public void testGlobalAdminCanSeeAllFirmAdminsAndGlobalAdminsWhenFiltered() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100&showFirmAdmins=true")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        int expectedSize = externalUserAdmins.size() + globalAdmins.size();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserProfileDto userProfile : users) {
            Assertions.assertThat(userProfile.getEntraUser().getLastName()).contains("Admin");
        }
    }

    @Test
    public void testInternalUserManagerCanSeeAllInternalUsers() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        int expectedSize = internalUserManagers.size() + internalAndExternalUserManagers.size() + globalAdmins.size();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserProfileDto userProfile : users) {
            Assertions.assertThat(userProfile.getEntraUser().getFirstName()).contains("Internal");
        }
    }

    @Test
    public void testInternalUserManagerCanSeeOnlyGlobalAdminWhenFiltered() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100&showFirmAdmins=true")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        int expectedSize = globalAdmins.size();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserProfileDto userProfile : users) {
            Assertions.assertThat(userProfile.getEntraUser().getLastName()).isEqualTo("GlobalAdmin");
        }
    }

    @Test
    public void testInternalAndExternalUserManagerCanSeeAllUsers() throws Exception {
        EntraUser loggedInUser = internalAndExternalUserManagers.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        int expectedSize = allUsers.size();
        Assertions.assertThat(users).hasSize(expectedSize);
    }

    @Test
    public void testInternalUserWithExternalUserManagerCanSeeExternalUsersOnly() throws Exception {
        EntraUser loggedInUser = internalWithExternalOnlyUserManagers.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");

        int expectedExternalUsers = externalUsersNoRoles.size() + externalUserAdmins.size() + externalOnlyUserManagers.size();

        Assertions.assertThat(users).hasSize(expectedExternalUsers);
    }

    @Test
    public void testInternalAndExternalUserManagerCanSeeAllFirmAdminsAndGlobalAdminsWhenFiltered() throws Exception {
        EntraUser loggedInUser = internalAndExternalUserManagers.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100&showFirmAdmins=true")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        int expectedSize = externalUserAdmins.size() + globalAdmins.size();
        Assertions.assertThat(users).hasSize(expectedSize);
    }

    @Test
    public void testExternalUserManagerCanSeeAllUsersInFirm() throws Exception {
        EntraUser loggedInUser = externalOnlyUserManagers.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        Firm loggedInUserFirm = loggedInUser.getUserProfiles().stream()
                .findFirst().get()
                .getFirm();
        int expectedSize = (int) allUsers.stream()
                        .map(user -> user.getUserProfiles().stream().findFirst().get().getFirm())
                        .filter(Objects::nonNull)
                        .filter(firm -> firm.getId().equals(loggedInUserFirm.getId()))
                        .count();

        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserProfileDto userProfile : users) {
            Assertions.assertThat(userProfile.getFirm().getId()).isEqualTo(loggedInUserFirm.getId());
        }
    }

    @Test
    public void testExternalUserManagerCanOnlySeeFirmAdminsInSameFirmWhenFiltered() throws Exception {
        EntraUser loggedInUser = externalOnlyUserManagers.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100&showFirmAdmins=true")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        Firm loggedInUserFirm = loggedInUser.getUserProfiles().stream()
                .findFirst().get()
                .getFirm();
        int expectedSize = (int) externalUserAdmins.stream()
                .map(user -> user.getUserProfiles().stream().findFirst().get())
                .map(UserProfile::getFirm)
                .filter(Objects::nonNull)
                .filter(firm -> firm.getId().equals(loggedInUserFirm.getId()))
                .count();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserProfileDto userProfile : users) {
            Assertions.assertThat(userProfile.getFirm().getId()).isEqualTo(loggedInUserFirm.getId());
        }
    }

    @Test
    public void testExternalUserAdminCanSeeAllUsersInFirm() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        Firm loggedInUserFirm = loggedInUser.getUserProfiles().stream()
                .findFirst().get()
                .getFirm();
        int expectedSize = (int) allUsers.stream()
                .map(user -> user.getUserProfiles().stream().findFirst().get().getFirm())
                .filter(Objects::nonNull)
                .filter(firm -> firm.getId().equals(loggedInUserFirm.getId()))
                .count();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserProfileDto userProfile : users) {
            Assertions.assertThat(userProfile.getFirm().getId()).isEqualTo(loggedInUserFirm.getId());
        }
    }

    @Test
    public void testExternalUserAdminCanOnlySeeFirmAdminsInSameFirmWhenFiltered() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100&showFirmAdmins=true")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        Firm loggedInUserFirm = loggedInUser.getUserProfiles().stream()
                .findFirst().get()
                .getFirm();
        int expectedSize = (int) externalUserAdmins.stream()
                .map(user -> user.getUserProfiles().stream().findFirst().get().getFirm())
                .filter(Objects::nonNull)
                .filter(firm -> firm.getId().equals(loggedInUserFirm.getId()))
                .count();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserProfileDto userProfile : users) {
            Assertions.assertThat(userProfile.getFirm().getId()).isEqualTo(loggedInUserFirm.getId());
        }
    }

}
