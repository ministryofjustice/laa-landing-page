package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
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
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "name") String sort,
            @RequestParam(name = "direction", required = false, defaultValue = "asc") String direction,
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "firmSearch", required = false) String firmSearch,
            @RequestParam(name = "selectedFirmId", required = false) String selectedFirmId,
            @RequestParam(name = "silasRole", required = false) String silasRole,
            @RequestParam(name = "selectedAppId", required = false) String selectedAppId,
            Model model) {

        log.debug("AuditController.displayAuditTable - search: '{}', firmSearch: '{}', silasRole: '{}'",
                search, firmSearch, silasRole);

        // Parse firm ID if provided
        UUID firmId = null;
        if (selectedFirmId != null && !selectedFirmId.isBlank()) {
            try {
                firmId = UUID.fromString(selectedFirmId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid firm ID format: {}", selectedFirmId);
            }
        }

        // Parse app ID if provided
        UUID appId = null;
        if (selectedAppId != null && !selectedAppId.isBlank()) {
            try {
                appId = UUID.fromString(selectedAppId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid app ID format: {}", selectedAppId);
            }
        }

        // Get audit users
        PaginatedAuditUsers paginatedUsers = userService.getAuditUsers(
                search, firmId, silasRole, appId, page, size, sort, direction);

        // Build firm search form
        FirmSearchForm firmSearchForm = new FirmSearchForm();
        firmSearchForm.setFirmSearch(firmSearch);
        firmSearchForm.setSelectedFirmId(firmId);

        // Add attributes to model
        model.addAttribute("users", paginatedUsers.getUsers());
        model.addAttribute("requestedPageSize", size);
        model.addAttribute("actualPageSize", paginatedUsers.getUsers().size());
        model.addAttribute("page", page);
        model.addAttribute("totalUsers", paginatedUsers.getTotalUsers());
        model.addAttribute("totalPages", paginatedUsers.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("firmSearch", firmSearchForm);
        // Get all SiLAS roles for dropdown filter
        List<AppRoleDto> silasRoles = userService.getAllSilasRoles();
        model.addAttribute("silasRoles", silasRoles);
        List<AppDto> apps = userService.getApps();
        model.addAttribute("apps", apps);
        model.addAttribute("selectedSilasRole", silasRole != null ? silasRole : "");
        model.addAttribute("selectedAppId", selectedAppId != null ? selectedAppId : "");
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);

        return "user-audit/users";
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
            Model model) {

        log.debug("AuditController.displayUserAuditDetail - userId: '{}'", userId);

        // Get detailed user audit data
        AuditUserDetailDto userDetail = userService.getAuditUserDetail(userId);

        // Add attributes to model
        model.addAttribute("user", userDetail);

        return "user-audit/details";
    }
}
