package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.AuditTableSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

/**
 * Controller for User Access Audit Table
 * Allows Global Admins and External User Admins to view all registered users
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AuditController {

    private final UserService userService;

    /**
     * Display the User Access Audit Table
     * Shows all registered users including those without firm profiles
     */
    @GetMapping("/users/audit")
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions("
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_AUDIT_TABLE)")
    public String displayAuditTable(
            @ModelAttribute AuditTableSearchCriteria criteria,
            Model model) {

        log.debug("AuditController.displayAuditTable - {}", criteria);
        // Get audit users
        PaginatedAuditUsers paginatedUsers = userService.getAuditUsers(
                criteria.getSearch(), criteria.getSelectedFirmId(), criteria.getSilasRole(),
                criteria.getSelectedAppId(), criteria.getSelectedUserType(), criteria.getMultiFirm(), criteria.getPage(), criteria.getSize(), criteria.getSort(), criteria.getDirection());
        // Build firm search form
        FirmSearchForm firmSearchForm = new FirmSearchForm(criteria.getFirmSearch(), criteria.getSelectedFirmId());
        // Add attributes to model
        buildDisplayAuditTableModel(criteria, model, paginatedUsers, firmSearchForm);

        return "user-audit/users";
    }

    private void buildDisplayAuditTableModel(AuditTableSearchCriteria criteria, Model model, PaginatedAuditUsers paginatedUsers, FirmSearchForm firmSearchForm) {
        model.addAttribute("users", paginatedUsers.getUsers());
        model.addAttribute("requestedPageSize", criteria.getSize());
        model.addAttribute("actualPageSize", paginatedUsers.getUsers().size());
        model.addAttribute("page", criteria.getPage());
        model.addAttribute("totalUsers", paginatedUsers.getTotalUsers());
        model.addAttribute("totalPages", paginatedUsers.getTotalPages());
        model.addAttribute("search", criteria.getSearch());
        model.addAttribute("firmSearch", firmSearchForm);
        // Get all SiLAS roles for dropdown filter
        List<AppRoleDto> silasRoles = userService.getAllSilasRoles();
        model.addAttribute("silasRoles", silasRoles);
        List<AppDto> apps = userService.getApps();
        model.addAttribute("apps", apps);
        model.addAttribute("selectedSilasRole", criteria.getSilasRole() != null ? criteria.getSilasRole() : "");
        model.addAttribute("selectedAppId", criteria.getSelectedAppId() != null ? criteria.getSelectedAppId().toString() : "");
        model.addAttribute("selectedUserType", criteria.getSelectedUserType() != null ? criteria.getSelectedUserType().toString() : "");
        model.addAttribute("multiFirm", criteria.getMultiFirm() != null ? criteria.getMultiFirm().toString() : "");
        model.addAttribute("sort", criteria.getSort());
        model.addAttribute("direction", criteria.getDirection());
    }

    /**
     * Display detailed audit information for a specific user
     * Shows complete identity and profile information including audit data
     */
    @GetMapping("/users/audit/{id}")
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions("
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_AUDIT_TABLE)")
    public String displayUserAuditDetail(
            @PathVariable("id") UUID userId,
            @RequestParam(name = "profilePage", defaultValue = "1") int profilePage,
            @RequestParam(name = "profileSize", defaultValue = "3") int profileSize,
            Model model) {

        log.debug("AuditController.displayUserAuditDetail - userId: '{}', profilePage: {}, profileSize: {}",
                userId, profilePage, profileSize);

        // Get detailed user audit data with pagination for profiles
        AuditUserDetailDto userDetail = userService.getAuditUserDetail(userId, profilePage, profileSize);

        // Add attributes to model
        model.addAttribute("user", userDetail);
        model.addAttribute("profileId", userId); // Add profile ID for pagination links
        model.addAttribute("profilePage", profilePage);
        model.addAttribute("profileSize", profileSize);

        return "user-audit/details";
    }
}
