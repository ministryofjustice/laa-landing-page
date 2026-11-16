package uk.gov.justice.laa.portal.landingpage.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import uk.gov.justice.laa.portal.landingpage.service.LoginService;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomAuthenticationSuccessHandler")
class CustomAuthenticationSuccessHandlerTest {

    @Mock
    private LoginService loginService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    private CustomAuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CustomAuthenticationSuccessHandler(loginService);
    }

    @Test
    @DisplayName("Should call LoginService to record successful login")
    void shouldCallLoginServiceToRecordSuccessfulLogin() throws IOException {
        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(loginService, times(1)).recordSuccessfulLogin(authentication);
        verify(response, times(1)).sendRedirect("/home");
    }

    @Test
    @DisplayName("Should redirect to /home even if recordSuccessfulLogin throws exception")
    void shouldRedirectToHomeEvenIfRecordSuccessfulLoginThrowsException() throws IOException {
        // Arrange
        doThrow(new RuntimeException("Service error")).when(loginService).recordSuccessfulLogin(any());

        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(response, times(1)).sendRedirect("/home");
    }
}
