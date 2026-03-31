package uk.gov.justice.laa.portal.landingpage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;

import java.util.Map;
import java.util.UUID;


/**
 * Controller for handling firm-directory requests
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/firmDirectory")
@PreAuthorize("@accessControlService.userHasAuthzRole(authentication, T(uk.gov.justice.laa.portal.landingpage.entity.AuthzRole).FIRM_DIRECTORY.roleName)")
public class FirmDirectoryController {

    private final FirmService firmService;
    private final OfficeService officeService;
    private final AccessControlService accessControlService;
    private final UserAccountStatusService userAccountStatusService;
    private final LoginService loginService;

    public static final String SEARCH_PAGE = "firm-directory/search-page";

    @Value("${feature.flag.firm.directory.enabled}")
    private boolean firmDirectoryEnabled;

    @Value("${feature.flag.bulk.disable.user}")
    public boolean bulkUserDisableFeatureEnabled;

    private boolean showDisableAllButton(String firmId) {
        return bulkUserDisableFeatureEnabled && accessControlService.authenticatedUserHasPermission(Permission.BULK_DISABLE_FIRM_USERS)
                && userAccountStatusService.hasActiveUserByFirmId(firmId);
    }

    @GetMapping()
    public String displayAllFirmDirectory(@ModelAttribute FirmDirectorySearchCriteria criteria,
                                          Model model) {
        if (!firmDirectoryEnabled) {
            return "redirect:/";
        }
        
        log.debug("FirmDirectoryController.displayAllFirmDirectory - {}", criteria);

        PaginatedFirmDirectory paginatedFirmDirectory = firmService.getFirmsPage(criteria.getFirmSearch(),
                criteria.getSelectedFirmType(),
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSort(),
                criteria.getDirection());

        // Build firm search form
        FirmSearchForm firmSearchForm = new FirmSearchForm(criteria.getFirmSearch(), null);
        // Add attributes to model
        buildDisplayAuditTableModel(criteria, model, paginatedFirmDirectory, firmSearchForm);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Firm Directory");

        return SEARCH_PAGE;
    }

    @GetMapping("/{id}")
    public String displayFirmDetails(
            @PathVariable UUID id,
            Model model,
            @ModelAttribute FirmOfficesCriteria criteria,
            Authentication authentication) {

        PaginatedOffices paginatedOffices = officeService.getOfficesPage(
                id,
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSort(),
                criteria.getDirection()
        );
        boolean showDisableAllButton = showDisableAllButton(String.valueOf(id));
        model.addAttribute("firm", firmService.getFirm(id));
        model.addAttribute("firmOffices", paginatedOffices);
        model.addAttribute("criteria", criteria);
        model.addAttribute("showDisableAllButton", showDisableAllButton);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Firm Details");

        return "firm-directory/firm-offices";
    }

    @GetMapping("/{id}/confirmation")
    @PreAuthorize("@accessControlService.canBulkDisableFirmUsers()")
    public String bulkDisableConfirmation(@PathVariable String id, Model model) {
        FirmDto firm = firmService.getFirm(id);
        Map<String, Long> counts = userAccountStatusService.getUserCountsForFirm(id);
        model.addAttribute("firm", firm);
        model.addAttribute("totalOfSingleFirm", counts.get("totalOfSingleFirm"));
        model.addAttribute("totalOfMultiFirm", counts.get("totalOfMultiFirm"));
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Remove access for all - " + firm.getName());
        return "firm-directory/bulk-confirmation";
    }

    @PostMapping("/{id}/confirmation")
    @PreAuthorize("@accessControlService.canBulkDisableFirmUsers()")
    public String confirmationBulkDisablePost(@PathVariable String id,
                                         Model model,
                                         RedirectAttributes redirectAttributes,
                                         Authentication authentication)  {
        try {
            UUID disabledByUserId = loginService.getCurrentEntraUser(authentication).getId();
            // Use cyber risk reason by default
            userAccountStatusService.disableUserAllUserByFirmIdWithCyberRisk(id, disabledByUserId);
            // Add success banner
            redirectAttributes.addFlashAttribute("successMessage", "All user accounts has been disabled");
            return "redirect:/admin/firmDirectory/" + id;
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
