package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginControllerUnitTest {

    @Mock private LoginService loginService;
    @InjectMocks private LoginController controller;

    @BeforeEach
    void init() {
        try (AutoCloseable closeable = MockitoAnnotations.openMocks(this)) {
            // mocks are initialized here
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize mocks", e);
        }
    }

    @Test
    void loginPageDisplaysIndex() {
        Model model = new org.springframework.ui.ConcurrentModel();
        String view = controller.login(model);
        assertThat(view).isEqualTo("index");
        assertThat(model.containsAttribute("user")).isTrue();
    }

    @Test
    void handleLogin_withBlankEmail_redirectsToLoginWithError() {
        RedirectAttributes attrs = mock(RedirectAttributes.class);
        RedirectView result = controller.handleLogin("   ", attrs);
        assertThat(result.getUrl()).isEqualTo("/");
        // flash attribute "errorMessage" set
        verify(attrs).addFlashAttribute(eq("errorMessage"), contains("incorrect Username"));
    }

    @Test
    void handleLogin_withValidEmail_redirectsToAzure() {
        when(loginService.buildAzureLoginUrl("foo@bar.com")).thenReturn("https://login.test/?hint=foo%40bar.com");
        RedirectAttributes attrs = mock(RedirectAttributes.class);
        RedirectView result = controller.handleLogin("foo@bar.com", attrs);
        assertThat(result.getUrl()).isEqualTo("https://login.test/?hint=foo%40bar.com");
        verify(loginService).buildAzureLoginUrl("foo@bar.com");
    }

    @Test
    void migrateEndpoint_returnsMigrateView() {
        String view = controller.migrate();
        assertThat(view).isEqualTo("migrate");
    }
}