package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.config.TestSecurityConfig;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.*;

@Import(TestSecurityConfig.class)
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

    @BeforeEach
    public void beforeEach() {
        userProfileRepository.deleteAll();
        entraUserRepository.deleteAll();
        roleAssignmentRepository.deleteAll();
        appRoleRepository.deleteAll();
        appRepository.deleteAll();
    }

    @Test
    @Transactional
    public void testGetEditUserAppsForUserHasCorrectProperties() throws Exception {
        EntraUser entraUser = buildTestUser();
        entraUser = entraUserRepository.saveAndFlush(entraUser);
        
        // Get the UserProfile from the saved EntraUser (it should be saved due to cascade)
        UserProfile userProfile = entraUser.getUserProfiles().iterator().next();
        
        this.mockMvc.perform(get(String.format("/admin/users/edit/%s/apps", userProfile.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-user-apps"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("apps"))
                .andExpect(model().attribute("user", hasProperty("id", is(userProfile.getId()))));
    }

    @Test
    public void testGetEditUserAppsForUserThrowsExceptionWhenNoUserExists() throws Exception {
        UUID userId = UUID.randomUUID();
        MvcResult result = this.mockMvc.perform(get(String.format("/admin/users/edit/%s/apps", userId)))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals("/error", result.getResponse().getRedirectedUrl());
    }

    @Test
    public void testSelectingAppsReturnsCorrectRedirectAndApps() throws Exception {
        UUID userId = UUID.randomUUID();
        String path = String.format("/admin/users/edit/%s/apps", userId);
        String[] selectedApps = { userId.toString() };
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post(path)
                .param("apps", selectedApps))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals(result.getResponse().getRedirectedUrl(), String.format("/admin/users/edit/%s/roles", userId));
        HttpSession session = result.getRequest().getSession();
        assertNotNull(session.getAttribute("selectedApps"));
        @SuppressWarnings("unchecked")
        List<String> returnedSelectApps = (List<String>) session.getAttribute("selectedApps");
        assertEquals(1, returnedSelectApps.size());
        assertEquals(selectedApps[0], returnedSelectApps.getFirst());
    }

    private EntraUser buildTestUser() {
        App app = buildLaaApp("Test App", "Entra App Id",
                "Security Group Id", "Security Group Name");
        app = appRepository.saveAndFlush(app);
        AppRole appRole = buildLaaAppRole(app, "Test App Role");
        appRole = appRoleRepository.saveAndFlush(appRole);
        EntraUser entraUser = buildEntraUser(generateEntraId(), "test@test.com", "Test", "User");
        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.INTERNAL, true);
        userProfile.setAppRoles(Set.of(appRole));
        // Use a mutable set to allow Hibernate to manage the relationship properly
        entraUser.getUserProfiles().add(userProfile);
        return entraUser;
    }
}
