package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionData;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private LoginService loginService;
    @Mock
    private FirmService firmService;
    @Mock
    private UserService userService;

    @Mock
    private HttpSession session;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2AuthorizedClient authClient;

    @InjectMocks
    private LoginController controller;

    @Test
    void givenEmptyEmail_whenLoginGet_thenReturnsIndexView() {

        // Arrange
        Model model = new ConcurrentModel();

        // Act
        String viewIndex = controller.login(null, model);

        // Assert
        assertThat(viewIndex).isEqualTo("index");
        assertThat(model.getAttribute("successMessage")).isNull();
    }

    @Test
    void givenLogout_whenLoginGet_thenDisplayLogoutOkView() {

        // Arrange
        Model model = new ConcurrentModel();

        // Act
        String viewIndex = controller.login("logout", model);

        // Assert
        assertThat(viewIndex).isEqualTo("index");
        assertThat(model.getAttribute("successMessage")).isEqualTo("You have been securely logged out");
    }

    @Test
    void givenBlankEmail_whenHandleLoginPost_thenRedirectsWithErrorFlash() {

        // Arrange
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        // Act
        RedirectView result = controller.handleLogin("   ", attrs);

        // Assert
        assertThat(result.getUrl()).isEqualTo("/");
        assertThat(attrs.getFlashAttributes().get("errorMessage")).isEqualTo("An incorrect Username or Password was specified");
    }

    @Test
    void givenNullEmail_whenHandleLoginPost_thenRedirectsWithErrorFlash() {

        // Arrange
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        // Act
        RedirectView result = controller.handleLogin(null, attrs);

        // Assert
        assertThat(result.getUrl()).isEqualTo("/");
        assertThat(attrs.getFlashAttributes().get("errorMessage")).isEqualTo("An incorrect Username or Password was specified");
    }

    @Test
    void givenValidEmail_whenHandleLoginPost_thenRedirectsToAzure() {

        // Arrange
        String email = "foo@bar.com";
        String expectedUrl = "https://login.test/?hint=foo%40bar.com";
        when(loginService.buildAzureLoginUrl(email)).thenReturn(expectedUrl);
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        // Act
        RedirectView result = controller.handleLogin(email, attrs);

        // Assert
        assertThat(result.getUrl()).isEqualTo(expectedUrl);
        verify(loginService).buildAzureLoginUrl(email);
        assertThat(attrs.getFlashAttributes()).isEmpty();
    }

    @Test
    void givenLoginServiceThrowsException_whenHandleLoginPost_thenRedirectsWithError() {

        // Arrange
        String email = "exception@test.com";
        when(loginService.buildAzureLoginUrl(email)).thenThrow(new RuntimeException("Azure URL build failed"));
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        // Act
        RedirectView result = controller.handleLogin(email, attrs);

        // Assert
        assertThat(result.getUrl()).isEqualTo("/");
    }

    @Test
    void whenMigrateEndpoint_thenReturnsMigrateView() {

        // Arrange & Act
        String view = controller.migrate();

        // Assert
        assertThat(view).isEqualTo("migrate");
    }

    @Test
    void givenAuthenticatedUser_whenHomeGet_thenPopulatesModelAndReturnsHomeView() {

        // Arrange
        Model model = new ConcurrentModel();
        UserSessionData mockSessionData = UserSessionData.builder()
                .name("Test User")
                .build();
        when(loginService.processUserSession(any(Authentication.class), any(OAuth2AuthorizedClient.class), any(HttpSession.class)))
                .thenReturn(mockSessionData);

        // Act
        String viewName = controller.home(model, authentication, session, authClient);

        // Assert
        assertThat(viewName).isEqualTo("home");
        assertThat(model.getAttribute("name")).isEqualTo("Test User");
        verify(loginService).processUserSession(authentication, authClient, session);
    }

    @Test
    void givenNullSessionData_whenHomeGet_thenReturnsHomeViewWithoutData() {

        // Arrange
        Model model = new ConcurrentModel();
        when(loginService.processUserSession(any(Authentication.class), any(OAuth2AuthorizedClient.class), any(HttpSession.class)))
                .thenReturn(null);

        // Act
        String viewName = controller.home(model, authentication, session, authClient);

        // Assert
        assertThat(viewName).isEqualTo("home");
        assertThat(model.asMap()).doesNotContainKey("name");
        verify(loginService).processUserSession(authentication, authClient, session);
    }

    @Test
    void givenLoginServiceThrowsException_whenHomeGet_thenReturnsHomeView() {

        // Arrange
        Model model = new ConcurrentModel();
        when(loginService.processUserSession(any(Authentication.class), any(OAuth2AuthorizedClient.class), any(HttpSession.class)))
                .thenThrow(new RuntimeException("Error processing session"));

        // Act
        String viewName = controller.home(model, authentication, session, authClient);

        // Assert
        assertThat(viewName).isEqualTo("home");
        assertThat(model.asMap()).doesNotContainKey("name");
    }

    @Test
    void whenHandleException_thenRedirectToErrorPage() {

        // Arrange & Act
        RedirectView result = controller.handleException(new Exception());

        // Assert
        assertThat(result.getUrl()).isEqualTo("/error");
    }

    @Test
    void switchFirm_get_active() {
        UUID firmId = UUID.randomUUID();
        UserProfile up = UserProfile.builder().activeProfile(true).firm(Firm.builder().id(firmId).name("name").build()).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().userProfiles(Set.of(up)).build());
        when(firmService.getUserAllFirms(any()))
                .thenReturn(List.of(FirmDto.builder().id(firmId).name("name").build()));
        Model model = new ConcurrentModel();
        String view = controller.userFirmsPage(model, authentication);
        assertThat(view).isEqualTo("switch-firm");
        List<FirmDto> firmDtoList = (List<FirmDto>) model.getAttribute("firmDtoList");
        assertThat(firmDtoList).hasSize(1);
        assertThat(firmDtoList.getFirst().getName()).isEqualTo("name - Active");
    }

    @Test
    void switchFirm_get_no_active() {
        UUID firmId = UUID.randomUUID();
        UserProfile up = UserProfile.builder().activeProfile(false).firm(Firm.builder().id(firmId).name("name").build()).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().userProfiles(Set.of(up)).build());
        when(firmService.getUserAllFirms(any()))
                .thenReturn(List.of(FirmDto.builder().id(firmId).name("name").build()));
        Model model = new ConcurrentModel();
        String view = controller.userFirmsPage(model, authentication);
        assertThat(view).isEqualTo("switch-firm");
        List<FirmDto> firmDtoList = (List<FirmDto>) model.getAttribute("firmDtoList");
        assertThat(firmDtoList).hasSize(1);
        assertThat(firmDtoList.getFirst().getName()).isEqualTo("name");
    }

    @Test
    void switchFirm_post() throws IOException {
        String firmId = UUID.randomUUID().toString();
        RedirectView view = controller.switchFirm(firmId, authentication, session, authClient);
        verify(loginService).getCurrentEntraUser(any());
        verify(userService).setDefaultActiveProfile(any(), any());
        verify(loginService).logout(authentication, authClient);
        verify(session).invalidate();
        assertThat(view.getUrl()).isEqualTo("/?message=logout");
    }
}