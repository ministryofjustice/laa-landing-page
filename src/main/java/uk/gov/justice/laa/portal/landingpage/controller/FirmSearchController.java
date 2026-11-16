package uk.gov.justice.laa.portal.landingpage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.CreateUserApiService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class FirmSearchController {

    private final LoginService loginService;
    private final FirmService firmService;
    private final AccessControlService accessControlService;
    private final DataApiFeatureFlagService dataApiFeatureFlagService;
    private final CreateUserApiService createUserApiService;

    @GetMapping("/user/firms/search")
    @ResponseBody
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_EXTERNAL_USER,"
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_INTERNAL_USER)")
    public List<FirmDto> getFirms(Authentication authentication,
                                  @RequestParam(value = "q", defaultValue = "") String query) {
        // If the query is blank/whitespace-only, return an empty result and do not
        // call the service to avoid unnecessary work.
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);

        if (accessControlService.authenticatedUserIsInternal()) {
            return firmService.searchFirms(query);
        }
        return firmService.getUserAccessibleFirms(entraUser, query);
    }

    @GetMapping("/user/create/firm/search")
    @ResponseBody
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_EXTERNAL_USER,"
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_INTERNAL_USER)")
    public List<Map<String, String>> searchFirms(Authentication authentication,
                                                 @RequestParam(value = "q",
                                                             defaultValue = "") String query,
                                                 @RequestParam(value = "firmSearchResultCount", defaultValue = "10") Integer count) {
        // If the query is blank/whitespace-only, return an empty result and do not
        // call the service to avoid unnecessary work.
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<FirmDto> firms;
        int validatedCount = Math.max(10, Math.min(count, 100));

        if (dataApiFeatureFlagService.isCreateUserDataApiEnabled()) {
            firms = createUserApiService.searchFirms(query.trim(), validatedCount);
        } else if (accessControlService.authenticatedUserIsInternal()) {
            EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
            firms = firmService.searchFirms(query.trim());
        } else {
            EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
            firms = firmService.getUserAccessibleFirms(entraUser, query.trim());
        }

        List<Map<String, String>> result = firms.stream()
                .limit(validatedCount) // Limit results to prevent overwhelming the UI
                .map(firm -> {
                    Map<String, String> firmData = new HashMap<>();
                    firmData.put("id", firm.getId().toString());
                    firmData.put("name", firm.getName());
                    firmData.put("code", firm.getCode());
                    return firmData;
                })
                .collect(Collectors.toList());
        return result;
    }

    /**
     * Type-ahead calls are made by script, so data API failures are returned as a bare status
     * rather than a page. Raw API details are logged only.
     */
    @ExceptionHandler(UserDataApiClientException.class)
    public ResponseEntity<Void> handleDataApiException(UserDataApiClientException ex) {
        log.warn("Firm search data API call failed: status={}", ex.getStatusCode());
        HttpStatus status = switch (ex.getStatusCode()) {
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(status).build();
    }

    /**
     * The page script sends the user to the returned URL to sign in again.
     */
    @ExceptionHandler(ClientAuthorizationRequiredException.class)
    public ResponseEntity<Map<String, String>> handleClientAuthorizationRequired(ClientAuthorizationRequiredException ex) {
        log.warn("OAuth2 authorization required for client: {}", ex.getClientRegistrationId());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("redirectUrl", "/oauth2/authorization/" + ex.getClientRegistrationId()));
    }
}
