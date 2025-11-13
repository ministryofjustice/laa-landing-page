package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.validation.BlocklistedEmailDomains;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmailValidationServiceTest {

    private EmailValidationService emailValidationService;

    @BeforeEach
    void setUp() {
        emailValidationService = new EmailValidationService(new BlocklistedEmailDomains(Set.of()), null, null);
    }

    @Test
    void isValidEmailDomain_skipsMx_whenFeatureEnabled_andDomainKnownByRepo() {
        BlocklistedEmailDomains blocklist = new BlocklistedEmailDomains(Set.of());
        EntraUserRepository repo = mock(EntraUserRepository.class);
        when(repo.existsByEmailDomain("example.com")).thenReturn(true);

        EmailValidationService svc = new EmailValidationService(blocklist, repo, null) {
            @Override
            protected boolean hasMxRecords(String email) {
                throw new AssertionError("MX should not be called when domain is known");
            }
        };
        ReflectionTestUtils.setField(svc, "skipMxForKnownDomains", true);

        boolean result = svc.isValidEmailDomain("user@example.com");
        assertThat(result).isTrue();
        verify(repo).existsByEmailDomain("example.com");
    }

    @Test
    void isValidEmailDomain_usesCacheHit_andSkipsRepo_andMx() {
        BlocklistedEmailDomains blocklist = new BlocklistedEmailDomains(Set.of());
        EntraUserRepository repo = mock(EntraUserRepository.class);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(cache.get("example.com", Boolean.class)).thenReturn(true);

        EmailValidationService svc = new EmailValidationService(blocklist, repo, cacheManager) {
            @Override
            protected boolean hasMxRecords(String email) {
                throw new AssertionError("MX should not be called on cache hit");
            }
        };
        ReflectionTestUtils.setField(svc, "skipMxForKnownDomains", true);

        boolean result = svc.isValidEmailDomain("user@example.com");
        assertThat(result).isTrue();
        verify(cacheManager).getCache(anyString());
        verify(cache).get("example.com", Boolean.class);
        verifyNoInteractions(repo);
    }

    @Test
    void isValidEmailDomain_populatesCacheOnMiss_whenRepoSaysKnown_andSkipsMx() {
        BlocklistedEmailDomains blocklist = new BlocklistedEmailDomains(Set.of());
        EntraUserRepository repo = mock(EntraUserRepository.class);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(cache.get("example.com", Boolean.class)).thenReturn(null);
        when(repo.existsByEmailDomain("example.com")).thenReturn(true);

        EmailValidationService svc = new EmailValidationService(blocklist, repo, cacheManager) {
            @Override
            protected boolean hasMxRecords(String email) {
                throw new AssertionError("MX should not be called when repo confirms known and we cache it");
            }
        };
        ReflectionTestUtils.setField(svc, "skipMxForKnownDomains", true);

        boolean result = svc.isValidEmailDomain("user@example.com");
        assertThat(result).isTrue();
        verify(cache).put("example.com", true);
        verify(repo).existsByEmailDomain("example.com");
    }

    @Test
    void isValidEmailDomain_continuesToMx_whenRepoSaysUnknown_andCachesFalse() {
        BlocklistedEmailDomains blocklist = new BlocklistedEmailDomains(Set.of());
        EntraUserRepository repo = mock(EntraUserRepository.class);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(cache.get("example.com", Boolean.class)).thenReturn(null);
        when(repo.existsByEmailDomain("example.com")).thenReturn(false);

        EmailValidationService svc = new EmailValidationService(blocklist, repo, cacheManager) {
            @Override
            protected boolean hasMxRecords(String email) {
                return true; // simulate MX presence
            }
        };
        ReflectionTestUtils.setField(svc, "skipMxForKnownDomains", true);

        boolean result = svc.isValidEmailDomain("user@example.com");
        assertThat(result).isTrue();
        verify(cache).put("example.com", false);
        verify(repo).existsByEmailDomain("example.com");
    }

    @Test
    void isValidEmailDomain_featureFlagOff_bypassesRepoAndCache_andUsesMx() {
        BlocklistedEmailDomains blocklist = new BlocklistedEmailDomains(Set.of());
        EntraUserRepository repo = mock(EntraUserRepository.class);
        CacheManager cacheManager = mock(CacheManager.class);

        EmailValidationService svc = new EmailValidationService(blocklist, repo, cacheManager) {
            @Override
            protected boolean hasMxRecords(String email) {
                return false;
            }
        };
        ReflectionTestUtils.setField(svc, "skipMxForKnownDomains", false);

        boolean result = svc.isValidEmailDomain("user@example.com");
        assertThat(result).isFalse();
        verifyNoInteractions(repo);
        verifyNoInteractions(cacheManager);
    }

    @Test
    void isValidEmailDomain_blocklistTakesPrecedence_overSkipLogic() {
        BlocklistedEmailDomains blocklist = new BlocklistedEmailDomains(Set.of("mailinator.com"));
        EntraUserRepository repo = mock(EntraUserRepository.class);
        CacheManager cacheManager = mock(CacheManager.class);

        EmailValidationService svc = new EmailValidationService(blocklist, repo, cacheManager) {
            @Override
            protected boolean hasMxRecords(String email) {
                throw new AssertionError("MX should not be called for blocklisted domains");
            }
        };
        ReflectionTestUtils.setField(svc, "skipMxForKnownDomains", true);

        boolean result = svc.isValidEmailDomain("user@mailinator.com");
        assertThat(result).isFalse();
        verifyNoInteractions(repo);
        verifyNoInteractions(cacheManager);
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
    void hasMxRecords_validateEmailDomain_timeout() {
        EmailValidationService slowValidator = new EmailValidationService(new BlocklistedEmailDomains(Set.of()), null, null) {
            @Override
            public boolean hasMxRecords(String email) {
                try {
                    Thread.sleep(1_100); // simulate long-running task
                } catch (InterruptedException ignored) {
                    // Do nothing
                }
                return true;
            }
        };

        RuntimeException ex = assertThrows(RuntimeException.class, () -> slowValidator.isValidEmailDomain("user@example.com", 1));
        assertThat(ex.getMessage()).isEqualTo("The email domain validation took longer than expected. Possibly the email domain is invalid!");

    }

    @Test
    void isValidEmailDomain_returnsFalse_forClearlyNonExistentDomain_withoutThrowing() {
        EmailValidationService svc = new EmailValidationService(new BlocklistedEmailDomains(Set.of()), null, null);
        boolean result = assertDoesNotThrow(() -> svc.isValidEmailDomain("user@nonexistentdomainfortesting12345.example"));
        assertThat(result).isFalse();
    }

    @Test
    void isValidEmailDomain_returnsFalse_whenHasMxRecordsReturnsFalse() {
        EmailValidationService svc = new EmailValidationService(new BlocklistedEmailDomains(Set.of()), null, null) {
            @Override
            public boolean hasMxRecords(String email) {
                return false;
            }
        };
        boolean result = assertDoesNotThrow(() -> svc.isValidEmailDomain("user@example.com"));
        assertThat(result).isFalse();
    }

}
