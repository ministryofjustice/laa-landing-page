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
import uk.gov.justice.laa.portal.landingpage.dto.AdminAppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.service.AppService;

/**
 * Controller for SiLAS Administration section
 * Requires "Global Admin" role to access
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("@accessControlService.userHasAuthzRole(authentication, T(uk.gov.justice.laa.portal.landingpage.entity.AuthzRole).SILAS_ADMINISTRATION.roleName)")
public class AdminController {

    private final AppService appService;

    /**
     * Display SiLAS Administration landing page with Admin Services tab by default
     */
    @GetMapping("/silas-administration")
    public String showAdministration(
            @RequestParam(defaultValue = "admin-apps") String tab,
            @RequestParam(required = false) String appFilter,
            Model model) {

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");
        model.addAttribute("activeTab", tab);

        // Load all admin apps data for admin-apps tab
        List<AppDto> adminApps = appService.getAllAdminApps();
        model.addAttribute("adminApps", adminApps);

        // Load all apps data for apps tab
        List<AppDto> apps = appService.getAllLaaApps();
        model.addAttribute("apps", apps);

        List<AppRoleDto> roles;
        if (appFilter != null && !appFilter.isEmpty()) {
            roles = appService.getAppRolesByApp(appFilter);
        } else {
            roles = appService.getAllAppRolesForAdmin();
        }
        model.addAttribute("roles", roles);
        model.addAttribute("appFilter", appFilter);

        // Get distinct app names for filter dropdown
        List<AppDto> allApps = appService.getAllAppsForAdmin();
        List<String> appNames = allApps.stream()
                .sorted()
                .map(AppDto::getName)
                .toList();
        model.addAttribute("appNames", appNames);

        return "silas-administration/administration";
    }
}
