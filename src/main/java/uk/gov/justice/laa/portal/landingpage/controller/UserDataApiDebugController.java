package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;

import java.util.Map;
import java.util.UUID;

/**
 * Temporary debug endpoint to verify the OBO flow between laa-landing-page and laa-data-user-api.
 * Enable with: app.test.user-data-api.enabled=true
 * Remove once OBO is wired into a real user journey.
 */
@RestController
@ConditionalOnProperty(name = "app.test.user-data-api.enabled", havingValue = "true")
@RequestMapping("/debug/user-data-api")
public class UserDataApiDebugController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UserDataApiClient userDataApiClient;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;

    public UserDataApiDebugController(UserDataApiClient userDataApiClient,
                                      OAuth2AuthorizedClientRepository authorizedClientRepository) {
        this.userDataApiClient = userDataApiClient;
        this.authorizedClientRepository = authorizedClientRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(
            OAuth2AuthenticationToken oauthToken,
            HttpServletRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String cid = correlationId != null ? correlationId : UUID.randomUUID().toString();

        OAuth2AuthorizedClient client = authorizedClientRepository.loadAuthorizedClient(
            oauthToken.getAuthorizedClientRegistrationId(), oauthToken, request);

        if (client == null || client.getAccessToken() == null) {
            logger.error("No authorized client or access token found: correlationId={}", cid);
            throw new UserDataApiClientException("No access token available for OBO exchange", 0);
        }

        logger.info("TEMPORARY LOG - registrationId={}",
                oauthToken.getAuthorizedClientRegistrationId()); //todo remove this asap stb-4390

        logger.info("TEMPORARY LOG - accessTokenScopes={}",
                client.getAccessToken().getScopes()); //todo remove this asap stb-4390

        String userAccessToken = client.getAccessToken().getTokenValue();
        String userOid = oauthToken.getPrincipal().getAttribute("oid");

        logger.info("DEBUG: calling data API /me, correlationId={}", cid);
        Map<String, String> response = userDataApiClient.me(userAccessToken, userOid, cid);
        return ResponseEntity.ok(response);
    }
}
