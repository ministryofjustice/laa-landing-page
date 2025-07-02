package uk.gov.justice.laa.portal.landingpage.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClaimEnrichmentException.class)
    public ResponseEntity<ClaimEnrichmentResponse> handleClaimEnrichmentException(ClaimEnrichmentException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ClaimEnrichmentResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        return createErrorResponse(HttpStatus.BAD_REQUEST, "Validation error: " + errorMessage);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ClaimEnrichmentResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Validation error: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ClaimEnrichmentResponse> handleGenericException(Exception ex) {
        return createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR, 
            "An unexpected error occurred: " + ex.getMessage()
        );
    }

    private ResponseEntity<ClaimEnrichmentResponse> createErrorResponse(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(ClaimEnrichmentResponse.builder()
                        .message(message)
                        .build());
    }
}