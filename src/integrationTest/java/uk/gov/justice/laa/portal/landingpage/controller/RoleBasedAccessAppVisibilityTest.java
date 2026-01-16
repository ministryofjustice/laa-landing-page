package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;

public class RoleBasedAccessAppVisibilityTest extends RoleBasedAccessIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;

    @Test
    public void testValidUserCanSeeRestrictedApp() throws Exception {
        // Build test app
        App testExternalApp = buildLaaApp("Test External App", generateEntraId(), "TestExternalAppSecurityGroupOid", "TestExternalAppSecurityGroup");

        // Build test role
        AppRole testExternalAppRole = buildLaaAppRole(testExternalApp, "Test External App Role");
        testExternalAppRole.setUserTypeRestriction(new UserType[] {UserType.EXTERNAL});

        // Persist app and role.
        testExternalApp.setAppRoles(Set.of(testExternalAppRole));
        testExternalApp = appRepository.saveAndFlush(testExternalApp);
        testExternalAppRole = testExternalApp.getAppRoles().stream().findFirst().orElseThrow();

        // Add restriction that test role can only be set by external user manager.
        AppRole externalUserManager = appRoleRepository.findByName(AuthzRole.EXTERNAL_USER_MANAGER.getRoleName()).orElseThrow();
        RoleAssignment assignment = new RoleAssignment(externalUserManager, testExternalAppRole);
        roleAssignmentRepository.saveAndFlush(assignment);

        EntraUser loggedInUser = internalWithExternalOnlyUserManagers.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        // Send GET request to edit apps endpoint
        MvcResult result = openEditAppScreen(loggedInUser, editedUser);

        Map<String, Object> model = result.getModelAndView().getModel();
        List<AppDto> returnedApps = (List<AppDto>) model.get("apps");

        final String testAppId = testExternalApp.getId().toString();
        boolean returnedAppContainsTestApp = returnedApps.stream()
                .map(AppDto::getId)
                .anyMatch(testAppId::equals);

        Assertions.assertThat(returnedAppContainsTestApp).isTrue();

        // Teardown
        roleAssignmentRepository.deleteAll();
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);
    }

    @Test
    public void testInvalidUserCannotSeeRestrictedApp() throws Exception {
        // Build test app
        App testExternalApp = buildLaaApp("Test External App", generateEntraId(), "TestExternalAppSecurityGroupOid", "TestExternalAppSecurityGroup");

        // Build test role
        AppRole testExternalAppRole = buildLaaAppRole(testExternalApp, "Test External App Role");
        testExternalAppRole.setUserTypeRestriction(new UserType[] {UserType.EXTERNAL});

        // Persist app and role.
        testExternalApp.setAppRoles(Set.of(testExternalAppRole));
        testExternalApp = appRepository.saveAndFlush(testExternalApp);
        testExternalAppRole = testExternalApp.getAppRoles().stream().findFirst().orElseThrow();

        // Add restriction that test role can only be set by external user manager.
        AppRole externalUserManager = appRoleRepository.findByName(AuthzRole.EXTERNAL_USER_MANAGER.getRoleName()).orElseThrow();
        RoleAssignment assignment = new RoleAssignment(externalUserManager, testExternalAppRole);
        roleAssignmentRepository.saveAndFlush(assignment);

        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = externalUsersNoRoles.getFirst();

        // Send GET request to edit apps endpoint
        MvcResult result = openEditAppScreen(loggedInUser, editedUser);

        Map<String, Object> model = result.getModelAndView().getModel();
        List<AppDto> returnedApps = (List<AppDto>) model.get("apps");

        final String testAppId = testExternalApp.getId().toString();
        boolean returnedAppContainsTestApp = returnedApps.stream()
                .map(AppDto::getId)
                .anyMatch(testAppId::equals);

        Assertions.assertThat(returnedAppContainsTestApp).isFalse();

        // Teardown
        roleAssignmentRepository.deleteAll();
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);
    }

    private MvcResult openEditAppScreen(EntraUser loggedInUser, EntraUser editedUser) throws Exception {
        UserProfile accessedUserProfile = editedUser.getUserProfiles().stream().findFirst().orElseThrow();
        return this.mockMvc.perform(get(String.format("/admin/users/edit/%s/apps", accessedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andReturn();
    }

}
