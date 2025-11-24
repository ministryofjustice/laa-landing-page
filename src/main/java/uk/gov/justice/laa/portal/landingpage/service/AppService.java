package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {

    private final AppRepository appRepository;

    private final ModelMapper mapper;

    private final CacheManager cacheManager;

    private static final String ALL_APPS = "all_apps";

    public Optional<App> getById(UUID id) {
        return appRepository.findById(id);
    }

    public List<AppDto> findAll() {
        return appRepository.findAll().stream()
                .map(app -> mapper.map(app, AppDto.class))
                .toList();
    }

    @Scheduled(cron = "${app.apps.cache.clear.schedule}")
    public void clearCache() {
        cacheManager.getCache(ALL_APPS).clear();
    }

    public List<AppDto> getAllAppsFromCache() {
        Cache cache = cacheManager.getCache(CachingConfig.LIST_OF_APPS_CACHE);
        if (cache != null) {
            try {
                Cache.ValueWrapper valueWrapper = cache.get(ALL_APPS);
                if (valueWrapper != null) {
                    @SuppressWarnings("unchecked")
                    List<AppDto> cachedApps = (List<AppDto>) valueWrapper.get();
                    return cachedApps;
                }
            } catch (Exception ex) {
                log.info("Error while getting apps from cache", ex);
            }
        }

        List<AppDto> allApps = findAll();
        if (cache != null) {
            cache.put(ALL_APPS, allApps);
        }
        return allApps;
    }
}
