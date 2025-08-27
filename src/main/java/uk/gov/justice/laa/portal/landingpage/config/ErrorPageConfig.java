package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * Configuration for custom error pages
 * Ensures that specific HTTP error codes redirect to appropriate error pages
 */
@Configuration
public class ErrorPageConfig implements ErrorPageRegistrar {

    @Override
    public void registerErrorPages(ErrorPageRegistry registry) {
        // Register custom error pages for specific HTTP status codes
        registry.addErrorPages(
                new ErrorPage(HttpStatus.NOT_FOUND, "/404"),
                new ErrorPage(HttpStatus.FORBIDDEN, "/not-authorised"),
                new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error"),
                new ErrorPage(HttpStatus.BAD_REQUEST, "/error"),
                new ErrorPage(HttpStatus.UNAUTHORIZED, "/error"));
    }
}
