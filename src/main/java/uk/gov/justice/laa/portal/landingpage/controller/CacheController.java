package uk.gov.justice.laa.portal.landingpage.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.portal.landingpage.service.CacheService;

@RestController
public class CacheController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/admin/firms/clear-cache")
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_INTERNAL_USER,"
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_INTERNAL_USER)")
    public ResponseEntity<String> clearFirmsCache() {
        logger.info("Clearing Firms Cache");
        cacheService.clearFirmsCache();
        return new ResponseEntity<>("Firms cache cleared!!", HttpStatus.OK);
    }

    @GetMapping("/admin/apps/clear-cache")
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_INTERNAL_USER,"
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_INTERNAL_USER)")
    public ResponseEntity<String> clearAppsCache() {
        logger.info("Clearing Apps Cache");
        cacheService.clearAppsCache();
        return new ResponseEntity<>("Apps cache cleared!!", HttpStatus.OK);
    }

}
