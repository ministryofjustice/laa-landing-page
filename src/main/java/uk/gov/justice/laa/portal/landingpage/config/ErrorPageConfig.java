package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * Configuration for custom error pages
 * Maps HTTP status codes to the custom error controller
 * 
 * This configuration ensures that specific HTTP error status codes are 
 * handled by our custom error controller instead of the default Spring Boot
 * error handling. All mapped status codes will be routed to the /error
 * endpoint which is handled by CustomErrorController.
 */
@Configuration
public class ErrorPageConfig {

    /**
     * Customize the web server to use custom error pages for specific HTTP status codes.
     * 
     * Maps common HTTP error status codes to our custom error controller:
     * - 400 Bad Request: Invalid request syntax
     * - 401 Unauthorized: Authentication required
     * - 403 Forbidden: Access denied
     * - 404 Not Found: Resource not found  
     * - 405 Method Not Allowed: HTTP method not supported
     * - 500 Internal Server Error: Server encountered an error
     * - 502 Bad Gateway: Invalid response from upstream server
     * - 503 Service Unavailable: Server temporarily unavailable
     * - 504 Gateway Timeout: Timeout from upstream server
     * 
     * @return WebServerFactoryCustomizer that configures error pages
     */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> containerCustomizer() {
        return container -> {
            container.addErrorPages(
                // Client error status codes (4xx)
                new ErrorPage(HttpStatus.BAD_REQUEST, "/error"),
                new ErrorPage(HttpStatus.UNAUTHORIZED, "/error"),
                new ErrorPage(HttpStatus.FORBIDDEN, "/error"), 
                new ErrorPage(HttpStatus.NOT_FOUND, "/error"),
                new ErrorPage(HttpStatus.METHOD_NOT_ALLOWED, "/error"),
                
                // Server error status codes (5xx)
                new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error"),
                new ErrorPage(HttpStatus.BAD_GATEWAY, "/error"),
                new ErrorPage(HttpStatus.SERVICE_UNAVAILABLE, "/error"),
                new ErrorPage(HttpStatus.GATEWAY_TIMEOUT, "/error")
            );
        };
    }
}
