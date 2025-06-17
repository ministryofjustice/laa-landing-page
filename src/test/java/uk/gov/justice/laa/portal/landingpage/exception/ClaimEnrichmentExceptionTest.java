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
}
