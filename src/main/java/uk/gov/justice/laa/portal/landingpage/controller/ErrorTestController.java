package uk.gov.justice.laa.portal.landingpage.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controller for testing error pages in development/test environments only
 * Enable with: app.test.error-pages.enabled=true
 */
@Controller
@ConditionalOnProperty(name = "app.test.error-pages.enabled", havingValue = "true")
@RequestMapping("/test-errors")
public class ErrorTestController {

    @GetMapping("/404")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void trigger404() {
        // This will trigger a 404 error
    }

    @GetMapping("/403")
    public void trigger403() {
        throw new AccessDeniedException("Test access denied error");
    }

    @GetMapping("/500")
    public void trigger500() {
        throw new RuntimeException("Test internal server error");
    }
}
