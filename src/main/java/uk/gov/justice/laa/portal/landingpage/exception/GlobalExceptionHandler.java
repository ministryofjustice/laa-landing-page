package uk.gov.justice.laa.portal.landingpage.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * Handle access denied exceptions for API requests only
     * Web requests will be handled by Spring Security's access denied handler or
     * error controller
     */
    @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
    public ResponseEntity<ClaimEnrichmentResponse> handleAccessException(Exception ex, HttpServletRequest request) {
        // Only handle API requests (requests that expect JSON responses)
        String acceptHeader = request.getHeader("Accept");
        String contentType = request.getHeader("Content-Type");

        if (isApiRequest(acceptHeader, contentType, request.getRequestURI())) {
            log.warn("Access denied in API request: {}", ex.getMessage());
            return createErrorResponse(
                    HttpStatus.UNAUTHORIZED,
                    "An unauthorized error occurred: " + ex.getMessage());
        }

        // Let web requests be handled by Spring Security's error handling
        throw new RuntimeException(ex);
    }

    @ExceptionHandler({ UserNotFoundException.class})
    public ResponseEntity<ClaimEnrichmentResponse> handleUserNotFoundExceptionException(Exception ex) {
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({ BadRequestException.class})
    public ResponseEntity<ClaimEnrichmentResponse> handleBadRequestExceptionException(Exception ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handle generic exceptions for API requests only
     * Web requests will be handled by the error controller
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ClaimEnrichmentResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        // Only handle API requests (requests that expect JSON responses)
        String acceptHeader = request.getHeader("Accept");
        String contentType = request.getHeader("Content-Type");

        if (isApiRequest(acceptHeader, contentType, request.getRequestURI())) {
            log.error("Unexpected error in API request", ex);
            return createErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred: " + ex.getMessage());
        }

        // Let web requests be handled by the error controller
        throw new RuntimeException(ex);
    }

    /**
     * Determine if this is an API request that should return JSON
     */
    private boolean isApiRequest(String acceptHeader, String contentType, String requestUri) {
        // Check if request explicitly asks for JSON
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            return true;
        }

        // Check if request sends JSON
        if (contentType != null && contentType.contains("application/json")) {
            return true;
        }

        // Check if URI indicates API endpoint
        return requestUri != null && requestUri.startsWith("/api/");
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