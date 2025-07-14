package uk.gov.justice.laa.portal.landingpage.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleCacheManager;

public class CachingConfigTest {

    @Test
    public void testCachingConfig() {
        CachingConfig config = new CachingConfig();
        SimpleCacheManager cacheManager = (SimpleCacheManager) config.cacheManager();
        cacheManager.initializeCaches();

        Assertions.assertThat(cacheManager).isNotNull();

        Cache cache = cacheManager.getCache(CachingConfig.TECH_SERVICES_DETAILS_CACHE);
        Assertions.assertThat(cache).isNotNull();

        cache.putIfAbsent("test", "test123");

        String result = cache.get("test", String.class);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).isEqualTo("test123");
    }
}
