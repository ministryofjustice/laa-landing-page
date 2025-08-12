package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmsCacheControllerTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private FirmsCacheController firmsCacheController;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCache(CachingConfig.LIST_OF_FIRMS_CACHE)).thenReturn(cache);
    }

    @Test
    void givenCacheExists_whenClearFirmsCache_thenCacheIsCleared() {
        // Act
        ResponseEntity<String> response = firmsCacheController.clearFirmsCache();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Firms cache cleared!!", response.getBody());
        verify(cache).clear();
    }

    @Test
    void givenCacheDoesNotExist_whenClearFirmsCache_thenReturnOk() {
        // Arrange
        when(cacheManager.getCache(CachingConfig.LIST_OF_FIRMS_CACHE)).thenReturn(null);

        // Act
        ResponseEntity<String> response = firmsCacheController.clearFirmsCache();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Firms cache cleared!!", response.getBody());
        verify(cache, never()).clear();
    }
}
