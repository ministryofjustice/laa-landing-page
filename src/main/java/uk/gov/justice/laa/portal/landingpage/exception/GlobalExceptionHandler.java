package uk.gov.justice.laa.portal.landingpage.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;

/**
 * Global exception handler for the application.
 */
@Slf4j
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

    @ExceptionHandler(CreateUserDetailsIncompleteException.class)
    public RedirectView handleCreateUserDetailsIncompleteException(CreateUserDetailsIncompleteException ex) {
        log.warn(
                "A user has tried to skip parts of user creation (usually by changing the URL). Redirecting to user creation screen...");
        return new RedirectView("/admin/user/create/details");
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception e) {
        log.error("An unexpected error occurred: ", e.getMessage());
        return "error";
    }

    private ResponseEntity<ClaimEnrichmentResponse> createErrorResponse(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(ClaimEnrichmentResponse.builder()
                        .success(false)
                        .message(message)
                        .build());
    }
}