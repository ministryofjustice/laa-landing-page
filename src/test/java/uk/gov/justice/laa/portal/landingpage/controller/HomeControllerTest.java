package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.AppService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private LoginService loginService;

    @Mock
    private UserService userService;

    @Mock
    private AppService appService;

    @Mock
    private Authentication authentication;

    private Model model;

    private HomeController homeController;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
        homeController = new HomeController(loginService, userService, appService, new MapperConfig().modelMapper());
        ReflectionTestUtils.setField(homeController, "puiUrl", "https://test-pui-url.com");
    }

    @Test
    void testPuiInterstitial_ReturnsCorrectViewAndModelAttributes() {
        // Act
        String viewName = homeController.puiInterstitial(model);

        // Assert: Verify view name
        assertEquals("pui-interstitial", viewName);

        // Assert: Verify model attributes
        assertThat(model.getAttribute("puiUrl")).isEqualTo("https://test-pui-url.com");
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE))
                .isEqualTo("Sign in again to access CCMS PUI");
    }

    @Test
    void testMyAccountDetails_ReturnsCorrectViewAndModelAttributes() {
        // Arrange: Set up test data
        Office office = Office.builder()
                .code("OF1")
                .address(Office.Address.builder().addressLine1("AL1").build())
                .build();

        Firm firm = Firm.builder()
                .name("Firm one")
                .code("F1")
                .offices(Set.of(office))
                .build();

        EntraUser entraUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("first")
                .lastName("last")
                .email("test@email.com")
                .build();

        UserProfile userProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .firm(firm)
                .offices(Set.of())
                .userType(UserType.EXTERNAL)
                .entraUser(entraUser)
                .build();

        App app = App.builder()
                .id(UUID.randomUUID())
                .name("App one")
                .build();

        AppRole appRole = AppRole.builder()
                .id(UUID.randomUUID())
                .name("Role one")
                .authzRole(false)
                .app(app)
                .build();

        app.setAppRoles(Set.of(appRole));
        userProfile.setAppRoles(Set.of(appRole));

        AppDto appDto = AppDto.builder()
                .id(app.getId().toString())
                .name("App one")
                .build();

        AppRoleDto appRoleDto = AppRoleDto.builder()
                .id(appRole.getId().toString())
                .name("Role one")
                .app(appDto)
                .build();

        // Arrange: Mock service responses
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);
        when(userService.getUserAppsByUserId(any())).thenReturn(Set.of(appDto));
        when(userService.getUserAppRolesByUserId(any())).thenReturn(List.of(appRoleDto));
        when(appService.getById(any())).thenReturn(Optional.of(app));

        // Act
        String viewName = homeController.myAccountDetails(model, authentication);

        // Assert: Verify view name
        assertEquals("home/my-account-details", viewName);

        // Assert: Verify model attributes
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("userOffices")).isNotNull();
        assertThat(model.getAttribute("firm")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, List<String>> appAssignments = (Map<String, List<String>>) model.getAttribute("appAssignments");
        assertThat(appAssignments).hasSize(1)
                .containsKey(appDto.getName())
                .containsValues(List.of("Access"));

        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE))
                .isEqualTo("My Account - first last");
    }

    @Test
    void testMyAccountDetails_AppWithMultipleRoles_AssignsActualRoles() {
        // Arrange: Setup test data
        Office office = Office.builder()
                .code("OF1")
                .address(Office.Address.builder().addressLine1("AL1").build())
                .build();

        Firm firm = Firm.builder()
                .name("Firm One")
                .code("F1")
                .offices(Set.of(office))
                .build();

        EntraUser entraUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("first")
                .lastName("last")
                .email("test@email.com")
                .build();

        UserProfile userProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .firm(firm)
                .offices(Set.of())
                .userType(UserType.EXTERNAL)
                .entraUser(entraUser)
                .build();

        UUID appId = UUID.randomUUID();
        App app = App.builder()
                .id(appId)
                .name("App One")
                .build();

        AppRole appRole1 = AppRole.builder()
                .id(UUID.randomUUID())
                .name("Role One")
                .authzRole(false)
                .app(app)
                .build();

        AppRole appRole2 = AppRole.builder()
                .id(UUID.randomUUID())
                .name("Role Two")
                .authzRole(false)
                .app(app)
                .build();

        app.setAppRoles(Set.of(appRole1, appRole2));
        userProfile.setAppRoles(Set.of(appRole1));

        AppDto appDto = AppDto.builder()
                .id(app.getId().toString())
                .name("App One")
                .build();

        AppRoleDto appRoleDto = AppRoleDto.builder()
                .id(appRole1.getId().toString())
                .name("Role One")
                .app(appDto)
                .build();

        // Mock service responses
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);
        when(userService.getUserAppsByUserId(any())).thenReturn(Set.of(appDto));
        when(userService.getUserAppRolesByUserId(any())).thenReturn(List.of(appRoleDto));
        when(appService.getById(appId)).thenReturn(Optional.of(app));

        // Act
        String viewName = homeController.myAccountDetails(model, authentication);

        // Assert: View name
        assertEquals("home/my-account-details", viewName);

        // Assert: Model attributes
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("userOffices")).isNotNull();
        assertThat(model.getAttribute("firm")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, List<String>> appAssignments = (Map<String, List<String>>) model.getAttribute("appAssignments");
        assertThat(appAssignments).hasSize(1)
                .containsKey(appDto.getName())
                .containsValues(List.of("Role One"));

        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE))
                .isEqualTo("My Account - first last");
    }

}
