package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
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
            + "T(Permission).VIEW_INTERNAL_USER, "
            + "T(Permission).VIEW_EXTERNAL_USER)")
    public String displayAuditTable(
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "name") String sort,
            @RequestParam(name = "direction", required = false, defaultValue = "asc") String direction,
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "firmSearch", required = false) String firmSearch,
            @RequestParam(name = "selectedFirmId", required = false) String selectedFirmId,
            @RequestParam(name = "silasRole", required = false) String silasRole,
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

        // Get audit users
        PaginatedAuditUsers paginatedUsers = userService.getAuditUsers(
                search, firmId, silasRole, page, size, sort, direction);

        // Build firm search form
        FirmSearchForm firmSearchForm = new FirmSearchForm();
        firmSearchForm.setFirmSearch(firmSearch);
        firmSearchForm.setSelectedFirmId(firmId);

        // Get all SiLAS roles for dropdown filter
        final List<AppRoleDto> silasRoles = userService.getAllSilasRoles();

        // Add attributes to model
        model.addAttribute("users", paginatedUsers.getUsers());
        model.addAttribute("requestedPageSize", size);
        model.addAttribute("actualPageSize", paginatedUsers.getUsers().size());
        model.addAttribute("page", page);
        model.addAttribute("totalUsers", paginatedUsers.getTotalUsers());
        model.addAttribute("totalPages", paginatedUsers.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("firmSearch", firmSearchForm);
        model.addAttribute("silasRoles", silasRoles);
        model.addAttribute("selectedSilasRole", silasRole != null ? silasRole : "");
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);

        return "user-audit";
    }
}
