package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache techServicesCache;

    @Mock
    private Cache firmsCache;

    @Mock
    private Cache knownEmailDomainsCache;

    @InjectMocks
    private CacheService cacheService;

    @Test
    void clearTechServicesCache_WhenCacheExists_ShouldClearCache() {
        // Given
        when(cacheManager.getCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE))
                .thenReturn(techServicesCache);

        // When
        cacheService.clearTechServicesCache();

        // Then
        verify(techServicesCache).clear();
        verifyNoInteractions(firmsCache);
    }

    @Test
    void clearTechServicesCache_WhenCacheIsNull_ShouldNotThrowException() {
        // Given
        when(cacheManager.getCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE))
                .thenReturn(null);

        // When
        cacheService.clearTechServicesCache();

        // Then - No exception should be thrown
        verifyNoInteractions(techServicesCache);
        verifyNoInteractions(firmsCache);
    }

    @Test
    void clearFirmsCache_WhenCacheExists_ShouldClearCache() {
        // Given
        when(cacheManager.getCache(CachingConfig.LIST_OF_FIRMS_CACHE))
                .thenReturn(firmsCache);

        // When
        cacheService.clearCache();

        // Then
        verify(firmsCache).clear();
        verifyNoInteractions(techServicesCache);
    }

    @Test
    void clearFirmsCache_WhenCacheIsNull_ShouldNotThrowException() {
        // Given
        when(cacheManager.getCache(CachingConfig.LIST_OF_FIRMS_CACHE))
                .thenReturn(null);

        // When
        cacheService.clearCache();

        // Then - No exception should be thrown
        verifyNoInteractions(firmsCache);
        verifyNoInteractions(techServicesCache);
    }

    @Test
    void clearKnownEmailDomainsCache_WhenCacheExists_ShouldClearCache() {
        when(cacheManager.getCache(CachingConfig.KNOWN_EMAIL_DOMAINS_CACHE))
                .thenReturn(knownEmailDomainsCache);

        cacheService.clearKnownEmailDomainsCache();

        verify(knownEmailDomainsCache).clear();
        verifyNoInteractions(techServicesCache);
        verifyNoInteractions(firmsCache);
    }

    @Test
    void clearKnownEmailDomainsCache_WhenCacheIsNull_ShouldNotThrowException() {
        when(cacheManager.getCache(CachingConfig.KNOWN_EMAIL_DOMAINS_CACHE))
                .thenReturn(null);

        cacheService.clearKnownEmailDomainsCache();

        verifyNoInteractions(knownEmailDomainsCache);
        verifyNoInteractions(techServicesCache);
        verifyNoInteractions(firmsCache);
    }
}
