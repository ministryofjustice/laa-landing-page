package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@EnableCaching
@Configuration
public class CachingConfig {

    public static final String TECH_SERVICES_DETAILS_CACHE = "tech_services_details_cache";
    public static final String LIST_OF_FIRMS_CACHE = "all_firms_cache";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                new ConcurrentMapCache(TECH_SERVICES_DETAILS_CACHE),
                new ConcurrentMapCache(LIST_OF_FIRMS_CACHE)
                )
        );
        return cacheManager;
    }
}
