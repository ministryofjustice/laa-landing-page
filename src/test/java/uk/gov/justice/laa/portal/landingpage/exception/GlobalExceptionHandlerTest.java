package uk.gov.justice.laa.portal.landingpage.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
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
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "Field validation failed");
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = exceptionHandler.handleValidationExceptions(exception);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Validation error"));
        assertTrue(response.getBody().getMessage().contains("Field validation failed"));
    }

    @Test
    void handleConstraintViolation() {
        // Arrange
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolationException exception = new ConstraintViolationException("Constraint violation", violations);

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = exceptionHandler.handleConstraintViolation(exception);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Validation error"));
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
        String errorMessage = "Unexpected error";
        Exception exception = new Exception(errorMessage);

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = exceptionHandler.handleGenericException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains(errorMessage));
    }
}
