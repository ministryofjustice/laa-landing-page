package uk.gov.justice.laa.portal.landingpage.validation;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BlocklistedEmailDomainsTest {

    @Test
    void isBlocklisted_returnsTrue_forKnownDomain() {
        BlocklistedEmailDomains blocklist = new BlocklistedEmailDomains(Set.of("mailinator.com", "yopmail.com"));
        assertThat(blocklist.isBlocklisted("mailinator.com")).isTrue();
        assertThat(blocklist.isBlocklisted("MAILINATOR.COM")).isTrue();
        assertThat(blocklist.isBlocklisted("  mailinator.com  ")).isTrue();
    }

    @Test
    void isBlocklisted_returnsFalse_forUnknownDomainOrNull() {
        BlocklistedEmailDomains blocklist = new BlocklistedEmailDomains(Set.of("mailinator.com"));
        assertThat(blocklist.isBlocklisted("example.com")).isFalse();
        assertThat(blocklist.isBlocklisted(null)).isFalse();
        assertThat(blocklist.isBlocklisted("")).isFalse();
    }
}


