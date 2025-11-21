package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.Objects;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchResultsDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.stream.Collectors;

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
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");
        List<String> expectedUserNames = allUsers.stream()
                .flatMap(user -> user.getUserProfiles().stream())
                .filter(UserProfile::isActiveProfile)
                .filter(profile -> profile.getAppRoles().stream()
                        .anyMatch(appRole -> appRole.isAuthzRole() && (appRole.getName().equals("External User Manager") || appRole.getName().equals("Firm User Manager"))))
                .map(profile -> profile.getEntraUser().getFirstName() + " " + profile.getEntraUser().getLastName())
                .toList();

        Assertions.assertThat(users).hasSize(expectedUserNames.size());

        List<String> actualUserNames = users.stream().map(UserSearchResultsDto::fullName).collect(Collectors.toList());

        Assertions.assertThat(actualUserNames).containsAll(expectedUserNames);
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
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");
        int expectedSize = (int) allUsers.stream()
                .filter(user -> user.getUserProfiles().stream().findFirst().orElseThrow().getUserType() == UserType.INTERNAL)
                .count();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserSearchResultsDto user : users) {
            Assertions.assertThat(user.userType()).isEqualTo(UserType.INTERNAL);
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
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");

        int expectedSize = (int) allUsers.stream()
                .filter(user -> UserType.EXTERNAL == user.getUserProfiles().stream().findFirst().orElseThrow().getUserType())
                .count();

        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserSearchResultsDto user : users) {
            Assertions.assertThat(user.userType()).isEqualTo(UserType.EXTERNAL);
        }
    }

    @Test
    @Transactional
    public void testExternalUserWithFirmUserManagerInParentFirmCanSeeParentAndChildFirmUsers() throws Exception {
        Firm parent = testFirm2;
        setParentFirmType(parent);
        Firm child = createChildFirm(parent, "Child Firm RB-P", "CRB-P");

        EntraUser loggedInUser = firmUserManagers.getFirst();

        EntraUser parentUser = createExternalUserAtFirm("rbp-parent@example.com", parent);
        EntraUser childUser = createExternalUserAtFirm("rbp-child@example.com", child);

        MvcResult result = this.mockMvc.perform(get("/admin/users?size=1000")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();

        ModelAndView modelAndView = result.getModelAndView();
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");

        Assertions.assertThat(users.stream().map(UserSearchResultsDto::email).toList())
                .contains("rbp-parent@example.com", "rbp-child@example.com");

    }

    @Test
    @Transactional
    public void testExternalUserWithFirmUserManagerInChildFirmCanSeeChildFirmUsersOnly() throws Exception {
        Firm parent = testFirm2;
        setParentFirmType(parent);
        Firm child = createChildFirm(parent, "Child Firm RB-C", "CRB-C");

        EntraUser loggedInUser = createExternalFirmUserManagerAtFirm("rbc-fum@example.com", child);

        EntraUser parentUser = createExternalUserAtFirm("rbc-parent@example.com", parent);
        EntraUser childUser = createExternalUserAtFirm("rbc-child@example.com", child);

        MvcResult result = this.mockMvc.perform(get("/admin/users?size=1000")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();

        ModelAndView modelAndView = result.getModelAndView();
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");

        Assertions.assertThat(users.stream().map(UserSearchResultsDto::email).toList())
                .contains("rbc-child@example.com")
                .doesNotContain("rbc-parent@example.com");

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
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");
        List<String> expectedUserNames = allUsers.stream()
                .flatMap(user -> user.getUserProfiles().stream())
                .filter(UserProfile::isActiveProfile)
                .filter(profile -> profile.getAppRoles().stream()
                        .anyMatch(appRole -> appRole.isAuthzRole() && (appRole.getName().equals("External User Manager") || appRole.getName().equals("Firm User Manager"))))
                .map(profile -> profile.getEntraUser().getFirstName() + " " + profile.getEntraUser().getLastName())
                .toList();


        Assertions.assertThat(users).hasSize(expectedUserNames.size());

        List<String> actualUserNames = users.stream().map(UserSearchResultsDto::fullName).collect(Collectors.toList());

        Assertions.assertThat(actualUserNames).containsAll(expectedUserNames);
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
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");
        Firm loggedInUserFirm = loggedInUser.getUserProfiles().stream()
                .findFirst().get()
                .getFirm();
        int expectedSize = (int) allUsers.stream()
                        .map(user -> user.getUserProfiles().stream().findFirst().get().getFirm())
                        .filter(Objects::nonNull)
                        .filter(firm -> firm.getId().equals(loggedInUserFirm.getId()))
                        .count();

        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserSearchResultsDto user : users) {
            Assertions.assertThat(user.firmName()).isEqualTo(loggedInUserFirm.getName());
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
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");
        Firm loggedInUserFirm = loggedInUser.getUserProfiles().stream()
                .findFirst().get()
                .getFirm();
        List<String> expectedUserNames = allUsers.stream()
                .flatMap(user -> user.getUserProfiles().stream())
                .filter(profile -> profile.isActiveProfile() && profile.getUserType() == UserType.EXTERNAL && profile.getFirm() != null
                        && profile.getFirm().getId().equals(loggedInUserFirm.getId()))
                .filter(profile -> profile.getAppRoles().stream()
                        .anyMatch(appRole -> appRole.isAuthzRole() && (appRole.getName().equals("External User Manager") || appRole.getName().equals("Firm User Manager"))))
                .map(userProfile -> userProfile.getEntraUser().getFirstName() + " " + userProfile.getEntraUser().getLastName())
                .toList();

        Assertions.assertThat(users).hasSize(expectedUserNames.size());

        List<String> actualUserNames = users.stream().map(UserSearchResultsDto::fullName).collect(Collectors.toList());

        Assertions.assertThat(actualUserNames).containsAll(expectedUserNames);

        for (UserSearchResultsDto user : users) {
            Assertions.assertThat(user.firmName()).isEqualTo(loggedInUserFirm.getName());
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
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");
        int expectedSize = (int) allUsers.stream()
                .filter(user -> UserType.EXTERNAL == user.getUserProfiles().stream().findFirst().orElseThrow().getUserType())
                .count();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserSearchResultsDto user : users) {
            Assertions.assertThat(user.userType()).isEqualTo(UserType.EXTERNAL);
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
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");
        List<String> expectedUserNames = allUsers.stream()
                .flatMap(user -> user.getUserProfiles().stream())
                .filter(UserProfile::isActiveProfile)
                .filter(profile -> UserType.EXTERNAL == profile.getUserType())
                .filter(profile -> profile.getAppRoles().stream()
                        .anyMatch(appRole -> appRole.isAuthzRole() && (appRole.getName().equals("External User Manager") || appRole.getName().equals("Firm User Manager"))))
                .map(userProfile -> userProfile.getEntraUser().getFirstName() + " " + userProfile.getEntraUser().getLastName())
                .toList();
        Assertions.assertThat(users).hasSize(expectedUserNames.size());

        List<String> actualUserNames = users.stream().map(UserSearchResultsDto::fullName).collect(Collectors.toList());

        Assertions.assertThat(actualUserNames).containsAll(expectedUserNames);
    }

    @Test
    public void testInternalUserManagerCanOnlySeeInternalUsers() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100&showFirmAdmins=true")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");

        List<String> expectedUserNames = allUsers.stream()
                .flatMap(user -> user.getUserProfiles().stream())
                .filter(UserProfile::isActiveProfile)
                .filter(profile -> profile.getAppRoles().stream()
                        .anyMatch(appRole -> appRole.isAuthzRole() && (appRole.getName().equals("External User Manager"))))
                .map(profile -> profile.getEntraUser().getFirstName() + " " + profile.getEntraUser().getLastName())
                .toList();

        Assertions.assertThat(users).hasSize(expectedUserNames.size());

        List<String> actualUserNames = users.stream().map(UserSearchResultsDto::fullName).collect(Collectors.toList());

        Assertions.assertThat(actualUserNames).containsAll(expectedUserNames);

        for (UserSearchResultsDto user : users) {
            Assertions.assertThat(user.userType()).isEqualTo(UserType.INTERNAL);
        }
    }

    @Test
    public void testInternalUserViewerCanSeeAllInternalUsers() throws Exception {
        EntraUser loggedInUser = internalUserViewers.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");
        int expectedSize = (int) allUsers.stream()
                .filter(user -> user.getUserProfiles().stream().findFirst().orElseThrow().getUserType() == UserType.INTERNAL)
                .count();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserSearchResultsDto user : users) {
            Assertions.assertThat(user.userType()).isEqualTo(UserType.INTERNAL);
        }
    }

    @Test
    public void testExternalUserViewerCanSeeAllExternalUsers() throws Exception {
        EntraUser loggedInUser = externalUserViewers.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");
        int expectedSize = (int) allUsers.stream()
                .filter(user -> user.getUserProfiles().stream().findFirst().orElseThrow().getUserType() == UserType.EXTERNAL)
                .count();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserSearchResultsDto user : users) {
            Assertions.assertThat(user.userType()).isEqualTo(UserType.EXTERNAL);
        }
    }

    @Test
    public void testGlobalAdminCanSeeOnlyMultiFirmUsersWhenFiltered() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        MvcResult result = this.mockMvc.perform(get("/admin/users?size=100&showMultiFirmUsers=true")
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        ModelAndView modelAndView = result.getModelAndView();
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");
        int expectedSize = multiFirmUsers.size();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserSearchResultsDto user : users) {
            Assertions.assertThat(user.multiFirmUser()).isTrue();
        }
    }

    @Test
    public void testGlobalAdminCanSeeOnlyMultiFirmUsersWhenUsingBackButtonAndFiltered() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        MockHttpSession testSession = new MockHttpSession();
        // Send an initial request for user list with filter applied.
        this.mockMvc.perform(get("/admin/users?size=100&showMultiFirmUsers=true")
                        .with(userOauth2Login(loggedInUser))
                        .session(testSession))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        // Send a second request with only the back button property set
        MvcResult result = this.mockMvc.perform(get("/admin/users?backButton=true")
                        .with(userOauth2Login(loggedInUser))
                        .session(testSession))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andReturn();
        // Check filter is kept when sending back button request.
        ModelAndView modelAndView = result.getModelAndView();
        List<UserSearchResultsDto> users = (List<UserSearchResultsDto>) modelAndView.getModel().get("users");
        int expectedSize = multiFirmUsers.size();
        Assertions.assertThat(users).hasSize(expectedSize);
        for (UserSearchResultsDto user : users) {
            Assertions.assertThat(user.multiFirmUser()).isTrue();
        }
    }

}
