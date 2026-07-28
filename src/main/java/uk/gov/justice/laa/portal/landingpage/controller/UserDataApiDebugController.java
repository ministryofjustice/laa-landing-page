package uk.gov.justice.laa.portal.landingpage.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;

import java.util.Map;
import java.util.UUID;

/**
 * Temporary debug endpoint to verify the OBO flow between laa-landing-page and laa-data-user-api.
 * Enable with: app.test.user-data-api.enabled=true
 * Remove (or keep behind the flag) once OBO is wired into a real user journey.
 */
@RestController
@ConditionalOnProperty(name = "app.test.user-data-api.enabled", havingValue = "true")
@RequestMapping("/debug/user-data-api")
public class UserDataApiDebugController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UserDataApiClient userDataApiClient;

    public UserDataApiDebugController(UserDataApiClient userDataApiClient) {
        this.userDataApiClient = userDataApiClient;
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello(
            Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String cid = correlationId != null ? correlationId : UUID.randomUUID().toString();
        logger.info("DEBUG: calling data API /hello, correlationId={}", cid);
        Map<String, String> response = userDataApiClient.hello(authentication, cid);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(
            Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String cid = correlationId != null ? correlationId : UUID.randomUUID().toString();
        logger.info("DEBUG: calling data API /me, correlationId={}", cid);
        Map<String, String> response = userDataApiClient.me(authentication, cid);
        return ResponseEntity.ok(response);
    }
}
