package uk.gov.justice.laa.portal.landingpage.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final CacheManager cacheManager;

    @Scheduled(fixedRateString = "${app.tech.services.clear.cache.interval:3300000}")
    public void clearTechServicesCache() {
        log.debug("Clearing tech services cache");
        Cache cache = cacheManager.getCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/London")
    public void clearCache() {
        log.debug("Clearing Firms Cache");
        Cache cache = cacheManager.getCache(CachingConfig.LIST_OF_FIRMS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }
}
