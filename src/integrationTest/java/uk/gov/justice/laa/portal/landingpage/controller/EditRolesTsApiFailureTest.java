package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.exception.BadRequestException;
import uk.gov.justice.laa.portal.landingpage.service.TechServicesClient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class EditRolesTsApiFailureTest extends RoleBasedAccessIntegrationTest {

    @MockitoBean
    private TechServicesClient techServicesClient;

    // Used to ensure that the transaction rollback is complete before the test method ends, so that the changes are visible to the test method.
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private TransactionTemplate transactionTemplate;


    @Test
    public void testRoleAssignmentRedirectsWhenTechServicesApiFailsDueToUnexpectedError() throws Exception {
        // Create a test app.
        App testApp = buildLaaApp("Test App", UUID.randomUUID().toString(), UUID.randomUUID().toString());
        appRepository.saveAndFlush(testApp);

        // Create test app role.
        AppRole testAppRole = buildLaaExternalAppRole(testApp, "Test App Role");
        testApp.setAppRoles(Set.of(testAppRole));
        appRoleRepository.saveAndFlush(testAppRole);

        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        UserProfile accessedUserProfile = accessedUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .findFirst()
                .orElseThrow();

        doThrow(new RuntimeException("Tech Services API failure"))
                .when(techServicesClient).updateRoleAssignment(any());

        MvcResult result = editRolesCheckAnswersPost(loggedInUser, accessedUserProfile, testAppRole);

        transactionTemplate.execute(status -> {
            UserProfile userProfile = userProfileRepository.findById(accessedUserProfile.getId()).orElseThrow();
            assertThat(userProfile.getAppRoles()).hasSize(0);
            // Teardown
            appRoleRepository.delete(testAppRole);
            appRepository.delete(testApp);
            return null;
        });

        assertThat(result.getResponse()).isNotNull();
        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).contains("/admin/users/edit/" + accessedUserProfile.getId() + "/roles-check-answer");
        assertThat(response.getRedirectedUrl()).contains("Please try again later.");
    }

    @Test
    public void testRoleAssignmentRedirectsWhenTechServicesApiFailsDueToBadRequest() throws Exception {
        // Create a test app.
        App testApp = buildLaaApp("Test App", UUID.randomUUID().toString(), UUID.randomUUID().toString());
        appRepository.saveAndFlush(testApp);

        // Create test app role.
        AppRole testAppRole = buildLaaExternalAppRole(testApp, "Test App Role");
        testApp.setAppRoles(Set.of(testAppRole));
        appRoleRepository.saveAndFlush(testAppRole);

        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        UserProfile accessedUserProfile = accessedUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .findFirst()
                .orElseThrow();

        doThrow(new BadRequestException("Bad Request"))
                .when(techServicesClient).updateRoleAssignment(any());

        MvcResult result = editRolesCheckAnswersPost(loggedInUser, accessedUserProfile, testAppRole);

        transactionTemplate.execute(status -> {
            UserProfile userProfile = userProfileRepository.findById(accessedUserProfile.getId()).orElseThrow();
            assertThat(userProfile.getAppRoles()).hasSize(0);
            // Teardown
            appRoleRepository.delete(testAppRole);
            appRepository.delete(testApp);
            return null;
        });

        assertThat(result.getResponse()).isNotNull();
        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).contains("/admin/users/edit/" + accessedUserProfile.getId() + "/roles-check-answer");
        assertThat(response.getRedirectedUrl()).contains("Please contact support.");
    }

    private MvcResult editRolesCheckAnswersPost(EntraUser loggedInUser, UserProfile accessedUserProfile, AppRole role) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("editUserAllSelectedRoles", Map.of(0, List.of(role.getId().toString())));
        return mockMvc.perform(post("/admin/users/edit/" + accessedUserProfile.getId() + "/roles-check-answer")
                        .with(userOauth2Login(loggedInUser))
                        .with(csrf())
                        .session(session))
                        .andReturn();
    }
}
