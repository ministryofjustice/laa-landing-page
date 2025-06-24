package uk.gov.justice.laa.portal.landingpage.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClaimEnrichmentExceptionTest {

    @Test
    void constructor_WithMessage() {
        // Arrange
        String errorMessage = "Test error message";

        // Act
        ClaimEnrichmentException exception = new ClaimEnrichmentException(errorMessage);

        // Assert
        assertEquals(errorMessage, exception.getMessage());
    }

    @Test
    void constructor_WithMessageAndCause() {
        // Arrange
        String errorMessage = "Test error message";
        Throwable cause = new RuntimeException("Test cause");

        // Act
        ClaimEnrichmentException exception = new ClaimEnrichmentException(errorMessage, cause);

        // Assert
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void constructor_WithMessageAndArgs() {
        // Arrange
        String messageTemplate = "Test error message with args: %s, %s";
        String arg1 = "first";
        String arg2 = "second";

        // Act
        ClaimEnrichmentException exception = new ClaimEnrichmentException(messageTemplate, arg1, arg2);

        // Assert
        assertEquals(String.format(messageTemplate, arg1, arg2), exception.getMessage());
    }
}
