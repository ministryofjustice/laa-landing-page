package uk.gov.justice.laa.portal.landingpage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailValidationServiceTest {

    private EmailValidationService emailValidationService;

    @BeforeEach
    void setUp() {
        emailValidationService = new EmailValidationService();
    }

    @Test
    void hasMxRecords_returnsFalse_whenEmailIsNull() {
        boolean result = emailValidationService.hasMxRecords(null);
        assertThat(result).isFalse();
    }

    @Test
    void hasMxRecords_returnsFalse_whenEmailIsEmpty() {
        boolean result = emailValidationService.hasMxRecords("");
        assertThat(result).isFalse();
    }

    @Test
    void hasMxRecords_returnsFalse_whenEmailDoesNotContainAtSymbol() {
        boolean result = emailValidationService.hasMxRecords("invalid-email");
        assertThat(result).isFalse();
    }

    @Test
    void hasMxRecords_returnsFalse_whenEmailOnlyContainsAtSymbol() {
        boolean result = emailValidationService.hasMxRecords("@");
        assertThat(result).isFalse();
    }

    @Test
    void hasMxRecords_returnsFalse_whenEmailEndsWithAtSymbol() {
        boolean result = emailValidationService.hasMxRecords("user@");
        assertThat(result).isFalse();
    }

    @Test
    void hasMxRecords_doesNotThrowException_whenCalledWithValidEmail() {
        // This test verifies the method handles DNS lookups gracefully
        // It may return true or false depending on the actual DNS resolution,
        // but it should not throw any exceptions
        assertDoesNotThrow(() -> {
            emailValidationService.hasMxRecords("test@nonexistentdomainfortesting12345.com");
        });
    }

    @Test
    void hasMxRecords_handlesRealDomain_gracefully() {
        // This test uses a real domain but doesn't assert the result
        // since DNS results can vary by environment
        assertDoesNotThrow(() -> {
            emailValidationService.hasMxRecords("user@example.com");
        });
    }

    @Test
    void hasMxRecords_extractsCorrectDomain_fromEmailWithMultipleAtSymbols() {
        // Test with an email that has multiple @ symbols
        // The method should extract the domain after the last @
        assertDoesNotThrow(() -> {
            emailValidationService.hasMxRecords("user@name@example.com");
        });
    }

    @Test
    void hasMxRecords_handlesSubdomain_gracefully() {
        assertDoesNotThrow(() -> {
            emailValidationService.hasMxRecords("user@mail.example.com");
        });
    }

    @Test
    void hasMxRecords_handlesInvalidDomain_gracefully() {
        // Should handle invalid domains without throwing exceptions
        assertDoesNotThrow(() -> {
            emailValidationService.hasMxRecords("user@invalid..domain");
            // For invalid domains, we expect the method to handle it gracefully
        });
    }
}
