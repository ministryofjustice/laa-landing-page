package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;
//import uk.gov.justice.laa.portal.landingpage.config.TestSecurityConfig;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

public class AppSelectionTest extends BaseIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraUserRepository entraUserRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private UserProfileRepository userProfileRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRepository appRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRoleRepository appRoleRepository;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;

    private AppRole testAppRole;
    private int distinctIndex = 0;

    @BeforeEach
    public void beforeEach() {
        userProfileRepository.deleteAll();
        entraUserRepository.deleteAll();
        deleteNonAuthzAppRoles(appRoleRepository);
        deleteNonAuthzApps(appRepository);
        buildTestAppRole();
    }

    @Test
    @Transactional
    public void testGetEditUserAppsForUserHasCorrectProperties() throws Exception {
        EntraUser loggedInUser = buildTestUser();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);
        
        // Get the UserProfile from the saved EntraUser (it should be saved due to cascade)
        UserProfile userProfile = loggedInUser.getUserProfiles().iterator().next();
        
        this.mockMvc.perform(get(String.format("/admin/users/edit/%s/apps", userProfile.getId()))
                .with(defaultOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-user-apps"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("apps"))
                .andExpect(model().attribute("user", hasProperty("id", is(userProfile.getId()))));
    }

    @Test
    public void testGetEditUserAppsForUserThrowsExceptionWhenNoUserExists() throws Exception {
        EntraUser loggedInUser = buildTestUser();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);
        MvcResult result = this.mockMvc.perform(get(String.format("/admin/users/edit/%s/apps", UUID.randomUUID()))
                        .with(defaultOauth2Login(loggedInUser)))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals("/error", result.getResponse().getRedirectedUrl());
    }

    @Test
    public void testSelectingAppsReturnsCorrectRedirectAndApps() throws Exception {
        EntraUser loggedInUser = buildTestUser();
        loggedInUser = entraUserRepository.saveAndFlush(loggedInUser);
        EntraUser accessedUser = buildTestUser();
        accessedUser = entraUserRepository.saveAndFlush(accessedUser);
        UserProfile accessedUserProfile = accessedUser.getUserProfiles().stream().findFirst().orElseThrow();
        String path = String.format("/admin/users/edit/%s/apps", accessedUserProfile.getId());
        String[] selectedApps = { UUID.randomUUID().toString() };
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post(path)
                .with(defaultOauth2Login(loggedInUser))
                .with(csrf())
                .param("apps", selectedApps))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals(result.getResponse().getRedirectedUrl(), String.format("/admin/users/edit/%s/roles", accessedUserProfile.getId()));
        HttpSession session = result.getRequest().getSession();
        assertNotNull(session.getAttribute("selectedApps"));
        @SuppressWarnings("unchecked")
        List<String> returnedSelectApps = (List<String>) session.getAttribute("selectedApps");
        assertEquals(1, returnedSelectApps.size());
        assertEquals(selectedApps[0], returnedSelectApps.getFirst());
    }

    private EntraUser buildTestUser() {
        EntraUser entraUser = buildEntraUser(generateEntraId(), "test" + distinctIndex++ + "@test.com", "Test", "User");
        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.INTERNAL, true);
        userProfile.getAppRoles().add(testAppRole);
        // Use a mutable set to allow Hibernate to manage the relationship properly
        entraUser.getUserProfiles().add(userProfile);
        return entraUser;
    }

    private void buildTestAppRole() {
        App app = buildLaaApp("Test App " + distinctIndex++, UUID.randomUUID().toString(),
                "Security Group Id" + distinctIndex++, "Security Group Name" + distinctIndex++);
        app = appRepository.saveAndFlush(app);
        AppRole appRole = buildLaaAppRole(app, "Test App Role" + distinctIndex++);
        testAppRole = appRoleRepository.saveAndFlush(appRole);
    }
}
