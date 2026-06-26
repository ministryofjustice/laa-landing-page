package uk.gov.justice.laa.portal.landingpage.exception;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.view.RedirectView;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleUserNotFoundException() {
        // Arrange
        String errorMessage = "Resource not found";
        UserNotFoundException exception = new UserNotFoundException(errorMessage);

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = exceptionHandler.handleUserNotFoundExceptionException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleBadRequestException() {
        // Arrange
        String errorMessage = "Invalid request";
        BadRequestException exception = new BadRequestException(errorMessage);

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = exceptionHandler.handleBadRequestExceptionException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleClaimEnrichmentException() {
        // Arrange
        String errorMessage = "Test error message";
        ClaimEnrichmentException exception = new ClaimEnrichmentException(errorMessage);

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = exceptionHandler.handleClaimEnrichmentException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleMethodArgumentNotValidException() {
        // Arrange
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response =
                exceptionHandler.handleValidationExceptions(exception);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());

        assertEquals("Invalid request", response.getBody().getMessage());
    }

    @Test
    void handleConstraintViolation() {
        // Arrange
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolationException exception =
                new ConstraintViolationException("Constraint violation", violations);

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response =
                exceptionHandler.handleConstraintViolation(exception);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());

        assertEquals("Invalid request", response.getBody().getMessage());
    }

    @Test
    void handleCreateUserDetailsIncompleteException() {
        // Arrange
        CreateUserDetailsIncompleteException exception = new CreateUserDetailsIncompleteException();
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(GlobalExceptionHandler.class);

        // Act
        RedirectView response = exceptionHandler.handleCreateUserDetailsIncompleteException(exception);

        // Assert
        assertNotNull(response);
        assertEquals("/admin/user/create/details", response.getUrl());
        List<ILoggingEvent> warningLogs =  LogMonitoring.getLogsByLevel(listAppender, Level.WARN);
        assertEquals(1, warningLogs.size());
    }

    @Test
    void handleGenericException() {
        // Arrange
        String errorMessage = "An error occurred";
        Exception exception = new Exception(errorMessage);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/json"); // Make it an API request

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = exceptionHandler.handleGenericException(exception, request);

        // Assert
        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleAccessException() {
        // Arrange
        String errorMessage = "Access denied";
        AccessDeniedException exception = new AccessDeniedException(errorMessage);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/json"); // Make it an API request

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = exceptionHandler.handleAccessException(exception, request);

        // Assert
        assertNotNull(response);
        assertEquals(401, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleGenericException_webRequest_rethrowsException() {
        // Arrange
        String errorMessage = "Unexpected error";
        Exception exception = new Exception(errorMessage);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/html"); // Make it a web request

        // Act & Assert
        RuntimeException thrown = org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class,
            () -> exceptionHandler.handleGenericException(exception, request)
        );
        assertEquals(exception, thrown.getCause());
    }

    @Test
    void handleAccessException_webRequest_rethrowsException() {
        // Arrange
        String errorMessage = "unauthorized error";
        AccessDeniedException exception = new AccessDeniedException(errorMessage);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/html"); // Make it a web request

        // Act & Assert
        RuntimeException thrown = org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class,
            () -> exceptionHandler.handleAccessException(exception, request)
        );
        assertEquals(exception, thrown.getCause());
    }

    @Test
    void handleAccessException_apiRequestByContentType() {
        // Arrange
        String errorMessage = "Access denied";
        AccessDeniedException exception = new AccessDeniedException(errorMessage);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Content-Type", "application/json"); // API request via Content-Type

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = exceptionHandler.handleAccessException(exception, request);

        // Assert
        assertNotNull(response);
        assertEquals(401, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleAccessException_apiRequestByUri() {
        // Arrange
        String errorMessage = "Access denied";
        AccessDeniedException exception = new AccessDeniedException(errorMessage);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/pda/report"); // API request via URI

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = exceptionHandler.handleAccessException(exception, request);

        // Assert
        assertNotNull(response);
        assertEquals(401, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleGenericException_apiRequestByContentType() {
        // Arrange
        String errorMessage = "An error occurred";
        Exception exception = new Exception(errorMessage);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Content-Type", "application/json"); // API request via Content-Type

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = exceptionHandler.handleGenericException(exception, request);

        // Assert
        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleGenericException_apiRequestByUri() {
        // Arrange
        String errorMessage = "An error occurred";
        Exception exception = new Exception(errorMessage);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/claim/enrich"); // API request via URI

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = exceptionHandler.handleGenericException(exception, request);

        // Assert
        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleNoResourceFoundException() {
        // Arrange
        org.springframework.web.servlet.resource.NoResourceFoundException exception =
            new org.springframework.web.servlet.resource.NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "/favicon.ico");

        // Act
        ResponseEntity<Void> response = exceptionHandler.handleNoResourceFoundException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void handleAuthorizationDeniedException_apiRequest() {
        // Arrange
        String errorMessage = "Access denied";
        org.springframework.security.authorization.AuthorizationDeniedException exception =
            new org.springframework.security.authorization.AuthorizationDeniedException(
                errorMessage,
                new org.springframework.security.authorization.AuthorizationDecision(false));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/json"); // API request

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = exceptionHandler.handleAccessException(exception, request);

        // Assert
        assertNotNull(response);
        assertEquals(401, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleAuthorizationDeniedException_webRequest_rethrowsException() {
        // Arrange
        String errorMessage = "authorization denied";
        org.springframework.security.authorization.AuthorizationDeniedException exception =
            new org.springframework.security.authorization.AuthorizationDeniedException(
                errorMessage,
                new org.springframework.security.authorization.AuthorizationDecision(false));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/html"); // Web request

        // Act & Assert
        RuntimeException thrown = org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class,
            () -> exceptionHandler.handleAccessException(exception, request)
        );
        assertEquals(exception, thrown.getCause());
    }

    @Test
    void handleMalformedJson_apiRequest() {
        // Arrange
        HttpMessageNotReadableException exception =
                mock(HttpMessageNotReadableException.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        request.addHeader("Content-Type", "application/json");

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response =
                exceptionHandler.handleMalformedJson(exception, request);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid request", response.getBody().getMessage());
    }
}
