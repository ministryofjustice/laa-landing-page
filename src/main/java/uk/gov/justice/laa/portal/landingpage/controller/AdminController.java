package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.RoleAssignmentAdminDto;
import uk.gov.justice.laa.portal.landingpage.service.AdminService;

/**
 * Controller for SiLAS Administration section
 * Requires "SiLAS Administration" role to access
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/silas-administration")
@PreAuthorize("@accessControlService.userHasAuthzRole(authentication, T(uk.gov.justice.laa.portal.landingpage.entity.AuthzRole).GLOBAL_ADMIN.roleName)")
public class AdminController {

    private final AdminService adminService;

    /**
     * Display SiLAS Administration landing page with Apps tab by default
     */
    @GetMapping("")
    public String showAdministration(
            @RequestParam(defaultValue = "apps") String tab,
            @RequestParam(required = false) String appFilter,
            Model model) {

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");
        model.addAttribute("activeTab", tab);

        // Load data based on active tab
        switch (tab) {
            case "roles":
                List<AppRoleAdminDto> roles;
                if (appFilter != null && !appFilter.isEmpty()) {
                    roles = adminService.getAppRolesByApp(appFilter);
                } else {
                    roles = adminService.getAllAppRoles();
                }
                model.addAttribute("roles", roles);
                model.addAttribute("appFilter", appFilter);

                // Get distinct app names for filter dropdown
                List<String> appNames = adminService.getAllApps().stream()
                        .map(AppAdminDto::getName)
                        .distinct()
                        .sorted()
                        .toList();
                model.addAttribute("appNames", appNames);
                break;

            case "assignments":
                List<RoleAssignmentAdminDto> assignments = adminService.getAllRoleAssignments();
                model.addAttribute("assignments", assignments);
                break;

            case "apps":
            default:
                List<AppAdminDto> apps = adminService.getAllApps();
                model.addAttribute("apps", apps);
                break;
        }

        return "silas-administration/administration";
    }
}
