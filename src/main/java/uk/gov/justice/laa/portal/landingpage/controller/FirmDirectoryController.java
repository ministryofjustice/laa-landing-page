package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserTypeReasonDisable;
import uk.gov.justice.laa.portal.landingpage.forms.DisableUserReasonForm;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;
import uk.gov.justice.laa.portal.landingpage.viewmodel.DisableUserReasonViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getObjectFromHttpSession;

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

    private final FirmService firmService;
    private final OfficeService officeService;
    private final AccessControlService accessControlService;
    private final ModelMapper mapper;
    private final UserAccountStatusService userAccountStatusService;
    private final LoginService loginService;

    public static final String SEARCH_PAGE = "firm-directory/search-page";

    @Value("${feature.flag.firm.directory.enabled}")
    private boolean firmDirectoryEnabled;

    private boolean showDisableAllButton(Authentication authentication, String firmId) {
        return accessControlService.userHasAuthzRole(authentication, AuthzRole.SECURITY_RESPONSE.getRoleName())
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
        boolean showDisableAllButton = showDisableAllButton(authentication, String.valueOf(id));
        model.addAttribute("firm", firmService.getFirm(id));
        model.addAttribute("firmOffices", paginatedOffices);
        model.addAttribute("criteria", criteria);
        model.addAttribute("showDisableAllButton", showDisableAllButton);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Firm Details");

        return "firm-directory/firm-offices";
    }

    @GetMapping("/{id}/reasonForDisable")
    public String reasonForDisableGet(@PathVariable String id,
                                      DisableUserReasonForm disableUserReasonForm,
                                      Model model,
                                      HttpSession session,
                                      Authentication authentication) {
        Optional<Model> modelFromSession = getObjectFromHttpSession(session, "disableUserReasonModel", Model.class);
        modelFromSession.ifPresent(value -> model.addAttribute("reasonIdSelected",
                value.getAttribute("reasonIdSelected")));

        FirmDto firm =  firmService.getFirm(id);
        List<DisableUserReasonViewModel> reasons = new ArrayList<>(userAccountStatusService.getDisableUserReasons(UserTypeReasonDisable.BULK_DISABLE).stream()
                .map(reason -> mapper.map(reason, DisableUserReasonViewModel.class))
                .toList());
        model.addAttribute("firm", firm);
        model.addAttribute("reasons", reasons);
        model.addAttribute("disableUserReasonsForm", disableUserReasonForm);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Choose a reason to disable access for - " + firm.getName());
        session.setAttribute("disableUserReasonModel", model);
        return "firm-directory/bulk-disable-user-reason";
    }

    @PostMapping("/{id}/reasonForDisable")
    public String reasonForDisablePost(@PathVariable String id,
                                       @Valid @ModelAttribute("disableUserReasonsForm") DisableUserReasonForm disableUserReasonForm,
                                       BindingResult result,
                                       Authentication authentication,
                                       Model model,
                                       HttpSession session)  {
        Model modelFromSession = getObjectFromHttpSession(session, "disableUserReasonModel", Model.class).orElseThrow();
        model.addAttribute("firm", modelFromSession.getAttribute("firm"));
        model.addAttribute("disableUserReasonsForm", disableUserReasonForm);
        model.addAttribute("reasons", modelFromSession.getAttribute("reasons"));
        FirmDto firm = (FirmDto) modelFromSession.getAttribute("firm");
        if (result.hasErrors()) {
            log.info("Validation errors occurred while post reason for disable: {}", result.getAllErrors());
            return "firm-directory/bulk-disable-user-reason";
        }
        // add all the variables of confirmation
        modelFromSession.addAttribute("reasonIdSelected", disableUserReasonForm.getReasonId());
        Map<String, Long> counts = userAccountStatusService.getUserCountsForFirm(id);
        model.addAttribute("totalOfSingleFirm", counts.get("totalOfSingleFirm"));
        model.addAttribute("totalOfMultiFirm", counts.get("totalOfMultiFirm"));
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Remove access for all - " + firm.getName());

        return "firm-directory/bulk-confirmation";
    }

    @PostMapping("/{id}/confirmation")
    public String confirmationBulkDisablePost(@PathVariable String id,
                                         Model model,
                                         RedirectAttributes redirectAttributes,
                                         HttpSession session,
                                         Authentication authentication)  {
        Model modelFromSession = getObjectFromHttpSession(session, "disableUserReasonModel", Model.class).orElseThrow();

        UUID disabledByUserId = loginService.getCurrentEntraUser(authentication).getId();

        UUID disabledReasonId = UUID.fromString((String) modelFromSession.getAttribute("reasonIdSelected"));
        try {
            userAccountStatusService.disableUserAllUserByFirmId(id, disabledReasonId, disabledByUserId);
            // Add success banner
            redirectAttributes.addFlashAttribute("successMessage", "All user accounts has been disabled");
            session.removeAttribute("disableUserReasonModel");
            return "redirect:/admin/firmDirectory/" + id;
        } catch (Exception e) {
            log.error("Error during Bulk disable user {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @GetMapping("/cancel")
    public String cancelBulkUserDisable(HttpSession session) {
        session.removeAttribute("disableUserReasonModel");
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
