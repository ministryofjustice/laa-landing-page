package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.laa.portal.landingpage.dto.CcmsUserDetailsResponse;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.service.CcmsUserDetailsService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class CcmsEbsValidationTest extends RoleBasedAccessIntegrationTest {

    private App legacySyncApp;
    private AppRole legacySyncRole;


    @BeforeAll
    public void setup() {
        createAndPersistLegacySyncRole();
    }

    @Test
    @Transactional
    public void testEditRoleCheckAnswersGetHasNoErrorsWhenInternalUserIsMigrated() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();

        // Set migrated status to true
        editedUser.setCcmsEbsUser(true);
        entraUserRepository.saveAndFlush(editedUser);

        // Request check answers page.
        MvcResult result = editRolesCheckAnswersGet(loggedInUser, editedUser, legacySyncRole);

        // Assert request was success with no error message.
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getModelAndView()).isNotNull();
        ModelAndView modelAndView = result.getModelAndView();
        assertThat(modelAndView.getModel().get("errorMessage")).isNull();
    }

    @Test
    @Transactional
    public void testEditRoleCheckAnswersGetHasErrorWhenInternalUserIsNotMigrated() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();

        // Request check answers page.
        MvcResult result = editRolesCheckAnswersGet(loggedInUser, editedUser, legacySyncRole);

        // Assert request has error message.
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getModelAndView()).isNotNull();
        ModelAndView modelAndView = result.getModelAndView();
        assertThat(modelAndView.getModel().get("errorMessage")).isNotNull();
    }

    @Test
    @Transactional
    public void testEditRoleCheckAnswersPostHasNoErrorsWhenInternalUserIsMigrated() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();

        // Set migrated status to true
        editedUser.setCcmsEbsUser(true);
        entraUserRepository.saveAndFlush(editedUser);

        // Request check answers post.
        MvcResult result = editRolesCheckAnswersPost(loggedInUser, editedUser, legacySyncRole);

        // Assert request was success
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(result.getResponse().getRedirectedUrl()).contains("/confirmation");

        // Assert user now has role.
        assertThat(userHasAppRole(editedUser, legacySyncRole)).isTrue();
    }

    @Test
    @Transactional
    public void testEditRoleCheckAnswersPostThrowsErrorWhenInternalUserIsNotMigrated() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();

        // Request check answers post.
        MvcResult result = editRolesCheckAnswersPost(loggedInUser, editedUser, legacySyncRole);

        // Assert request was not success
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(result.getResponse().getRedirectedUrl()).contains("/error");

        // Assert user does not have role.
        assertThat(userHasAppRole(editedUser, legacySyncRole)).isFalse();
    }


    @Test
    @Transactional
    public void testGrantAccessCheckAnswersGetHasNoErrorsWhenInternalUserIsMigrated() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();

        // Set migrated status to true
        editedUser.setCcmsEbsUser(true);
        entraUserRepository.saveAndFlush(editedUser);

        // Set up Ccms mock response to be successful
        CcmsUserDetailsResponse ccmsResponse = new CcmsUserDetailsResponse();
        // Request check answers page.
        MvcResult result = grantAccessCheckAnswersGet(loggedInUser, editedUser, legacySyncRole);

        // Assert request was success with no error message.
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getModelAndView()).isNotNull();
        ModelAndView modelAndView = result.getModelAndView();
        assertThat(modelAndView.getModel().get("errorMessage")).isNull();
    }

    @Test
    @Transactional
    public void testGrantAccessCheckAnswersGetHasErrorWhenInternalUserIsNotMigrated() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();

        // Request check answers page.
        MvcResult result = grantAccessCheckAnswersGet(loggedInUser, editedUser, legacySyncRole);

        // Assert request has error message.
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getModelAndView()).isNotNull();
        ModelAndView modelAndView = result.getModelAndView();
        assertThat(modelAndView.getModel().get("errorMessage")).isNotNull();
    }

    @Test
    @Transactional
    public void testGrantAccessCheckAnswersPostHasNoErrorsWhenInternalUserIsMigrated() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();

        // Set migrated status to true
        editedUser.setCcmsEbsUser(true);
        entraUserRepository.saveAndFlush(editedUser);

        // Request check answers post.
        MvcResult result = grantAccessCheckAnswersPost(loggedInUser, editedUser, legacySyncRole);

        // Assert request was success
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(result.getResponse().getRedirectedUrl()).contains("/confirmation");

        // Assert user now has role.
        assertThat(userHasAppRole(editedUser, legacySyncRole)).isTrue();
    }

    @Test
    @Transactional
    public void testGrantAccessCheckAnswersPostThrowsErrorWhenInternalUserIsNotMigrated() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser editedUser = internalUsersNoRoles.getFirst();

        // Request check answers post.
        MvcResult result = grantAccessCheckAnswersPost(loggedInUser, editedUser, legacySyncRole);

        // Assert request was not success
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(result.getResponse().getRedirectedUrl()).contains("/confirmation");

        // Assert user does not have role.
        assertThat(userHasAppRole(editedUser, legacySyncRole)).isFalse();
    }



    public MvcResult editRolesCheckAnswersGet(EntraUser loggedInUser, EntraUser editedUser, AppRole role) throws Exception {
        UserProfile accessedUserProfile = editedUser.getUserProfiles().stream().findFirst().orElseThrow();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("editUserAllSelectedRoles", Map.of(0, List.of(role.getId().toString())));
        return this.mockMvc.perform(get(String.format("/admin/users/edit/%s/roles-check-answer", accessedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                        .andReturn();
    }

    public MvcResult grantAccessCheckAnswersGet(EntraUser loggedInUser, EntraUser editedUser, AppRole role) throws Exception {
        UserProfile accessedUserProfile = editedUser.getUserProfiles().stream().findFirst().orElseThrow();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("allSelectedRoles", Set.of(role.getId().toString()));
        session.setAttribute("selectedOffices", List.of("NO_OFFICES"));
        return this.mockMvc.perform(get(String.format("/admin/users/grant-access/%s/check-answers", accessedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .session(session))
                .andReturn();
    }

    public MvcResult editRolesCheckAnswersPost(EntraUser loggedInUser, EntraUser editedUser, AppRole role) throws Exception {
        UserProfile accessedUserProfile = editedUser.getUserProfiles().stream().findFirst().orElseThrow();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("editUserAllSelectedRoles", Map.of(0, List.of(role.getId().toString())));
        return this.mockMvc.perform(post(String.format("/admin/users/edit/%s/roles-check-answer", accessedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                        .andReturn();
    }

    public MvcResult grantAccessCheckAnswersPost(EntraUser loggedInUser, EntraUser editedUser, AppRole role) throws Exception {
        UserProfile accessedUserProfile = editedUser.getUserProfiles().stream().findFirst().orElseThrow();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("allSelectedRoles", Set.of(role.getId().toString()));
        session.setAttribute("selectedOffices", List.of("NO_OFFICES"));
        return this.mockMvc.perform(post(String.format("/admin/users/grant-access/%s/check-answers", accessedUserProfile.getId()))
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                        .andReturn();
    }

    public void createAndPersistLegacySyncRole() {
        App app = buildLaaApp("Legacy Sync App", UUID.randomUUID().toString(), UUID.randomUUID().toString());
        app = appRepository.saveAndFlush(app);
        AppRole appRole = buildLaaAppRole(app, "LegacySyncAppRole");
        appRole.setLegacySync(true);
        appRole = appRoleRepository.saveAndFlush(appRole);
        legacySyncApp = app;
        legacySyncRole = appRole;
    }

    private boolean userHasAppRole(EntraUser user, AppRole role) {
        user = entraUserRepository.findById(user.getId()).orElseThrow();
        return user.getUserProfiles().stream()
                .flatMap(profile -> profile.getAppRoles().stream())
                .anyMatch(appRole -> appRole.getId().equals(role.getId()));
    }

    @AfterEach
    public void tearDown() {
        AppRole role = appRoleRepository.findById(legacySyncRole.getId()).orElseThrow();
        Set<UserProfile> profiles = role.getUserProfiles();
        if (profiles != null) {
            for (UserProfile profile : profiles) {
                profile.getAppRoles().remove(legacySyncRole);
                userProfileRepository.saveAndFlush(profile);
            }
        }
        List<EntraUser> migratedUsers = entraUserRepository.findEntraUserByCcmsEbsUserIsTrue();
        for (EntraUser user : migratedUsers) {
            user.setCcmsEbsUser(false);
            entraUserRepository.saveAndFlush(user);
        }
    }

    @AfterAll
    public void removeLegacyData() {
        appRoleRepository.delete(legacySyncRole);
        appRepository.delete(legacySyncApp);
    }

}
