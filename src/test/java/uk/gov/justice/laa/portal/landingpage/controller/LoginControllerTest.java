package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)            // ① Enable Mockito auto-initialization  [oai_citation:1‡JUnit](https://junit.org/junit5/docs/current/user-guide/?utm_source=chatgpt.com)
class LoginControllerTest {

    @Mock
    private LoginService loginService;          // ② Mocked dependency

    @InjectMocks
    private LoginController controller;         // ③ Class under test

    @Test
    void givenEmptyEmail_whenHandleLogin_thenRedirectToLoginWithError() {
        // Arrange
        Model model = new ConcurrentModel();
        RedirectAttributes attrs = org.mockito.Mockito.mock(RedirectAttributes.class);

        // Act
        String viewIndex = controller.login(model);

        // Assert
        assertThat(viewIndex).isEqualTo("index");
        assertThat(model.containsAttribute("user")).isTrue();
    }

    @Test
    void givenBlankEmail_whenHandleLogin_thenRedirectsWithErrorFlash() {
        // Arrange
        RedirectAttributes attrs = org.mockito.Mockito.mock(RedirectAttributes.class);

        // Act
        RedirectView result = controller.handleLogin("   ", attrs);

        // Assert
        assertThat(result.getUrl()).isEqualTo("/");
        verify(attrs).addFlashAttribute(
                eq("errorMessage"), contains("incorrect Username")
        );
    }

    @Test
    void givenValidEmail_whenHandleLogin_thenRedirectsToAzure() {
        // Arrange
        String email = "foo@bar.com";
        when(loginService.buildAzureLoginUrl(email))
                .thenReturn("https://login.test/?hint=foo%40bar.com");
        RedirectAttributes attrs = org.mockito.Mockito.mock(RedirectAttributes.class);

        // Act
        RedirectView result = controller.handleLogin(email, attrs);

        // Assert
        assertThat(result.getUrl())
                .isEqualTo("https://login.test/?hint=foo%40bar.com");
        verify(loginService).buildAzureLoginUrl(email);
    }

    @Test
    void whenMigrateEndpoint_thenReturnsMigrateView() {
        // Act
        String view = controller.migrate();

        // Assert
        assertThat(view).isEqualTo("migrate");
    }
}