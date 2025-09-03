package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final CacheManager cacheManager;

    @Scheduled(fixedRateString = "${app.tech.services.clear.cache.interval:3300000}")
    public void clearTechServicesCache() {
        clearCacheByName(CachingConfig.TECH_SERVICES_DETAILS_CACHE, "tech services cache");
    }

    @Scheduled(cron = "${app.firms.clear.cache.schedule}")
    public void clearCache() {
        clearCacheByName(CachingConfig.LIST_OF_FIRMS_CACHE, "Firms Cache");
    }

    private void clearCacheByName(String cacheName, String cacheDescription) {
        log.debug("Clearing {}", cacheDescription);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
