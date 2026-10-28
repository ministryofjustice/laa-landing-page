package uk.gov.justice.laa.portal.landingpage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;
import uk.gov.justice.laa.portal.landingpage.service.ExternalUserCreationApiService;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
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
    private final ExternalUserCreationApiService externalUserCreationApiService;

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
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_INTERNAL_USER)"
            + " and (!@dataApiFeatureFlagService.isCreateUserDataApiEnabled() or "
            + "@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER))")
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
        boolean apiEnabled = dataApiFeatureFlagService.isCreateUserDataApiEnabled();
        if (apiEnabled) {
            firms = externalUserCreationApiService.searchFirms(query.trim(), validatedCount);
        } else {
            EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
            if (accessControlService.authenticatedUserIsInternal()) {
                firms = firmService.searchFirms(query.trim());
            } else {
                firms = firmService.getUserAccessibleFirms(entraUser, query.trim());
            }
        }

        List<Map<String, String>> result = firms.stream()
                .limit(validatedCount) // Limit results to prevent overwhelming the UI
                .map(firm -> {
                    Map<String, String> firmData = new HashMap<>();
                    firmData.put("id", apiEnabled ? firm.getCode() : firm.getId().toString());
                    firmData.put("name", firm.getName());
                    firmData.put("code", firm.getCode());
                    return firmData;
                })
                .collect(Collectors.toList());
        return result;
    }

    @GetMapping("/user/create/firm/error")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
    public ModelAndView searchError(@RequestParam(defaultValue = "502") int status) {
        return CreateUserApiExceptionHandler.errorPage(status);
    }
}
