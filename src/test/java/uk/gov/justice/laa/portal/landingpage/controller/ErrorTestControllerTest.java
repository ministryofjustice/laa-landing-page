package uk.gov.justice.laa.portal.landingpage.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ErrorTestControllerTest {

    private ErrorTestController errorTestController;

    @BeforeEach
    void setUp() {
        errorTestController = new ErrorTestController();
    }

    @Test
    void trigger403_shouldThrowAccessDeniedException() {
        assertThrows(AccessDeniedException.class, () -> {
            errorTestController.trigger403();
        }, "Should throw AccessDeniedException");
    }

    @Test
    void trigger500_shouldThrowRuntimeException() {
        assertThrows(RuntimeException.class, () -> {
            errorTestController.trigger500();
        }, "Should throw RuntimeException with message 'Test internal server error'");
    }

    @Test
    void trigger500_shouldThrowRuntimeExceptionWithCorrectMessage() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            errorTestController.trigger500();
        });
        
        org.junit.jupiter.api.Assertions.assertEquals("Test internal server error", 
            exception.getMessage(), "Exception should have correct message");
    }

    @Test
    void trigger403_shouldThrowAccessDeniedExceptionWithCorrectMessage() {
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            errorTestController.trigger403();
        });
        
        org.junit.jupiter.api.Assertions.assertEquals("Test access denied error", 
            exception.getMessage(), "Exception should have correct message");
    }

    @Test
    void trigger404_shouldCompleteSuccessfully() {
        // The 404 method doesn't throw an exception, it just completes
        // The @ResponseStatus annotation is handled by Spring
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            errorTestController.trigger404();
        }, "trigger404 should not throw any exception");
    }
}
