package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FirmsCacheControllerTest extends BaseIntegrationTest {

    @MockitoBean
    private CacheManager cacheManager;

    @MockitoBean
    private Cache cache;

    @Test
    void clearFirmsCache_WhenAuthorized_ShouldClearCacheAndReturnSuccess() throws Exception {

        // Arrange
        when(cacheManager.getCache(CachingConfig.LIST_OF_FIRMS_CACHE)).thenReturn(cache);

        // Act & Assert
        mockMvc.perform(get("/admin/firms/clear-cache")
                        .with(csrf())
                        .with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().isOk())
                .andExpect(content().string("Firms cache cleared!!"));

        // Verify
        verify(cache).clear();
    }

    @Test
    void clearFirmsCache_WhenCacheIsNull_ShouldReturnSuccess() throws Exception {
        // Arrange
        when(cacheManager.getCache(CachingConfig.LIST_OF_FIRMS_CACHE)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/admin/firms/clear-cache")
                        .with(csrf())
                        .with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().isOk())
                .andExpect(content().string("Firms cache cleared!!"));

        // Verify
        verify(cache, never()).clear();
    }

    @Test
    void clearFirmsCache_WhenUnauthenticated_ShouldRedirectToLogin() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/admin/firms/clear-cache"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/azure"));

        // Verify
        verify(cacheManager, never()).getCache(anyString());
        verify(cache, never()).clear();
    }
}
