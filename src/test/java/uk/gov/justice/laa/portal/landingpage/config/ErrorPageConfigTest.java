package uk.gov.justice.laa.portal.landingpage.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ErrorPageConfigTest {

    private ErrorPageConfig errorPageConfig;
    private ConfigurableServletWebServerFactory mockFactory;

    @Captor
    private ArgumentCaptor<ErrorPage[]> errorPagesCaptor;

    @BeforeEach
    void setUp() {
        errorPageConfig = new ErrorPageConfig();
        mockFactory = mock(ConfigurableServletWebServerFactory.class);
    }

    @Test
    void containerCustomizer_shouldReturnWebServerFactoryCustomizer() {
        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> customizer = 
            errorPageConfig.containerCustomizer();

        assertNotNull(customizer, "Container customizer should not be null");
    }

    @Test
    void containerCustomizer_shouldConfigureAllExpectedErrorPages() {
        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> customizer = 
            errorPageConfig.containerCustomizer();

        customizer.customize(mockFactory);

        verify(mockFactory).addErrorPages(errorPagesCaptor.capture());
        ErrorPage[] errorPages = errorPagesCaptor.getValue();

        assertEquals(9, errorPages.length, "Should configure 9 error pages");
    }

    @Test
    void containerCustomizer_shouldConfigureClientErrorStatusCodes() {
        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> customizer = 
            errorPageConfig.containerCustomizer();

        customizer.customize(mockFactory);

        verify(mockFactory).addErrorPages(errorPagesCaptor.capture());
        ErrorPage[] errorPages = errorPagesCaptor.getValue();

        // Check for 4xx status codes
        assertTrue(containsErrorPage(errorPages, HttpStatus.BAD_REQUEST, "/error"), 
            "Should contain 400 Bad Request error page");
        assertTrue(containsErrorPage(errorPages, HttpStatus.UNAUTHORIZED, "/error"), 
            "Should contain 401 Unauthorized error page");
        assertTrue(containsErrorPage(errorPages, HttpStatus.FORBIDDEN, "/error"), 
            "Should contain 403 Forbidden error page");
        assertTrue(containsErrorPage(errorPages, HttpStatus.NOT_FOUND, "/error"), 
            "Should contain 404 Not Found error page");
        assertTrue(containsErrorPage(errorPages, HttpStatus.METHOD_NOT_ALLOWED, "/error"), 
            "Should contain 405 Method Not Allowed error page");
    }

    @Test
    void containerCustomizer_shouldConfigureServerErrorStatusCodes() {
        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> customizer = 
            errorPageConfig.containerCustomizer();

        customizer.customize(mockFactory);

        verify(mockFactory).addErrorPages(errorPagesCaptor.capture());
        ErrorPage[] errorPages = errorPagesCaptor.getValue();

        // Check for 5xx status codes
        assertTrue(containsErrorPage(errorPages, HttpStatus.INTERNAL_SERVER_ERROR, "/error"), 
            "Should contain 500 Internal Server Error error page");
        assertTrue(containsErrorPage(errorPages, HttpStatus.BAD_GATEWAY, "/error"), 
            "Should contain 502 Bad Gateway error page");
        assertTrue(containsErrorPage(errorPages, HttpStatus.SERVICE_UNAVAILABLE, "/error"), 
            "Should contain 503 Service Unavailable error page");
        assertTrue(containsErrorPage(errorPages, HttpStatus.GATEWAY_TIMEOUT, "/error"), 
            "Should contain 504 Gateway Timeout error page");
    }

    @Test
    void containerCustomizer_shouldMapAllErrorPagesToErrorEndpoint() {
        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> customizer = 
            errorPageConfig.containerCustomizer();

        customizer.customize(mockFactory);

        verify(mockFactory).addErrorPages(errorPagesCaptor.capture());
        ErrorPage[] errorPages = errorPagesCaptor.getValue();

        // Verify all error pages map to /error endpoint
        for (ErrorPage errorPage : errorPages) {
            assertEquals("/error", errorPage.getPath(), 
                "All error pages should map to /error endpoint");
        }
    }

    @Test
    void containerCustomizer_shouldContainExpectedStatusCodes() {
        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> customizer = 
            errorPageConfig.containerCustomizer();

        customizer.customize(mockFactory);

        verify(mockFactory).addErrorPages(errorPagesCaptor.capture());
        ErrorPage[] errorPages = errorPagesCaptor.getValue();

        // Verify all expected status codes are present
        HttpStatus[] expectedStatusCodes = {
            HttpStatus.BAD_REQUEST,           // 400
            HttpStatus.UNAUTHORIZED,          // 401
            HttpStatus.FORBIDDEN,             // 403
            HttpStatus.NOT_FOUND,             // 404
            HttpStatus.METHOD_NOT_ALLOWED,    // 405
            HttpStatus.INTERNAL_SERVER_ERROR, // 500
            HttpStatus.BAD_GATEWAY,           // 502
            HttpStatus.SERVICE_UNAVAILABLE,   // 503
            HttpStatus.GATEWAY_TIMEOUT        // 504
        };

        for (HttpStatus status : expectedStatusCodes) {
            assertTrue(containsStatusCode(errorPages, status), 
                "Should contain error page for status code: " + status.value());
        }
    }

    /**
     * Helper method to check if an array of ErrorPage contains a specific status code and path
     */
    private boolean containsErrorPage(ErrorPage[] errorPages, HttpStatus status, String path) {
        return Arrays.stream(errorPages)
            .anyMatch(page -> page.getStatus() == status && page.getPath().equals(path));
    }

    /**
     * Helper method to check if an array of ErrorPage contains a specific status code
     */
    private boolean containsStatusCode(ErrorPage[] errorPages, HttpStatus status) {
        return Arrays.stream(errorPages)
            .anyMatch(page -> page.getStatus() == status);
    }
}
