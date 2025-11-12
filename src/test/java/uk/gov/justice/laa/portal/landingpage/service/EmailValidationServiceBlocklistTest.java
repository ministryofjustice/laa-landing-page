package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.validation.BlocklistedEmailDomains;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailValidationServiceBlocklistTest {

    @Test
    void isValidEmailDomain_returnsFalse_whenDomainIsBlocklisted() {
        BlocklistedEmailDomains blocklist = new BlocklistedEmailDomains(Set.of("mailinator.com"));
        EntraUserRepository repo = mock(EntraUserRepository.class);
        CacheManager cacheManager = mock(CacheManager.class);
        EmailValidationService svc = new EmailValidationService(blocklist, repo, cacheManager);
        boolean result = svc.isValidEmailDomain("user@mailinator.com");
        assertThat(result).isFalse();
    }

    @Test
    void isValidEmailDomain_proceeds_whenDomainNotBlocklisted() {
        // Use an empty blocklist; result depends on MX but should not throw and returns boolean
        BlocklistedEmailDomains blocklist = new BlocklistedEmailDomains(Set.of());
        EntraUserRepository repo = mock(EntraUserRepository.class);
        CacheManager cacheManager = mock(CacheManager.class);
        EmailValidationService svc = new EmailValidationService(blocklist, repo, cacheManager);
        ReflectionTestUtils.setField(svc, "skipMxForKnownDomains", true);
        when(repo.existsByEmailDomain("example.com")).thenReturn(true);
        boolean result = svc.isValidEmailDomain("user@example.com");
        assertThat(result).isTrue();
    }
}


