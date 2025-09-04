package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.Objects;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.web.servlet.ModelAndView;

import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    public void testGlobalAdminCanSeeAllUsersWithExternalUserManagerWhenFiltered() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100&showFirmAdmins=true")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        int expectedSize = (int) allUsers.stream()
                .flatMap(user -> user.getUserProfiles().stream())
                .filter(UserProfile::isActiveProfile)
                .filter(profile -> profile.getAppRoles().stream().anyMatch(appRole -> appRole.isAuthzRole() && appRole.getName().equals("External User Manager")))
                .count();

        Assertions.assertThat(users).hasSize(expectedSize);

        for (UserProfileDto userProfile : users) {
            Set<String> authzRoleNames = userProfile.getAppRoles().stream()
                    .map(AppRoleDto::getName)
                    .collect(Collectors.toSet());
            Assertions.assertThat(authzRoleNames).contains("External User Manager");
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
        int expectedSize = (int) allUsers.stream()
                .filter(user -> user.getUserProfiles().stream().findFirst().orElseThrow().getUserType() == UserType.INTERNAL)
                .count();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserProfileDto userProfile : users) {
            Assertions.assertThat(userProfile.getUserType()).isEqualTo(UserType.INTERNAL);
        }
    }

    @Test
    public void testInternalUserManagerWithExternalUserManagerCanSeeAllUsers() throws Exception {
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

        int expectedSize = (int) allUsers.stream()
                .filter(user -> UserType.EXTERNAL == user.getUserProfiles().stream().findFirst().orElseThrow().getUserType())
                .count();

        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserProfileDto userProfile : users) {
            Assertions.assertThat(userProfile.getUserType()).isEqualTo(UserType.EXTERNAL);
        }
    }

    @Test
    public void testInternalUserManagersWithExternalUserManagerCanSeeAllExternalUserManagersWhenFiltered() throws Exception {
        EntraUser loggedInUser = internalAndExternalUserManagers.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100&showFirmAdmins=true")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        int expectedSize = (int) allUsers.stream()
                .flatMap(user -> user.getUserProfiles().stream())
                .filter(UserProfile::isActiveProfile)
                .filter(profile -> profile.getAppRoles().stream().anyMatch(appRole -> appRole.isAuthzRole() && appRole.getName().equals("External User Manager")))
                .count();

        Assertions.assertThat(users).hasSize(expectedSize);

        for (UserProfileDto userProfile : users) {
            Set<String> authzRoleNames = userProfile.getAppRoles().stream()
                    .map(AppRoleDto::getName)
                    .collect(Collectors.toSet());
            Assertions.assertThat(authzRoleNames).contains("External User Manager");
        }
    }

    @Test
    public void testExternalUserWithExternalUserManagerCanSeeAllUsersInFirm() throws Exception {
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
    public void testExternalUserWithExternalUserManagerCanOnlySeeExternalUserManagersInSameFirmWhenFiltered() throws Exception {
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
        int expectedSize = (int) allUsers.stream()
                .flatMap(user -> user.getUserProfiles().stream())
                .filter(profile -> profile.isActiveProfile() && profile.getUserType() == UserType.EXTERNAL && profile.getFirm() != null
                        && profile.getFirm().getId().equals(loggedInUserFirm.getId()))
                .filter(profile -> profile.getAppRoles().stream().anyMatch(appRole -> appRole.isAuthzRole() && appRole.getName().equals("External User Manager")))
                .count();

        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserProfileDto userProfile : users) {
            Assertions.assertThat(userProfile.getFirm().getId()).isEqualTo(loggedInUserFirm.getId());
            Set<String> authzRoleNames = userProfile.getAppRoles().stream()
                    .map(AppRoleDto::getName)
                    .collect(Collectors.toSet());
            Assertions.assertThat(authzRoleNames).contains("External User Manager");
        }
    }

    @Test
    public void testExternalUserWithExternalUserAdminCanSeeAllExternalUsers() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        int expectedSize = (int) allUsers.stream()
                .filter(user -> UserType.EXTERNAL == user.getUserProfiles().stream().findFirst().orElseThrow().getUserType())
                .count();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserProfileDto userProfile : users) {
            Assertions.assertThat(userProfile.getUserType()).isEqualTo(UserType.EXTERNAL);
        }
    }

    @Test
    public void testInternalUserWithExternalUserAdminCanSeeAllExternalUsersWithExternalUserManagersWhenFiltered() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100&showFirmAdmins=true")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");
        int expectedSize = (int) allUsers.stream()
                .flatMap(user -> user.getUserProfiles().stream())
                .filter(UserProfile::isActiveProfile)
                .filter(profile -> UserType.EXTERNAL == profile.getUserType())
                .filter(profile -> profile.getAppRoles().stream().anyMatch(appRole -> appRole.isAuthzRole() && appRole.getName().equals("External User Manager")))
                .count();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserProfileDto userProfile : users) {
            Assertions.assertThat(userProfile.getUserType()).isEqualTo(UserType.EXTERNAL);
            Set<String> authzRoleNames = userProfile.getAppRoles().stream()
                    .map(AppRoleDto::getName)
                    .collect(Collectors.toSet());
            Assertions.assertThat(authzRoleNames).contains("External User Manager");
        }
    }

    @Test
    public void testInternalUserManagerCanOnlySeeInternalUsersWithExternalUserManagersWhenFiltered() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100&showFirmAdmins=true")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserProfileDto> users = (List<UserProfileDto>) modelAndView.getModel().get("users");

        for (UserProfileDto userProfile : users) {
            Assertions.assertThat(userProfile.getUserType()).isEqualTo(UserType.INTERNAL);
            Set<String> authzRoleNames = userProfile.getAppRoles().stream()
                    .map(AppRoleDto::getName)
                    .collect(Collectors.toSet());
            Assertions.assertThat(authzRoleNames).contains("External User Manager");
        }
    }

}
