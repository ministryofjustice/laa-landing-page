package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AdminAppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.AdminService;
import uk.gov.justice.laa.portal.landingpage.service.RoleCreationService;

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

    private final AdminService adminService;
    private final RoleCreationService roleCreationService;

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
        List<AdminAppDto> adminApps = adminService.getAllAdminApps();
        model.addAttribute("adminApps", adminApps);

        // Load all apps data for apps tab
        List<AppAdminDto> apps = adminService.getAllApps();
        model.addAttribute("apps", apps);

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

        return "silas-administration/administration";
    }

    /**
     * Display role creation form
     */
    @GetMapping("/roles/create")
    public String showRoleCreationForm(Model model, HttpSession session) {
        RoleCreationDto roleCreationDto = (RoleCreationDto) session.getAttribute("roleCreationDto");
        if (roleCreationDto == null) {
            roleCreationDto = new RoleCreationDto();
        }
        
        model.addAttribute("roleCreationDto", roleCreationDto);
        model.addAttribute("apps", adminService.getAllApps());
        model.addAttribute("userTypes", UserType.values());
        model.addAttribute("firmTypes", FirmType.values());
        
        return "silas-administration/create-role";
    }

    @PostMapping("/roles/create")
    public String processRoleCreation(@Valid @ModelAttribute RoleCreationDto roleCreationDto,
                                      BindingResult bindingResult,
                                      Model model,
                                      HttpSession session) {
        
        // Validate role name uniqueness within app
        if (roleCreationDto.getParentAppId() != null && roleCreationDto.getName() != null) {
            if (roleCreationService.isRoleNameExistsInApp(roleCreationDto.getName(), roleCreationDto.getParentAppId())) {
                bindingResult.rejectValue("name", "role.name.exists", 
                    "A role with this name already exists in the selected application");
            }
        }
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("apps", adminService.getAllApps());
            model.addAttribute("userTypes", UserType.values());
            model.addAttribute("firmTypes", FirmType.values());
            return "silas-administration/create-role";
        }

        roleCreationDto = roleCreationService.enrichRoleCreationDto(roleCreationDto);

        session.setAttribute("roleCreationDto", roleCreationDto);
        
        return "redirect:/admin/roles/create/check-your-answers";
    }

    @GetMapping("/roles/create/check-your-answers")
    public String showCheckYourAnswers(Model model, HttpSession session) {
        RoleCreationDto roleCreationDto = (RoleCreationDto) session.getAttribute("roleCreationDto");
        if (roleCreationDto == null) {
            return "redirect:/admin/roles/create";
        }
        
        model.addAttribute("roleCreationDto", roleCreationDto);
        return "silas-administration/create-role-check-answers";
    }

    @PostMapping("/roles/create/confirm")
    public String confirmRoleCreation(HttpSession session, RedirectAttributes redirectAttributes) {
        RoleCreationDto roleCreationDto = (RoleCreationDto) session.getAttribute("roleCreationDto");
        if (roleCreationDto == null) {
            return "redirect:/admin/roles/create";
        }
        
        try {
            roleCreationService.createRole(roleCreationDto);
            session.removeAttribute("roleCreationDto");
            redirectAttributes.addFlashAttribute("successMessage", 
                "Role '" + roleCreationDto.getName() + "' has been created successfully.");
        } catch (Exception e) {
            log.error("Error creating role: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to create role: " + e.getMessage());
        }
        
        return "redirect:/admin/silas-administration?tab=roles";
    }
}
