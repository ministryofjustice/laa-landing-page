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

    @Scheduled(cron = "${app.firms.clear.cache.schedule}")
    public void clearFirmsCache() {
        log.info("Clearing Firms Cache");
        Cache cache = cacheManager.getCache(CachingConfig.LIST_OF_FIRMS_CACHE);
        if (cache != null) {
            cache.clear();
            log.info("Cleared Firms Cache");
        }
    }

    @Scheduled(cron = "${app.apps.cache.clear.schedule}")
    public void clearAppsCache() {
        log.info("Clearing Apps Cache");
        Cache cache = cacheManager.getCache(CachingConfig.LIST_OF_APPS_CACHE);
        if (cache != null) {
            cache.clear();
            log.info("Cleared Apps Cache");
        }
    }
}
