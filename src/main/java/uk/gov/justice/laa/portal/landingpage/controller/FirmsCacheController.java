package uk.gov.justice.laa.portal.landingpage.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;

@RestController
public class FirmsCacheController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final CacheManager cacheManager;

    public FirmsCacheController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @GetMapping("/admin/firms/clear-cache")
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_INTERNAL_USER,"
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_INTERNAL_USER)")
    public ResponseEntity<String> clearFirmsCache() {
        Cache cache = cacheManager.getCache(CachingConfig.LIST_OF_FIRMS_CACHE);
        if (cache != null) {
            cache.clear();
            logger.info("Firms cache cleared");
        }
        return new ResponseEntity<>("Firms cache cleared!!", HttpStatus.OK);
    }

}
