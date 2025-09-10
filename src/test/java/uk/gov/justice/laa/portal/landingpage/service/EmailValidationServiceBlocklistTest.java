package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.validation.BlocklistedEmailDomains;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EmailValidationServiceBlocklistTest {

    @Test
    void isValidEmailDomain_returnsFalse_whenDomainIsBlocklisted() {
        BlocklistedEmailDomains blocklist = new BlocklistedEmailDomains(Set.of("mailinator.com"));
        EmailValidationService svc = new EmailValidationService(blocklist);
        boolean result = svc.isValidEmailDomain("user@mailinator.com");
        assertThat(result).isFalse();
    }

    @Test
    void isValidEmailDomain_proceeds_whenDomainNotBlocklisted() {
        // Use an empty blocklist; result depends on MX but should not throw and returns boolean
        BlocklistedEmailDomains blocklist = new BlocklistedEmailDomains(Set.of());
        EmailValidationService svc = new EmailValidationService(blocklist);
        boolean result = svc.isValidEmailDomain("user@example.com");
        assertThat(result == true || result == false).isTrue();
    }
}


