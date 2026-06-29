package uk.gov.justice.laa.portal.landingpage.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ErrorTestControllerTest {

    private ErrorTestController errorTestController;

    @BeforeEach
    void setUp() {
        errorTestController = new ErrorTestController();
    }

    @Test
    void trigger403_shouldThrowResponseStatusException() {
        assertThrows(ResponseStatusException.class,
                () -> errorTestController.trigger403(), "Should throw ResponseStatusException");
    }

    @Test
    void trigger500_shouldThrowRuntimeException() {
        assertThrows(RuntimeException.class,
                () -> errorTestController.trigger500(), "Should throw RuntimeException with message 'Test internal server error'");
    }

    @Test
    void trigger500_shouldThrowRuntimeExceptionWithCorrectMessage() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> errorTestController.trigger500());

        Assertions.assertEquals("Test internal server error",
                exception.getMessage(), "Exception should have correct message");
    }

    @Test
    void trigger403_shouldThrowResponseStatusExceptionWithCorrectMessage() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> errorTestController.trigger403());

        Assertions.assertEquals("Test 403", exception.getReason());
        Assertions.assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void trigger404_shouldThrowResponseStatusExceptionWithCorrectMessage() {
        // The 404 method doesn't throw an exception, it just completes
        // The @ResponseStatus annotation is handled by Spring
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> errorTestController.trigger404());

        Assertions.assertEquals("Test 404 error", exception.getReason());
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
