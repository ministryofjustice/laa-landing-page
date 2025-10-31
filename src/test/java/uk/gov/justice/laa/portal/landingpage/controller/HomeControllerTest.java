package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private LoginService loginService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    private Model model;

    private HomeController homeController;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
        homeController = new HomeController(loginService, userService, new MapperConfig().modelMapper());
    }

    @Test
    void testMyAccountDetails() {
        // Arrange
        Office office = Office.builder().code("OF1").address(Office.Address.builder().addressLine1("AL1").build()).build();
        Firm firm = Firm.builder().name("Firm one").code("F1").offices(Set.of(office)).build();
        EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).firstName("first").lastName("last").email("test@email.com").build();
        UserProfile userProfile = UserProfile.builder().id(UUID.randomUUID()).firm(firm).offices(Set.of()).userType(UserType.EXTERNAL).entraUser(entraUser).build();
        App app = App.builder().id(UUID.randomUUID()).name("App one").build();
        AppRole appRole = AppRole.builder().id(UUID.randomUUID()).name("Role one").authzRole(false).app(app).build();
        app.setAppRoles(Set.of(appRole));
        AppDto appDto = AppDto.builder().name("App one").id(app.getId().toString()).build();
        userProfile.setAppRoles(Set.of(appRole));

        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);
        when(userService.getUserAppsByUserId(any())).thenReturn(Set.of(appDto));

        // Act
        String viewName = homeController.myAccountDetails(model, authentication);

        // Assert
        assertEquals("home/my-account-details", viewName);
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("userOffices")).isNotNull();
        assertThat(model.getAttribute("firm")).isNotNull();
        assertThat(model.getAttribute("appAssignments")).isNotNull();
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("My Account - first last");
    }
}
