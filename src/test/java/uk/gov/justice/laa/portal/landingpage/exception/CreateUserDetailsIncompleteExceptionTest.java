package uk.gov.justice.laa.portal.landingpage.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CreateUserDetailsIncompleteExceptionTest {

    @Test
    public void testEmptyConstructorReturnsNullMessage() {
        CreateUserDetailsIncompleteException exception = new CreateUserDetailsIncompleteException();
        assertNull(exception.getMessage());
    }

    @Test
    public void testConstructorWithMessageReturnsProvidedMessage() {
        final String message = "Test Message";
        CreateUserDetailsIncompleteException exception = new CreateUserDetailsIncompleteException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    public void testConstructorWithFormattedMessageReturnsProvidedMessage() {
        final String message = "Test Message 1";
        CreateUserDetailsIncompleteException exception = new CreateUserDetailsIncompleteException("Test Message %d", 1);
        assertEquals(message, exception.getMessage());
    }

}
