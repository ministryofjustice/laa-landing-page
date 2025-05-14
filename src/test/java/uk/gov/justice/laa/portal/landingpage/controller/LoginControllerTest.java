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
import uk.gov.justice.laa.portal.landingpage.model.UserSessionData;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private LoginService loginService;

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
        String viewIndex = controller.login(model);

        // Assert
        assertThat(viewIndex).isEqualTo("index");
        assertThat(model.containsAttribute("user")).isTrue();
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
}