package uk.gov.justice.laa.portal.landingpage.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import java.io.IOException;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationSuccessHandlerTest {

    @Mock
    private LoginService loginService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CustomAuthenticationSuccessHandler handler;

    @Test
    void onAuthenticationSuccess_recordsSuccessfulLoginAndRedirects() throws IOException {
        handler.onAuthenticationSuccess(request, response, authentication);
        verify(loginService).recordSuccessfulLogin(authentication);
        verify(response).sendRedirect("/home");
    }

    @Test
    void onAuthenticationSuccess_callsServicesOnlyOnce() throws IOException {
        handler.onAuthenticationSuccess(request, response, authentication);
        verify(loginService, times(1)).recordSuccessfulLogin(authentication);
        verify(response, times(1)).sendRedirect("/home");
        verifyNoMoreInteractions(loginService, response);
    }
}
