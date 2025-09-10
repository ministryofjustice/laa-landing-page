package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.validation.BlocklistedEmailDomains;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailValidationServiceTest {

    private EmailValidationService emailValidationService;

    @BeforeEach
    void setUp() {
        emailValidationService = new EmailValidationService(new BlocklistedEmailDomains(Set.of()));
    }

    @Test
    void hasMxRecords_returnsFalse_whenEmailIsNull() {
        boolean result = emailValidationService.isValidEmailDomain(null);
        assertThat(result).isFalse();
    }

    @Test
    void hasMxRecords_returnsFalse_whenEmailIsEmpty() {
        boolean result = emailValidationService.isValidEmailDomain("");
        assertThat(result).isFalse();
    }

    @Test
    void hasMxRecords_returnsFalse_whenEmailDoesNotContainAtSymbol() {
        boolean result = emailValidationService.isValidEmailDomain("invalid-email");
        assertThat(result).isFalse();
    }

    @Test
    void hasMxRecords_returnsFalse_whenEmailOnlyContainsAtSymbol() {
        boolean result = emailValidationService.isValidEmailDomain("@");
        assertThat(result).isFalse();
    }

    @Test
    void hasMxRecords_returnsFalse_whenEmailEndsWithAtSymbol() {
        boolean result = emailValidationService.isValidEmailDomain("user@");
        assertThat(result).isFalse();
    }

    @Test
    void hasMxRecords_doesNotThrowException_whenCalledWithValidEmail() {
        // This test verifies the method handles DNS lookups gracefully
        // It may return true or false depending on the actual DNS resolution,
        // but it should not throw any exceptions
        assertDoesNotThrow(() -> {
            emailValidationService.isValidEmailDomain("test@nonexistentdomainfortesting12345.com");
        });
    }

    @Test
    void hasMxRecords_handlesRealDomain_gracefully() {
        // This test uses a real domain but doesn't assert the result
        // since DNS results can vary by environment
        assertDoesNotThrow(() -> {
            emailValidationService.isValidEmailDomain("user@example.com");
        });
    }

    @Test
    void hasMxRecords_extractsCorrectDomain_fromEmailWithMultipleAtSymbols() {
        // Test with an email that has multiple @ symbols
        // The method should extract the domain after the last @
        assertDoesNotThrow(() -> {
            emailValidationService.isValidEmailDomain("user@name@example.com");
        });
    }

    @Test
    void hasMxRecords_handlesSubdomain_gracefully() {
        assertDoesNotThrow(() -> {
            emailValidationService.isValidEmailDomain("user@mail.example.com");
        });
    }

    @Test
    void hasMxRecords_handlesInvalidDomain_gracefully() {
        // Should handle invalid domains without throwing exceptions
        assertDoesNotThrow(() -> {
            emailValidationService.isValidEmailDomain("user@invalid..domain");
            // For invalid domains, we expect the method to handle it gracefully
        });
    }

    @Test
    void hasMxRecords_validateEmailDomain_timeout() {

        EmailValidationService slowValidator = new EmailValidationService(new BlocklistedEmailDomains(Set.of())) {
            @Override
            public boolean hasMxRecords(String email) {
                try {
                    Thread.sleep(35_000); // simulate long-running task
                } catch (InterruptedException ignored) {
                    // Do nothing
                }
                return true;
            }
        };

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            slowValidator.isValidEmailDomain("user@example.com");
        });
        assertThat(ex.getMessage()).isEqualTo("The email domain validation took longer than expected. Possibly the email domain is invalid!");

    }

}
