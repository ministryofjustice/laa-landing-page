package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.justice.laa.portal.landingpage.config.TestSecurityConfig;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRegistration;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRegistrationRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@Import(TestSecurityConfig.class)
public class AppSelectionTest extends BaseIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraUserRepository entraUserRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRegistrationRepository appRegistrationRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private UserProfileRepository userProfileRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRepository appRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRoleRepository appRoleRepository;

    @BeforeEach
    public void beforeEach() {
        userProfileRepository.deleteAll();
        entraUserRepository.deleteAll();
        appRoleRepository.deleteAll();
        appRepository.deleteAll();
        appRegistrationRepository.deleteAll();
    }

    @Test
    public void testGetEditUserAppsForUserHasCorrectProperties() throws Exception {
        EntraUser entraUser = buildTestUser();
        entraUserRepository.saveAndFlush(entraUser);
        this.mockMvc.perform(get(String.format("/admin/users/edit/%s/apps", entraUser.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-user-apps"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("userAssignedApps"))
                .andExpect(model().attributeExists("availableApps"))
                .andExpect(model().attribute("user", hasProperty("id", is(entraUser.getId().toString()))))
                .andExpect(model().attribute("userAssignedApps", hasSize(1)))
                .andExpect(model().attribute("availableApps", hasSize(1)));
    }

    @Test
    public void testGetEditUserAppsForUserThrowsExceptionWhenNoUserExists() throws Exception {
        UUID userId = UUID.randomUUID();
        ServletException servletException = assertThrows(ServletException.class, () -> this.mockMvc.perform(get(String.format("/admin/users/edit/%s/apps", userId))));
        assertInstanceOf(NoSuchElementException.class, servletException.getCause());
    }

    @Test
    public void testSelectingAppsReturnsCorrectRedirectAndApps() throws Exception {
        UUID userId = UUID.randomUUID();
        String path = String.format("/admin/users/edit/%s/apps", userId);
        String[] selectedApps = { userId.toString() };
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post(path)
                .param("selectedApps", selectedApps))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertNotNull(result.getResponse().getRedirectedUrl());
        assertEquals(result.getResponse().getRedirectedUrl(), String.format("/users/edit/%s/roles", userId));
        HttpSession session = result.getRequest().getSession();
        assertNotNull(session.getAttribute("selectedApps"));
        List<String> returnedSelectApps = (List<String>) session.getAttribute("selectedApps");
        assertEquals(1, returnedSelectApps.size());
        assertEquals(selectedApps[0], returnedSelectApps.getFirst());
    }

    private EntraUser buildTestUser() {
        AppRegistration appRegistration = buildEntraAppRegistration("Test App Registration");
        appRegistration = appRegistrationRepository.saveAndFlush(appRegistration);
        App app = buildLaaApp(appRegistration, "Test App");
        app = appRepository.saveAndFlush(app);
        AppRole appRole = buildLaaAppRole(app, "Test App Role");
        appRole = appRoleRepository.saveAndFlush(appRole);
        EntraUser entraUser = buildEntraUser("test@test.com", "Test", "User");
        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.INTERNAL);
        userProfile.setAppRoles(Set.of(appRole));
        entraUser.setUserProfiles(Set.of(userProfile));
        return entraUser;
    }
}
