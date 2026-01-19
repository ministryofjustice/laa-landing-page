package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDirectoryDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDirectorySearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.AppRoleService;
import uk.gov.justice.laa.portal.landingpage.service.EmailValidationService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.RoleAssignmentService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for handling firm-directory requests
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/firmDirectory")
public class FirmDirectoryController {

    private final UserService userService;
    public static final String SEARCH_PAGE = "/firm-directory/search-page";

    @GetMapping()
/*    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_EXTERNAL_USER,"
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_INTERNAL_USER)")*/
    public String displayAllFirmDirectory(@ModelAttribute FirmDirectorySearchCriteria criteria,
                                          Model model) {
        log.debug("FirmDirectoryController.displayAllFirmDirectory - {}", criteria);
        // Get Firm Directory info
        PaginatedFirmDirectory paginatedFirmDirectory = userService.getFirmDirectories(criteria.getFirmSearch(),
                criteria.getSelectedFirmId(),
                criteria.getSelectedFirmType(),
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSort(),
                criteria.getDirection());

        // Build firm search form
        FirmSearchForm firmSearchForm = new FirmSearchForm(criteria.getFirmSearch(), criteria.getSelectedFirmId());
        // Add attributes to model

        buildDisplayAuditTableModel(criteria, model, paginatedFirmDirectory, firmSearchForm);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Firm Directory");

        return SEARCH_PAGE;
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
        // Get all firm type
        List<FirmType> firmTypes = List.of(
                FirmType.LEGAL_SERVICES_PROVIDER,
                FirmType.CHAMBERS,
                FirmType.ADVOCATE);
        model.addAttribute("firmTypes", firmTypes);
    }

}
