package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;

public class RoleBaseAccessEditUserRoleTest extends RoleBasedAccessIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;


    @Test
    @Transactional
    public void testGlobalAdminCannotAssignInternalUserManagerRoleToExternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.INTERNAL_USER_MANAGER.getRoleName(), false);
    }

    @Test
    @Transactional
    public void testGlobalAdminCanAssignInternalUserManagerRoleToInternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.INTERNAL_USER_MANAGER.getRoleName(), true);
    }

    @Test
    @Transactional
    public void testGlobalAdminCanAssignExternalUserManagerRoleToInternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.EXTERNAL_USER_MANAGER.getRoleName(), true);
    }

    @Test
    @Transactional
    public void testGlobalAdminCannotAssignExternalUserManagerRoleToExternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.EXTERNAL_USER_MANAGER.getRoleName(), false);
    }

    @Test
    @Transactional
    public void testGlobalAdminCanAssignFirmUserManagerRoleToExternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.FIRM_USER_MANAGER.getRoleName(), true);
    }

    @Test
    @Transactional
    public void testGlobalAdminCannotAssignGlobalAdminRoleToExternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.GLOBAL_ADMIN.getRoleName(), false);
    }

    @Test
    @Transactional
    public void testGlobalAdminCannotAssignExternalUserAdminRoleToExternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.EXTERNAL_USER_ADMIN.getRoleName(), false);
    }

    @Test
    @Transactional
    public void testGlobalAdminCanAssignInternalUserViewerRoleToInternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.INTERNAL_USER_VIEWER.getRoleName(), true);
    }

    @Test
    @Transactional
    public void testGlobalAdminCannotAssignInternalUserViewerRoleToExternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.INTERNAL_USER_VIEWER.getRoleName(), false);
    }

    @Test
    @Transactional
    public void testGlobalAdminCanAssignExternalUserViewerRoleToInternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.EXTERNAL_USER_VIEWER.getRoleName(), true);
    }

    @Test
    @Transactional
    public void testGlobalAdminCannotAssignExternalUserViewerRoleToExternalUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.EXTERNAL_USER_VIEWER.getRoleName(), false);
    }

    @Test
    @Transactional
    public void testInternalUserManagerCanAssignInternalUserManagerRoleToInternalUser() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.INTERNAL_USER_MANAGER.getRoleName(), true);
    }

    @Test
    @Transactional
    public void testInternalUserManagerCanAssignExternalUserManagerRoleToInternalUser() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.EXTERNAL_USER_MANAGER.getRoleName(), true);
    }

    @Test
    @Transactional
    public void testInternalUserManagerCanAssignInternalUserViewerRoleToInternalUser() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.INTERNAL_USER_VIEWER.getRoleName(), true);
    }

    @Test
    @Transactional
    public void testInternalUserManagerCanAssignExternalUserViewerRoleToInternalUser() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();
        assignAuthzRoleToUser(loggedInUser, editedUser, AuthzRole.EXTERNAL_USER_VIEWER.getRoleName(), true);
    }

    @Test
    @Transactional
    public void testExternalUserRolesCannotBeAssignedToInternalUsers() throws Exception {
        // Build test app
        App testExternalApp = buildLaaApp("Test External App", generateEntraId(), "TestExternalAppSecurityGroupOid", "TestExternalAppSecurityGroup");

        // Build test role
        AppRole testExternalAppRole = buildLaaAppRole(testExternalApp, "Test External App Role");
        testExternalAppRole.setUserTypeRestriction(new UserType[] {UserType.EXTERNAL});

        // Persist app and role.
        testExternalApp.setAppRoles(Set.of(testExternalAppRole));
        testExternalApp = appRepository.saveAndFlush(testExternalApp);
        testExternalAppRole = testExternalApp.getAppRoles().stream().findFirst().orElseThrow();


        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();

        // Send requests to update user roles
        Set<AppRole> editedUserRoles = assignRoleToUserAndReturnRoles(loggedInUser, editedUser, testExternalApp, testExternalAppRole);

        // Make sure role was not assigned.
        Assertions.assertThat(editedUserRoles).doesNotContain(testExternalAppRole);

        // Teardown
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);
    }

    @Test
    @Transactional
    public void testInternalUserRolesCanBeAssignedToInternalUsers() throws Exception {
        // Build test app
        App testInternalApp = buildLaaApp("Test Internal App", generateEntraId(), "TestExternalAppSecurityGroupOid", "TestExternalAppSecurityGroup");

        // Build test role
        AppRole testInternalAppRole = buildLaaAppRole(testInternalApp, "Test Internal App Role");
        testInternalAppRole.setUserTypeRestriction(new UserType[] {UserType.INTERNAL});

        // Persist app and role.
        testInternalApp.setAppRoles(Set.of(testInternalAppRole));
        testInternalApp = appRepository.saveAndFlush(testInternalApp);
        testInternalAppRole = testInternalApp.getAppRoles().stream().findFirst().orElseThrow();


        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();

        // Send requests to update user roles
        Set<AppRole> editedUserRoles = assignRoleToUserAndReturnRoles(loggedInUser, editedUser, testInternalApp, testInternalAppRole);

        // Make sure role was assigned.
        Assertions.assertThat(editedUserRoles).contains(testInternalAppRole);

        // Teardown
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);
        System.out.println();
    }

    @Test
    @Transactional
    public void testExternalUserRolesCanBeAssignedToExternalUsers() throws Exception {
        // Build test app
        App testExternalApp = buildLaaApp("Test External App", generateEntraId(), "TestExternalAppSecurityGroupOid", "TestExternalAppSecurityGroup");

        // Build test role
        AppRole testExternalAppRole = buildLaaAppRole(testExternalApp, "Test External App Role");
        testExternalAppRole.setUserTypeRestriction(new UserType[] {UserType.EXTERNAL});

        // Persist app and role.
        testExternalApp.setAppRoles(Set.of(testExternalAppRole));
        testExternalApp = appRepository.saveAndFlush(testExternalApp);
        testExternalAppRole = testExternalApp.getAppRoles().stream().findFirst().orElseThrow();


        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        // Send requests to update user roles
        Set<AppRole> editedUserRoles = assignRoleToUserAndReturnRoles(loggedInUser, editedUser, testExternalApp, testExternalAppRole);

        // Make sure role was not assigned.
        Assertions.assertThat(editedUserRoles).contains(testExternalAppRole);

        // Teardown
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);
    }

    @Test
    @Transactional
    public void testInternalAndExternalUserRolesCanBeAssignedToExternalUsers() throws Exception {
        // Build test app
        App testExternalApp = buildLaaApp("Test External App", generateEntraId(), "TestExternalAppSecurityGroupOid", "TestExternalAppSecurityGroup");

        // Build test role
        AppRole testInternalAndExternalAppRole = buildLaaAppRole(testExternalApp, "Test External App Role");
        testInternalAndExternalAppRole.setUserTypeRestriction(new UserType[] {UserType.INTERNAL, UserType.EXTERNAL});

        // Persist app and role.
        testExternalApp.setAppRoles(Set.of(testInternalAndExternalAppRole));
        testExternalApp = appRepository.saveAndFlush(testExternalApp);
        testInternalAndExternalAppRole = testExternalApp.getAppRoles().stream().findFirst().orElseThrow();


        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        // Send requests to update user roles
        Set<AppRole> editedUserRoles = assignRoleToUserAndReturnRoles(loggedInUser, editedUser, testExternalApp, testInternalAndExternalAppRole);

        // Make sure role was not assigned.
        Assertions.assertThat(editedUserRoles).contains(testInternalAndExternalAppRole);

        // Teardown
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);
    }

    @Test
    @Transactional
    public void testInternalUserRolesCannotBeAssignedToExternalUsers() throws Exception {
        // Build test app
        App testInternalApp = buildLaaApp("Test Internal App", generateEntraId(), "TestExternalAppSecurityGroupOid", "TestExternalAppSecurityGroup");

        // Build test role
        AppRole testInternalAppRole = buildLaaAppRole(testInternalApp, "Test Internal App Role");
        testInternalAppRole.setUserTypeRestriction(new UserType[] {UserType.INTERNAL});

        // Persist app and role.
        testInternalApp.setAppRoles(Set.of(testInternalAppRole));
        testInternalApp = appRepository.saveAndFlush(testInternalApp);
        testInternalAppRole = testInternalApp.getAppRoles().stream().findFirst().orElseThrow();


        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        // Send requests to update user roles
        Set<AppRole> editedUserRoles = assignRoleToUserAndReturnRoles(loggedInUser, editedUser, testInternalApp, testInternalAppRole);

        // Make sure role was assigned.
        Assertions.assertThat(editedUserRoles).doesNotContain(testInternalAppRole);

        // Teardown
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);
    }

    @Test
    @Transactional
    public void testInternalAndExternalUserRolesCanBeAssignedToInternalUsers() throws Exception {
        // Build test app
        App testInternalApp = buildLaaApp("Test Internal App", generateEntraId(), "TestExternalAppSecurityGroupOid", "TestExternalAppSecurityGroup");

        // Build test role
        AppRole testInternalAndExternalAppRole = buildLaaAppRole(testInternalApp, "Test Internal App Role");
        testInternalAndExternalAppRole.setUserTypeRestriction(new UserType[] {UserType.INTERNAL, UserType.EXTERNAL});

        // Persist app and role.
        testInternalApp.setAppRoles(Set.of(testInternalAndExternalAppRole));
        testInternalApp = appRepository.saveAndFlush(testInternalApp);
        testInternalAndExternalAppRole = testInternalApp.getAppRoles().stream().findFirst().orElseThrow();


        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();

        // Send requests to update user roles
        Set<AppRole> editedUserRoles = assignRoleToUserAndReturnRoles(loggedInUser, editedUser, testInternalApp, testInternalAndExternalAppRole);

        // Make sure role was assigned.
        Assertions.assertThat(editedUserRoles).contains(testInternalAndExternalAppRole);

        // Teardown
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);
    }

    @Test
    @Transactional
    public void testRestrictedAppRoleCanBeAssignedByValidUser() throws Exception {
        // Build test app
        App testInternalApp = buildLaaApp("Test External App", generateEntraId(), "TestExternalAppSecurityGroupOid", "TestExternalAppSecurityGroup");

        // Build test role
        AppRole testExternalAppRole = buildLaaAppRole(testInternalApp, "Test External App Role");
        testExternalAppRole.setUserTypeRestriction(new UserType[] {UserType.EXTERNAL});

        // Persist app and role.
        testInternalApp.setAppRoles(Set.of(testExternalAppRole));
        testInternalApp = appRepository.saveAndFlush(testInternalApp);
        testExternalAppRole = testInternalApp.getAppRoles().stream().findFirst().orElseThrow();

        // Add restriction that test role can only be set by external user manager.
        AppRole externalUserManager = appRoleRepository.findByName(AuthzRole.EXTERNAL_USER_MANAGER.getRoleName()).orElseThrow();
        RoleAssignment assignment = new RoleAssignment(externalUserManager, testExternalAppRole);
        roleAssignmentRepository.saveAndFlush(assignment);

        EntraUser loggedInUser = internalWithExternalOnlyUserManagers.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        // Send requests to update user roles
        Set<AppRole> editedUserRoles = assignRoleToUserAndReturnRoles(loggedInUser, editedUser, testInternalApp, testExternalAppRole);

        // Make sure role was assigned.
        Assertions.assertThat(editedUserRoles).contains(testExternalAppRole);

        // Teardown
        roleAssignmentRepository.deleteAll();
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);
    }

    @Test
    @Transactional
    public void testRestrictedAppRoleCannotBeAssignedByInvalidUser() throws Exception {
        // Build test app
        App testInternalApp = buildLaaApp("Test External App", generateEntraId(), "TestExternalAppSecurityGroupOid", "TestExternalAppSecurityGroup");

        // Build test role
        AppRole testExternalAppRole = buildLaaAppRole(testInternalApp, "Test External App Role");
        testExternalAppRole.setUserTypeRestriction(new UserType[] {UserType.EXTERNAL});

        // Persist app and role.
        testInternalApp.setAppRoles(Set.of(testExternalAppRole));
        testInternalApp = appRepository.saveAndFlush(testInternalApp);
        testExternalAppRole = testInternalApp.getAppRoles().stream().findFirst().orElseThrow();

        // Add restriction that test role can only be set by external user manager (internal).
        AppRole externalUserManager = appRoleRepository.findByName(AuthzRole.EXTERNAL_USER_MANAGER.getRoleName()).orElseThrow();
        RoleAssignment assignment = new RoleAssignment(externalUserManager, testExternalAppRole);
        roleAssignmentRepository.saveAndFlush(assignment);

        EntraUser loggedInUser = externalOnlyUserManagers.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        // Send requests to update user roles
        Set<AppRole> editedUserRoles = assignRoleToUserAndReturnRoles(loggedInUser, editedUser, testInternalApp, testExternalAppRole);

        // Make sure role was not assigned.
        Assertions.assertThat(editedUserRoles).doesNotContain(testExternalAppRole);

        // Teardown
        roleAssignmentRepository.deleteAll();
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);

    }




    private Set<AppRole> assignRoleToUserAndReturnRoles(EntraUser loggedInUser, EntraUser editedUser, App app, AppRole role) throws Exception {
        UserProfile editedUserProfile = editedUser.getUserProfiles().stream().findFirst().orElseThrow();
        MockHttpSession session = new MockHttpSession();

        // Select app using post request.
        MvcResult postAppsResult = this.mockMvc.perform(post(String.format("/admin/users/edit/%s/apps", editedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("apps", app.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Fetch redirected URL from response and request it.
        String redirectedUrl = postAppsResult.getResponse().getRedirectedUrl();
        Assertions.assertThat(redirectedUrl).isNotNull();
        this.mockMvc.perform(get(redirectedUrl)
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        // Post Role
        postAppsResult = this.mockMvc.perform(post(String.format("/admin/users/edit/%s/roles", editedUserProfile.getId().toString()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .param("roles", role.getId().toString())
                        .param("selectedAppIndex", "0")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String cyaUrl = postAppsResult.getResponse().getRedirectedUrl();
        Assertions.assertThat(cyaUrl).isNotNull();
        this.mockMvc.perform(get(cyaUrl)
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        //Check your answer
        postAppsResult = this.mockMvc.perform(post(String.format("/admin/users/edit/%s/roles-check-answer", editedUserProfile.getId().toString()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String confirmUrl = postAppsResult.getResponse().getRedirectedUrl();
        Assertions.assertThat(confirmUrl).isNotNull();
        this.mockMvc.perform(get(confirmUrl)
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        return userProfileRepository.findById(editedUserProfile.getId())
                .orElseThrow()
                .getAppRoles();
    }

    @SuppressWarnings("unchecked")
    private void assignAuthzRoleToUser(EntraUser loggedInUser, EntraUser editedUser, String authzRoleName, boolean expectedSuccess) throws Exception {
        UserProfile editedUserProfile = editedUser.getUserProfiles().stream().findFirst().orElseThrow();
        MockHttpSession session = new MockHttpSession();

        // Open App editing screen
        MvcResult selectAppsResult = this.mockMvc.perform(get(String.format("/admin/users/edit/%s/apps", editedUserProfile.getId()))
                .with(userOauth2Login(loggedInUser))
                .session(session))
                .andExpect(status().isOk())
                .andReturn();

        // Fetch AuthZ app from response.
        ModelAndView modelAndView = selectAppsResult.getModelAndView();
        List<AppDto> availableApps = (List<AppDto>) modelAndView.getModel().get("apps");
        AppDto authzApp = availableApps.stream()
                .filter(app -> app.getName().equals("Manage Your Users"))
                .findFirst()
                .orElseThrow();

        // Select AuthZ app using post request.
        MvcResult postAppsResult = this.mockMvc.perform(post(String.format("/admin/users/edit/%s/apps", editedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("apps", authzApp.getId()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Fetch redirected URL from response and request it.
        String redirectedUrl = postAppsResult.getResponse().getRedirectedUrl();
        Assertions.assertThat(redirectedUrl).isNotNull();
        MvcResult getRolesResult = this.mockMvc.perform(get(redirectedUrl)
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        // Fetch returned roles.
        List<AppRoleViewModel> roles = (List<AppRoleViewModel>) getRolesResult.getModelAndView().getModel().get("roles");

        // Get Authz Role from list.
        Optional<AppRoleViewModel> optionalAuthzRole = roles.stream()
                .filter(role -> role.getName().equals(authzRoleName))
                .findFirst();

        if (optionalAuthzRole.isEmpty()) {
            if (!expectedSuccess) {
                return;
            } else {
                Assertions.fail(String.format("Authz role \"%s\" was not available for selection when it should have been", authzRoleName));
            }
        }

        AppRoleViewModel authzRole = optionalAuthzRole.get();

        // Post request to set Internal User Manager role
        MvcResult postRolesResult = this.mockMvc.perform(post(redirectedUrl)
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session)
                        .param("roles", authzRole.getId())
                        .param("selectedAppIndex", "0"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Check we're redirected to cya screen
        String cyaUrl = postAppsResult.getResponse().getRedirectedUrl();
        Assertions.assertThat(cyaUrl).isNotNull();
        this.mockMvc.perform(get(cyaUrl)
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        //Check your answer
        postAppsResult = this.mockMvc.perform(post(String.format("/admin/users/edit/%s/roles-check-answer", editedUserProfile.getId().toString()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String confirmUrl = postAppsResult.getResponse().getRedirectedUrl();
        Assertions.assertThat(confirmUrl).isNotNull();
        Assertions.assertThat(confirmUrl).contains("confirm");
        this.mockMvc.perform(get(confirmUrl)
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        Set<String> editedUserRoles = userProfileRepository.findById(editedUserProfile.getId())
                .orElseThrow()
                .getAppRoles().stream()
                .map(role -> role.getId().toString())
                .collect(Collectors.toSet());

        if (expectedSuccess) {
            Assertions.assertThat(editedUserRoles).contains(authzRole.getId());
        } else {
            Assertions.assertThat(editedUserRoles).doesNotContain(authzRole.getId());
        }
    }
}
