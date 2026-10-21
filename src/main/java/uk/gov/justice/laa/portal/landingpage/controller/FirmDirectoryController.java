package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDirectorySearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmOfficesCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.firmdirectory.FirmDirectoryProvider;
import uk.gov.justice.laa.portal.landingpage.service.firmdirectory.FirmDirectoryProviderResolver;

import java.util.List;
import java.util.Map;


/**
 * Controller for handling firm-directory requests
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/firmDirectory")
@PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions("
        + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_FIRM_DIRECTORY)")
public class FirmDirectoryController {

    private final AccessControlService accessControlService;
    private final LoginService loginService;
    private final FirmDirectoryProviderResolver firmDirectoryProviderResolver;

    public static final String SEARCH_PAGE = "firm-directory/search-page";

    @Value("${feature.flag.firm.directory.enabled}")
    private boolean firmDirectoryEnabled;

    @Value("${feature.flag.bulk.disable.user}")
    public boolean bulkUserDisableFeatureEnabled;

    private boolean showDisableAllButton(FirmDirectoryProvider provider, String firmId) {
        return bulkUserDisableFeatureEnabled && accessControlService.authenticatedUserHasPermission(Permission.BULK_DISABLE_FIRM_USERS)
                && provider.hasActiveUsers(firmId);
    }

    @GetMapping()
    public String displayAllFirmDirectory(@ModelAttribute FirmDirectorySearchCriteria criteria,
                                          Model model) {
        if (!firmDirectoryEnabled) {
            return "redirect:/";
        }
        
        log.debug("FirmDirectoryController.displayAllFirmDirectory - {}", criteria);

        FirmDirectoryProvider provider = firmDirectoryProviderResolver.resolve();

        PaginatedFirmDirectory paginatedFirmDirectory = provider.getFirmsPage(criteria.getFirmSearch(),
                criteria.getSelectedFirmType(), criteria.getPage(), criteria.getSize(), criteria.getSort(),
                criteria.getDirection());

        // Build firm search form
        FirmSearchForm firmSearchForm = new FirmSearchForm(criteria.getFirmSearch(), null);
        // Add attributes to model
        buildDisplayAuditTableModel(criteria, model, paginatedFirmDirectory, firmSearchForm);
        model.addAttribute("page", paginatedFirmDirectory.getCurrentPage());
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Firm Directory");

        return SEARCH_PAGE;
    }

    @GetMapping("/firms/search")
    @ResponseBody
    public ResponseEntity<List<FirmDto>> searchFirms(@RequestParam(value = "q", defaultValue = "") String query) {
        if (query.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        try {
            return ResponseEntity.ok(firmDirectoryProviderResolver.resolve().searchFirms(query.trim()));
        } catch (ClientAuthorizationRequiredException e) {
            // fetch() can't follow an OAuth redirect, so signal the page to re-authenticate itself
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/{id}")
    public String displayFirmDetails(
            @PathVariable String id,
            Model model,
            @ModelAttribute FirmOfficesCriteria criteria,
            Authentication authentication) {

        FirmDirectoryProvider provider = firmDirectoryProviderResolver.resolve();
        FirmDto firm = provider.getFirm(id);
        PaginatedOffices paginatedOffices = provider.getOfficesPage(id, criteria.getPage(), criteria.getSize(),
                criteria.getSort(), criteria.getDirection());
        model.addAttribute("firm", firm);
        model.addAttribute("firmRef", id);
        model.addAttribute("userSearchFirmId", provider.findLocalFirmId(id).orElse(null));
        model.addAttribute("firmOffices", paginatedOffices);
        model.addAttribute("criteria", criteria);
        model.addAttribute("showDisableAllButton", showDisableAllButton(provider, id));
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Firm Details");

        return "firm-directory/firm-offices";
    }

    @GetMapping("/{id}/confirmation")
    @PreAuthorize("@accessControlService.canBulkDisableFirmUsers()")
    public String bulkDisableConfirmation(@PathVariable String id, Model model) {
        FirmDirectoryProvider provider = firmDirectoryProviderResolver.resolve();
        FirmDto firm = provider.getFirm(id);
        Map<String, Long> counts = provider.getUserCounts(id);
        model.addAttribute("firm", firm);
        model.addAttribute("firmRef", id);
        model.addAttribute("userSearchFirmId", provider.findLocalFirmId(id).orElse(null));
        model.addAttribute("totalOfSingleFirm", counts.getOrDefault("totalOfSingleFirm", 0L));
        model.addAttribute("totalOfMultiFirm", counts.getOrDefault("totalOfMultiFirm", 0L));
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Remove access for all - " + firm.getName());
        return "firm-directory/bulk-confirmation";
    }

    @PostMapping("/{id}/confirmation")
    @PreAuthorize("@accessControlService.canBulkDisableFirmUsers()")
    public String confirmationBulkDisablePost(@PathVariable String id,
                                         Model model,
                                         RedirectAttributes redirectAttributes,
                                         Authentication authentication)  {
        FirmDirectoryProvider provider = firmDirectoryProviderResolver.resolve();
        // Use the validated id rather than the raw path variable to avoid an open redirect
        final String firmId = provider.validateFirmId(id);
        final String redirectUrl = "redirect:/admin/firmDirectory/" + firmId;
        try {
            // Use cyber risk reason by default
            provider.bulkDeactivateFirmUsers(firmId,
                    () -> loginService.getCurrentEntraUser(authentication).getId());
            // Add success banner
            redirectAttributes.addFlashAttribute("successMessage", "All user accounts has been disabled");
            return redirectUrl;
        } catch (UserDataApiClientException e) {
            if (e.getStatusCode() == 400) {
                log.warn("Bulk disable rejected by data API for firm {}: {}", firmId, e.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage",
                        "The request to remove access for all users was not valid. Check the details and try again.");
                return redirectUrl;
            }
            throw e;
        } catch (ClientAuthorizationRequiredException e) {
            // Let the global handler redirect the user to re-authenticate
            throw e;
        } catch (Exception e) {
            log.error("Error during Bulk disable user {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @GetMapping("/cancel")
    @PreAuthorize("@accessControlService.canBulkDisableFirmUsers()")
    public String cancelBulkUserDisable() {
        return "redirect:/admin/firmDirectory";
    }

    /**
     * Maps data API failures to user-facing pages. Raw API details are logged only, never shown.
     * 401 is handled upstream by re-authentication (see FirmDirectoryApiService).
     */
    @ExceptionHandler(UserDataApiClientException.class)
    public Object handleDataApiException(UserDataApiClientException ex, HttpServletRequest request) {
        int status = ex.getStatusCode();
        log.warn("Data API call failed for {} {}: status={}", request.getMethod(), request.getRequestURI(), status);

        if (status == 403) {
            return new RedirectView("/not-authorised");
        }
        if (status == 404) {
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                return errorPage(HttpStatus.NOT_FOUND, "Firm not found",
                        "The firm could not be found. Go back to the firm directory and try again.");
            }
            ModelAndView notFound = new ModelAndView("errors/error-404");
            notFound.setStatus(HttpStatus.NOT_FOUND);
            return notFound;
        }
        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR, "Sorry, there is a problem with the service",
                "Unable to complete your request. Please try again later.");
    }

    private ModelAndView errorPage(HttpStatus status, String title, String message) {
        ModelAndView mav = new ModelAndView("errors/error-generic");
        mav.setStatus(status);
        mav.addObject("errorTitle", title);
        mav.addObject("errorMessage", message);
        return mav;
    }

    private void buildDisplayAuditTableModel(FirmDirectorySearchCriteria criteria, Model model,
                                             PaginatedFirmDirectory paginatedFirmDirectory, FirmSearchForm firmSearchForm) {
        model.addAttribute("firmDirectories", paginatedFirmDirectory.getFirmDirectories());
        model.addAttribute("requestedPageSize", criteria.getSize());
        model.addAttribute("actualPageSize", paginatedFirmDirectory.getFirmDirectories().size());
        model.addAttribute("page", criteria.getPage());
        model.addAttribute("totalRows", paginatedFirmDirectory.getTotalElements());
        model.addAttribute("totalPages", paginatedFirmDirectory.getTotalPages());
        model.addAttribute("search", criteria.getSearch());
        model.addAttribute("firmSearch", firmSearchForm);
        model.addAttribute("sort", criteria.getSort());
        model.addAttribute("direction", criteria.getDirection());
        model.addAttribute("selectedFirmType",
                criteria.getSelectedFirmType() != null ? criteria.getSelectedFirmType() : "");
        model.addAttribute("firmTypes", FirmType.values());
    }

}
