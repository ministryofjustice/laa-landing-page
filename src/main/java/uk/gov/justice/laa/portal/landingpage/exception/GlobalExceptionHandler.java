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
import jakarta.servlet.http.HttpSession;
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

    // @ExceptionHandler(CreateUserDetailsIncompleteException.class)
    // public RedirectView handleCreateUserDetailsIncompleteException(CreateUserDetailsIncompleteException ex) {
    //     log.warn(
    //             "A user has tried to skip parts of user creation (usually by changing the URL). Redirecting to user creation screen...");
    //     return new RedirectView("/admin/user/create/details");
    // }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ClaimEnrichmentResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        // Only handle API requests (JSON responses), let ErrorController handle web
        // page requests

        String acceptHeader = request.getHeader("Accept");
        String requestURI = request.getRequestURI();

        // If it's an API request (JSON Accept header or API path), return JSON
        if ((acceptHeader != null && acceptHeader.contains("application/json")) ||
                requestURI.startsWith("/api/")) {

            // Check if this is a 404-related exception
            String exMessage = ex.getMessage();
            if (exMessage != null && exMessage.contains("No static resource")) {
                log.warn("API resource not found: {}", requestURI);
                return createNotFoundResponse("Resource not found: " + requestURI);
            }

            log.error("API error occurred for request: {}", requestURI, ex);
            return createErrorResponse(
                    HttpStatus.FORBIDDEN,
                    "An unexpected error occurred: " + ex.getMessage());
        }

        // For web page requests, re-throw the exception to let ErrorController handle
        // it
        // This allows the custom error pages to be displayed
        if (ex instanceof RuntimeException runtimeEx) {
            throw runtimeEx;
        }
        throw new RuntimeException(ex);
    }

    private ResponseEntity<ClaimEnrichmentResponse> createErrorResponse(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(ClaimEnrichmentResponse.builder()
                        .success(false)
                        .message(message)
                        .build());
    }

    private ResponseEntity<ClaimEnrichmentResponse> createNotFoundResponse(String message) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ClaimEnrichmentResponse.builder()
                        .success(false)
                        .message(message)
                        .data(null)
                        .build());
    }

    /**
     * Handle authorization exceptions when user lacks permissions to access
     * specific users
     */
    @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
    public RedirectView handleAuthorizationException(Exception ex, HttpSession session) {
        log.warn("Authorization denied while accessing user: {}", ex.getMessage());
        return new RedirectView("/not-authorised");
    }

    /**
     * Handle IllegalArgumentException (typically invalid UUIDs or invalid
     * parameters)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public RedirectView handleIllegalArgumentException(IllegalArgumentException ex, HttpSession session) {
        log.warn("Invalid argument provided: {}", ex.getMessage());
        session.setAttribute("errorMessage", "Invalid request parameters provided.");
        return new RedirectView("/error");
    }

    /**
     * Handle EntityNotFoundException (user not found, etc.)
     */
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public RedirectView handleEntityNotFoundException(jakarta.persistence.EntityNotFoundException ex,
            HttpSession session) {
        log.warn("Entity not found: {}", ex.getMessage());
        session.setAttribute("errorMessage", "The requested resource could not be found.");
        return new RedirectView("/404");
    }

    /**
     * Handle NoSuchElementException (Optional.get() failures)
     */
    @ExceptionHandler(java.util.NoSuchElementException.class)
    public RedirectView handleNoSuchElementException(java.util.NoSuchElementException ex, HttpSession session) {
        log.warn("Required element not found: {}", ex.getMessage());
        session.setAttribute("errorMessage", "The requested user or resource could not be found.");
        return new RedirectView("/404");
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public RedirectView handleConstraintViolationException(jakarta.validation.ConstraintViolationException ex,
            HttpSession session) {
        log.warn("Validation constraint violation: {}", ex.getMessage());
        session.setAttribute("errorMessage", "The provided data does not meet validation requirements.");
        return new RedirectView("/error");
    }

    /**
     * Handle IO exceptions (file operations, external service calls)
     */
    @ExceptionHandler(java.io.IOException.class)
    public RedirectView handleIOException(java.io.IOException ex, HttpSession session) {
        log.error("IO error occurred: {}", ex.getMessage(), ex);
        session.setAttribute("errorMessage",
                "A system error occurred while processing your request. Please try again.");
        return new RedirectView("/error");
    }

    /**
     * Handle timeout exceptions
     */
    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public RedirectView handleTimeoutException(java.util.concurrent.TimeoutException ex, HttpSession session) {
        log.error("Operation timed out: {}", ex.getMessage());
        session.setAttribute("errorMessage", "The operation took too long to complete. Please try again.");
        return new RedirectView("/error");
    }

    /**
     * Handle HTTP client errors (external API failures)
     */
    @ExceptionHandler(org.springframework.web.client.HttpClientErrorException.class)
    public RedirectView handleHttpClientErrorException(org.springframework.web.client.HttpClientErrorException ex,
            HttpSession session) {
        log.error("HTTP client error: {} - {}", ex.getStatusCode(), ex.getMessage());

        if (ex.getStatusCode().is4xxClientError()) {
            session.setAttribute("errorMessage", "Invalid request to external service.");
            return new RedirectView("/error");
        } else {
            session.setAttribute("errorMessage", "External service is currently unavailable. Please try again later.");
            return new RedirectView("/error");
        }
    }

    /**
     * Handle HTTP server errors (external API failures)
     */
    @ExceptionHandler(org.springframework.web.client.HttpServerErrorException.class)
    public RedirectView handleHttpServerErrorException(org.springframework.web.client.HttpServerErrorException ex,
            HttpSession session) {
        log.error("HTTP server error: {} - {}", ex.getStatusCode(), ex.getMessage());
        session.setAttribute("errorMessage", "External service is experiencing issues. Please try again later.");
        return new RedirectView("/error");
    }

    /**
     * Handle resource access exceptions (external service connectivity)
     */
    @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
    public RedirectView handleResourceAccessException(org.springframework.web.client.ResourceAccessException ex,
            HttpSession session) {
        log.error("Resource access error: {}", ex.getMessage());
        session.setAttribute("errorMessage", "Unable to connect to external services. Please try again later.");
        return new RedirectView("/error");
    }

    /**
     * Handle SQL exceptions (database errors)
     */
    @ExceptionHandler(java.sql.SQLException.class)
    public RedirectView handleSQLException(java.sql.SQLException ex, HttpSession session) {
        log.error("Database error occurred: {}", ex.getMessage(), ex);
        session.setAttribute("errorMessage",
                "A database error occurred. Please try again or contact support if the problem persists.");
        return new RedirectView("/error");
    }

    /**
     * Handle data integrity violations
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public RedirectView handleDataIntegrityViolationException(
            org.springframework.dao.DataIntegrityViolationException ex, HttpSession session) {
        log.error("Data integrity violation: {}", ex.getMessage());
        session.setAttribute("errorMessage",
                "The operation could not be completed due to data constraints. Please check your input and try again.");
        return new RedirectView("/error");
    }

    /**
     * Handle optimistic locking failures
     */
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public RedirectView handleOptimisticLockingFailureException(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex, HttpSession session) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        session.setAttribute("errorMessage", "The record was modified by another user. Please refresh and try again.");
        return new RedirectView("/error");
    }

    /**
     * Handle session-related exceptions
     */
    @ExceptionHandler(org.springframework.web.HttpSessionRequiredException.class)
    public RedirectView handleHttpSessionRequiredException(org.springframework.web.HttpSessionRequiredException ex) {
        log.warn("Session required but not found: {}", ex.getMessage());
        return new RedirectView("/login");
    }

    /**
     * Handle security exceptions
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public RedirectView handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        log.warn("Authentication exception: {}", ex.getMessage());
        return new RedirectView("/login");
    }

    /**
     * Handle custom application exceptions
     */
    @ExceptionHandler(CreateUserDetailsIncompleteException.class)
    public RedirectView handleCreateUserDetailsIncompleteException(CreateUserDetailsIncompleteException ex,
            HttpSession session) {
        log.warn("User creation details incomplete: {}", ex.getMessage());
        session.setAttribute("errorMessage", "User creation process is incomplete. Please start over.");
        return new RedirectView("/admin/user/create/details");
    }

    /**
     * Handle method argument validation failures
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public RedirectView handleMethodArgumentNotValidException(
            org.springframework.web.bind.MethodArgumentNotValidException ex, HttpSession session) {
        log.warn("Method argument validation failed: {}", ex.getMessage());
        session.setAttribute("errorMessage", "Invalid form data provided. Please check your input and try again.");
        return new RedirectView("/error");
    }

    /**
     * Handle missing servlet request parameters
     */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public RedirectView handleMissingServletRequestParameterException(
            org.springframework.web.bind.MissingServletRequestParameterException ex, HttpSession session) {
        log.warn("Missing required parameter: {}", ex.getParameterName());
        session.setAttribute("errorMessage", "Required information is missing from your request.");
        return new RedirectView("/error");
    }

    /**
     * Handle type mismatch exceptions
     */
    @ExceptionHandler(org.springframework.beans.TypeMismatchException.class)
    public RedirectView handleTypeMismatchException(org.springframework.beans.TypeMismatchException ex,
            HttpSession session) {
        log.warn("Type mismatch for property '{}': expected {} but got {}",
                ex.getPropertyName(), ex.getRequiredType(), ex.getValue());
        session.setAttribute("errorMessage", "Invalid data format provided.");
        return new RedirectView("/error");
    }

    /**
     * Handle general runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public RedirectView handleRuntimeException(RuntimeException ex, HttpSession session) {
        log.error("Runtime exception occurred: {}", ex.getMessage(), ex);
        session.setAttribute("errorMessage", "An unexpected error occurred. Please try again.");
        return new RedirectView("/error");
    }

    /**
     * Handle general exceptions (catch-all)
     */
    @ExceptionHandler(Exception.class)
    public RedirectView handleException(Exception ex, HttpSession session) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        session.setAttribute("errorMessage",
                "An unexpected system error occurred. Please contact support if the problem persists.");
        return new RedirectView("/error");
    }
}